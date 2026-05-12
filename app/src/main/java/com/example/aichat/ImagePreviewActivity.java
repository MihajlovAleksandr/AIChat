package com.example.aichat;

import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.github.chrisbanes.photoview.PhotoView;

import java.util.ArrayList;

public class ImagePreviewActivity extends AppCompatActivity {

    private GestureDetector tapDetector;
    private ViewPager2 viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);

        Log.e("IMG_DEBUG", "==== ImagePreviewActivity START ====");

        View closeBtn = findViewById(R.id.btn_close);
        viewPager = findViewById(R.id.view_pager);

        ArrayList<String> urls = getIntent().getStringArrayListExtra("urls");

        if (urls == null || urls.isEmpty()) {
            urls = getIntent().getStringArrayListExtra("images");
        }

        if (urls == null || urls.isEmpty()) {
            Log.e("IMG_DEBUG", "URLs list is NULL or EMPTY, fallback to single URL");

            String singleUrl = getIntent().getStringExtra("url");

            if (singleUrl != null) {
                urls = new ArrayList<>();
                urls.add(singleUrl);
            }
        }

        if (urls == null || urls.isEmpty()) {
            Log.e("IMG_DEBUG", "NO IMAGES AT ALL -> closing activity");
            finish();
            return;
        }

        Log.e("IMG_DEBUG", "TOTAL URLS RECEIVED: " + urls.size());
        for (String u : urls) {
            Log.e("IMG_DEBUG", "URL: " + u);
        }

        int startIndex = getIntent().getIntExtra("index", -1);
        if (startIndex == -1) {
            startIndex = getIntent().getIntExtra("position", 0);
        }

        if (startIndex < 0 || startIndex >= urls.size()) {
            startIndex = 0;
        }

        Log.e("IMG_DEBUG", "START INDEX: " + startIndex);

        ImagePagerAdapter adapter = new ImagePagerAdapter(urls);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(startIndex, false);

        closeBtn.setOnClickListener(v -> finish());

        tapDetector = new GestureDetector(this,
                new GestureDetector.SimpleOnGestureListener() {

                    @Override
                    public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
                        toggleSystemUI();
                        return true;
                    }

                    @Override
                    public boolean onDoubleTap(@NonNull MotionEvent e) {
                        PhotoView currentView = getCurrentPhotoView();
                        if (currentView != null) {
                            float scale = currentView.getScale();

                            if (scale > currentView.getMinimumScale()) {
                                currentView.setScale(currentView.getMinimumScale(), true);
                            } else {
                                currentView.setScale(currentView.getMediumScale(), true);
                            }
                        }
                        return true;
                    }
                });

        // ИСПРАВЛЕНИЕ: правильная обработка касаний для ViewPager2
        viewPager.setOnTouchListener((v, event) -> {
            tapDetector.onTouchEvent(event);
            return false; // Не поглощаем события, чтобы ViewPager2 мог обрабатывать свайпы
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                Log.e("IMG_DEBUG", "Page changed to: " + position);
                PhotoView current = getCurrentPhotoView();
                if (current != null) {
                    current.setScale(current.getMinimumScale(), false);
                }
            }
        });
    }

    @Nullable
    private PhotoView getCurrentPhotoView() {
        // Безопасное получение текущего PhotoView
        if (viewPager == null || viewPager.getChildAt(0) == null) {
            return null;
        }

        RecyclerView recyclerView = (RecyclerView) viewPager.getChildAt(0);

        if (recyclerView == null) return null;

        RecyclerView.ViewHolder vh =
                recyclerView.findViewHolderForAdapterPosition(viewPager.getCurrentItem());

        if (vh instanceof ImagePagerAdapter.VH) {
            return ((ImagePagerAdapter.VH) vh).image;
        }

        return null;
    }

    private void toggleSystemUI() {
        View decor = getWindow().getDecorView();
        int flags = decor.getSystemUiVisibility();

        if ((flags & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
            decor.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        } else {
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    @Override
    public void finishAfterTransition() {
        super.finishAfterTransition();
        overridePendingTransition(0, android.R.anim.fade_out);
    }
}