package com.example.aichat.view.theme.binders;

import android.content.res.ColorStateList;
import android.view.View;
import android.view.ViewParent;
import com.example.aichat.databinding.ActivityCheckoutBinding;
import com.example.aichat.model.utils.theme.ThemeAttrResolver;
import com.example.aichat.R;
import com.google.android.material.textfield.TextInputLayout;

public class CheckoutThemeBinder {

    private CheckoutThemeBinder() {
    }

    public static void bind(
            View root,
            ActivityCheckoutBinding binding
    ) {

        int primary =
                ThemeAttrResolver.resolveColor(
                        root.getContext(),
                        R.attr.colorPrimary
                );

        int surface =
                ThemeAttrResolver.resolveColor(
                        root.getContext(),
                        R.attr.colorSurface
                );

        int background =
                ThemeAttrResolver.resolveColor(
                        root.getContext(),
                        android.R.attr.colorBackground
                );

        int onSurface =
                ThemeAttrResolver.resolveColor(
                        root.getContext(),
                        R.attr.colorOnSurface
                );

        int onSurfaceVariant =
                ThemeAttrResolver.resolveColor(
                        root.getContext(),
                        R.attr.colorOnSurfaceVariant
                );

        int secondary =
                ThemeAttrResolver.resolveColor(
                        root.getContext(),
                        R.attr.colorSecondary
                );

        binding.getRoot()
                .setBackgroundColor(
                        background
                );

        binding.topBar
                .setBackgroundColor(
                        background
                );

        binding.btnBack
                .setImageTintList(
                        ColorStateList.valueOf(
                                secondary
                        )
                );

        binding.titleText
                .setTextColor(
                        onSurface
                );

        binding.productCard
                .setCardBackgroundColor(
                        surface
                );

        binding.iconCard
                .setCardBackgroundColor(
                        secondary
                );

        binding.productNameText
                .setTextColor(
                        onSurface
                );

        binding.productCurrencyText
                .setTextColor(
                        onSurface
                );

        binding.productPriceText
                .setTextColor(
                        onSurface
                );

        binding.productDescriptionText
                .setTextColor(
                        onSurfaceVariant
                );

        binding.productBadgeText
                .setTextColor(
                        onSurface
                );

        binding.productBadgeText
                .setBackgroundTintList(
                        ColorStateList.valueOf(
                                secondary
                        )
                );

        binding.payButton
                .setBackgroundTintList(
                        ColorStateList.valueOf(
                                primary
                        )
                );

        binding.payButton
                .setTextColor(
                        onSurface
                );

        binding.quantityEditText
                .setTextColor(
                        onSurface
                );

        binding.quantityEditText
                .setHintTextColor(
                        onSurfaceVariant
                );

        bindQuantityInputLayout(
                binding,
                surface,
                primary,
                onSurfaceVariant
        );
    }

    private static void bindQuantityInputLayout(
            ActivityCheckoutBinding binding,
            int surface,
            int primary,
            int onSurfaceVariant
    ) {

        ViewParent parent =
                binding.quantityEditText
                        .getParent();

        if (!(parent instanceof View)) {
            return;
        }

        ViewParent grandParent =
                ((View) parent)
                        .getParent();

        if (grandParent instanceof TextInputLayout) {

            TextInputLayout layout =
                    (TextInputLayout) grandParent;

            layout.setBoxBackgroundColor(
                    surface
            );

            layout.setBoxStrokeColor(
                    primary
            );

            layout.setHintTextColor(
                    ColorStateList.valueOf(
                            onSurfaceVariant
                    )
            );
        }
    }
}
