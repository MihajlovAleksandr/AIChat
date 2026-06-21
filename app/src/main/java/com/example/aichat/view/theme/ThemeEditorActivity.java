package com.example.aichat.view.theme;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.example.aichat.controller.ThemeController;
import com.example.aichat.databinding.ActivityThemeEditorBinding;
import com.example.aichat.dto.response.ThemeContentResponse;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.files.UploadProgressListener;
import com.example.aichat.model.connection.files.UploadProgress;
import com.example.aichat.model.entities.ThemeType;
import com.example.aichat.model.utils.JsonHelper;
import com.example.aichat.model.utils.mappers.ThemeMapper;
import com.example.aichat.model.utils.theme.ThemeAttrResolver;
import com.example.aichat.model.utils.theme.ThemeModel;
import com.example.aichat.model.utils.theme.ThemeSelectionCoordinator;
import com.example.aichat.model.utils.theme.ThemeStorage;
import com.example.aichat.R;
import com.example.aichat.view.helpers.HoneycombRevealView;
import com.example.aichat.view.main.BaseActivity;
import com.example.aichat.view.main.MainActivity;
import com.example.aichat.view.theme.binders.ThemeEditorUiBinder;
import com.example.aichat.view.theme.binders.ThemeMessageAnimationBinder;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.yalantis.ucrop.UCrop;
import java.io.File;
import java.util.Locale;
import java.util.UUID;

public class ThemeEditorActivity extends BaseActivity {

    public static final String EXTRA_THEME_ID = "extra_theme_id";

    private static final String THEME_STORAGE_PREFS = "themes_storage";
    private static final String THEME_ANIMATION_PREFS = "theme_animation_storage";
    private static final String THEME_ANIMATION_PREFIX = "theme_animation_";
    private static final String DEFAULT_ANIMATION = "default";
    private static final String[] MESSAGE_ANIMATION_CODES = {
            ThemeMessageAnimationBinder.ANIMATION_DEFAULT,
            ThemeMessageAnimationBinder.ANIMATION_SMOOTH,
            ThemeMessageAnimationBinder.ANIMATION_SLIDE_UP,
            ThemeMessageAnimationBinder.ANIMATION_SLIDE_UP_BOUNCE,
            ThemeMessageAnimationBinder.ANIMATION_FADE,
            ThemeMessageAnimationBinder.ANIMATION_SCALE
    };

    private ActivityThemeEditorBinding binding;
    private ThemeStorage themeStorage;

    private ActivityResultLauncher<String> backgroundImagePickerLauncher;
    private ActivityResultLauncher<Intent> backgroundImageCropLauncher;

    @Nullable
    private UUID editingThemeId;

    private String colorPrimary = "#20A39A";
    private String colorSurface = "#FFFFFF";
    private String colorOnSurface = "#1A1A1A";
    private String colorOnSurfaceVariant = "#666666";
    private String colorSecondary = "#20A39A";

    private String myMessageColor = "#20A39A";
    private String otherMessageColor = "#F1F1F1";
    private String backgroundColor = "#FFFFFF";

    private String backgroundImagePath;
    private String backgroundImageName;
    private String pendingBackgroundImageName;

    private String messageAnimation = DEFAULT_ANIMATION;

    private static final String[] PALETTE_COLORS = {
            "#20A39A", "#2F5AC9", "#4169E1", "#9B59B6", "#FF5A5F", "#FF3B3B",
            "#FF7A00", "#FF8C2A", "#FFA22B", "#FFC043", "#C9C22E", "#7DB957",
            "#777777", "#666666", "#000000", "#FFFFFF", "#F1F1F1", "#121212",
            "#2B2B2B", "#FF9800", "#E91E63", "#3F51B5", "#009688", "#607D8B"
    };

    private ThemeController sendThemeController;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sendThemeController = new ThemeController(ConnectionSingleton.getInstance().getConnectionDispatcher());

        themeStorage = new ThemeStorage(this);
        restoreSelectedTheme();

        binding = ActivityThemeEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        @Nullable String id = getIntent().getStringExtra(EXTRA_THEME_ID);
        if(id==null)
            editingThemeId = null;
        else
            editingThemeId = UUID.fromString(id);

        initBackgroundImagePicker();
        loadEditingThemeIfNeeded();
        setupBackButton();
        setupBackPressedHandler();
        setupClickListeners();

        ThemeEditorUiBinder.applyScreenTheme(this, binding);
        updateColorViews();
        updatePreview();
    }

    private void restoreSelectedTheme() {
        String selectedThemeId = ThemeSelectionCoordinator.getSelectedCustomThemeId(this);
        ThemeModel selectedTheme = selectedThemeId != null
                ? themeStorage.getThemeById(selectedThemeId)
                : null;

        if (selectedTheme != null) {
            ThemeAttrResolver.applyTheme(selectedTheme);
        } else {
            ThemeAttrResolver.clear();
        }
    }

    private void initBackgroundImagePicker() {
        backgroundImagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        startBackgroundImageCrop(uri);
                    }
                }
        );

        backgroundImageCropLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::handleBackgroundImageCropResult
        );
    }

    private void startBackgroundImageCrop(Uri sourceUri) {
        try {
            File directory = new File(getFilesDir(), "theme_backgrounds");

            if (!directory.exists()) {
                directory.mkdirs();
            }

            File destinationFile = new File(
                    directory,
                    "theme_background_" + System.currentTimeMillis() + ".jpg"
            );

            pendingBackgroundImageName = getDisplayName(sourceUri);

            Uri destinationUri = Uri.fromFile(destinationFile);

            UCrop.Options options = new UCrop.Options();

            options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
            options.setCompressionQuality(92);
            options.setToolbarTitle(getString(R.string.theme_editor_choose_background_image));
            options.setHideBottomControls(false);
            options.setFreeStyleCropEnabled(true);
            options.setShowCropGrid(true);
            options.setShowCropFrame(true);
            options.setToolbarColor(parseColorOrDefault(colorPrimary, "#20A39A"));
            options.setStatusBarColor(parseColorOrDefault(colorPrimary, "#168A83"));
            options.setActiveControlsWidgetColor(parseColorOrDefault(colorPrimary, "#20A39A"));

            Intent cropIntent = UCrop.of(sourceUri, destinationUri)
                    .withAspectRatio(9, 16)
                    .withMaxResultSize(1440, 2560)
                    .withOptions(options)
                    .getIntent(this);

            backgroundImageCropLauncher.launch(cropIntent);

        } catch (Exception ex) {
            pendingBackgroundImageName = null;

            Toast.makeText(
                    this,
                    R.string.theme_editor_background_image_error,
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void handleBackgroundImageCropResult(ActivityResult result) {
        if (result.getData() == null) {
            pendingBackgroundImageName = null;
            return;
        }

        if (result.getResultCode() == Activity.RESULT_OK) {
            Uri resultUri = UCrop.getOutput(result.getData());

            if (resultUri == null || resultUri.getPath() == null) {
                pendingBackgroundImageName = null;

                Toast.makeText(
                        this,
                        R.string.theme_editor_background_image_error,
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            File croppedFile = new File(resultUri.getPath());

            backgroundImagePath = croppedFile.getAbsolutePath();
            backgroundImageName = pendingBackgroundImageName != null
                    && !pendingBackgroundImageName.trim().isEmpty()
                    ? pendingBackgroundImageName
                    : croppedFile.getName();

            pendingBackgroundImageName = null;

            Toast.makeText(
                    this,
                    R.string.theme_editor_background_image_selected,
                    Toast.LENGTH_SHORT
            ).show();

            updateColorViews();
            updatePreview();
            return;
        }

        pendingBackgroundImageName = null;

        if (result.getResultCode() == UCrop.RESULT_ERROR) {
            Throwable cropError = UCrop.getError(result.getData());

            Toast.makeText(
                    this,
                    cropError != null && cropError.getLocalizedMessage() != null
                            ? cropError.getLocalizedMessage()
                            : getString(R.string.theme_editor_background_image_error),
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private int parseColorOrDefault(String color, String fallback) {
        try {
            return Color.parseColor(color);
        } catch (Exception ex) {
            return Color.parseColor(fallback);
        }
    }

    private void loadEditingThemeIfNeeded() {
        if (editingThemeId == null) {
            return;
        }

        ThemeModel theme = themeStorage.getThemeById(editingThemeId.toString());

        if (theme == null) {
            Toast.makeText(
                    this,
                    R.string.theme_not_found,
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        binding.etThemeName.setText(theme.getName());

        colorPrimary = safeColor(theme.getColorPrimary(), colorPrimary);
        colorSurface = safeColor(theme.getColorSurface(), colorSurface);
        colorOnSurface = safeColor(theme.getColorOnSurface(), colorOnSurface);
        colorOnSurfaceVariant = safeColor(theme.getColorOnSurfaceVariant(), colorOnSurfaceVariant);
        colorSecondary = safeColor(theme.getColorSecondary(), colorSecondary);

        myMessageColor = safeColor(theme.getMyMessageColor(), myMessageColor);
        otherMessageColor = safeColor(theme.getOtherMessageColor(), otherMessageColor);
        backgroundColor = safeColor(theme.getBackgroundColor(), backgroundColor);

        backgroundImagePath = theme.getBackgroundImagePath();
        backgroundImageName = theme.getBackgroundImageName();
        pendingBackgroundImageName = null;

        messageAnimation = loadThemeAnimation(theme);
    }

    private String safeColor(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }

        try {
            Color.parseColor(value);
            return value;
        } catch (Exception ex) {
            return fallback;
        }
    }

    private String loadThemeAnimation(ThemeModel theme) {
        if (theme == null) {
            return ThemeMessageAnimationBinder.ANIMATION_DEFAULT;
        }

        String modelAnimation = ThemeMessageAnimationBinder.normalize(
                theme.getMessageAnimation()
        );

        if (!ThemeMessageAnimationBinder.isDefault(modelAnimation)) {
            return modelAnimation;
        }

        if (theme.getId() == null) {
            return modelAnimation;
        }

        String savedAnimation = loadSavedThemeAnimation(theme.getId().toString());

        if (!ThemeMessageAnimationBinder.isDefault(savedAnimation)) {
            return savedAnimation;
        }

        return modelAnimation;
    }

    private String loadSavedThemeAnimation(@Nullable String themeId) {
        if (themeId == null || themeId.trim().isEmpty()) {
            return ThemeMessageAnimationBinder.ANIMATION_DEFAULT;
        }

        String key = THEME_ANIMATION_PREFIX + themeId.trim();

        String animation = getSharedPreferences(THEME_ANIMATION_PREFS, MODE_PRIVATE)
                .getString(key, null);

        if (animation == null || animation.trim().isEmpty()) {
            animation = getSharedPreferences(THEME_STORAGE_PREFS, MODE_PRIVATE)
                    .getString(key, ThemeMessageAnimationBinder.ANIMATION_DEFAULT);
        }

        return ThemeMessageAnimationBinder.normalize(animation);
    }

    private String getAnimationDisplayName(String animationCode) {
        String normalizedCode = ThemeMessageAnimationBinder.normalize(animationCode);

        String[] animations = getResources().getStringArray(
                R.array.theme_editor_message_animations
        );

        for (String animation : animations) {
            if (ThemeMessageAnimationBinder.normalize(animation).equals(normalizedCode)) {
                return animation;
            }
        }

        for (int i = 0; i < MESSAGE_ANIMATION_CODES.length && i < animations.length; i++) {
            if (MESSAGE_ANIMATION_CODES[i].equals(normalizedCode)) {
                return animations[i];
            }
        }

        return getString(R.string.theme_editor_default_value);
    }

    private String getAnimationCodeByIndex(int index, String fallbackLabel) {
        String codeFromLabel = ThemeMessageAnimationBinder.normalize(fallbackLabel);

        if (!ThemeMessageAnimationBinder.ANIMATION_DEFAULT.equals(codeFromLabel) || index == 0) {
            return codeFromLabel;
        }

        if (index >= 0 && index < MESSAGE_ANIMATION_CODES.length) {
            return MESSAGE_ANIMATION_CODES[index];
        }

        return codeFromLabel;
    }

    private void setupBackButton() {
        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void setupBackPressedHandler() {
        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        finish();
                    }
                }
        );
    }

    private void setupClickListeners() {
        binding.btnSave.setOnClickListener(v -> saveTheme());

        binding.rowPrimary.setOnClickListener(v -> showColorPicker(
                getString(R.string.theme_editor_choose_primary_color),
                colorPrimary,
                color -> {
                    colorPrimary = color;
                    colorSecondary = color;
                    updateColorViews();
                    updatePreview();
                }
        ));

        binding.rowText.setOnClickListener(v -> showColorPicker(
                getString(R.string.theme_editor_choose_text_color),
                colorOnSurface,
                color -> {
                    colorOnSurface = color;
                    updateColorViews();
                    updatePreview();
                }
        ));

        binding.rowTextSecondary.setOnClickListener(v -> showColorPicker(
                getString(R.string.theme_editor_choose_secondary_text_color),
                colorOnSurfaceVariant,
                color -> {
                    colorOnSurfaceVariant = color;
                    updateColorViews();
                    updatePreview();
                }
        ));

        binding.rowMyMessage.setOnClickListener(v -> showColorPicker(
                getString(R.string.theme_editor_choose_my_messages_color),
                myMessageColor,
                color -> {
                    myMessageColor = color;
                    updateColorViews();
                    updatePreview();
                }
        ));

        binding.rowOtherMessage.setOnClickListener(v -> showColorPicker(
                getString(R.string.theme_editor_choose_other_messages_color),
                otherMessageColor,
                color -> {
                    otherMessageColor = color;
                    updateColorViews();
                    updatePreview();
                }
        ));

        binding.rowChatBackground.setOnClickListener(v -> showChatBackgroundPicker());
        binding.rowMessageAnimation.setOnClickListener(v -> showAnimationPicker());
        binding.previewCard.setOnClickListener(v -> showFullScreenPreview());
    }

    private void saveTheme() {
        String themeName = binding.etThemeName.getText() == null
                ? ""
                : binding.etThemeName.getText().toString().trim();

        if (themeName.isEmpty()) {
            binding.tilThemeName.setError(
                    getString(R.string.theme_editor_enter_theme_name_error)
            );
            return;
        }

        binding.tilThemeName.setError(null);
        binding.btnSave.setEnabled(false);

        boolean editingExistingTheme = editingThemeId != null;
        boolean editingSelectedTheme = editingExistingTheme
                && editingThemeId.toString().equals(getSelectedCustomThemeIdFromSettings());
        boolean shouldApplyAfterEdit = editingSelectedTheme;

        String normalizedAnimation = ThemeMessageAnimationBinder.normalize(messageAnimation);
        String themeContentJson = createThemeContentJson(normalizedAnimation);

        java.util.concurrent.CompletableFuture<com.example.aichat.dto.response.ThemeResponse> request;

        if (editingThemeId == null) {
            request = sendThemeController.sendTheme(
                    themeName,
                    themeContentJson
            );
        } else {
            request = sendThemeController.updateTheme(
                    editingThemeId,
                    themeName,
                    themeContentJson
            );
        }

        request.thenAccept(theme -> {
                    ThemeMapper mapper = new ThemeMapper();
                    ThemeModel model = mapper.ToDTO(theme);

                    if (model != null && model.getId() != null) {
                        model.setMessageAnimation(normalizedAnimation);

                        boolean animationSaved = saveThemeAnimation(
                                model.getId(),
                                normalizedAnimation
                        );

                        Log.d(
                                "MessageAnimation",
                                "theme animation saved = "
                                        + animationSaved
                                        + ", themeId = "
                                        + model.getId()
                                        + ", animation = "
                                        + normalizedAnimation
                        );

                        upsertTheme(model);

                        if (shouldApplyAfterEdit) {
                            themeStorage.setSelectedThemeId(model.getId().toString());
                            sendThemeController.selectTheme(model.getId());
                        }
                    }

                    runOnUiThread(() -> {
                        binding.btnSave.setEnabled(true);

                        Toast.makeText(
                                this,
                                R.string.theme_editor_theme_saved,
                                Toast.LENGTH_SHORT
                        ).show();

                        if (model != null && model.getId() != null && shouldApplyAfterEdit) {
                            ThemeSelectionCoordinator.saveCustomThemeOnly(
                                    this,
                                    model.getId().toString()
                            );

                            restartAppAfterThemeChanged();
                            return;
                        }

                        setResult(RESULT_OK);
                        finish();
                    });
                })
                .exceptionally(throwable -> {
                    runOnUiThread(() -> {
                        binding.btnSave.setEnabled(true);

                        Toast.makeText(
                                this,
                                getThemeSaveErrorMessage(throwable),
                                Toast.LENGTH_SHORT
                        ).show();
                    });

                    return null;
                });
    }

    @Nullable
    private String getSelectedCustomThemeIdFromSettings() {
        String coordinatorThemeId = ThemeSelectionCoordinator.getSelectedCustomThemeId(this);

        if (isExistingLocalThemeId(coordinatorThemeId)) {
            return coordinatorThemeId;
        }

        String currentThemeMode = getSharedPreferences(
                ThemeSelectionCoordinator.SETTINGS_PREFS,
                MODE_PRIVATE
        ).getString(
                ThemeSelectionCoordinator.KEY_APP_THEME,
                ThemeSelectionCoordinator.THEME_SYSTEM
        );

        if (currentThemeMode != null
                && !currentThemeMode.trim().isEmpty()
                && !ThemeSelectionCoordinator.isDefaultThemeMode(currentThemeMode)
                && isExistingLocalThemeId(currentThemeMode)) {
            return currentThemeMode;
        }

        return null;
    }

    private boolean isExistingLocalThemeId(@Nullable String themeId) {
        if (themeId == null || themeId.trim().isEmpty()) {
            return false;
        }

        return themeStorage.getThemeById(themeId.trim()) != null;
    }

    private String createThemeContentJson(String animation) {
        String normalizedAnimation = ThemeMessageAnimationBinder.normalize(animation);

        return JsonHelper.Serialize(
                new ThemeContentResponse(
                        colorPrimary,
                        colorSurface,
                        colorOnSurface,
                        colorOnSurfaceVariant,
                        colorSecondary,
                        myMessageColor,
                        otherMessageColor,
                        backgroundColor,
                        backgroundImagePath,
                        backgroundImageName,
                        normalizedAnimation
                )
        );
    }

    private void upsertTheme(ThemeModel model) {
        if (model == null || model.getId() == null) {
            return;
        }

        ThemeModel existingTheme = themeStorage.getThemeById(
                model.getId().toString()
        );

        if (existingTheme == null) {
            themeStorage.addTheme(model);
        } else {
            themeStorage.updateTheme(model);
        }
    }

    private boolean saveThemeAnimation(UUID themeId, String animation) {
        if (themeId == null) {
            return false;
        }

        String normalizedAnimation = ThemeMessageAnimationBinder.normalize(animation);
        String key = THEME_ANIMATION_PREFIX + themeId;

        boolean savedToDedicatedPrefs = getSharedPreferences(THEME_ANIMATION_PREFS, MODE_PRIVATE)
                .edit()
                .putString(key, normalizedAnimation)
                .commit();

        getSharedPreferences(THEME_STORAGE_PREFS, MODE_PRIVATE)
                .edit()
                .putString(key, normalizedAnimation)
                .apply();

        return savedToDedicatedPrefs;
    }

    private String getThemeSaveErrorMessage(Throwable throwable) {
        String message = throwable != null ? throwable.getMessage() : null;

        if (message != null && message.contains("409")) {
            return "Тема с таким названием уже существует";
        }

        return "Не удалось сохранить тему";
    }

    private void restartAppAfterThemeChanged() {
        HoneycombRevealView honey =
                new HoneycombRevealView(
                        this
                );

        addContentView(
                honey,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                )
        );

        honey.bringToFront();

        honey.start(
                false,
                () -> ThemeSelectionCoordinator.restartApp(
                        this,
                        MainActivity.class
                )
        );
    }

    private BottomSheetDialog createThemedBottomSheetDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);

        dialog.setOnShowListener(dialogInterface -> {
            Window window = dialog.getWindow();

            if (window != null) {
                window.setDimAmount(0.45f);
                window.setBackgroundDrawableResource(android.R.color.transparent);
            }

            View bottomSheet = dialog.findViewById(
                    com.google.android.material.R.id.design_bottom_sheet
            );

            if (bottomSheet != null) {
                bottomSheet.setAlpha(1f);
                bottomSheet.setBackground(createSheetBackgroundDrawable());
                bottomSheet.setBackgroundTintList(null);
            }
        });

        return dialog;
    }

    private void showColorPicker(
            String title,
            String selectedColor,
            ColorSelectedListener listener
    ) {
        BottomSheetDialog dialog = createThemedBottomSheetDialog();

        LinearLayout root = createBottomSheetRoot();
        TextView titleView = createBottomSheetTitle(title);
        LinearLayout tabs = createTabs();

        TextView paletteTab = createTabText(
                getString(R.string.theme_editor_palette),
                true
        );

        TextView spectrumTab = createTabText(
                getString(R.string.theme_editor_spectrum),
                false
        );

        FrameLayout content = new FrameLayout(this);

        content.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        tabs.addView(paletteTab);
        tabs.addView(spectrumTab);

        root.addView(titleView);
        root.addView(tabs);
        root.addView(content);

        Runnable showPalette = () -> {
            paletteTab.setTextColor(getSheetPrimaryColor());
            spectrumTab.setTextColor(getSheetSecondaryTextColor());

            content.removeAllViews();
            content.addView(createPaletteView(selectedColor, color -> {
                listener.onSelected(color);
                dialog.dismiss();
            }));
        };

        Runnable showSpectrum = () -> {
            paletteTab.setTextColor(getSheetSecondaryTextColor());
            spectrumTab.setTextColor(getSheetPrimaryColor());

            content.removeAllViews();
            content.addView(createSpectrumView(selectedColor, color -> {
                listener.onSelected(color);
                dialog.dismiss();
            }));
        };

        paletteTab.setOnClickListener(v -> showPalette.run());
        spectrumTab.setOnClickListener(v -> showSpectrum.run());

        showPalette.run();

        dialog.setContentView(root);
        dialog.show();
    }

    private void showChatBackgroundPicker() {
        BottomSheetDialog dialog = createThemedBottomSheetDialog();

        LinearLayout root = createBottomSheetRoot();

        TextView title = createBottomSheetTitle(
                getString(R.string.theme_editor_background_picker_title)
        );

        TextView chooseColor = createSheetAction(
                getString(R.string.theme_editor_choose_background_color_action),
                backgroundColor
        );

        TextView chooseImage = createSheetAction(
                getString(R.string.theme_editor_choose_background_image),
                backgroundImageName != null
                        ? backgroundImageName
                        : getString(R.string.theme_editor_default_value)
        );

        TextView removeImage = createSheetAction(
                getString(R.string.theme_editor_remove_background_image),
                ""
        );

        chooseColor.setOnClickListener(v -> {
            dialog.dismiss();

            showColorPicker(
                    getString(R.string.theme_editor_choose_chat_background),
                    backgroundColor,
                    color -> {
                        backgroundColor = color;
                        clearBackgroundImageSelection();
                        updateColorViews();
                        updatePreview();
                    }
            );
        });

        chooseImage.setOnClickListener(v -> {
            pendingBackgroundImageName = null;
            dialog.dismiss();
            backgroundImagePickerLauncher.launch("image/*");
        });

        removeImage.setOnClickListener(v -> {
            clearBackgroundImageSelection();
            updateColorViews();
            updatePreview();
            dialog.dismiss();
        });

        root.addView(title);
        root.addView(chooseColor);
        root.addView(chooseImage);

        if (backgroundImagePath != null && !backgroundImagePath.trim().isEmpty()) {
            root.addView(removeImage);
        }

        dialog.setContentView(root);
        dialog.show();
    }

    private void clearBackgroundImageSelection() {
        backgroundImagePath = null;
        backgroundImageName = null;
        pendingBackgroundImageName = null;
    }

    private void showAnimationPicker() {
        BottomSheetDialog dialog = createThemedBottomSheetDialog();

        LinearLayout root = createBottomSheetRoot();

        TextView title = createBottomSheetTitle(
                getString(R.string.theme_editor_message_animation)
        );

        String[] animations = getResources().getStringArray(
                R.array.theme_editor_message_animations
        );

        String currentAnimationCode = ThemeMessageAnimationBinder.normalize(messageAnimation);

        root.addView(title);

        RadioGroup radioGroup = new RadioGroup(this);
        radioGroup.setOrientation(RadioGroup.VERTICAL);
        radioGroup.setShowDividers(LinearLayout.SHOW_DIVIDER_NONE);

        final int[] checkedId = {-1};

        for (int i = 0; i < animations.length; i++) {
            String animationLabel = animations[i];
            String animationCode = getAnimationCodeByIndex(i, animationLabel);

            RadioButton radioButton = new RadioButton(this);
            radioButton.setId(View.generateViewId());
            radioButton.setTag(animationCode);
            radioButton.setText(animationLabel);
            radioButton.setTextSize(15);
            radioButton.setTextColor(getSheetTextColor());
            radioButton.setButtonTintList(
                    ColorStateList.valueOf(getSheetPrimaryColor())
            );
            radioButton.setPadding(0, dp(8), 0, dp(8));

            if (checkedId[0] == -1 && animationCode.equals(currentAnimationCode)) {
                checkedId[0] = radioButton.getId();
            }

            radioGroup.addView(radioButton);
        }

        if (checkedId[0] != -1) {
            radioGroup.check(checkedId[0]);
        }

        radioGroup.setOnCheckedChangeListener((group, checkedRadioId) -> {
            View selectedView = group.findViewById(checkedRadioId);

            if (!(selectedView instanceof RadioButton)) {
                return;
            }

            Object tag = selectedView.getTag();
            String animationCode = ThemeMessageAnimationBinder.normalize(
                    tag != null ? tag.toString() : null
            );

            messageAnimation = animationCode;
            updateColorViews();
            updatePreview();
            ThemeEditorUiBinder.playPreviewAnimation(
                    this,
                    binding,
                    createEditorState()
            );
            dialog.dismiss();
        });

        root.addView(radioGroup);

        dialog.setContentView(root);
        dialog.show();
    }

    private void showFullScreenPreview() {
        ThemeEditorState state = createEditorState();

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        FrameLayout root = new FrameLayout(this);

        root.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        applyFullPreviewBackground(
                root,
                state
        );

        LinearLayout content = new LinearLayout(this);

        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(
                dp(18),
                dp(48),
                dp(18),
                dp(24)
        );

        content.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        ImageButton closeButton = createFullPreviewCloseButton(dialog);
        TextView title = createFullPreviewTitle();

        View spacer = new View(this);

        spacer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));

        content.addView(
                createFullPreviewTopBar(
                        closeButton,
                        title
                )
        );

        content.addView(spacer);
        content.addView(
                createFullPreviewMessages(state)
        );

        root.addView(content);

        dialog.setContentView(root);

        dialog.setOnShowListener(dialogInterface -> {
            Window shownWindow = dialog.getWindow();

            if (shownWindow != null) {
                shownWindow.setBackgroundDrawable(
                        new ColorDrawable(Color.TRANSPARENT)
                );

                shownWindow.setLayout(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT
                );
            }
        });

        dialog.show();

        Window window = dialog.getWindow();

        if (window != null) {
            window.setBackgroundDrawable(
                    new ColorDrawable(Color.TRANSPARENT)
            );

            window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
            );
        }
    }

    private void applyFullPreviewBackground(
            FrameLayout root,
            ThemeEditorState state
    ) {
        boolean hasImage = state.backgroundImagePath != null
                && !state.backgroundImagePath.trim().isEmpty()
                && new File(state.backgroundImagePath).exists();

        if (hasImage) {
            ImageView imageView = new ImageView(this);

            imageView.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));

            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            root.addView(imageView);

            Glide.with(this)
                    .load(new File(state.backgroundImagePath))
                    .centerCrop()
                    .into(imageView);

            View scrim = new View(this);

            scrim.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));

            scrim.setBackgroundColor(Color.parseColor("#33000000"));

            root.addView(scrim);

            return;
        }

        root.setBackgroundColor(
                parseColorOrDefault(
                        state.backgroundColor,
                        "#FFFFFF"
                )
        );
    }

    private LinearLayout createFullPreviewTopBar(
            ImageButton closeButton,
            TextView title
    ) {
        LinearLayout topBar = new LinearLayout(this);

        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);

        topBar.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        title.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        ));

        View rightSpacer = new View(this);

        rightSpacer.setLayoutParams(new LinearLayout.LayoutParams(
                dp(48),
                dp(48)
        ));

        topBar.addView(closeButton);
        topBar.addView(title);
        topBar.addView(rightSpacer);

        return topBar;
    }

    private ImageButton createFullPreviewCloseButton(Dialog dialog) {
        ImageButton closeButton = new ImageButton(this);

        closeButton.setLayoutParams(new LinearLayout.LayoutParams(
                dp(48),
                dp(48)
        ));

        closeButton.setBackgroundResource(
                android.R.color.transparent
        );

        closeButton.setImageResource(
                R.drawable.ic_arrow_back
        );

        closeButton.setColorFilter(
                parseColorOrDefault(
                        colorOnSurface,
                        "#1A1A1A"
                )
        );

        closeButton.setPadding(
                dp(12),
                dp(12),
                dp(12),
                dp(12)
        );

        closeButton.setOnClickListener(v -> dialog.dismiss());

        return closeButton;
    }

    private TextView createFullPreviewTitle() {
        TextView title = new TextView(this);

        title.setText(R.string.theme_editor_title);
        title.setTextSize(20);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(
                parseColorOrDefault(
                        colorOnSurface,
                        "#1A1A1A"
                )
        );

        return title;
    }

    private LinearLayout createFullPreviewMessages(ThemeEditorState state) {
        LinearLayout messagesRoot = new LinearLayout(this);

        messagesRoot.setOrientation(LinearLayout.VERTICAL);

        messagesRoot.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        messagesRoot.setPadding(
                0,
                dp(12),
                0,
                dp(12)
        );

        LinearLayout otherRow = new LinearLayout(this);

        otherRow.setOrientation(LinearLayout.HORIZONTAL);
        otherRow.setGravity(Gravity.CENTER_VERTICAL);

        View avatar = new View(this);

        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(
                dp(38),
                dp(38)
        );

        avatarParams.setMargins(
                0,
                0,
                dp(10),
                0
        );

        avatar.setLayoutParams(avatarParams);

        ThemeEditorDrawableUtils.setCircleColor(
                avatar,
                state.colorPrimary
        );

        TextView otherMessage = createFullPreviewBubble(
                getString(R.string.theme_editor_preview_other_message),
                state.otherMessageColor,
                true
        );

        otherRow.addView(avatar);
        otherRow.addView(otherMessage);

        TextView otherTime = createFullPreviewTime(
                getString(R.string.theme_editor_preview_time),
                state
        );

        LinearLayout.LayoutParams otherTimeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        otherTimeParams.setMargins(
                dp(48),
                dp(5),
                0,
                0
        );

        otherTime.setLayoutParams(otherTimeParams);

        TextView myMessage = createFullPreviewBubble(
                getString(R.string.theme_editor_preview_my_message),
                state.myMessageColor,
                false
        );

        LinearLayout.LayoutParams myMessageParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        myMessageParams.gravity = Gravity.END;
        myMessageParams.setMargins(
                0,
                dp(16),
                0,
                0
        );

        myMessage.setLayoutParams(myMessageParams);

        TextView myTime = createFullPreviewTime(
                getString(R.string.theme_editor_preview_time_read),
                state
        );

        LinearLayout.LayoutParams myTimeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        myTimeParams.gravity = Gravity.END;
        myTimeParams.setMargins(
                0,
                dp(5),
                dp(4),
                0
        );

        myTime.setLayoutParams(myTimeParams);

        messagesRoot.addView(otherRow);
        messagesRoot.addView(otherTime);
        messagesRoot.addView(myMessage);
        messagesRoot.addView(myTime);

        return messagesRoot;
    }

    private TextView createFullPreviewBubble(
            String text,
            String color,
            boolean isOtherMessage
    ) {
        TextView bubble = new TextView(this);

        bubble.setText(text);
        bubble.setTextSize(15);
        bubble.setPadding(
                dp(14),
                dp(10),
                dp(14),
                dp(10)
        );

        bubble.setBackground(
                ThemeEditorDrawableUtils.createBubbleDrawable(
                        this,
                        color,
                        isOtherMessage
                )
        );

        bubble.setTextColor(
                ThemeEditorDrawableUtils.getContrastColor(color)
        );

        return bubble;
    }

    private TextView createFullPreviewTime(
            String text,
            ThemeEditorState state
    ) {
        TextView time = new TextView(this);

        time.setText(text);
        time.setTextSize(11);
        time.setTextColor(
                ThemeEditorDrawableUtils.parseColor(
                        state.colorOnSurfaceVariant
                )
        );

        return time;
    }

    private String getDisplayName(Uri uri) {
        String result = null;

        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);

                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            }
        }

        if (result == null) {
            result = uri.getLastPathSegment();
        }

        return result != null
                ? result
                : getString(R.string.theme_editor_background_image);
    }

    private void updateColorViews() {
        ThemeEditorUiBinder.updateColorViews(
                binding,
                createEditorState()
        );
    }

    private void updatePreview() {
        ThemeEditorUiBinder.updatePreview(
                this,
                binding,
                createEditorState()
        );
    }

    private ThemeEditorState createEditorState() {
        ThemeEditorState state = new ThemeEditorState();

        state.colorPrimary = colorPrimary;
        state.colorSurface = colorSurface;
        state.colorOnSurface = colorOnSurface;
        state.colorOnSurfaceVariant = colorOnSurfaceVariant;
        state.colorSecondary = colorSecondary;

        state.myMessageColor = myMessageColor;
        state.otherMessageColor = otherMessageColor;
        state.backgroundColor = backgroundColor;

        state.backgroundImagePath = backgroundImagePath;
        state.backgroundImageName = backgroundImageName;
        state.messageAnimation = ThemeMessageAnimationBinder.normalize(messageAnimation);
        state.messageAnimationDisplayName = getAnimationDisplayName(state.messageAnimation);

        return state;
    }

    private LinearLayout createBottomSheetRoot() {
        LinearLayout root = new LinearLayout(this);

        root.setOrientation(LinearLayout.VERTICAL);
        root.setAlpha(1f);
        root.setPadding(dp(20), dp(18), dp(20), dp(18));
        root.setBackground(createSheetBackgroundDrawable());

        return root;
    }

    private TextView createBottomSheetTitle(String text) {
        TextView title = new TextView(this);

        title.setText(text);
        title.setTextSize(18);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(getSheetTextColor());
        title.setPadding(0, 0, 0, dp(14));

        return title;
    }

    private LinearLayout createTabs() {
        LinearLayout tabs = new LinearLayout(this);

        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setGravity(Gravity.CENTER);
        tabs.setPadding(0, 0, 0, dp(12));

        return tabs;
    }

    private TextView createTabText(String text, boolean selected) {
        TextView tab = new TextView(this);

        tab.setText(text);
        tab.setTextSize(14);
        tab.setGravity(Gravity.CENTER);
        tab.setTextColor(
                selected
                        ? getSheetPrimaryColor()
                        : getSheetSecondaryTextColor()
        );
        tab.setPadding(0, dp(8), 0, dp(8));
        tab.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        ));

        return tab;
    }

    private TextView createSheetAction(String title, String subtitle) {
        TextView view = new TextView(this);

        view.setText(
                subtitle == null || subtitle.trim().isEmpty()
                        ? title
                        : title + "\n" + subtitle
        );
        view.setTextSize(16);
        view.setTextColor(getSheetTextColor());
        view.setPadding(0, dp(14), 0, dp(14));

        return view;
    }

    private View createPaletteView(
            String selectedColor,
            ColorSelectedListener listener
    ) {
        GridLayout grid = new GridLayout(this);

        grid.setColumnCount(6);
        grid.setPadding(0, dp(4), 0, dp(12));

        for (String color : PALETTE_COLORS) {
            FrameLayout item = new FrameLayout(this);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();

            params.width = dp(46);
            params.height = dp(46);
            params.setMargins(0, 0, dp(10), dp(10));

            item.setLayoutParams(params);

            View circle = new View(this);

            circle.setLayoutParams(
                    new FrameLayout.LayoutParams(dp(32), dp(32), Gravity.CENTER)
            );
            circle.setBackground(createCircleDrawable(color));

            item.addView(circle);

            if (color.equalsIgnoreCase(selectedColor)) {
                TextView check = new TextView(this);

                check.setText("✓");
                check.setTextColor(Color.WHITE);
                check.setGravity(Gravity.CENTER);
                check.setTextSize(16);
                check.setTypeface(Typeface.DEFAULT_BOLD);
                check.setLayoutParams(
                        new FrameLayout.LayoutParams(dp(32), dp(32), Gravity.CENTER)
                );

                item.addView(check);
            }

            item.setOnClickListener(v -> listener.onSelected(color));

            grid.addView(item);
        }

        return grid;
    }

    private View createSpectrumView(
            String selectedColor,
            ColorSelectedListener listener
    ) {
        LinearLayout root = new LinearLayout(this);

        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, dp(8), 0, dp(12));

        SpectrumView spectrumView = new SpectrumView(this);

        spectrumView.setInitialColor(selectedColor);
        spectrumView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(220)
        ));

        HueSliderView hueSliderView = new HueSliderView(this);

        hueSliderView.setHue(spectrumView.getHue());
        hueSliderView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(28)
        ));

        LinearLayout infoRow = new LinearLayout(this);

        infoRow.setOrientation(LinearLayout.HORIZONTAL);
        infoRow.setGravity(Gravity.CENTER_VERTICAL);
        infoRow.setPadding(0, dp(14), 0, dp(8));

        View colorPreview = new View(this);

        LinearLayout.LayoutParams previewParams =
                new LinearLayout.LayoutParams(dp(28), dp(28));

        previewParams.setMargins(0, 0, dp(12), 0);

        colorPreview.setLayoutParams(previewParams);

        TextView hexText = new TextView(this);

        hexText.setText(spectrumView.getSelectedColor());
        hexText.setTextSize(15);
        hexText.setTextColor(getSheetTextColor());
        hexText.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        ));

        TextView select = createSheetAction(
                getString(R.string.theme_editor_choose),
                ""
        );

        select.setGravity(Gravity.END);
        select.setTextColor(getSheetPrimaryColor());
        select.setTypeface(Typeface.DEFAULT_BOLD);
        select.setPadding(dp(12), dp(8), 0, dp(8));

        final String[] selected = {
                spectrumView.getSelectedColor()
        };

        colorPreview.setBackground(
                createCircleDrawable(selected[0])
        );

        spectrumView.setListener(color -> {
            selected[0] = color;
            hexText.setText(color);
            colorPreview.setBackground(
                    createCircleDrawable(color)
            );
        });

        hueSliderView.setListener(hue -> {
            spectrumView.setHue(hue);
            selected[0] = spectrumView.getSelectedColor();
            hexText.setText(selected[0]);
            colorPreview.setBackground(
                    createCircleDrawable(selected[0])
            );
        });

        select.setOnClickListener(v ->
                listener.onSelected(selected[0])
        );

        infoRow.addView(colorPreview);
        infoRow.addView(hexText);
        infoRow.addView(select);

        root.addView(spectrumView);
        root.addView(hueSliderView);
        root.addView(infoRow);

        return root;
    }

    private GradientDrawable createCircleDrawable(String color) {
        GradientDrawable drawable = new GradientDrawable();

        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(Color.parseColor(color));
        drawable.setStroke(dp(1), getSheetStrokeColor());

        return drawable;
    }

    private GradientDrawable createSheetBackgroundDrawable() {
        GradientDrawable drawable = new GradientDrawable();

        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(getSheetSurfaceColor());
        drawable.setAlpha(255);

        drawable.setCornerRadii(new float[]{
                dp(22), dp(22),
                dp(22), dp(22),
                0, 0,
                0, 0
        });

        return drawable;
    }

    private int getSheetSurfaceColor() {
        if (!isDarkUiMode()) {
            return Color.WHITE;
        }

        int surface = ThemeAttrResolver.resolveColor(this, R.attr.colorSurface);

        surface = makeOpaque(surface);

        if (isLightColor(surface)) {
            return Color.parseColor("#2B302F");
        }

        if (isVeryDarkColor(surface)) {
            return blendColors(surface, Color.WHITE, 0.14f);
        }

        return surface;
    }

    private int getSheetTextColor() {
        if (!isDarkUiMode()) {
            return Color.parseColor("#1A1A1A");
        }

        int color = ThemeAttrResolver.resolveColor(this, R.attr.colorOnSurface);
        color = makeOpaque(color);

        if (isVeryDarkColor(color)) {
            return Color.WHITE;
        }

        return color;
    }

    private int getSheetSecondaryTextColor() {
        if (!isDarkUiMode()) {
            return Color.parseColor("#666666");
        }

        int color = ThemeAttrResolver.resolveColor(this, R.attr.colorOnSurfaceVariant);
        color = makeOpaque(color);

        if (isVeryDarkColor(color)) {
            return Color.parseColor("#B8C2C0");
        }

        return color;
    }

    private int getSheetPrimaryColor() {
        return makeOpaque(
                ThemeAttrResolver.resolveColor(this, R.attr.colorPrimary)
        );
    }

    private int getSheetStrokeColor() {
        if (!isDarkUiMode()) {
            return Color.parseColor("#DDDDDD");
        }

        return blendColors(
                getSheetSurfaceColor(),
                Color.WHITE,
                0.24f
        );
    }

    private boolean isDarkUiMode() {
        int nightMode = getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;

        return nightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    private boolean isLightColor(int color) {
        double darkness =
                1.0 - (
                        0.299 * Color.red(color)
                                + 0.587 * Color.green(color)
                                + 0.114 * Color.blue(color)
                ) / 255.0;

        return darkness < 0.35;
    }

    private boolean isVeryDarkColor(int color) {
        double darkness =
                1.0 - (
                        0.299 * Color.red(color)
                                + 0.587 * Color.green(color)
                                + 0.114 * Color.blue(color)
                ) / 255.0;

        return darkness > 0.82;
    }

    private int blendColors(int baseColor, int overlayColor, float ratio) {
        baseColor = makeOpaque(baseColor);
        overlayColor = makeOpaque(overlayColor);

        float inverseRatio = 1f - ratio;

        int red = Math.round(
                Color.red(baseColor) * inverseRatio
                        + Color.red(overlayColor) * ratio
        );

        int green = Math.round(
                Color.green(baseColor) * inverseRatio
                        + Color.green(overlayColor) * ratio
        );

        int blue = Math.round(
                Color.blue(baseColor) * inverseRatio
                        + Color.blue(overlayColor) * ratio
        );

        return Color.rgb(red, green, blue);
    }

    private int makeOpaque(int color) {
        return Color.rgb(
                Color.red(color),
                Color.green(color),
                Color.blue(color)
        );
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private interface ColorSelectedListener {
        void onSelected(String color);
    }

    private interface HueSelectedListener {
        void onSelected(float hue);
    }

    private static class SpectrumView extends View {

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private Bitmap bitmap;
        private float hue = 174f;
        private float saturation = 0.8f;
        private float value = 0.64f;
        private String selectedColor = "#20A39A";
        private ColorSelectedListener listener;

        SpectrumView(Context context) {
            super(context);
        }

        private void setInitialColor(String color) {
            try {
                int parsedColor = Color.parseColor(color);
                float[] hsv = new float[3];

                Color.colorToHSV(parsedColor, hsv);

                hue = hsv[0];
                saturation = hsv[1];
                value = hsv[2];
                selectedColor = toHexColor();
            } catch (Exception ex) {
                selectedColor = "#20A39A";
            }

            bitmap = null;
            invalidate();
        }

        private void setListener(ColorSelectedListener listener) {
            this.listener = listener;
        }

        private float getHue() {
            return hue;
        }

        private void setHue(float hue) {
            this.hue = Math.max(0f, Math.min(360f, hue));
            bitmap = null;
            selectedColor = toHexColor();

            if (listener != null) {
                listener.onSelected(selectedColor);
            }

            invalidate();
        }

        private String getSelectedColor() {
            return selectedColor;
        }

        @Override
        protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            super.onSizeChanged(width, height, oldWidth, oldHeight);
            bitmap = null;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            if (getWidth() <= 0 || getHeight() <= 0) {
                return;
            }

            if (bitmap == null) {
                bitmap = createSpectrumBitmap(getWidth(), getHeight());
            }

            canvas.drawBitmap(bitmap, 0, 0, null);

            float markerX = saturation * getWidth();
            float markerY = (1f - value) * getHeight();

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(markerX, markerY, 13, paint);

            paint.setStrokeWidth(2);
            paint.setColor(Color.BLACK);
            canvas.drawCircle(markerX, markerY, 16, paint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN
                    || event.getAction() == MotionEvent.ACTION_MOVE) {

                float x = Math.max(0f, Math.min(event.getX(), getWidth()));
                float y = Math.max(0f, Math.min(event.getY(), getHeight()));

                saturation = getWidth() == 0 ? 0f : x / getWidth();
                value = getHeight() == 0 ? 1f : 1f - y / getHeight();

                saturation = Math.max(0f, Math.min(1f, saturation));
                value = Math.max(0f, Math.min(1f, value));

                selectedColor = toHexColor();

                if (listener != null) {
                    listener.onSelected(selectedColor);
                }

                invalidate();
                return true;
            }

            return true;
        }

        private Bitmap createSpectrumBitmap(int width, int height) {
            Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            for (int x = 0; x < width; x++) {
                float currentSaturation = x / (float) width;

                for (int y = 0; y < height; y++) {
                    float currentValue = 1f - y / (float) height;

                    int color = Color.HSVToColor(
                            new float[]{
                                    hue,
                                    currentSaturation,
                                    currentValue
                            }
                    );

                    result.setPixel(x, y, color);
                }
            }

            return result;
        }

        private String toHexColor() {
            int color = Color.HSVToColor(
                    new float[]{
                            hue,
                            saturation,
                            value
                    }
            );

            return String.format(Locale.US, "#%06X", 0xFFFFFF & color);
        }
    }

    private static class HueSliderView extends View {

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private Bitmap bitmap;
        private float hue = 174f;
        private HueSelectedListener listener;

        HueSliderView(Context context) {
            super(context);
        }

        private void setListener(HueSelectedListener listener) {
            this.listener = listener;
        }

        private void setHue(float hue) {
            this.hue = Math.max(0f, Math.min(360f, hue));
            invalidate();
        }

        @Override
        protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            super.onSizeChanged(width, height, oldWidth, oldHeight);
            bitmap = null;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            if (getWidth() <= 0 || getHeight() <= 0) {
                return;
            }

            if (bitmap == null) {
                bitmap = createHueBitmap(getWidth(), getHeight());
            }

            canvas.drawBitmap(bitmap, 0, 0, null);

            float markerX = hue / 360f * getWidth();

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(markerX, getHeight() / 2f, getHeight() / 2.3f, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);
            paint.setColor(Color.parseColor("#666666"));
            canvas.drawCircle(markerX, getHeight() / 2f, getHeight() / 2.3f, paint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN
                    || event.getAction() == MotionEvent.ACTION_MOVE) {

                float x = Math.max(0f, Math.min(event.getX(), getWidth()));

                hue = getWidth() == 0
                        ? 0f
                        : x / getWidth() * 360f;

                if (listener != null) {
                    listener.onSelected(hue);
                }

                invalidate();
                return true;
            }

            return true;
        }

        private Bitmap createHueBitmap(int width, int height) {
            Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            for (int x = 0; x < width; x++) {
                float currentHue = x * 360f / width;

                int color = Color.HSVToColor(
                        new float[]{
                                currentHue,
                                1f,
                                1f
                        }
                );

                for (int y = 0; y < height; y++) {
                    result.setPixel(x, y, color);
                }
            }

            return result;
        }
    }
}
