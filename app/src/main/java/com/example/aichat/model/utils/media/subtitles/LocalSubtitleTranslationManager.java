package com.example.aichat.model.utils.media.subtitles;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.aichat.view.main.chat.VideoPlayerActivity;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;


public class LocalSubtitleTranslationManager {

    public interface Callback {
        void onTranslated(@NonNull String originalText, @NonNull String translatedText);

        void onOriginalFallback(@NonNull String originalText);

        void onModelDownloadStarted(@NonNull String sourceLanguage, @NonNull String targetLanguage);

        void onError(@NonNull String message);
    }

    private static final String TAG = "LocalSubtitleTranslator";
    private static final int MAX_CACHE_SIZE = 80;

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private Translator translator;
    private String activeSourceLanguage;
    private String activeTargetLanguage;
    private boolean modelReady;
    private boolean modelDownloading;
    private int requestGeneration;

    private final LinkedHashMap<String, String> translationCache =
            new LinkedHashMap<String, String>(MAX_CACHE_SIZE, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                    return size() > MAX_CACHE_SIZE;
                }
            };

    public LocalSubtitleTranslationManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    public synchronized void translate(
            @Nullable String rawText,
            @Nullable String sourceLanguageCode,
            @Nullable String targetLanguageCode,
            @NonNull Callback callback
    ) {
        String text = normalizeText(rawText);

        if (text.isEmpty()) {
            return;
        }

        String source = normalizeMlKitLanguage(resolveEffectiveSourceLanguage(text, sourceLanguageCode));
        String target = normalizeMlKitLanguage(targetLanguageCode);

        if (source == null || target == null) {
            callback.onOriginalFallback(text);
            callback.onError("ML Kit translation language is not supported: "
                    + sourceLanguageCode
                    + " -> "
                    + targetLanguageCode);
            return;
        }

        if (source.equals(target)) {
            callback.onOriginalFallback(text);
            return;
        }

        String cacheKey = source + ">" + target + ":" + text;
        String cached = translationCache.get(cacheKey);

        if (cached != null) {
            callback.onTranslated(text, cached);
            return;
        }

        int generation = ++requestGeneration;

        try {
            ensureTranslator(source, target);
        } catch (Exception exception) {
            Log.e(TAG, "Failed to create translator", exception);
            callback.onOriginalFallback(text);
            callback.onError("Не удалось создать локальный переводчик: " + exception.getMessage());
            return;
        }

        if (translator == null) {
            callback.onOriginalFallback(text);
            return;
        }

        if (!modelReady) {
            if (!modelDownloading) {
                modelDownloading = true;
                callback.onModelDownloadStarted(source, target);

                DownloadConditions conditions =
                        new DownloadConditions.Builder()
                                .build();

                translator.downloadModelIfNeeded(conditions)
                        .addOnSuccessListener(unused -> {
                            synchronized (LocalSubtitleTranslationManager.this) {
                                modelReady = true;
                                modelDownloading = false;
                            }

                            translateAfterModelReady(text, source, target, cacheKey, generation, callback);
                        })
                        .addOnFailureListener(exception -> {
                            synchronized (LocalSubtitleTranslationManager.this) {
                                modelReady = false;
                                modelDownloading = false;
                            }

                            Log.e(TAG, "ML Kit translation model download failed", exception);
                            callback.onOriginalFallback(text);
                            callback.onError("Не удалось скачать локальную модель перевода: "
                                    + exception.getMessage());
                        });
            } else {
                callback.onOriginalFallback(text);
            }

            return;
        }

        translateAfterModelReady(text, source, target, cacheKey, generation, callback);
    }

    private void translateAfterModelReady(
            @NonNull String text,
            @NonNull String source,
            @NonNull String target,
            @NonNull String cacheKey,
            int generation,
            @NonNull Callback callback
    ) {
        Translator currentTranslator;

        synchronized (this) {
            if (generation != requestGeneration) {
                return;
            }

            currentTranslator = translator;
        }

        if (currentTranslator == null) {
            callback.onOriginalFallback(text);
            return;
        }

        currentTranslator.translate(text)
                .addOnSuccessListener(translatedText -> {
                    String cleanTranslated = normalizeText(translatedText);

                    if (cleanTranslated.isEmpty()) {
                        callback.onOriginalFallback(text);
                        return;
                    }

                    synchronized (LocalSubtitleTranslationManager.this) {
                        translationCache.put(cacheKey, cleanTranslated);
                    }

                    callback.onTranslated(text, cleanTranslated);
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "ML Kit translate failed", exception);
                    callback.onOriginalFallback(text);
                    callback.onError("Ошибка локального перевода: " + exception.getMessage());
                });
    }

    private synchronized void ensureTranslator(
            @NonNull String source,
            @NonNull String target
    ) {
        if (translator != null
                && source.equals(activeSourceLanguage)
                && target.equals(activeTargetLanguage)) {
            return;
        }

        releaseTranslatorOnly();

        TranslatorOptions options =
                new TranslatorOptions.Builder()
                        .setSourceLanguage(source)
                        .setTargetLanguage(target)
                        .build();

        translator = Translation.getClient(options);
        activeSourceLanguage = source;
        activeTargetLanguage = target;
        modelReady = false;
        modelDownloading = false;

        Log.i(TAG, "Translator selected: " + source + " -> " + target);
    }


    @NonNull
    private String resolveEffectiveSourceLanguage(
            @NonNull String text,
            @Nullable String sourceLanguageCode
    ) {
        String normalized = sourceLanguageCode != null
                ? sourceLanguageCode.trim().toLowerCase(Locale.US)
                : "";

        if ((normalized.startsWith("en")
                || normalized.startsWith("zh")
                || normalized.startsWith("cn")
                || normalized.startsWith("cmn")
                || normalized.startsWith("yue"))
                && containsCjk(text)) {
            return "zh";
        }

        return normalized.isEmpty() ? "en" : normalized;
    }

    private boolean containsCjk(@NonNull String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if ((c >= '\u4E00' && c <= '\u9FFF')
                    || (c >= '\u3400' && c <= '\u4DBF')
                    || (c >= '\uF900' && c <= '\uFAFF')) {
                return true;
            }
        }

        return false;
    }

    @Nullable
    public static String normalizeMlKitLanguage(@Nullable String languageCode) {
        if (languageCode == null || languageCode.trim().isEmpty()) {
            return null;
        }

        String normalized =
                languageCode
                        .trim()
                        .replace('_', '-')
                        .toLowerCase(Locale.US);

        if (normalized.startsWith("zh")) {
            return TranslateLanguage.CHINESE;
        }

        if (normalized.startsWith("cn")) {
            return TranslateLanguage.CHINESE;
        }

        if (normalized.startsWith("en")) {
            return TranslateLanguage.ENGLISH;
        }

        if (normalized.startsWith("ru")) {
            return TranslateLanguage.RUSSIAN;
        }

        if (normalized.startsWith("es")) {
            return TranslateLanguage.SPANISH;
        }

        String fromTag = TranslateLanguage.fromLanguageTag(normalized);

        if (fromTag != null) {
            return fromTag;
        }

        int dashIndex = normalized.indexOf('-');

        if (dashIndex > 0) {
            return TranslateLanguage.fromLanguageTag(normalized.substring(0, dashIndex));
        }

        return null;
    }

    @NonNull
    private String normalizeText(@Nullable String text) {
        if (text == null) {
            return "";
        }

        return text
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    public synchronized void release() {
        releaseTranslatorOnly();
        translationCache.clear();
    }

    private void releaseTranslatorOnly() {
        if (translator != null) {
            try {
                translator.close();
            } catch (Exception ignored) {
            }
        }

        translator = null;
        activeSourceLanguage = null;
        activeTargetLanguage = null;
        modelReady = false;
        modelDownloading = false;
        requestGeneration++;
    }
}
