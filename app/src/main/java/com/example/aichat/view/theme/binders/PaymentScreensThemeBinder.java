package com.example.aichat.view.theme.binders;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.example.aichat.databinding.ActivityCheckoutBinding;
import com.example.aichat.databinding.ActivityPaymentDetailsBinding;
import com.example.aichat.databinding.ActivityProductsBinding;
import com.example.aichat.R;

public final class PaymentScreensThemeBinder {

    private PaymentScreensThemeBinder() {
    }

    public static void applyProducts(
            @NonNull Activity activity,
            @Nullable ActivityProductsBinding binding
    ) {
        if (binding == null) {
            return;
        }

        ScreenThemeBinder.tintIconPrimary(
                binding.btnBack
        );

        ScreenThemeBinder.tintIconPrimary(
                binding.btnHistory
        );

        ScreenThemeBinder.headerGradientBackground(
                binding.premiumHeader,
                32
        );

        ScreenThemeBinder.whiteText(
                binding.tvPremiumTitle
        );

        ScreenThemeBinder.whiteText(
                binding.tvPremiumDescription
        );

        ScreenThemeBinder.whiteText(
                binding.tvPremiumStatus
        );

        binding.tvPremiumDescription.setAlpha(
                0.87f
        );

        if (binding.layoutPremiumStatus != null) {
            binding.layoutPremiumStatus.setBackgroundTintList(
                    ColorStateList.valueOf(
                            ScreenThemeBinder.withAlpha(
                                    Color.WHITE,
                                    42
                            )
                    )
            );
        }

        ScreenThemeBinder.tintIcon(
                binding.ivAutoRenew,
                Color.WHITE
        );

        if (binding.btnPremiumDetails != null) {
            binding.btnPremiumDetails.setBackgroundTintList(
                    ColorStateList.valueOf(
                            ScreenThemeBinder.withAlpha(
                                    Color.WHITE,
                                    42
                            )
                    )
            );

            binding.btnPremiumDetails.setTextColor(
                    Color.WHITE
            );
        }

        ScreenThemeBinder.surfaceText(
                binding.tvSubscriptionsTitle
        );

        ScreenThemeBinder.surfaceText(
                binding.tvTokensTitle
        );

    }

    public static void applyPaymentsHistory(@NonNull Activity activity) {
        ScreenThemeBinder.tintIconPrimary(
                activity.findViewById(
                        R.id.btn_back
                )
        );

        ScreenThemeBinder.surfaceText(
                activity.findViewById(
                        R.id.tv_title
                )
        );

        ScreenThemeBinder.headerGradientBackground(
                activity.findViewById(
                        R.id.headerContainer
                ),
                32
        );

        ScreenThemeBinder.whiteText(
                activity.findViewById(
                        R.id.headerTitle
                )
        );

        ScreenThemeBinder.whiteText(
                activity.findViewById(
                        R.id.headerDescription
                )
        );

        View loading =
                activity.findViewById(
                        R.id.loadingContainer
                );

        if (loading != null) {
            loading.setBackgroundTintList(
                    ColorStateList.valueOf(
                            ScreenThemeBinder.withAlpha(
                                    Color.WHITE,
                                    42
                            )
                    )
            );
        }

        ScreenThemeBinder.whiteText(
                activity.findViewById(
                        R.id.loadingText
                )
        );

        ScreenThemeBinder.surfaceText(
                activity.findViewById(
                        R.id.paymentsTitle
                )
        );

        ScreenThemeBinder.primaryButton(
                activity.findViewById(
                        R.id.productsButton
                )
        );
    }

    public static void applyPaymentDetails(
            @NonNull Activity activity,
            @Nullable ActivityPaymentDetailsBinding binding
    ) {
        if (binding == null) {
            return;
        }

        ScreenThemeBinder.tintIconPrimary(
                binding.btnBack
        );

        ScreenThemeBinder.surfaceText(
                binding.tvPaymentId
        );

        ScreenThemeBinder.surfaceText(
                binding.tvAmount
        );

        ScreenThemeBinder.surfaceText(
                binding.tvCurrency
        );

        ScreenThemeBinder.variantText(
                binding.tvStatusLabel
        );

        ScreenThemeBinder.whiteText(
                binding.tvStatus
        );

        ScreenThemeBinder.surfaceText(
                binding.tvCreatedAt
        );

        ScreenThemeBinder.primaryButton(
                binding.btnOpenInvoice
        );

        tintPaymentDetailsStaticIcons(
                binding.getRoot(),
                binding.rvItems
        );

        RecyclerView recyclerView =
                binding.rvItems;

        if (recyclerView != null) {
            recyclerView.setBackgroundColor(
                    Color.TRANSPARENT
            );
        }
    }

    public static void applyCheckout(
            @NonNull Activity activity,
            @Nullable ActivityCheckoutBinding binding
    ) {
        if (binding == null) {
            return;
        }

        ScreenThemeBinder.tintIconPrimary(
                binding.btnBack
        );

        ScreenThemeBinder.surfaceText(
                binding.titleText
        );

        ScreenThemeBinder.surfaceText(
                binding.productNameText
        );

        ScreenThemeBinder.surfaceText(
                binding.productCurrencyText
        );

        ScreenThemeBinder.surfaceText(
                binding.productPriceText
        );

        ScreenThemeBinder.variantText(
                binding.productDescriptionText
        );

        ScreenThemeBinder.surfaceText(
                binding.productBadgeText
        );

        ScreenThemeBinder.surfaceText(
                binding.quantityTitle
        );

        ScreenThemeBinder.textInputLayout(
                binding.quantityEditTextLayout
        );

        ScreenThemeBinder.primaryButton(
                binding.payButton
        );

        ScreenThemeBinder.variantText(
                binding.securePaymentText
        );

        ScreenThemeBinder.tintTextCompoundDrawables(
                binding.securePaymentText,
                ScreenThemeBinder.onSurfaceVariant(
                        activity
                )
        );
    }

    public static void applyPaymentResultDialog(@Nullable View dialogView) {
        if (dialogView == null) {
            return;
        }

        int primary =
                ScreenThemeBinder.primary(
                        dialogView.getContext()
                );

        int onPrimary =
                ScreenThemeBinder.onPrimary(
                        dialogView.getContext()
                );

        int onSurface =
                ScreenThemeBinder.onSurface(
                        dialogView.getContext()
                );

        int onSurfaceVariant =
                ScreenThemeBinder.onSurfaceVariant(
                        dialogView.getContext()
                );

        TextView title =
                findFirstLargeTitle(
                        dialogView
                );

        if (title != null) {
            title.setTextColor(
                    onSurface
            );
        }

        TextView message =
                dialogView.findViewById(
                        R.id.messageText
                );

        if (message != null) {
            message.setTextColor(
                    onSurfaceVariant
            );
        }

        Button okButton =
                dialogView.findViewById(
                        R.id.okButton
                );

        if (okButton != null) {
            okButton.setBackgroundTintList(
                    ColorStateList.valueOf(
                            primary
                    )
            );

            okButton.setTextColor(
                    onPrimary
            );
        }

        View supportButton =
                dialogView.findViewById(
                        R.id.supportButton
                );

        if (supportButton instanceof android.view.ViewGroup) {
            android.view.ViewGroup group =
                    (android.view.ViewGroup) supportButton;

            for (int i = 0; i < group.getChildCount(); i++) {
                View child =
                        group.getChildAt(
                                i
                        );

                if (child instanceof ImageView) {
                    ScreenThemeBinder.tintIcon(
                            child,
                            primary
                    );
                }

                if (child instanceof TextView) {
                    ((TextView) child).setTextColor(
                            primary
                    );
                }
            }
        }

        ImageView mainIcon =
                findFirstImageOutsideSupport(
                        dialogView,
                        supportButton
                );

        if (mainIcon != null) {
            ScreenThemeBinder.tintIcon(
                    mainIcon,
                    primary
            );
        }
    }

    public static void applyPaymentItem(@Nullable View itemView) {
        if (itemView == null) {
            return;
        }

        ScreenThemeBinder.surfaceText(
                itemView.findViewById(
                        R.id.amountText
                )
        );

        ScreenThemeBinder.variantText(
                itemView.findViewById(
                        R.id.createdAtText
                )
        );

        TextView statusText =
                itemView.findViewById(
                        R.id.statusText
                );

        if (statusText != null) {
            statusText.setTextColor(
                    Color.WHITE
            );
        }

        ScreenThemeBinder.primaryButton(
                itemView.findViewById(
                        R.id.detailsButton
                )
        );
    }

    public static void applyPremiumItem(@Nullable View itemView) {
        if (itemView == null) {
            return;
        }

        ScreenThemeBinder.surfaceText(
                itemView.findViewById(
                        R.id.tv_premium_period
                )
        );

        ScreenThemeBinder.variantText(
                itemView.findViewById(
                        R.id.tv_premium_id
                )
        );

        ScreenThemeBinder.variantText(
                itemView.findViewById(
                        R.id.tv_auto_renew_status
                )
        );

        ScreenThemeBinder.primaryButton(
                itemView.findViewById(
                        R.id.btn_view_payment
                )
        );
    }

    private static void tintPaymentDetailsStaticIcons(
            @Nullable View view,
            @Nullable View excludedRecyclerView
    ) {
        if (view == null || view == excludedRecyclerView) {
            return;
        }

        if (view instanceof ImageButton) {
            ScreenThemeBinder.tintIconPrimary(
                    view
            );
        } else if (view instanceof ImageView) {
            ScreenThemeBinder.tintIconPrimary(
                    view
            );
        }

        if (view instanceof FrameLayout && hasDirectImageChild((FrameLayout) view)) {
            ScreenThemeBinder.primarySoftIconBackground(
                    view,
                    18
            );
        }

        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group =
                    (android.view.ViewGroup) view;

            for (int i = 0; i < group.getChildCount(); i++) {
                tintPaymentDetailsStaticIcons(
                        group.getChildAt(
                                i
                        ),
                        excludedRecyclerView
                );
            }
        }
    }

    private static boolean hasDirectImageChild(@NonNull android.view.ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            if (group.getChildAt(i) instanceof ImageView) {
                return true;
            }
        }

        return false;
    }

    @Nullable
    private static TextView findFirstLargeTitle(@NonNull View root) {
        if (root instanceof TextView) {
            TextView textView =
                    (TextView) root;

            float sp =
                    textView.getTextSize()
                            / root.getResources()
                            .getDisplayMetrics()
                            .scaledDensity;

            if (sp >= 28f) {
                return textView;
            }
        }

        if (root instanceof android.view.ViewGroup) {
            android.view.ViewGroup group =
                    (android.view.ViewGroup) root;

            for (int i = 0; i < group.getChildCount(); i++) {
                TextView result =
                        findFirstLargeTitle(
                                group.getChildAt(
                                        i
                                )
                        );

                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    @Nullable
    private static ImageView findFirstImageOutsideSupport(
            @NonNull View root,
            @Nullable View supportButton
    ) {
        if (root == supportButton) {
            return null;
        }

        if (root instanceof ImageView) {
            return (ImageView) root;
        }

        if (root instanceof android.view.ViewGroup) {
            android.view.ViewGroup group =
                    (android.view.ViewGroup) root;

            for (int i = 0; i < group.getChildCount(); i++) {
                ImageView result =
                        findFirstImageOutsideSupport(
                                group.getChildAt(
                                        i
                                ),
                                supportButton
                        );

                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }
}
