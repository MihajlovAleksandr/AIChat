package com.example.aichat.model.utils.theme;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.aichat.model.entities.ThemeType;
import com.example.aichat.view.theme.binders.ThemeMessageAnimationBinder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ThemeStorage {

    private static final String PREFS = "themes_storage";
    private static final String KEY_THEMES = "themes";
    private static final String KEY_SELECTED_THEME_ID = "selected_theme_id";
    private static final String KEY_ANIMATION_PREFIX = "theme_animation_";
    private static final String LEGACY_STANDARD_LIGHT_THEME_ID = "00000000-0000-0000-0000-000000000101";
    private static final String LEGACY_STANDARD_DARK_THEME_ID = "00000000-0000-0000-0000-000000000102";

    private final SharedPreferences preferences;
    private final Gson gson;

    public ThemeStorage(Context context) {
        preferences = context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
        );

        gson = new Gson();

        migrateThemesIfNeeded();
    }

    public void saveThemes(List<ThemeModel> themes) {
        String json = gson.toJson(themes);

        preferences.edit()
                .putString(KEY_THEMES, json)
                .apply();
    }

    public List<ThemeModel> getThemes() {
        String json = preferences.getString(KEY_THEMES, null);

        if (json == null) {
            return new ArrayList<>();
        }

        Type type = new TypeToken<List<ThemeModel>>() {
        }.getType();

        List<ThemeModel> themes = gson.fromJson(json, type);

        return themes != null
                ? themes
                : new ArrayList<>();
    }

    public List<ThemeModel> getMyThemes() {
        List<ThemeModel> result = new ArrayList<>();

        for (ThemeModel theme : getThemes()) {
            if (theme.getSource() == ThemeType.Custom) {
                result.add(theme);
            }
        }

        return result;
    }

    public List<ThemeModel> getLibraryThemes() {
        List<ThemeModel> result = new ArrayList<>();

        for (ThemeModel theme : getThemes()) {
            if (theme.getSource() == ThemeType.System) {
                result.add(theme);
            }
        }

        return result;
    }

    public void addTheme(ThemeModel themeModel) {
        if (themeModel == null) {
            return;
        }

        if (themeModel.getId() != null
                && isLegacyStandardThemeId(themeModel.getId().toString())) {
            return;
        }

        if (themeModel.getSource() == null) {
            themeModel.setSource(ThemeType.Custom);
        }

        themeModel.setMessageAnimation(
                resolveAnimationForTheme(themeModel)
        );

        List<ThemeModel> themes = getThemes();

        themes.add(themeModel);

        saveThemes(themes);
        saveLegacyAnimation(themeModel);
    }

    public void updateTheme(ThemeModel updatedTheme) {
        if (updatedTheme == null || updatedTheme.getId() == null) {
            return;
        }

        if (isLegacyStandardThemeId(updatedTheme.getId().toString())) {
            return;
        }

        List<ThemeModel> themes = getThemes();

        for (int i = 0; i < themes.size(); i++) {
            ThemeModel current = themes.get(i);

            if (updatedTheme.getId().equals(current.getId())) {
                if (updatedTheme.getSource() == null) {
                    updatedTheme.setSource(current.getSource());
                }

                String incomingAnimation = ThemeMessageAnimationBinder.normalize(
                        updatedTheme.getRawMessageAnimation()
                );

                if (ThemeModel.DEFAULT_MESSAGE_ANIMATION.equals(incomingAnimation)) {
                    String preservedAnimation = resolveAnimationForTheme(current);

                    if (ThemeModel.DEFAULT_MESSAGE_ANIMATION.equals(preservedAnimation)) {
                        preservedAnimation = resolveAnimationForTheme(updatedTheme);
                    }

                    updatedTheme.setMessageAnimation(preservedAnimation);
                } else {
                    updatedTheme.setMessageAnimation(incomingAnimation);
                }

                themes.set(i, updatedTheme);

                saveThemes(themes);
                saveLegacyAnimation(updatedTheme);

                return;
            }
        }

        addTheme(updatedTheme);
    }

    public void removeTheme(String id) {
        if (id == null) {
            return;
        }

        List<ThemeModel> themes = getThemes();
        List<ThemeModel> updatedThemes = new ArrayList<>();

        for (ThemeModel theme : themes) {
            if (!id.equals(theme.getId().toString())) {
                updatedThemes.add(theme);
            }
        }

        saveThemes(updatedThemes);

        preferences.edit()
                .remove(KEY_ANIMATION_PREFIX + id)
                .apply();

        if (id.equals(getSelectedThemeId())) {
            clearSelectedThemeId();
        }
    }

    public ThemeModel getThemeById(String id) {
        if (id == null) {
            return null;
        }

        for (ThemeModel theme : getThemes()) {
            if (id.equals(theme.getId().toString())) {
                return theme;
            }
        }

        return null;
    }

    public void setSelectedThemeId(String id) {
        preferences.edit()
                .putString(KEY_SELECTED_THEME_ID, id)
                .apply();
    }

    public String getSelectedThemeId() {
        return preferences.getString(KEY_SELECTED_THEME_ID, null);
    }

    public ThemeModel getSelectedTheme() {
        return getThemeById(getSelectedThemeId());
    }

    public void clearSelectedThemeId() {
        preferences.edit()
                .remove(KEY_SELECTED_THEME_ID)
                .apply();
    }

    private void saveLegacyAnimation(ThemeModel theme) {
        if (theme == null
                || theme.getId() == null) {
            return;
        }

        preferences.edit()
                .putString(
                        KEY_ANIMATION_PREFIX + theme.getId(),
                        ThemeMessageAnimationBinder.normalize(
                                theme.getMessageAnimation()
                        )
                )
                .apply();
    }

    private String resolveAnimationForTheme(ThemeModel theme) {
        if (theme == null) {
            return ThemeModel.DEFAULT_MESSAGE_ANIMATION;
        }

        String fromModel =
                ThemeMessageAnimationBinder.normalize(
                        theme.getRawMessageAnimation()
                );

        if (!ThemeModel.DEFAULT_MESSAGE_ANIMATION.equals(fromModel)) {
            return fromModel;
        }

        if (theme.getId() == null) {
            return ThemeModel.DEFAULT_MESSAGE_ANIMATION;
        }

        String fromPrefs = preferences.getString(
                KEY_ANIMATION_PREFIX + theme.getId(),
                null
        );

        return ThemeMessageAnimationBinder.normalize(fromPrefs);
    }

    private void migrateThemesIfNeeded() {
        List<ThemeModel> themes = getThemes();
        List<ThemeModel> migratedThemes = new ArrayList<>();

        boolean changed = false;

        for (ThemeModel theme : themes) {
            if (theme == null) {
                changed = true;
                continue;
            }

            if (theme.getId() != null
                    && isLegacyStandardThemeId(theme.getId().toString())) {
                changed = true;
                preferences.edit()
                        .remove(KEY_ANIMATION_PREFIX + theme.getId())
                        .apply();
                continue;
            }

            if (theme.getSource() == null) {
                theme.setSource(ThemeType.Custom);
                changed = true;
            }

            String animation = resolveAnimationForTheme(theme);

            if (theme.getRawMessageAnimation() == null
                    || !ThemeMessageAnimationBinder.normalize(
                    theme.getRawMessageAnimation()
            ).equals(animation)) {
                theme.setMessageAnimation(animation);
                changed = true;
            }

            migratedThemes.add(theme);
        }

        String selectedThemeId = getSelectedThemeId();

        if (isLegacyStandardThemeId(selectedThemeId)) {
            clearSelectedThemeId();
            changed = true;
        }

        if (changed) {
            saveThemes(migratedThemes);

            for (ThemeModel theme : migratedThemes) {
                saveLegacyAnimation(theme);
            }
        }
    }

    private boolean isLegacyStandardThemeId(String id) {
        return LEGACY_STANDARD_LIGHT_THEME_ID.equals(id)
                || LEGACY_STANDARD_DARK_THEME_ID.equals(id);
    }
}
