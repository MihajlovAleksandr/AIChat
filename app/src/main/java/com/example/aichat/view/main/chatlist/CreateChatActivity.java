package com.example.aichat.view.main.chatlist;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aichat.MyApp;
import com.example.aichat.R;
import com.example.aichat.controller.main.chatlist.CreateChatController;
import com.example.aichat.model.entities.ChatType;

import java.util.UUID;

public class CreateChatActivity extends AppCompatActivity {

    private CreateChatController createChatController;

    private View circleJoinGroup, circleCreateChat;
    private View submenuOverlay;
    private View loadingOverlay;
    private ProgressBar loadingProgress;
    private TextView loadingText;
    private Button btnCancelLoading;

    private View circleAI, circleSingle, circleGroup, circleJoinExisting;

    private Handler loadingHandler;
    private Runnable loadingTimeoutRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_chat);

        circleJoinGroup = findViewById(R.id.circle_join_group);
        circleCreateChat = findViewById(R.id.circle_create_chat);

        submenuOverlay = findViewById(R.id.submenu_overlay);
        submenuOverlay.setVisibility(View.GONE);

        circleAI = findViewById(R.id.circle_ai);
        circleSingle = findViewById(R.id.circle_single);
        circleGroup = findViewById(R.id.circle_group);
        circleJoinExisting = findViewById(R.id.circle_join_existing);

        loadingOverlay = findViewById(R.id.loading_overlay);
        loadingOverlay.setVisibility(View.GONE);
        loadingProgress = loadingOverlay.findViewById(R.id.loading_progress);
        loadingText = loadingOverlay.findViewById(R.id.loading_text);
        btnCancelLoading = loadingOverlay.findViewById(R.id.btn_cancel_loading);

        loadingHandler = new Handler(Looper.getMainLooper());

        setupListeners();
    }

    private void setupListeners() {
        findViewById(R.id.btn_back).setOnClickListener(v -> onBackPressed());

        setupGestureClick(circleJoinGroup, ChatType.Group);
        setupGestureClick(circleAI, ChatType.AI);
        setupGestureClick(circleSingle, ChatType.Human);
        setupGestureClick(circleGroup, ChatType.Group);
        setupGestureClick(circleJoinExisting, ChatType.Group);

        btnCancelLoading.setOnClickListener(v -> {
            hideLoadingOverlay();
            stopLoadingAndOperations();
        });

        GestureDetector mainCircleGesture = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                startChatOperation(ChatType.Human);
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                showSubMenu();
            }
        });

        circleCreateChat.setOnTouchListener((v, event) -> {
            mainCircleGesture.onTouchEvent(event);
            return true;
        });
    }

    private void setupGestureClick(View target, ChatType type) {
        GestureDetector gesture = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                startChatOperation(type);
                return true;
            }
        });
        target.setOnTouchListener((v, event) -> {
            gesture.onTouchEvent(event);
            return true;
        });
    }

    private void startChatOperation(ChatType type) {
        hideSubMenu();
        showLoading("Подключение к серверу...", true);
        startConnectionTimeout();

        UUID userId = MyApp.getInstance().getCurrentUserId();
       if (userId != null) {
            if (createChatController == null)
                createChatController = new CreateChatController(userId);

            createChatController.addChat(type);
        }
    }

    private void showLoading(String message, boolean initialConnect) {
        loadingText.setText(message);
        btnCancelLoading.setVisibility(View.VISIBLE);
        if (loadingOverlay.getVisibility() != View.VISIBLE) {
            loadingOverlay.setAlpha(0f);
            loadingOverlay.setVisibility(View.VISIBLE);
            loadingOverlay.animate().alpha(1f).setDuration(400).start();
        }
        loadingProgress.setAlpha(initialConnect ? 1f : 0.5f);
        loadingText.setAlpha(1f);
    }

    private void hideLoadingOverlay() {
        if (loadingOverlay.getVisibility() == View.VISIBLE) {
            loadingOverlay.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> loadingOverlay.setVisibility(View.GONE))
                    .start();
        }
    }

    private void startConnectionTimeout() {
        if (loadingTimeoutRunnable != null) loadingHandler.removeCallbacks(loadingTimeoutRunnable);
        loadingTimeoutRunnable = () -> {
            hideLoadingOverlay();
            Toast.makeText(this, "Соединение с сервером не удалось", Toast.LENGTH_SHORT).show();
            stopLoadingAndOperations();
        };
        loadingHandler.postDelayed(loadingTimeoutRunnable, 10000);
    }

    private void stopLoadingAndOperations() {
        if (loadingTimeoutRunnable != null) {
            loadingHandler.removeCallbacks(loadingTimeoutRunnable);
            loadingTimeoutRunnable = null;
        }
        if (createChatController != null) {
            createChatController.stopAddingUserToChat();
            createChatController.stopSearchingChat();
        }
    }

    private void showSubMenu() {
        submenuOverlay.bringToFront();
        submenuOverlay.setVisibility(View.VISIBLE);

        View[] circles = {circleAI, circleSingle, circleGroup, circleJoinExisting};
        for (View circle : circles) {
            circle.setTranslationX(0);
            circle.setTranslationY(0);
            circle.setScaleX(0f);
            circle.setScaleY(0f);
            circle.setAlpha(0f);
            circle.setVisibility(View.VISIBLE);
        }

        float radius = dpToPxLocal(120);
        int n = circles.length;
        for (int i = 0; i < n; i++) {
            double angle = Math.toRadians(-90 + 360.0 / n * i);
            float x = (float) (radius * Math.cos(angle));
            float y = (float) (radius * Math.sin(angle));
            circles[i].animate().translationX(x).translationY(y)
                    .scaleX(1f).scaleY(1f).alpha(1f).setDuration(300).start();
        }

        submenuOverlay.setOnTouchListener((v, event) -> {
            for (View circle : circles) {
                int[] loc = new int[2];
                circle.getLocationOnScreen(loc);
                float x = event.getRawX(), y = event.getRawY();
                if (x >= loc[0] && x <= loc[0] + circle.getWidth() &&
                        y >= loc[1] && y <= loc[1] + circle.getHeight()) return false;
            }
            hideSubMenu();
            return true;
        });
    }

    private void hideSubMenu() {
        View[] circles = {circleAI, circleSingle, circleGroup, circleJoinExisting};
        for (View circle : circles) {
            circle.animate().scaleX(0f).scaleY(0f).alpha(0f).setDuration(200).start();
        }
        submenuOverlay.postDelayed(() -> submenuOverlay.setVisibility(View.GONE), 200);
    }

    private float dpToPxLocal(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    @Override
    public void onBackPressed() {
        if (submenuOverlay.getVisibility() == View.VISIBLE) {
            hideSubMenu();
        } else if (loadingOverlay.getVisibility() == View.VISIBLE) {
            hideLoadingOverlay();
            stopLoadingAndOperations();
        } else {
            super.onBackPressed();
        }
    }
}