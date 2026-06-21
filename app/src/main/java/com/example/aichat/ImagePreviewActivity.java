package com.example.aichat;

import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.aichat.view.main.BaseActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.github.chrisbanes.photoview.PhotoView;
import java.util.ArrayList;

public class ImagePreviewActivity extends BaseActivity {

    private static final String TAG = "IMG_DEBUG";

    private GestureDetector tapDetector;
    private ViewPager2 viewPager;
    private View closeBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_image_preview);

        Log.d(TAG, "==== ImagePreviewActivity START ====");

        closeBtn = findViewById(R.id.btn_close);
        viewPager = findViewById(R.id.view_pager);

        setupCloseButtonInsets();
        setupImages();
        setupGestures();
    }

    private void setupCloseButtonInsets() {
        if (closeBtn == null) {
            return;
        }

        closeBtn.setOnClickListener(v -> finish());
        closeBtn.setClickable(true);
        closeBtn.setFocusable(true);
        closeBtn.bringToFront();
        closeBtn.setElevation(dp(32));

        ViewCompat.setOnApplyWindowInsetsListener(closeBtn, (view, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(
                    WindowInsetsCompat.Type.statusBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );

            ViewGroup.LayoutParams rawParams = view.getLayoutParams();

            if (rawParams instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) rawParams;
                params.topMargin = systemBars.top + dp(12);
                params.leftMargin = systemBars.left + dp(12);
                params.setMarginStart(systemBars.left + dp(12));
                view.setLayoutParams(params);
            } else {
                view.setPadding(
                        view.getPaddingLeft(),
                        systemBars.top + dp(12),
                        view.getPaddingRight(),
                        view.getPaddingBottom()
                );
            }

            return insets;
        });

        ViewCompat.requestApplyInsets(closeBtn);
    }

    private void setupImages() {
        ArrayList<String> urls = getIntent().getStringArrayListExtra("urls");

        if (urls == null || urls.isEmpty()) {
            urls = getIntent().getStringArrayListExtra("images");
        }

        if (urls == null || urls.isEmpty()) {
            Log.d(TAG, "URLs list is null or empty, fallback to single URL");

            String singleUrl = getIntent().getStringExtra("url");

            if (singleUrl != null && !singleUrl.trim().isEmpty()) {
                urls = new ArrayList<>();
                urls.add(singleUrl);
            }
        }

        if (urls == null || urls.isEmpty()) {
            Log.e(TAG, "No images received, closing activity");
            finish();
            return;
        }

        int startIndex = getIntent().getIntExtra("index", -1);

        if (startIndex == -1) {
            startIndex = getIntent().getIntExtra("position", 0);
        }

        if (startIndex < 0 || startIndex >= urls.size()) {
            startIndex = 0;
        }

        ImagePagerAdapter adapter = new ImagePagerAdapter(urls);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(startIndex, false);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                PhotoView current = getCurrentPhotoView();

                if (current != null) {
                    current.setScale(current.getMinimumScale(), false);
                }
            }
        });
    }

    private void setupGestures() {
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

                        if (currentView == null) {
                            return true;
                        }

                        float scale = currentView.getScale();

                        if (scale > currentView.getMinimumScale()) {
                            currentView.setScale(currentView.getMinimumScale(), true);
                        } else {
                            currentView.setScale(currentView.getMediumScale(), true);
                        }

                        return true;
                    }
                });

        viewPager.setOnTouchListener((v, event) -> {
            if (tapDetector != null) {
                tapDetector.onTouchEvent(event);
            }

            return false;
        });
    }

    @Nullable
    private PhotoView getCurrentPhotoView() {
        if (viewPager == null || viewPager.getChildAt(0) == null) {
            return null;
        }

        RecyclerView recyclerView = (RecyclerView) viewPager.getChildAt(0);
        RecyclerView.ViewHolder viewHolder =
                recyclerView.findViewHolderForAdapterPosition(viewPager.getCurrentItem());

        if (viewHolder instanceof ImagePagerAdapter.VH) {
            return ((ImagePagerAdapter.VH) viewHolder).image;
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
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );
        } else {
            decor.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );
        }

        if (closeBtn != null) {
            closeBtn.bringToFront();
            closeBtn.setVisibility(View.VISIBLE);
            ViewCompat.requestApplyInsets(closeBtn);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void finishAfterTransition() {
        super.finishAfterTransition();
        overridePendingTransition(0, android.R.anim.fade_out);
    }
}
