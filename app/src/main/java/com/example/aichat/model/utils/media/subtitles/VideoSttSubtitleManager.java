package com.example.aichat.model.utils.media.subtitles;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.Manifest;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import java.io.InputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@SuppressWarnings({"SpellCheckingInspection", "RegExpRedundantEscape", "SameParameterValue"})
public class VideoSttSubtitleManager {
    public interface Listener {
        void onPartialText(@NonNull String text);
        void onFinalText(@NonNull String text);
        void onInfo(@NonNull String message);
        void onError(@NonNull String message);
    }
    private static final String TAG = "SherpaSubtitleManager";
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int CHUNK_MS = 40;
    private static final int CHUNK_SAMPLES = SAMPLE_RATE * CHUNK_MS / 1000;
    private static final int PARTIAL_EMIT_MIN_INTERVAL_MS = 35;
    private static final String LANGUAGE_RU = "ru";
    private static final String LANGUAGE_EN = "en";
    private static final String LANGUAGE_ZH = "zh";
    private static final String DEFAULT_RU_MODEL_DIR =
            "sherpa-onnx-streaming-zipformer-small-ru-vosk-int8-2025-08-16";
    private static final String[] RU_MODEL_DIR_CANDIDATES = {"asr/ru", DEFAULT_RU_MODEL_DIR, ""};
    private static final String[] EN_MODEL_DIR_CANDIDATES = {"asr/en", "sherpa-onnx-streaming-zipformer-en", "sherpa-onnx-streaming-zipformer-en-2023-06-26", "sherpa-onnx-streaming-zipformer-en-2023-06-21"};
    private static final String[] ZH_MODEL_DIR_CANDIDATES = {"asr/zh", "asr/cn", "sherpa-onnx-streaming-zipformer-zh", "sherpa-onnx-streaming-zipformer-zh-14M-2023-02-23"};
    private static final String[] ENCODER_FILE_CANDIDATES = {"encoder.int8.onnx", "encoder.onnx", "encoder-epoch-99-avg-1.int8.onnx", "encoder-epoch-99-avg-1.onnx", "encoder-epoch-30-avg-9.int8.onnx", "encoder-epoch-30-avg-9.onnx", "encoder-epoch-20-avg-1.int8.onnx", "encoder-epoch-20-avg-1.onnx"};
    private static final String[] DECODER_FILE_CANDIDATES = {"decoder.onnx", "decoder.int8.onnx", "decoder-epoch-99-avg-1.int8.onnx", "decoder-epoch-99-avg-1.onnx", "decoder-epoch-30-avg-9.int8.onnx", "decoder-epoch-30-avg-9.onnx", "decoder-epoch-20-avg-1.int8.onnx", "decoder-epoch-20-avg-1.onnx"};
    private static final String[] JOINER_FILE_CANDIDATES = {"joiner.int8.onnx", "joiner.onnx", "joiner-epoch-99-avg-1.int8.onnx", "joiner-epoch-99-avg-1.onnx", "joiner-epoch-30-avg-9.int8.onnx", "joiner-epoch-30-avg-9.onnx", "joiner-epoch-20-avg-1.int8.onnx", "joiner-epoch-20-avg-1.onnx"};
    private static final String[] TOKENS_FILE_CANDIDATES = {"tokens.txt"};
    private static final String[] BPE_FILE_CANDIDATES = {"bpe.model"};
    private static final String[] RU_MODEL_TYPE_CANDIDATES = {"zipformer2"};
    private static final String[] EN_ZH_MODEL_TYPE_CANDIDATES = {"zipformer"};
    private static final String PUNCT_MODEL_DIR =
            "sherpa-onnx-punct-ct-transformer-zh-en-vocab272727-2024-04-12-int8";
    private static final String PUNCT_MODEL_FILE = "model.int8.onnx";
    private static final String[] PUNCT_MODEL_CANDIDATES = {PUNCT_MODEL_DIR + "/" + PUNCT_MODEL_FILE, PUNCT_MODEL_FILE};
    private final Context context;
    private final Listener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Locale recognitionLocale = Locale.getDefault();
    private String activeAsrLanguage = "";
    private String requestedAsrLanguage = "";
    private String activeModelDirectory = "";
    private String encoderAssetPath;
    private String decoderAssetPath;
    private String joinerAssetPath;
    private String tokensAssetPath;
    private String bpeAssetPath;
    private volatile boolean keepRunning;
    private volatile boolean destroyed;
    private volatile boolean workerActive;
    private Thread workerThread;
    private AudioRecord audioRecord;
    private Object recognizer;
    private Object stream;
    private Method createStreamMethod;
    private Method acceptWaveformMethod;
    private Method inputFinishedMethod;
    private Method isReadyMethod;
    private Method decodeMethod;
    private Method getResultMethod;
    private Method isEndpointMethod;
    private Method resetMethod;
    private Method releaseRecognizerMethod;
    private Method releaseStreamMethod;
    private Method getTextMethod;
    private Object offlinePunctuation;
    private Method addPunctuationMethod;
    private Method releasePunctuationMethod;
    private boolean punctuationTriedToInit;
    private boolean punctuationAvailable;
    private String punctuationModelPath;
    private String lastPartialText = "";
    private String lastFinalText = "";
    private long lastPartialEmitTime = 0L;
    private final Map<String, String> commonCorrections = new LinkedHashMap<>();
    public VideoSttSubtitleManager(@NonNull Context context, @NonNull Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        initCommonCorrections();
    }

    public static boolean isAnyModelAvailable(@NonNull Context context) {
        return isModelAvailableForLanguage(context, LANGUAGE_RU)
                || isModelAvailableForLanguage(context, LANGUAGE_EN)
                || isModelAvailableForLanguage(context, LANGUAGE_ZH);
    }

    public static boolean isModelAvailableForLanguage(
            @NonNull Context context,
            @Nullable String languageCode
    ) {
        if (!isSherpaRuntimePresent()) {
            return false;
        }

        String language = normalizeLanguageCodeForAvailability(languageCode);
        String[] dirs = modelDirectoriesForAvailability(language);

        for (String dir : dirs) {
            String safeDir = dir == null ? "" : dir;

            if (hasAnyAsset(context, safeDir, ENCODER_FILE_CANDIDATES)
                    && hasAnyAsset(context, safeDir, DECODER_FILE_CANDIDATES)
                    && hasAnyAsset(context, safeDir, JOINER_FILE_CANDIDATES)
                    && hasAnyAsset(context, safeDir, TOKENS_FILE_CANDIDATES)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isSherpaRuntimePresent() {
        try {
            Class.forName("com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig");
            Class.forName("com.k2fsa.sherpa.onnx.OnlineModelConfig");
            Class.forName("com.k2fsa.sherpa.onnx.OnlineRecognizerConfig");
            Class.forName("com.k2fsa.sherpa.onnx.OnlineRecognizer");
            Class.forName("com.k2fsa.sherpa.onnx.OnlineStream");
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @NonNull
    private static String normalizeLanguageCodeForAvailability(@Nullable String languageCode) {
        if (languageCode == null) {
            return LANGUAGE_RU;
        }

        String normalized = languageCode.trim().toLowerCase(Locale.US);

        if (normalized.startsWith(LANGUAGE_EN)) {
            return LANGUAGE_EN;
        }

        if (normalized.startsWith("zh") || normalized.startsWith("cn")) {
            return LANGUAGE_ZH;
        }

        return LANGUAGE_RU;
    }

    @NonNull
    private static String[] modelDirectoriesForAvailability(@NonNull String language) {
        if (LANGUAGE_EN.equals(language)) {
            return EN_MODEL_DIR_CANDIDATES;
        }

        if (LANGUAGE_ZH.equals(language)) {
            return ZH_MODEL_DIR_CANDIDATES;
        }

        return RU_MODEL_DIR_CANDIDATES;
    }

    private static boolean hasAnyAsset(
            @NonNull Context context,
            @NonNull String directory,
            @NonNull String[] fileNames
    ) {
        for (String fileName : fileNames) {
            if (staticAssetExists(context, joinAssetPathStatic(directory, fileName))) {
                return true;
            }
        }

        return false;
    }

    private static String joinAssetPathStatic(@Nullable String dir, @NonNull String fileName) {
        if (dir == null || dir.trim().isEmpty()) {
            return fileName;
        }

        return dir + "/" + fileName;
    }

    private static boolean staticAssetExists(@NonNull Context context, @NonNull String assetPath) {
        try (InputStream ignored = context.getApplicationContext().getAssets().open(assetPath)) {
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    public void start(@Nullable Locale locale) {
        Locale nextLocale = locale != null ? locale : Locale.getDefault();
        mainHandler.post(() -> startInternal(nextLocale));
    }
    public void startByLanguageCode(@Nullable String languageCode) {
        Locale locale = localeFromLanguageCode(languageCode);
        start(locale);
    }
    public void stop() {
        mainHandler.post(this::stopInternal);
    }
    public void release() {
        mainHandler.post(() -> {
            destroyed = true;
            stopInternal();
            releaseSherpaObjects();
            releasePunctuationObjects();
        });
    }
    private void startInternal(@NonNull Locale locale) {
        if (destroyed) {
            destroyed = false;
        }
        recognitionLocale = locale;
        String nextLanguage = normalizeLanguageForAsr(locale);
        requestedAsrLanguage = nextLanguage;

        if (!isModelAvailableForLanguage(context, nextLanguage)) {
            stopInternal();
            emitError("Автосубтитры недоступны: модели Sherpa-ONNX не установлены.");
            return;
        }

        boolean languageChanged = !nextLanguage.equals(activeAsrLanguage);
        if (languageChanged && workerActive) {
            stopInternal();
            mainHandler.postDelayed(() -> startInternal(locale), 180L);
            return;
        }
        if (languageChanged) {
            releaseSherpaObjects();
            releasePunctuationObjects();
            resetResolvedModelAssets();
            activeAsrLanguage = nextLanguage;
            punctuationTriedToInit = false;
            punctuationAvailable = false;
            punctuationModelPath = null;
            Log.i(TAG, "ASR language switched to: " + activeAsrLanguage);
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            emitError("Нет разрешения на микрофон для Sherpa-ONNX субтитров");
            return;
        }
        if (keepRunning || workerActive) {
            return;
        }
        keepRunning = true;
        lastPartialText = "";
        lastFinalText = "";
        lastPartialEmitTime = 0L;
        workerThread = new Thread(this::runRecognitionLoop, "SherpaOnnxSubtitleThread");
        workerThread.start();
    }
    private void stopInternal() {
        keepRunning = false;
        Thread thread = workerThread;
        workerThread = null;
        stopAudioRecord();
        if (thread != null && thread != Thread.currentThread()) {
            thread.interrupt();
        }
    }
    private void runRecognitionLoop() {
        workerActive = true;
        try {
            ensureSherpaReady();
            tryInitEnglishChinesePunctuationIfAvailable();
            createNewStream();
            startAudioRecord();
            Log.i(TAG, "Sherpa subtitles started. language="
                    + activeAsrLanguage
                    + ", modelDir="
                    + activeModelDirectory);
            short[] pcm16 = new short[CHUNK_SAMPLES];
            float[] samples = new float[CHUNK_SAMPLES];
            while (keepRunning && !destroyed && !Thread.currentThread().isInterrupted()) {
                int read = audioRecord != null ? audioRecord.read(pcm16, 0, pcm16.length) : 0;
                if (read <= 0) {
                    continue;
                }
                for (int i = 0; i < read; i++) {
                    samples[i] = pcm16[i] / 32768.0f;
                }
                float[] chunk;
                if (read == samples.length) {
                    chunk = samples;
                } else {
                    chunk = new float[read];
                    System.arraycopy(samples, 0, chunk, 0, read);
                }
                acceptWaveformMethod.invoke(stream, chunk, SAMPLE_RATE);
                while (keepRunning && invokeBoolean(isReadyMethod, recognizer, stream)) {
                    decodeMethod.invoke(recognizer, stream);
                }
                emitPartialIfChanged(readCurrentText());
                boolean endpoint = invokeBoolean(isEndpointMethod, recognizer, stream);
                if (endpoint) {
                    String finalText = normalizeFinal(readCurrentText());
                    if (!finalText.isEmpty() && !finalText.equals(lastFinalText)) {
                        lastFinalText = finalText;
                        emitFinal(finalText);
                    }
                    resetMethod.invoke(recognizer, stream);
                    lastPartialText = "";
                }
            }
        } catch (Throwable throwable) {
            Log.e(TAG, "Sherpa-ONNX recognition loop error", throwable);
            emitError(buildUserFriendlyError(throwable));
        } finally {
            stopAudioRecord();
            releaseStreamOnly();
            workerActive = false;
        }
    }
    private void ensureSherpaReady() throws Exception {
        if (recognizer != null) {
            return;
        }
        resolveModelAssetsForLanguage(activeAsrLanguage);
        Class<?> transducerConfigClass =
                Class.forName("com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig");
        Class<?> modelConfigClass =
                Class.forName("com.k2fsa.sherpa.onnx.OnlineModelConfig");
        Class<?> recognizerConfigClass =
                Class.forName("com.k2fsa.sherpa.onnx.OnlineRecognizerConfig");
        Class<?> recognizerClass =
                Class.forName("com.k2fsa.sherpa.onnx.OnlineRecognizer");
        Throwable lastError = null;
        String[] modelTypeCandidates = getModelTypeCandidatesForCurrentLanguage();
        for (String modelType : modelTypeCandidates) {
            try {
                buildRecognizer(
                        transducerConfigClass,
                        modelConfigClass,
                        recognizerConfigClass,
                        recognizerClass,
                        modelType
                );
                Log.i(TAG, "Sherpa recognizer created. language="
                        + activeAsrLanguage
                        + ", modelType="
                        + modelType);
                return;
            } catch (Throwable throwable) {
                lastError = throwable;
                releaseSherpaObjects();
                Log.w(TAG, "Sherpa recognizer failed with modelType="
                        + modelType
                        + ". Trying next model type if available.", throwable);
            }
        }
        if (lastError instanceof Exception) {
            throw (Exception) lastError;
        }
        throw new RuntimeException(lastError);
    }
    @NonNull
    private String[] getModelTypeCandidatesForCurrentLanguage() {
        if (isEnglishAsrLanguage() || isChineseAsrLanguage()) {
            return EN_ZH_MODEL_TYPE_CANDIDATES;
        }
        return RU_MODEL_TYPE_CANDIDATES;
    }
    private void buildRecognizer(@NonNull Class<?> transducerConfigClass,
                                 @NonNull Class<?> modelConfigClass,
                                 @NonNull Class<?> recognizerConfigClass,
                                 @NonNull Class<?> recognizerClass,
                                 @NonNull String modelType) throws Exception {
        Object transducerConfig = newInstance(transducerConfigClass);
        callSetter(transducerConfig, "setEncoder", String.class, encoderAssetPath);
        callSetter(transducerConfig, "setDecoder", String.class, decoderAssetPath);
        callSetter(transducerConfig, "setJoiner", String.class, joinerAssetPath);
        Object modelConfig = newInstance(modelConfigClass);
        callSetter(modelConfig, "setTransducer", transducerConfigClass, transducerConfig);
        callSetter(modelConfig, "setTokens", String.class, tokensAssetPath);
        callSetter(
                modelConfig,
                "setNumThreads",
                int.class,
                Math.max(2, Runtime.getRuntime().availableProcessors() / 2)
        );
        callSetter(modelConfig, "setProvider", String.class, "cpu");
        callSetter(modelConfig, "setModelType", String.class, modelType);
        Object recognizerConfig = newInstance(recognizerConfigClass);
        callSetter(recognizerConfig, "setModelConfig", modelConfigClass, modelConfig);
        callSetter(recognizerConfig, "setEnableEndpoint", boolean.class, true);
        callSetter(recognizerConfig, "setDecodingMethod", String.class, "greedy_search");
        Constructor<?> constructor =
                recognizerClass.getConstructor(AssetManager.class, recognizerConfigClass);
        recognizer = constructor.newInstance(context.getAssets(), recognizerConfig);
        Class<?> streamClass = Class.forName("com.k2fsa.sherpa.onnx.OnlineStream");
        createStreamMethod = recognizerClass.getMethod("createStream", String.class);
        isReadyMethod = recognizerClass.getMethod("isReady", streamClass);
        decodeMethod = recognizerClass.getMethod("decode", streamClass);
        getResultMethod = recognizerClass.getMethod("getResult", streamClass);
        isEndpointMethod = recognizerClass.getMethod("isEndpoint", streamClass);
        resetMethod = recognizerClass.getMethod("reset", streamClass);
        releaseRecognizerMethod = recognizerClass.getMethod("release");
    }
    private void tryInitEnglishChinesePunctuationIfAvailable() {
        if (punctuationTriedToInit) {
            return;
        }
        punctuationTriedToInit = true;
        if (!isEnglishOrChineseAsrLanguage()) {
            punctuationAvailable = false;
            Log.i(TAG, "Sherpa punctuation skipped: Java postprocessor is used for language "
                    + activeAsrLanguage);
            return;
        }
        punctuationModelPath = findOptionalAsset(PUNCT_MODEL_CANDIDATES);
        if (punctuationModelPath == null) {
            punctuationAvailable = false;
            Log.i(TAG, "Sherpa EN/ZH punctuation model was not found. Checked: "
                    + readableCandidates(PUNCT_MODEL_CANDIDATES));
            return;
        }
        try {
            Class<?> modelConfigClass =
                    Class.forName("com.k2fsa.sherpa.onnx.OfflinePunctuationModelConfig");
            Class<?> punctuationConfigClass =
                    Class.forName("com.k2fsa.sherpa.onnx.OfflinePunctuationConfig");
            Class<?> punctuationClass =
                    Class.forName("com.k2fsa.sherpa.onnx.OfflinePunctuation");
            Object modelConfig = createPunctuationModelConfig(modelConfigClass, punctuationModelPath);
            setIfExists(modelConfig, "setCtTransformer", String.class, punctuationModelPath);
            setIfExists(
                    modelConfig,
                    "setNumThreads",
                    int.class,
                    Math.max(1, Runtime.getRuntime().availableProcessors() / 2)
            );
            setIfExists(modelConfig, "setDebug", boolean.class, false);
            setIfExists(modelConfig, "setProvider", String.class, "cpu");
            Object punctuationConfig = createPunctuationConfig(
                    punctuationConfigClass,
                    modelConfigClass,
                    modelConfig
            );
            offlinePunctuation = createOfflinePunctuation(
                    punctuationClass,
                    punctuationConfigClass,
                    punctuationConfig
            );
            addPunctuationMethod = punctuationClass.getMethod("addPunctuation", String.class);
            try {
                releasePunctuationMethod = punctuationClass.getMethod("release");
            } catch (NoSuchMethodException ignored) {
                releasePunctuationMethod = null;
            }
            punctuationAvailable = true;
            Log.i(TAG, "Sherpa EN/ZH punctuation is enabled: " + punctuationModelPath);
        } catch (Throwable throwable) {
            punctuationAvailable = false;
            offlinePunctuation = null;
            addPunctuationMethod = null;
            releasePunctuationMethod = null;
            Log.w(TAG, "Sherpa EN/ZH punctuation initialization failed. Java postprocessor will be used", throwable);
        }
    }
    private Object createPunctuationModelConfig(
            @NonNull Class<?> modelConfigClass,
            @NonNull String modelPath
    ) throws Exception {
        try {
            return newInstance(modelConfigClass);
        } catch (Throwable ignored) {
        }
        for (Constructor<?> constructor : modelConfigClass.getConstructors()) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length == 4
                    && parameterTypes[0] == String.class
                    && parameterTypes[1] == int.class
                    && parameterTypes[2] == boolean.class
                    && parameterTypes[3] == String.class) {
                return constructor.newInstance(
                        modelPath,
                        Math.max(1, Runtime.getRuntime().availableProcessors() / 2),
                        false,
                        "cpu"
                );
            }
        }
        throw new NoSuchMethodException("Cannot create OfflinePunctuationModelConfig");
    }
    private Object createPunctuationConfig(
            @NonNull Class<?> punctuationConfigClass,
            @NonNull Class<?> modelConfigClass,
            @NonNull Object modelConfig
    ) throws Exception {
        try {
            Constructor<?> constructor = punctuationConfigClass.getConstructor(modelConfigClass);
            return constructor.newInstance(modelConfig);
        } catch (Throwable ignored) {
        }
        Object config = newInstance(punctuationConfigClass);
        setIfExists(config, "setModel", modelConfigClass, modelConfig);
        setIfExists(config, "setModelConfig", modelConfigClass, modelConfig);
        return config;
    }
    private Object createOfflinePunctuation(
            @NonNull Class<?> punctuationClass,
            @NonNull Class<?> punctuationConfigClass,
            @NonNull Object punctuationConfig
    ) throws Exception {
        try {
            Constructor<?> constructor =
                    punctuationClass.getConstructor(AssetManager.class, punctuationConfigClass);
            return constructor.newInstance(context.getAssets(), punctuationConfig);
        } catch (Throwable ignored) {
        }
        Constructor<?> constructor = punctuationClass.getConstructor(punctuationConfigClass);
        return constructor.newInstance(punctuationConfig);
    }
    private void resolveModelAssetsForLanguage(@NonNull String language) throws IOException {
        String[] dirs = getModelDirectoriesForLanguage(language);
        activeModelDirectory = findFirstDirectoryWithRequiredAssets(dirs);
        encoderAssetPath = findRequiredAsset(
                "encoder",
                buildAssetCandidates(new String[]{activeModelDirectory}, ENCODER_FILE_CANDIDATES)
        );
        decoderAssetPath = findRequiredAsset(
                "decoder",
                buildAssetCandidates(new String[]{activeModelDirectory}, DECODER_FILE_CANDIDATES)
        );
        joinerAssetPath = findRequiredAsset(
                "joiner",
                buildAssetCandidates(new String[]{activeModelDirectory}, JOINER_FILE_CANDIDATES)
        );
        tokensAssetPath = findRequiredAsset(
                "tokens",
                buildAssetCandidates(new String[]{activeModelDirectory}, TOKENS_FILE_CANDIDATES)
        );
        bpeAssetPath = findOptionalAsset(
                buildAssetCandidates(new String[]{activeModelDirectory}, BPE_FILE_CANDIDATES)
        );
        if (bpeAssetPath == null) {
            Log.w(TAG, "Optional BPE model was not found for language "
                    + language
                    + ". Directory: "
                    + activeModelDirectory);
        }
        Log.i(TAG, "Sherpa ASR assets resolved. language="
                + language
                + ", dir="
                + activeModelDirectory
                + ", encoder="
                + encoderAssetPath
                + ", decoder="
                + decoderAssetPath
                + ", joiner="
                + joinerAssetPath
                + ", tokens="
                + tokensAssetPath
                + ", bpe="
                + (bpeAssetPath != null ? bpeAssetPath : "missing"));
    }
    @NonNull
    private String findFirstDirectoryWithRequiredAssets(@NonNull String[] directories) throws IOException {
        ArrayList<String> checked = new ArrayList<>();
        for (String dir : directories) {
            String safeDir = dir == null ? "" : dir;
            String[] encoderCandidates = buildAssetCandidates(new String[]{safeDir}, ENCODER_FILE_CANDIDATES);
            String[] decoderCandidates = buildAssetCandidates(new String[]{safeDir}, DECODER_FILE_CANDIDATES);
            String[] joinerCandidates = buildAssetCandidates(new String[]{safeDir}, JOINER_FILE_CANDIDATES);
            String[] tokensCandidates = buildAssetCandidates(new String[]{safeDir}, TOKENS_FILE_CANDIDATES);
            boolean hasEncoder = findOptionalAsset(encoderCandidates) != null;
            boolean hasDecoder = findOptionalAsset(decoderCandidates) != null;
            boolean hasJoiner = findOptionalAsset(joinerCandidates) != null;
            boolean hasTokens = findOptionalAsset(tokensCandidates) != null;
            checked.add((safeDir.trim().isEmpty() ? "<assets-root>" : safeDir)
                    + " [encoder="
                    + hasEncoder
                    + ", decoder="
                    + hasDecoder
                    + ", joiner="
                    + hasJoiner
                    + ", tokens="
                    + hasTokens
                    + "]");
            if (hasEncoder && hasDecoder && hasJoiner && hasTokens) {
                return safeDir;
            }
        }
        throw new IOException("Missing ASR model for language "
                + activeAsrLanguage
                + ". Checked directories: "
                + checked);
    }
    private String[] getModelDirectoriesForLanguage(@NonNull String language) {
        if (LANGUAGE_EN.equals(language)) {
            return EN_MODEL_DIR_CANDIDATES;
        }
        if (LANGUAGE_ZH.equals(language)) {
            return ZH_MODEL_DIR_CANDIDATES;
        }
        return RU_MODEL_DIR_CANDIDATES;
    }
    private String[] buildAssetCandidates(
            @NonNull String[] directories,
            @NonNull String[] fileNames
    ) {
        ArrayList<String> candidates = new ArrayList<>();
        for (String dir : directories) {
            for (String fileName : fileNames) {
                candidates.add(joinAssetPath(dir, fileName));
            }
        }
        return candidates.toArray(new String[0]);
    }
    private String joinAssetPath(@Nullable String dir, @NonNull String fileName) {
        if (dir == null || dir.trim().isEmpty()) {
            return fileName;
        }
        return dir + "/" + fileName;
    }
    @NonNull
    private String findRequiredAsset(
            @NonNull String label,
            @NonNull String[] candidates
    ) throws IOException {
        String found = findOptionalAsset(candidates);
        if (found != null) {
            return found;
        }
        throw new IOException("Missing asset for "
                + label
                + ". Checked: "
                + readableCandidates(candidates));
    }
    @Nullable
    private String findOptionalAsset(@NonNull String[] candidates) {
        for (String candidate : candidates) {
            if (assetExists(candidate)) {
                return candidate;
            }
        }
        return null;
    }
    private String readableCandidates(@NonNull String[] candidates) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < candidates.length; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(candidates[i]);
        }
        return builder.toString();
    }
    private boolean assetExists(@NonNull String assetPath) {
        try (InputStream ignored = context.getAssets().open(assetPath)) {
            return true;
        } catch (IOException exception) {
            return false;
        }
    }
    private void createNewStream() throws Exception {
        releaseStreamOnly();
        stream = createStreamMethod.invoke(recognizer, "");
        Class<?> streamClass = Class.forName("com.k2fsa.sherpa.onnx.OnlineStream");
        acceptWaveformMethod = streamClass.getMethod("acceptWaveform", float[].class, int.class);
        inputFinishedMethod = streamClass.getMethod("inputFinished");
        releaseStreamMethod = streamClass.getMethod("release");
    }
    private Object newInstance(Class<?> clazz) throws Exception {
        Constructor<?> constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }
    private void callSetter(
            @NonNull Object target,
            @NonNull String methodName,
            @NonNull Class<?> parameterClass,
            @Nullable Object value
    ) throws Exception {
        Method method = target.getClass().getMethod(methodName, parameterClass);
        method.invoke(target, value);
    }
    private void setIfExists(
            @Nullable Object target,
            @NonNull String methodName,
            @NonNull Class<?> parameterClass,
            @Nullable Object value
    ) {
        if (target == null) {
            return;
        }
        try {
            Method method = target.getClass().getMethod(methodName, parameterClass);
            method.invoke(target, value);
        } catch (Throwable ignored) {
        }
    }
    @SuppressLint("MissingPermission")
    private void startAudioRecord() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("RECORD_AUDIO permission is not granted");
        }
        int minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        if (minBuffer <= 0) {
            throw new IllegalStateException("AudioRecord minimum buffer size is invalid: " + minBuffer);
        }
        int bufferSize = Math.max(minBuffer, CHUNK_SAMPLES * 6 * 2);
        try {
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
            );
        } catch (SecurityException exception) {
            throw new SecurityException("RECORD_AUDIO permission was rejected by the system", exception);
        }
        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            throw new IllegalStateException("AudioRecord не инициализирован");
        }
        audioRecord.startRecording();
    }
    private void stopAudioRecord() {
        AudioRecord record = audioRecord;
        audioRecord = null;
        if (record == null) {
            return;
        }
        try {
            if (record.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                record.stop();
            }
        } catch (Exception ignored) {
        }
        try {
            record.release();
        } catch (Exception ignored) {
        }
    }
    private String readCurrentText() throws Exception {
        if (recognizer == null || stream == null || getResultMethod == null) {
            return "";
        }
        Object result = getResultMethod.invoke(recognizer, stream);
        if (result == null) {
            return "";
        }
        if (getTextMethod == null) {
            getTextMethod = result.getClass().getMethod("getText");
        }
        Object text = getTextMethod.invoke(result);
        return text != null ? text.toString() : "";
    }
    private boolean invokeBoolean(@NonNull Method method, @NonNull Object target, @NonNull Object argument) throws Exception {
        Object result = method.invoke(target, argument);
        return result instanceof Boolean && (Boolean) result;
    }
    private void emitPartialIfChanged(@Nullable String rawText) {
        String text = normalizePartial(rawText);
        if (text.isEmpty() || text.equals(lastPartialText)) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastPartialEmitTime < PARTIAL_EMIT_MIN_INTERVAL_MS) {
            return;
        }
        lastPartialText = text;
        lastPartialEmitTime = now;
        emitPartial(text);
    }
    private String normalizePartial(@Nullable String value) {
        String text = normalizeBase(value);
        if (text.isEmpty()) {
            return "";
        }
        text = removeFillerWords(text, false);
        text = fixCommonWords(text);
        text = collapseRepeatedWords(text);
        text = cleanupBrokenPunctuation(text);
        text = capitalizeSentences(text);
        return text;
    }
    private String normalizeFinal(@Nullable String value) {
        String text = normalizeBase(value);
        if (text.isEmpty()) {
            return "";
        }
        text = normalizeSpokenPunctuation(text);
        text = removeFillerWords(text, true);
        text = fixCommonWords(text);
        text = collapseRepeatedWords(text);
        text = cleanupBrokenPunctuation(text);
        if (isEnglishOrChineseAsrLanguage()) {
            text = applyEnglishChinesePunctuationIfAvailable(text);
        }
        text = normalizeBase(text);
        text = fixCommonWords(text);
        text = cleanupBrokenPunctuation(text);
        text = fixSpacesAroundPunctuation(text);
        if (isRussianAsrLanguage()) {
            text = addRussianSmartPunctuation(text);
        } else if (isEnglishAsrLanguage()) {
            text = addEnglishSmartCommas(text);
        }
        text = cleanupBrokenPunctuation(text);
        text = fixSpacesAroundPunctuation(text);
        text = capitalizeSentences(text);
        text = ensureFinalPunctuation(text);
        return text;
    }
    private String applyEnglishChinesePunctuationIfAvailable(@NonNull String text) {
        if (!punctuationAvailable || offlinePunctuation == null || addPunctuationMethod == null) {
            return text;
        }
        try {
            Object result = addPunctuationMethod.invoke(offlinePunctuation, text);
            if (result == null) {
                return text;
            }
            String punctuated = result.toString();
            if (punctuated.trim().isEmpty()) {
                return text;
            }
            return punctuated;
        } catch (Throwable throwable) {
            Log.w(TAG, "Sherpa EN/ZH punctuation failed. Java postprocessor will be used", throwable);
            punctuationAvailable = false;
            return text;
        }
    }
    private String normalizeBase(@Nullable String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace('ё', 'е')
                .replace('Ё', 'Е')
                .replace('–', '-')
                .replace('—', '-')
                .replaceAll("\\s+", " ")
                .trim();
    }
    private String normalizeSpokenPunctuation(@NonNull String text) {
        String result = " " + text + " ";
        if (isRussianAsrLanguage()) {
            result = result.replaceAll("(?iu)\\s+(точка\\s+с\\s+запятой)\\s+", "; ");
            result = result.replaceAll("(?iu)\\s+(двоеточие)\\s+", ": ");
            result = result.replaceAll("(?iu)\\s+(вопросительный\\s+знак|знак\\s+вопроса)\\s+", "? ");
            result = result.replaceAll("(?iu)\\s+(восклицательный\\s+знак|знак\\s+восклицания)\\s+", "! ");
            result = result.replaceAll("(?iu)\\s+(запятая)\\s+", ", ");
            result = result.replaceAll("(?iu)\\s+(точка)\\s+", ". ");
        } else if (isEnglishAsrLanguage()) {
            result = result.replaceAll("(?iu)\\s+(comma)\\s+", ", ");
            result = result.replaceAll("(?iu)\\s+(period|full stop)\\s+", ". ");
            result = result.replaceAll("(?iu)\\s+(question mark)\\s+", "? ");
            result = result.replaceAll("(?iu)\\s+(exclamation mark)\\s+", "! ");
            result = result.replaceAll("(?iu)\\s+(colon)\\s+", ": ");
            result = result.replaceAll("(?iu)\\s+(semicolon)\\s+", "; ");
        }
        return normalizeBase(result);
    }
    private String removeFillerWords(@NonNull String text, boolean aggressive) {
        String result = " " + text + " ";
        result = result.replaceAll("(?iu)\\s+(ээ+|эм+|мм+|м-м+|а-а+|э-э+|м-м-м+)\\s+", " ");
        if (aggressive) {
            result = result.replaceAll(
                    "(?iu)\\s+(типа|как бы|короче|значит|ну вот|в общем|в принципе|скажем так|так сказать|это самое)\\s+",
                    " "
            );
            result = result.replaceAll(
                    "(?iu)\\s+(you know|like|basically|actually|well|i mean|sort of|kind of)\\s+",
                    " "
            );
        }
        return normalizeBase(result);
    }
    private String fixCommonWords(@NonNull String text) {
        String result = " " + text + " ";
        for (Map.Entry<String, String> entry : commonCorrections.entrySet()) {
            result = result.replaceAll(entry.getKey(), entry.getValue());
        }
        return normalizeBase(result);
    }
    private String collapseRepeatedWords(@NonNull String text) {
        String previous;
        String current = text;
        do {
            previous = current;
            current = current.replaceAll("(?iu)\\b(\\p{L}{2,})\\s+\\1\\b", "$1");
            current = current.replaceAll("(?iu)\\b(\\p{L}{2,}\\s+\\p{L}{2,})\\s+\\1\\b", "$1");
        } while (!previous.equals(current));
        return normalizeBase(current);
    }
    private String addRussianSmartPunctuation(@NonNull String text) {
        String result = text;
        result = addRussianIntroCommas(result);
        result = addRussianPhraseCommas(result);
        result = result.replaceAll("(?iu)(\\S)\\s+(но|однако|зато)\\s+", "$1, $2 ");
        result = result.replaceAll("(?iu)(\\S)\\s+(а)\\s+(не|вот|если|когда|потом|также|еще|ещё)", "$1, $2 $3");
        result = result.replaceAll("(?iu)(\\S)\\s+(потому\\s+что|так\\s+как|хотя|если|когда|пока|после\\s+того\\s+как|перед\\s+тем\\s+как)\\s+", "$1, $2 ");
        result = result.replaceAll("(?iu)(\\S)\\s+(что|чтобы|где|куда|откуда|почему|зачем)\\s+", "$1, $2 ");
        result = result.replaceAll("(?iu)(\\S)\\s+(который|которая|которое|которые|которого|которой|которых|которым|которыми)\\s+", "$1, $2 ");
        result = cleanupCommas(result);
        result = fixWrongRussianCommas(result);
        return cleanupCommas(result);
    }
    private String addRussianIntroCommas(@NonNull String text) {
        String result = text;
        result = result.replaceAll(
                "(?iu)^(например|кстати|конечно|возможно|вероятно|наверное|поэтому|итак|во-первых|во-вторых|в-третьих|честно\\s+говоря|скорее\\s+всего|по\\s+сути|к\\s+сожалению|к\\s+счастью)\\s+",
                "$1, "
        );
        result = result.replaceAll(
                "(?iu)([.!?]\\s+)(например|кстати|конечно|возможно|вероятно|наверное|поэтому|итак|во-первых|во-вторых|в-третьих|честно\\s+говоря|скорее\\s+всего|по\\s+сути|к\\s+сожалению|к\\s+счастью)\\s+",
                "$1$2, "
        );
        return result;
    }
    private String addRussianPhraseCommas(@NonNull String text) {
        String result = text;
        result = result.replaceAll("(?iu)\\bдело\\s+в\\s+том\\s+что\\b", "дело в том, что");
        result = result.replaceAll("(?iu)\\bсуть\\s+в\\s+том\\s+что\\b", "суть в том, что");
        result = result.replaceAll("(?iu)\\bпроблема\\s+в\\s+том\\s+что\\b", "проблема в том, что");
        result = result.replaceAll("(?iu)\\bя\\s+думаю\\s+что\\b", "я думаю, что");
        result = result.replaceAll("(?iu)\\bя\\s+считаю\\s+что\\b", "я считаю, что");
        result = result.replaceAll("(?iu)\\bмне\\s+кажется\\s+что\\b", "мне кажется, что");
        result = result.replaceAll("(?iu)\\bполучается\\s+что\\b", "получается, что");
        result = result.replaceAll("(?iu)\\bпонятно\\s+что\\b", "понятно, что");
        result = result.replaceAll("(?iu)\\bважно\\s+что\\b", "важно, что");
        result = result.replaceAll("(?iu)\\bнужно\\s+чтобы\\b", "нужно, чтобы");
        return result;
    }
    private String fixWrongRussianCommas(@NonNull String text) {
        String result = text;
        result = result.replaceAll("(?iu)\\bпотому,\\s+что\\b", "потому что");
        result = result.replaceAll("(?iu)\\bтак,\\s+как\\b", "так как");
        result = result.replaceAll("(?iu)\\bпосле\\s+того,\\s+как\\b", "после того как");
        result = result.replaceAll("(?iu)\\bперед\\s+тем,\\s+как\\b", "перед тем как");
        result = result.replaceAll("(?iu)\\bкак,\\s+бы\\b", "как бы");
        result = result.replaceAll("(?iu)\\bто,\\s+есть\\b", "то есть");
        return result;
    }
    private String addEnglishSmartCommas(@NonNull String text) {
        String result = text;
        result = result.replaceAll("(?iu)(\\S)\\s+(although|because|but|however|therefore|so)\\s+", "$1, $2 ");
        result = result.replaceAll("(?iu)^(however|therefore|for example|actually|basically)\\s+", "$1, ");
        return cleanupCommas(result);
    }
    private String cleanupCommas(@NonNull String text) {
        return text
                .replaceAll("^,\\s*", "")
                .replaceAll("\\s+,", ",")
                .replaceAll(",\\s*,+", ", ")
                .replaceAll(",\\s+([.!?;:])", "$1")
                .replaceAll("\\(\\s+", "(")
                .replaceAll("\\s+\\)", ")")
                .replaceAll("\\s+", " ")
                .trim();
    }
    private String cleanupBrokenPunctuation(@NonNull String text) {
        return text
                .replaceAll("\\s+([,.!?;:])", "$1")
                .replaceAll("([,.!?;:])\\s*([,.!?;:])+", "$1")
                .replaceAll("\\s+-\\s+", " - ")
                .replaceAll("\\s+", " ")
                .trim();
    }
    private String fixSpacesAroundPunctuation(@NonNull String text) {
        return text
                .replaceAll("\\s+([,.!?;:])", "$1")
                .replaceAll("([,.!?;:])(\\S)", "$1 $2")
                .replaceAll("\\s+", " ")
                .trim();
    }
    private String capitalizeSentences(@NonNull String text) {
        String clean = normalizeBase(text);
        if (clean.isEmpty()) {
            return "";
        }
        if (isChineseAsrLanguage()) {
            return clean;
        }
        StringBuilder builder = new StringBuilder(clean.length());
        boolean capitalizeNext = true;
        for (int i = 0; i < clean.length(); i++) {
            char current = clean.charAt(i);
            if (capitalizeNext && Character.isLetter(current)) {
                builder.append(String.valueOf(current).toUpperCase(recognitionLocale));
                capitalizeNext = false;
            } else {
                builder.append(current);
            }
            if (current == '.' || current == '!' || current == '?' || current == '…') {
                capitalizeNext = true;
            }
        }
        return fixCommonWords(builder.toString().trim());
    }
    private String ensureFinalPunctuation(@NonNull String text) {
        String clean = normalizeBase(text);
        if (clean.isEmpty()) {
            return "";
        }
        if (clean.matches(".*[.!?…。！？]$")) {
            return clean;
        }
        if (looksLikeQuestion(clean)) {
            if (isChineseAsrLanguage()) {
                return clean + "？";
            }
            return clean + "?";
        }
        if (isChineseAsrLanguage()) {
            return clean + "。";
        }
        return clean + ".";
    }
    private boolean looksLikeQuestion(@NonNull String text) {
        String lower = text.toLowerCase(recognitionLocale).trim();
        return lower.startsWith("кто ")
                || lower.startsWith("что ")
                || lower.startsWith("где ")
                || lower.startsWith("когда ")
                || lower.startsWith("почему ")
                || lower.startsWith("зачем ")
                || lower.startsWith("как ")
                || lower.startsWith("сколько ")
                || lower.startsWith("какой ")
                || lower.startsWith("какая ")
                || lower.startsWith("какое ")
                || lower.startsWith("какие ")
                || lower.startsWith("можно ли ")
                || lower.startsWith("нужно ли ")
                || lower.startsWith("правда ли ")
                || lower.startsWith("можешь ")
                || lower.startsWith("можете ")
                || lower.startsWith("подскажи ")
                || lower.startsWith("подскажите ")
                || lower.startsWith("есть ли ")
                || lower.startsWith("будет ли ")
                || lower.startsWith("разве ")
                || lower.startsWith("неужели ")
                || lower.startsWith("is ")
                || lower.startsWith("are ")
                || lower.startsWith("do ")
                || lower.startsWith("does ")
                || lower.startsWith("did ")
                || lower.startsWith("can ")
                || lower.startsWith("could ")
                || lower.startsWith("should ")
                || lower.startsWith("would ")
                || lower.startsWith("what ")
                || lower.startsWith("where ")
                || lower.startsWith("when ")
                || lower.startsWith("why ")
                || lower.startsWith("how ")
                || lower.endsWith(" 吗")
                || lower.endsWith("吗");
    }
    private boolean isRussianAsrLanguage() {
        return LANGUAGE_RU.equals(activeAsrLanguage);
    }
    private boolean isEnglishAsrLanguage() {
        return LANGUAGE_EN.equals(activeAsrLanguage);
    }
    private boolean isChineseAsrLanguage() {
        return LANGUAGE_ZH.equals(activeAsrLanguage);
    }
    private boolean isEnglishOrChineseAsrLanguage() {
        return isEnglishAsrLanguage() || isChineseAsrLanguage();
    }
    private String normalizeLanguageForAsr(@Nullable Locale locale) {
        if (locale == null) {
            return LANGUAGE_RU;
        }
        String language = locale.getLanguage().toLowerCase(Locale.US).trim();
        if (language.startsWith("en")) {
            return LANGUAGE_EN;
        }
        if (language.startsWith("zh")
                || language.startsWith("cn")
                || language.startsWith("cmn")
                || language.startsWith("yue")) {
            return LANGUAGE_ZH;
        }
        return LANGUAGE_RU;
    }
    private Locale localeFromLanguageCode(@Nullable String languageCode) {
        if (languageCode == null || languageCode.trim().isEmpty()) {
            return Locale.getDefault();
        }
        String normalized = languageCode.toLowerCase(Locale.US).trim();
        if (normalized.startsWith("en")) {
            return Locale.ENGLISH;
        }
        if (normalized.startsWith("zh") || normalized.startsWith("cn")) {
            return Locale.CHINESE;
        }
        return new Locale("ru");
    }
    private String buildUserFriendlyError(@NonNull Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.trim().isEmpty()) {
            message = throwable.getClass().getSimpleName();
        }
        if (message.contains("OrtGetApiBase")
                || message.contains("libsherpa-onnx-jni")
                || message.contains("UnsatisfiedLinkError")) {
            return "Конфликт native-библиотек Sherpa-ONNX/ONNX Runtime. Уберите отдельный onnxruntime-android и оставьте только lib-sherpa-onnx.";
        }
        if (message.contains("com.k2fsa.sherpa.onnx")) {
            return "Sherpa-ONNX не подключён или не попал в APK. Проверьте зависимость lib-sherpa-onnx.";
        }
        if (message.contains("Missing ASR model for language")) {
            return "ASR-модель для языка "
                    + requestedAsrLanguage
                    + " не найдена в assets. Проверьте папки asr/ru, asr/en, asr/zh и наличие encoder/decoder/joiner/tokens.";
        }
        if (message.contains("Missing asset")
                || message.contains("Missing asset for")
                || message.contains("onnx")
                || message.contains("tokens")
                || message.contains("No such file")) {
            return "Модель Sherpa-ONNX не найдена в assets. Для русского проверьте "
                    + DEFAULT_RU_MODEL_DIR
                    + " или asr/ru. Для английского — asr/en. Для китайского — asr/zh.";
        }
        if (message.contains("RECORD_AUDIO")) {
            return "Нет разрешения на микрофон для Sherpa-ONNX субтитров";
        }
        return "Ошибка Sherpa-ONNX STT: " + message;
    }
    private void releaseStreamOnly() {
        if (stream == null) {
            return;
        }
        try {
            if (inputFinishedMethod != null) {
                inputFinishedMethod.invoke(stream);
            }
        } catch (Exception ignored) {
        }
        try {
            if (releaseStreamMethod != null) {
                releaseStreamMethod.invoke(stream);
            }
        } catch (Exception ignored) {
        }
        stream = null;
        getTextMethod = null;
    }
    private void releaseSherpaObjects() {
        releaseStreamOnly();
        if (recognizer != null) {
            try {
                if (releaseRecognizerMethod != null) {
                    releaseRecognizerMethod.invoke(recognizer);
                }
            } catch (Exception ignored) {
            }
        }
        recognizer = null;
        createStreamMethod = null;
        acceptWaveformMethod = null;
        inputFinishedMethod = null;
        isReadyMethod = null;
        decodeMethod = null;
        getResultMethod = null;
        isEndpointMethod = null;
        resetMethod = null;
        releaseRecognizerMethod = null;
        releaseStreamMethod = null;
        getTextMethod = null;
    }
    private void releasePunctuationObjects() {
        if (offlinePunctuation != null && releasePunctuationMethod != null) {
            try {
                releasePunctuationMethod.invoke(offlinePunctuation);
            } catch (Exception ignored) {
            }
        }
        offlinePunctuation = null;
        addPunctuationMethod = null;
        releasePunctuationMethod = null;
        punctuationAvailable = false;
        punctuationModelPath = null;
        punctuationTriedToInit = false;
    }
    private void resetResolvedModelAssets() {
        activeModelDirectory = "";
        encoderAssetPath = null;
        decoderAssetPath = null;
        joinerAssetPath = null;
        tokensAssetPath = null;
        bpeAssetPath = null;
    }
    private void initCommonCorrections() {
        commonCorrections.put("(?iu)\\bютуб\\b", "YouTube");
        commonCorrections.put("(?iu)\\byoutube\\b", "YouTube");
        commonCorrections.put("(?iu)\\bгугл\\b", "Google");
        commonCorrections.put("(?iu)\\bандроид\\b", "Android");
        commonCorrections.put("(?iu)\\bайфон\\b", "iPhone");
        commonCorrections.put("(?iu)\\bайос\\b", "iOS");
        commonCorrections.put("(?iu)\\bчат\\s*gpt\\b", "ChatGPT");
        commonCorrections.put("(?iu)\\bчатджипити\\b", "ChatGPT");
        commonCorrections.put("(?iu)\\bopen\\s*ai\\b", "OpenAI");
        commonCorrections.put("(?iu)\\bопен\\s*аи\\b", "OpenAI");
        commonCorrections.put("(?iu)\\bшерпа\\b", "Sherpa-ONNX");
        commonCorrections.put("(?iu)\\bонникс\\b", "ONNX");
        commonCorrections.put("(?iu)\\bonnx\\b", "ONNX");
        commonCorrections.put("(?iu)\\bstt\\b", "STT");
        commonCorrections.put("(?iu)\\basr\\b", "ASR");
        commonCorrections.put("(?iu)\\bai\\s*chat\\b", "AIChat");
        commonCorrections.put("(?iu)\\bаичат\\b", "AIChat");
        commonCorrections.put("(?iu)\\bexoplayer\\b", "ExoPlayer");
        commonCorrections.put("(?iu)\\bmedia\\s*player\\b", "MediaPlayer");
        commonCorrections.put("(?iu)\\bweb\\s*view\\b", "WebView");
        commonCorrections.put("(?iu)\\bandroid\\s*studio\\b", "Android Studio");
        commonCorrections.put("(?iu)\\bgradle\\b", "Gradle");
        commonCorrections.put("(?iu)\\bglide\\b", "Glide");
        commonCorrections.put("(?iu)\\bgithub\\b", "GitHub");
        commonCorrections.put("(?iu)\\bgit\\s*hub\\b", "GitHub");
        commonCorrections.put("(?iu)\\bapi\\b", "API");
        commonCorrections.put("(?iu)\\bui\\b", "UI");
        commonCorrections.put("(?iu)\\bcpu\\b", "CPU");
        commonCorrections.put("(?iu)\\bgpu\\b", "GPU");
        commonCorrections.put("(?iu)\\bjava\\b", "Java");
        commonCorrections.put("(?iu)\\bkotlin\\b", "Kotlin");
    }
    private void emitPartial(@NonNull String text) {
        mainHandler.post(() -> listener.onPartialText(text));
    }
    private void emitFinal(@NonNull String text) {
        mainHandler.post(() -> listener.onFinalText(text));
    }
    private void emitError(@NonNull String message) {
        mainHandler.post(() -> listener.onError(message));
    }
}
