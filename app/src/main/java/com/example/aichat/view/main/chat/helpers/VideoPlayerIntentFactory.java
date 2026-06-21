package com.example.aichat.view.main.chat.helpers;

import com.example.aichat.model.utils.media.subtitles.VideoSubtitleConfig;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.aichat.view.main.chat.VideoPlayerActivity;
import java.util.ArrayList;

@androidx.media3.common.util.UnstableApi
public final class VideoPlayerIntentFactory {

    private VideoPlayerIntentFactory() {
    }

    public static Intent createLocalIntent(
            @NonNull Context context,
            @NonNull String localPath,
            @Nullable String mimeType,
            @Nullable String title
    ) {
        ArrayList<String> localPaths = new ArrayList<>();
        ArrayList<String> urls = new ArrayList<>();
        ArrayList<String> mimeTypes = new ArrayList<>();
        ArrayList<String> titles = new ArrayList<>();

        localPaths.add(localPath);
        urls.add("");
        mimeTypes.add(mimeType != null ? mimeType : "");
        titles.add(title != null ? title : "");

        return createGalleryIntent(context, localPaths, urls, mimeTypes, titles, 0, null);
    }

    public static Intent createLocalIntent(
            @NonNull Context context,
            @NonNull String localPath,
            @Nullable String mimeType,
            @Nullable String title,
            @Nullable String sttLanguage
    ) {
        Intent intent = createLocalIntent(context, localPath, mimeType, title);
        intent.putExtra(
                VideoPlayerActivity.EXTRA_STT_LANGUAGE,
                VideoSubtitleConfig.normalizeLanguageCode(sttLanguage)
        );
        return intent;
    }

    public static Intent createRemoteIntent(
            @NonNull Context context,
            @NonNull String url,
            @Nullable String mimeType,
            @Nullable String title,
            @Nullable String token
    ) {
        ArrayList<String> localPaths = new ArrayList<>();
        ArrayList<String> urls = new ArrayList<>();
        ArrayList<String> mimeTypes = new ArrayList<>();
        ArrayList<String> titles = new ArrayList<>();

        localPaths.add("");
        urls.add(url);
        mimeTypes.add(mimeType != null ? mimeType : "");
        titles.add(title != null ? title : "");

        return createGalleryIntent(context, localPaths, urls, mimeTypes, titles, 0, token);
    }

    public static Intent createRemoteIntent(
            @NonNull Context context,
            @NonNull String url,
            @Nullable String mimeType,
            @Nullable String title,
            @Nullable String token,
            @Nullable String sttLanguage
    ) {
        Intent intent = createRemoteIntent(context, url, mimeType, title, token);
        intent.putExtra(
                VideoPlayerActivity.EXTRA_STT_LANGUAGE,
                VideoSubtitleConfig.normalizeLanguageCode(sttLanguage)
        );
        return intent;
    }

    public static Intent createYouTubeIntent(
            @NonNull Context context,
            @NonNull String youtubeUrl,
            @Nullable String title
    ) {
        Intent intent = new Intent(context, VideoPlayerActivity.class);
        intent.putExtra(VideoPlayerActivity.EXTRA_IS_YOUTUBE, true);
        intent.putExtra(VideoPlayerActivity.EXTRA_YOUTUBE_URL, youtubeUrl);
        intent.putExtra(VideoPlayerActivity.EXTRA_TITLE, title != null ? title : "YouTube");
        intent.putExtra(VideoPlayerActivity.EXTRA_ENGINE, VideoPlayerActivity.ENGINE_EXO_PLAYER);
        return intent;
    }

    public static Intent createGalleryIntent(
            @NonNull Context context,
            @NonNull ArrayList<String> localPaths,
            @NonNull ArrayList<String> urls,
            @NonNull ArrayList<String> mimeTypes,
            @NonNull ArrayList<String> titles,
            int initialIndex,
            @Nullable String token
    ) {
        return createGalleryIntent(
                context,
                localPaths,
                urls,
                mimeTypes,
                titles,
                null,
                initialIndex,
                token
        );
    }

    public static Intent createGalleryIntent(
            @NonNull Context context,
            @NonNull ArrayList<String> localPaths,
            @NonNull ArrayList<String> urls,
            @NonNull ArrayList<String> mimeTypes,
            @NonNull ArrayList<String> titles,
            @Nullable ArrayList<String> sttLanguages,
            int initialIndex,
            @Nullable String token
    ) {
        Intent intent = new Intent(context, VideoPlayerActivity.class);

        intent.putStringArrayListExtra(VideoPlayerActivity.EXTRA_LOCAL_PATHS, localPaths);
        intent.putStringArrayListExtra(VideoPlayerActivity.EXTRA_URLS, urls);
        intent.putStringArrayListExtra(VideoPlayerActivity.EXTRA_MIME_TYPES, mimeTypes);
        intent.putStringArrayListExtra(VideoPlayerActivity.EXTRA_TITLES, titles);

        if (sttLanguages != null) {
            ArrayList<String> normalizedLanguages = new ArrayList<>();
            for (String language : sttLanguages) {
                normalizedLanguages.add(VideoSubtitleConfig.normalizeLanguageCode(language));
            }
            intent.putStringArrayListExtra(VideoPlayerActivity.EXTRA_STT_LANGUAGES, normalizedLanguages);
        }

        intent.putExtra(VideoPlayerActivity.EXTRA_INITIAL_INDEX, initialIndex);
        intent.putExtra(VideoPlayerActivity.EXTRA_TOKEN, token);
        intent.putExtra(VideoPlayerActivity.EXTRA_ENGINE, VideoPlayerActivity.ENGINE_EXO_PLAYER);

        return intent;
    }
}
