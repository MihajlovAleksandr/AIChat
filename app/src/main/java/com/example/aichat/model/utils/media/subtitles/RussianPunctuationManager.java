package com.example.aichat.model.utils.media.subtitles;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


public class RussianPunctuationManager {

    private static final String TAG = "RussianPunctuation";

    public RussianPunctuationManager(@NonNull Context context) {
    }

    public synchronized boolean isAvailable() {
        return false;
    }

    public synchronized boolean isInitialized() {
        return false;
    }

    public synchronized boolean initializeIfAvailable() {
        Log.i(TAG, "RUPunct disabled because ONNX Runtime conflicts with Sherpa-ONNX native runtime");
        return false;
    }

    @NonNull
    public synchronized String addPunctuation(@Nullable String rawText) {
        if (rawText == null) {
            return "";
        }

        return rawText.trim();
    }

    public synchronized void release() {
    }
}
