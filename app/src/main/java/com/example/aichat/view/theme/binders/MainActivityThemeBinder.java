package com.example.aichat.view.theme.binders;

import android.app.Activity;
import android.view.View;
import android.widget.ImageView;
import androidx.viewpager2.widget.ViewPager2;
import com.example.aichat.R;

public final class MainActivityThemeBinder {

    private MainActivityThemeBinder() {
    }

    public static void apply(
            Activity activity
    ) {

        if (activity == null) {
            return;
        }

        ImageView backgroundImage =
                activity.findViewById(
                        R.id.theme_background_image
                );

        View backgroundScrim =
                activity.findViewById(
                        R.id.theme_background_scrim
                );

        View mainContainer =
                activity.findViewById(
                        R.id.main_container
                );

        ViewPager2 viewPager =
                activity.findViewById(
                        R.id.view_pager
                );

        View chatContainer =
                activity.findViewById(
                        R.id.view_pager_fragment_container
                );

        ThemeBackgroundBinder.applyToActivity(
                activity,
                backgroundImage,
                backgroundScrim,
                mainContainer
        );

        ThemeBackgroundBinder.makeViewPagerTransparent(
                viewPager
        );

        ThemeBackgroundBinder.makeTransparent(
                chatContainer
        );
    }
}
