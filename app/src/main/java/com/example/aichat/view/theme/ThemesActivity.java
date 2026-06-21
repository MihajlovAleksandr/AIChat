package com.example.aichat.view.theme;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.aichat.controller.ThemeController;
import com.example.aichat.databinding.ActivityThemesBinding;
import com.example.aichat.dto.response.ThemeContentResponse;
import com.example.aichat.dto.response.ThemeResponse;
import com.example.aichat.model.connection.ConnectionSingleton;
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
import com.example.aichat.view.theme.binders.ThemeMessageAnimationBinder;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@androidx.media3.common.util.UnstableApi
public class ThemesActivity extends BaseActivity {

    private static final String DEFAULT_MESSAGE_ANIMATION = "default";
    private static final String THEME_PREFS = "themes_storage";
    private static final String THEME_ANIMATION_PREFIX = "theme_animation_";
    private static final String HIDDEN_LIBRARY_THEMES_KEY = "hidden_library_theme_ids";
    private static final UUID STANDARD_LIGHT_THEME_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID STANDARD_DARK_THEME_ID = UUID.fromString("00000000-0000-0000-0000-000000000102");

    private ActivityThemesBinding binding;
    private ThemeStorage themeStorage;
    private ThemeListAdapter myThemesAdapter;
    private ThemeListAdapter libraryThemesAdapter;
    private ThemeController controller;
    private ActivityResultLauncher<Intent> themeEditorLauncher;
    private SharedPreferences prefs;
    private boolean isRestartingWithReveal = false;
    private final ThemeMapper mapper = new ThemeMapper();

    private final Set<String> currentMyThemeIds = new HashSet<>();
    private List<ThemeModel> currentMyThemes = new ArrayList<>();
    private List<ThemeModel> currentLibraryThemes = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        themeStorage = new ThemeStorage(this);
        prefs = getSharedPreferences(
                ThemeSelectionCoordinator.SETTINGS_PREFS,
                MODE_PRIVATE
        );

        ThemeSelectionCoordinator.migrateLegacyStandardThemeSelection(this);
        applyThemeFromSettings();

        binding = ActivityThemesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applySafeTopInsets();

        controller = new ThemeController(ConnectionSingleton.getInstance().getConnectionDispatcher());

        initThemeEditorLauncher();
        setupBackButton();
        setupCreateThemeButton();
        setupRecyclerViews();
        setupBackPressedHandler();

        applyScreenTheme();
        loadThemes();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isRestartingWithReveal) {
            return;
        }

        applyThemeFromSettings();
        loadThemes();
        applyScreenTheme();
    }

    private void initThemeEditorLauncher() {
        themeEditorLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    loadThemes();
                    applyScreenTheme();
                }
        );
    }

    private void applyThemeFromSettings() {
        ThemeModel selectedCustomTheme = getSelectedCustomTheme();

        if (selectedCustomTheme != null) {
            ThemeSelectionCoordinator.applyNightMode(
                    ThemeSelectionCoordinator.THEME_LIGHT
            );

            ThemeAttrResolver.applyTheme(selectedCustomTheme);
            return;
        }

        ThemeAttrResolver.clear();

        String mode = prefs != null
                ? prefs.getString(
                ThemeSelectionCoordinator.KEY_APP_THEME,
                ThemeSelectionCoordinator.THEME_SYSTEM
        )
                : ThemeSelectionCoordinator.THEME_SYSTEM;

        ThemeSelectionCoordinator.applyNightMode(mode);
    }

    @Nullable
    private ThemeModel getSelectedCustomTheme() {
        String selectedCustomThemeId = getSelectedCustomThemeIdRaw();

        if (selectedCustomThemeId == null) {
            return null;
        }

        return themeStorage.getThemeById(selectedCustomThemeId);
    }

    @Nullable
    private String getSelectedCustomThemeId() {
        return getSelectedCustomThemeIdRaw();
    }

    @Nullable
    private String getSelectedThemeIdForList() {
        String customThemeId = getSelectedCustomThemeIdRaw();

        if (customThemeId != null) {
            return customThemeId;
        }

        String mode = prefs != null
                ? prefs.getString(
                ThemeSelectionCoordinator.KEY_APP_THEME,
                ThemeSelectionCoordinator.THEME_SYSTEM
        )
                : ThemeSelectionCoordinator.THEME_SYSTEM;

        if (ThemeSelectionCoordinator.THEME_LIGHT.equals(mode)) {
            return STANDARD_LIGHT_THEME_ID.toString();
        }

        if (ThemeSelectionCoordinator.THEME_DARK.equals(mode)) {
            return STANDARD_DARK_THEME_ID.toString();
        }

        return null;
    }

    @Nullable
    private String getSelectedCustomThemeIdRaw() {
        String coordinatorThemeId = ThemeSelectionCoordinator.getSelectedCustomThemeId(this);

        if (isExistingLocalThemeId(coordinatorThemeId)) {
            return coordinatorThemeId;
        }

        String currentSetting = prefs != null
                ? prefs.getString(
                ThemeSelectionCoordinator.KEY_APP_THEME,
                ThemeSelectionCoordinator.THEME_SYSTEM
        )
                : null;

        if (currentSetting != null
                && !currentSetting.trim().isEmpty()
                && !ThemeSelectionCoordinator.isDefaultThemeMode(currentSetting)
                && isExistingLocalThemeId(currentSetting)) {
            return currentSetting;
        }

        return null;
    }

    private boolean isExistingLocalThemeId(@Nullable String themeId) {
        if (themeId == null || themeId.trim().isEmpty()) {
            return false;
        }

        return themeStorage.getThemeById(themeId.trim()) != null;
    }

    private void applySafeTopInsets() {
        if (binding == null || binding.topBar == null) {
            return;
        }

        final int initialTopPadding = binding.topBar.getPaddingTop();
        final int initialLeftPadding = binding.topBar.getPaddingLeft();
        final int initialRightPadding = binding.topBar.getPaddingRight();
        final int initialBottomPadding = binding.topBar.getPaddingBottom();
        final int minimumTopInset = dp(18);

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            Insets systemInsets = insets.getInsets(
                    WindowInsetsCompat.Type.statusBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );

            int topInset = Math.max(systemInsets.top, minimumTopInset);

            binding.topBar.setPadding(
                    initialLeftPadding,
                    initialTopPadding + topInset,
                    initialRightPadding,
                    initialBottomPadding
            );

            return insets;
        });

        ViewCompat.requestApplyInsets(binding.getRoot());
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void setupBackButton() {
        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void setupCreateThemeButton() {
        binding.cardCreateTheme.setOnClickListener(v -> {
            Intent intent = new Intent(this, ThemeEditorActivity.class);
            themeEditorLauncher.launch(intent);
        });
    }

    private void setupRecyclerViews() {
        myThemesAdapter = new ThemeListAdapter(
                new ArrayList<>(),
                getSelectedThemeIdForList(),
                true,
                new ThemeListAdapter.ThemeActionListener() {
                    @Override
                    public void onThemeClick(ThemeModel theme) {
                        applyTheme(theme);
                    }

                    @Override
                    public void onThemeMenuClick(View anchor, ThemeModel theme) {
                        showThemeMenu(anchor, theme);
                    }
                }
        );

        libraryThemesAdapter = new ThemeListAdapter(
                new ArrayList<>(),
                getSelectedThemeIdForList(),
                false,
                new ThemeListAdapter.ThemeActionListener() {
                    @Override
                    public void onThemeClick(ThemeModel theme) {
                        applyTheme(theme);
                    }

                    @Override
                    public void onThemeMenuClick(View anchor, ThemeModel theme) {
                        showThemeMenu(anchor, theme);
                    }
                }
        );

        binding.rvMyThemes.setLayoutManager(new LinearLayoutManager(this));
        binding.rvLibraryThemes.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMyThemes.setAdapter(myThemesAdapter);
        binding.rvLibraryThemes.setAdapter(libraryThemesAdapter);
        binding.rvMyThemes.setBackgroundColor(Color.TRANSPARENT);
        binding.rvLibraryThemes.setBackgroundColor(Color.TRANSPARENT);
    }

    private void loadThemes() {
        String selectedThemeId = getSelectedThemeIdForList();

        myThemesAdapter.setSelectedThemeId(selectedThemeId);
        libraryThemesAdapter.setSelectedThemeId(selectedThemeId);

        List<ThemeModel> localMyThemes = deduplicateThemes(themeStorage.getMyThemes());
        List<ThemeModel> localLibraryThemes = buildLibraryThemeList(currentLibraryThemes);
        applyThemeLists(localMyThemes, localLibraryThemes, selectedThemeId);

        controller.getThemes()
                .thenAccept(responses -> {
                    List<ThemeModel> remoteThemes = mapResponses(responses);
                    List<ThemeModel> visibleRemoteThemes = filterHiddenThemes(remoteThemes);

                    LinkedHashMap<String, ThemeModel> myMap = new LinkedHashMap<>();

                    for (ThemeModel theme : localMyThemes) {
                        putTheme(myMap, theme);
                    }

                    Set<String> myIds = collectIds(new ArrayList<>(myMap.values()));

                    LinkedHashMap<String, ThemeModel> libraryMap = new LinkedHashMap<>();

                    for (ThemeModel systemTheme : getBuiltInSystemThemes()) {
                        putTheme(libraryMap, systemTheme);
                    }

                    for (ThemeModel remoteTheme : visibleRemoteThemes) {
                        if (remoteTheme == null || remoteTheme.getId() == null) {
                            continue;
                        }

                        if (isBuiltInSystemTheme(remoteTheme)) {
                            continue;
                        }

                        String remoteId = remoteTheme.getId().toString();

                        if (myIds.contains(remoteId)) {
                            ThemeModel existingLocalTheme = themeStorage.getThemeById(remoteId);

                            if (existingLocalTheme != null) {
                                themeStorage.updateTheme(remoteTheme);
                                putTheme(myMap, remoteTheme);
                            }

                        } else {
                            putTheme(libraryMap, remoteTheme);
                        }
                    }

                    List<ThemeModel> myThemes = new ArrayList<>(myMap.values());
                    List<ThemeModel> libraryThemes = new ArrayList<>(libraryMap.values());

                    runOnUiThread(() -> applyThemeLists(
                            myThemes,
                            libraryThemes,
                            getSelectedThemeIdForList()
                    ));
                })
                .exceptionally(throwable -> {
                    runOnUiThread(() -> applyThemeLists(
                            localMyThemes,
                            buildLibraryThemeList(currentLibraryThemes),
                            getSelectedThemeIdForList()
                    ));

                    return null;
                });
    }

    private void applyThemeLists(
            List<ThemeModel> myThemes,
            List<ThemeModel> libraryThemes,
            @Nullable String selectedThemeId
    ) {
        currentMyThemes = deduplicateThemes(myThemes);
        currentLibraryThemes = deduplicateThemes(libraryThemes);

        currentMyThemeIds.clear();
        currentMyThemeIds.addAll(collectIds(currentMyThemes));

        myThemesAdapter.setSelectedThemeId(selectedThemeId);
        libraryThemesAdapter.setSelectedThemeId(selectedThemeId);

        myThemesAdapter.submitList(currentMyThemes);
        libraryThemesAdapter.submitList(currentLibraryThemes);

        binding.rvMyThemes.setVisibility(
                currentMyThemes.isEmpty() ? View.GONE : View.VISIBLE
        );

        binding.rvLibraryThemes.setVisibility(
                currentLibraryThemes.isEmpty() ? View.GONE : View.VISIBLE
        );
    }

    private List<ThemeModel> buildLibraryThemeList(@Nullable List<ThemeModel> source) {
        LinkedHashMap<String, ThemeModel> result = new LinkedHashMap<>();

        for (ThemeModel systemTheme : getBuiltInSystemThemes()) {
            putTheme(result, systemTheme);
        }

        if (source != null) {
            Set<String> hiddenIds = getHiddenLibraryThemeIds();

            for (ThemeModel theme : source) {
                if (theme == null || theme.getId() == null) {
                    continue;
                }

                if (isBuiltInSystemTheme(theme)) {
                    continue;
                }

                if (!hiddenIds.contains(theme.getId().toString())) {
                    putTheme(result, theme);
                }
            }
        }

        return new ArrayList<>(result.values());
    }

    private List<ThemeModel> getBuiltInSystemThemes() {
        List<ThemeModel> themes = new ArrayList<>();

        themes.add(new ThemeModel(
                STANDARD_LIGHT_THEME_ID,
                getString(R.string.theme_light),
                ThemeType.System,
                "#20A39A",
                "#FFFFFF",
                "#16191C",
                "#70757A",
                "#20A39A",
                "#20A39A",
                "#F1F1F1",
                "#FFFFFF"
        ));

        themes.add(new ThemeModel(
                STANDARD_DARK_THEME_ID,
                getString(R.string.theme_dark),
                ThemeType.System,
                "#20A39A",
                "#243D3E",
                "#FFFFFF",
                "#B8C0C0",
                "#20A39A",
                "#20A39A",
                "#243D3E",
                "#001F20"
        ));

        return themes;
    }

    private List<ThemeModel> filterHiddenThemes(List<ThemeModel> source) {
        Set<String> hiddenIds = getHiddenLibraryThemeIds();

        if (hiddenIds.isEmpty()) {
            return source;
        }

        List<ThemeModel> result = new ArrayList<>();

        for (ThemeModel theme : source) {
            if (theme == null || theme.getId() == null) {
                continue;
            }

            if (isBuiltInSystemTheme(theme) || !hiddenIds.contains(theme.getId().toString())) {
                result.add(theme);
            }
        }

        return result;
    }

    private Set<String> getHiddenLibraryThemeIds() {
        Set<String> result = new HashSet<>();

        if (prefs == null) {
            return result;
        }

        String rawValue = prefs.getString(HIDDEN_LIBRARY_THEMES_KEY, "");

        if (rawValue == null || rawValue.trim().isEmpty()) {
            return result;
        }

        String[] ids = rawValue.split(",");

        for (String id : ids) {
            if (id != null && !id.trim().isEmpty()) {
                result.add(id.trim());
            }
        }

        return result;
    }

    private void addHiddenLibraryThemeId(String themeId) {
        if (prefs == null || themeId == null || themeId.trim().isEmpty()) {
            return;
        }

        Set<String> ids = getHiddenLibraryThemeIds();
        ids.add(themeId.trim());

        prefs.edit()
                .putString(HIDDEN_LIBRARY_THEMES_KEY, joinIds(ids))
                .apply();
    }

    private void removeHiddenLibraryThemeId(String themeId) {
        if (prefs == null || themeId == null || themeId.trim().isEmpty()) {
            return;
        }

        Set<String> ids = getHiddenLibraryThemeIds();
        ids.remove(themeId.trim());

        prefs.edit()
                .putString(HIDDEN_LIBRARY_THEMES_KEY, joinIds(ids))
                .apply();
    }

    private String joinIds(Set<String> ids) {
        StringBuilder builder = new StringBuilder();

        for (String id : ids) {
            if (id == null || id.trim().isEmpty()) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append(',');
            }

            builder.append(id.trim());
        }

        return builder.toString();
    }

    private List<ThemeModel> mapResponses(@Nullable ThemeResponse[] responses) {
        List<ThemeModel> result = new ArrayList<>();

        if (responses == null) {
            return result;
        }

        for (ThemeResponse response : responses) {
            if (response == null) {
                continue;
            }

            try {
                ThemeModel themeModel = mapper.ToDTO(response);

                if (themeModel != null && themeModel.getId() != null) {
                    result.add(themeModel);
                }
            } catch (Exception ignored) {
            }
        }

        return deduplicateThemes(result);
    }

    private List<ThemeModel> deduplicateThemes(@Nullable List<ThemeModel> source) {
        LinkedHashMap<String, ThemeModel> result = new LinkedHashMap<>();

        if (source == null) {
            return new ArrayList<>();
        }

        for (ThemeModel model : source) {
            putTheme(result, model);
        }

        return new ArrayList<>(result.values());
    }

    private void putTheme(LinkedHashMap<String, ThemeModel> map, @Nullable ThemeModel theme) {
        if (theme == null || theme.getId() == null) {
            return;
        }

        map.put(theme.getId().toString(), theme);
    }

    private Set<String> collectIds(@Nullable List<ThemeModel> themes) {
        Set<String> result = new HashSet<>();

        if (themes == null) {
            return result;
        }

        for (ThemeModel theme : themes) {
            if (theme != null && theme.getId() != null) {
                result.add(theme.getId().toString());
            }
        }

        return result;
    }

    private boolean isMyTheme(@Nullable ThemeModel theme) {
        return theme != null
                && theme.getId() != null
                && currentMyThemeIds.contains(theme.getId().toString());
    }

    private void upsertThemes(@Nullable List<ThemeModel> themes) {
        if (themes == null) {
            return;
        }

        for (ThemeModel theme : themes) {
            upsertTheme(theme);
        }
    }

    private void upsertTheme(@Nullable ThemeModel theme) {
        if (theme == null || theme.getId() == null) {
            return;
        }

        ThemeModel existing = themeStorage.getThemeById(theme.getId().toString());

        removeHiddenLibraryThemeId(theme.getId().toString());

        if (existing == null) {
            themeStorage.addTheme(theme);
        } else {
            themeStorage.updateTheme(theme);
        }
    }

    private void applyTheme(ThemeModel theme) {
        if (theme == null || theme.getId() == null) {
            Toast.makeText(this, R.string.theme_not_found, Toast.LENGTH_SHORT).show();
            return;
        }

        if (isBuiltInSystemTheme(theme)) {
            String mode = STANDARD_DARK_THEME_ID.equals(theme.getId())
                    ? ThemeSelectionCoordinator.THEME_DARK
                    : ThemeSelectionCoordinator.THEME_LIGHT;

            restartAppWithReveal(() ->
                    ThemeSelectionCoordinator.saveDefaultThemeOnly(this, mode)
            );
            return;
        }

        String themeId = theme.getId().toString();
        String selectedThemeId = getSelectedThemeIdForList();

        if (selectedThemeId != null && selectedThemeId.equals(themeId)) {
            return;
        }

        upsertTheme(theme);
        saveThemeAnimation(theme.getId(), resolveAnimationForTheme(theme));
        themeStorage.setSelectedThemeId(themeId);

        myThemesAdapter.setSelectedThemeId(themeId);
        libraryThemesAdapter.setSelectedThemeId(themeId);

        if (!isBuiltInSystemTheme(theme)) {
            controller.selectTheme(theme.getId());
        }

        Toast.makeText(
                this,
                getString(R.string.theme_applied, theme.getName()),
                Toast.LENGTH_SHORT
        ).show();

        restartAppWithReveal(() ->
                ThemeSelectionCoordinator.saveCustomThemeOnly(this, themeId)
        );
    }

    private boolean isBuiltInSystemTheme(@Nullable ThemeModel theme) {
        return theme != null
                && theme.getId() != null
                && (STANDARD_LIGHT_THEME_ID.equals(theme.getId())
                || STANDARD_DARK_THEME_ID.equals(theme.getId()));
    }

    private void showThemeMenu(View anchor, ThemeModel theme) {
        PopupMenu popupMenu = new PopupMenu(this, anchor);

        popupMenu.getMenu().add(R.string.theme_menu_apply);

        if (isBuiltInSystemTheme(theme)) {
        } else if (isMyTheme(theme)) {
            popupMenu.getMenu().add(R.string.theme_menu_edit);
            popupMenu.getMenu().add(R.string.theme_menu_duplicate);
            popupMenu.getMenu().add(R.string.theme_menu_delete);
        } else {
            popupMenu.getMenu().add(R.string.theme_menu_duplicate);
        }

        popupMenu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();

            if (title.equals(getString(R.string.theme_menu_apply))) {
                applyTheme(theme);
                return true;
            }

            if (title.equals(getString(R.string.theme_menu_edit))) {
                openThemeEditor(theme);
                return true;
            }

            if (title.equals(getString(R.string.theme_menu_duplicate))) {
                duplicateTheme(theme);
                return true;
            }

            if (title.equals(getString(R.string.theme_menu_delete))) {
                confirmDeleteTheme(theme);
                return true;
            }

            return false;
        });

        popupMenu.show();
    }

    private void openThemeEditor(ThemeModel theme) {
        if (theme == null || theme.getId() == null) {
            return;
        }

        Intent intent = new Intent(this, ThemeEditorActivity.class);
        intent.putExtra(ThemeEditorActivity.EXTRA_THEME_ID, theme.getId().toString());
        themeEditorLauncher.launch(intent);
    }

    private void duplicateTheme(ThemeModel theme) {
        if (theme == null) {
            return;
        }

        ThemeModel copy = theme.copyAsUserTheme(getString(R.string.theme_copy_suffix));
        String sourceAnimation = resolveAnimationForTheme(theme);

        controller.sendTheme(copy.getName(), createServerSafeThemeContentJson(copy))
                .thenAccept(response -> {
                    ThemeModel model = mapper.ToDTO(response);

                    if (model != null && model.getId() != null) {
                        model.setMessageAnimation(sourceAnimation);
                        saveThemeAnimation(model.getId(), sourceAnimation);
                        upsertTheme(model);
                    }

                    runOnUiThread(() -> {
                        Toast.makeText(
                                this,
                                R.string.theme_duplicated,
                                Toast.LENGTH_SHORT
                        ).show();

                        loadThemes();
                    });
                })
                .exceptionally(throwable -> {
                    runOnUiThread(() -> Toast.makeText(
                            this,
                            "Не удалось создать копию темы",
                            Toast.LENGTH_SHORT
                    ).show());
                    return null;
                });
    }

    private String createServerSafeThemeContentJson(ThemeModel theme) {
        return JsonHelper.Serialize(
                new ThemeContentResponse(
                        theme.getColorPrimary(),
                        theme.getColorSurface(),
                        theme.getColorOnSurface(),
                        theme.getColorOnSurfaceVariant(),
                        theme.getColorSecondary(),
                        theme.getMyMessageColor(),
                        theme.getOtherMessageColor(),
                        theme.getBackgroundColor(),
                        theme.getBackgroundImagePath(),
                        theme.getBackgroundImageName(),
                        ThemeMessageAnimationBinder.normalize(theme.getMessageAnimation())
                )
        );
    }

    private String resolveAnimationForTheme(ThemeModel theme) {
        if (theme == null || theme.getId() == null) {
            return DEFAULT_MESSAGE_ANIMATION;
        }

        String savedAnimation = getSharedPreferences(THEME_PREFS, MODE_PRIVATE)
                .getString(
                        THEME_ANIMATION_PREFIX + theme.getId(),
                        null
                );

        if (savedAnimation != null && !savedAnimation.trim().isEmpty()) {
            return ThemeMessageAnimationBinder.normalize(savedAnimation);
        }

        return ThemeMessageAnimationBinder.normalize(theme.getMessageAnimation());
    }

    private void saveThemeAnimation(UUID themeId, String animation) {
        if (themeId == null) {
            return;
        }

        String normalizedAnimation = ThemeMessageAnimationBinder.normalize(animation);

        getSharedPreferences(THEME_PREFS, MODE_PRIVATE)
                .edit()
                .putString(THEME_ANIMATION_PREFIX + themeId, normalizedAnimation)
                .apply();
    }

    private void confirmDeleteTheme(ThemeModel theme) {
        if (theme == null) {
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.theme_delete_title)
                .setMessage(getString(R.string.theme_delete_message, theme.getName()))
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteTheme(theme))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteTheme(ThemeModel theme) {
        if (theme == null || theme.getId() == null) {
            return;
        }

        if (isBuiltInSystemTheme(theme)) {
            return;
        }

        if (!isMyTheme(theme)) {
            addHiddenLibraryThemeId(theme.getId().toString());
            loadThemes();
            return;
        }

        String selectedThemeId = getSelectedCustomThemeIdRaw();

        if (selectedThemeId != null && selectedThemeId.equals(theme.getId().toString())) {
            Toast.makeText(
                    this,
                    "Тема используется на этом устройстве. Сначала выберите другую тему.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        deleteThemeFromServer(theme);
    }

    private void deleteThemeFromServer(ThemeModel theme) {
        controller.deleteTheme(theme.getId())
                .thenAccept(isComplete -> {
                    if (isComplete) {
                        themeStorage.removeTheme(theme.getId().toString());
                        removeHiddenLibraryThemeId(theme.getId().toString());
                        clearThemeAnimation(theme.getId());

                        runOnUiThread(() -> {
                            Toast.makeText(
                                    this,
                                    R.string.theme_deleted,
                                    Toast.LENGTH_SHORT
                            ).show();

                            loadThemes();
                            applyScreenTheme();
                        });

                        return;
                    }

                    showThemeInUseMessage();
                })
                .exceptionally(throwable -> {
                    showThemeInUseMessage();
                    return null;
                });
    }

    private void showThemeInUseMessage() {
        runOnUiThread(() -> Toast.makeText(
                this,
                "Тема используется на одном из устройств. Сначала смените её на всех устройствах.",
                Toast.LENGTH_LONG
        ).show());
    }

    private void clearThemeAnimation(UUID themeId) {
        if (themeId == null) {
            return;
        }

        getSharedPreferences(THEME_PREFS, MODE_PRIVATE)
                .edit()
                .remove(THEME_ANIMATION_PREFIX + themeId)
                .apply();
    }

    @Nullable
    private ThemeModel findFallbackThemeForServerSelection(String deletingThemeId) {
        for (ThemeModel theme : currentMyThemes) {
            if (theme != null
                    && theme.getId() != null
                    && !theme.getId().toString().equals(deletingThemeId)) {
                return theme;
            }
        }

        for (ThemeModel theme : currentLibraryThemes) {
            if (theme != null
                    && theme.getId() != null
                    && !theme.getId().toString().equals(deletingThemeId)) {
                return theme;
            }
        }

        return null;
    }

    private void applyScreenTheme() {
        int surface = ThemeAttrResolver.resolveColor(this, R.attr.colorSurface);
        int onSurface = ThemeAttrResolver.resolveColor(this, R.attr.colorOnSurface);
        int onSurfaceVariant = ThemeAttrResolver.resolveColor(this, R.attr.colorOnSurfaceVariant);

        boolean hasRuntimeTheme = ThemeAttrResolver.hasRuntimeTheme()
                && ThemeAttrResolver.getCurrentTheme() != null;

        if (hasRuntimeTheme) {
            int background = ThemeAttrResolver.resolveColor(
                    this,
                    android.R.attr.colorBackground
            );

            binding.getRoot().setBackgroundColor(background);
        } else {
            applyWindowBackground(binding.getRoot());
        }

        binding.topBar.setBackgroundColor(Color.TRANSPARENT);
        binding.rvMyThemes.setBackgroundColor(Color.TRANSPARENT);
        binding.rvLibraryThemes.setBackgroundColor(Color.TRANSPARENT);
        binding.btnBack.setImageTintList(ColorStateList.valueOf(onSurface));
        binding.tvTitle.setTextColor(onSurface);
        binding.tvMyThemes.setTextColor(onSurface);
        binding.tvLibrary.setTextColor(onSurface);
        binding.cardCreateTheme.setCardBackgroundColor(surface);

        myThemesAdapter.setThemeColors(surface, onSurface, onSurfaceVariant);
        libraryThemesAdapter.setThemeColors(surface, onSurface, onSurfaceVariant);
    }

    private void applyWindowBackground(View view) {
        TypedValue typedValue = new TypedValue();

        getTheme().resolveAttribute(
                android.R.attr.windowBackground,
                typedValue,
                true
        );

        if (typedValue.resourceId != 0) {
            view.setBackgroundResource(typedValue.resourceId);
        } else {
            view.setBackgroundColor(typedValue.data);
        }
    }

    private void restartAppWithReveal() {
        restartAppWithReveal(null);
    }

    private void restartAppWithReveal(@Nullable Runnable beforeRestart) {
        if (isRestartingWithReveal) {
            return;
        }

        isRestartingWithReveal = true;

        HoneycombRevealView honey = new HoneycombRevealView(this);

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
                () -> {
                    if (beforeRestart != null) {
                        beforeRestart.run();
                    }

                    ThemeSelectionCoordinator.restartApp(this, MainActivity.class);
                }
        );
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
}
