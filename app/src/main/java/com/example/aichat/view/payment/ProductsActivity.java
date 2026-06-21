package com.example.aichat.view.payment;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.aichat.controller.payment.ProductController;
import com.example.aichat.databinding.ActivityProductsBinding;
import com.example.aichat.dto.response.PremiumInfoResponse;
import com.example.aichat.dto.response.ProductResponse;
import com.example.aichat.model.payment.PaymentType;
import com.example.aichat.R;
import com.example.aichat.view.main.BaseActivity;
import com.example.aichat.view.theme.binders.PaymentScreensThemeBinder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProductsActivity
        extends BaseActivity {

    private ActivityProductsBinding binding;

    private ProductController controller;

    private ProductAdapter subscriptionsAdapter;

    private ProductAdapter tokensAdapter;

    private ProductUiMapper productUiMapper;

    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState
    ) {

        super.onCreate(savedInstanceState);

        binding =
                ActivityProductsBinding.inflate(
                        getLayoutInflater()
                );

        setContentView(
                binding.getRoot()
        );

        PaymentScreensThemeBinder.applyProducts(
                this,
                binding
        );

        productUiMapper =
                new ProductUiMapper(this);

        controller =
                new ProductController(this);

        setupInsets();

        setupRecyclerViews();

        setupListeners();

        setupBackPressedHandler();

        controller.loadProducts(null);

        controller.loadPremiumInfo();
    }

    @Override
    protected void onResume() {

        super.onResume();

        PaymentScreensThemeBinder.applyProducts(
                this,
                binding
        );
    }

    private void setupInsets() {

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(android.R.id.content),
                (view, insets) -> {

                    Insets systemBars =
                            insets.getInsets(
                                    WindowInsetsCompat
                                            .Type
                                            .systemBars()
                            );

                    view.setPadding(
                            0,
                            systemBars.top,
                            0,
                            systemBars.bottom
                    );

                    return insets;
                }
        );
    }

    private void setupRecyclerViews() {

        subscriptionsAdapter =
                new ProductAdapter(
                        this::openCheckout,
                        true
                );

        tokensAdapter =
                new ProductAdapter(
                        this::openCheckout,
                        true
                );

        binding.rvSubscriptions.setLayoutManager(
                new LinearLayoutManager(this)
        );

        binding.rvSubscriptions.setAdapter(
                subscriptionsAdapter
        );

        binding.rvTokens.setLayoutManager(
                new LinearLayoutManager(this)
        );

        binding.rvTokens.setAdapter(
                tokensAdapter
        );
    }

    private void setupListeners() {

        binding.btnBack.setOnClickListener(
                v -> finish()
        );

        binding.btnHistory.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            this,
                            PaymentsActivity.class
                    );

            startActivity(intent);
        });

        binding.btnPremiumDetails.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            this,
                            PremiumListActivity.class
                    );

            startActivity(intent);
        });
    }

    private void setupBackPressedHandler() {

        OnBackPressedCallback callback =
                new OnBackPressedCallback(true) {

                    @Override
                    public void handleOnBackPressed() {

                        finish();
                    }
                };

        getOnBackPressedDispatcher()
                .addCallback(this, callback);
    }

    public void showProducts(
            ProductResponse[] products
    ) {

        List<UiProduct> subscriptions =
                new ArrayList<>();

        List<UiProduct> tokens =
                new ArrayList<>();

        for (ProductResponse product : products) {

            UiProduct uiProduct =
                    productUiMapper.map(
                            product
                    );

            if (product.paymentType
                    == PaymentType.Subscription) {

                subscriptions.add(
                        uiProduct
                );

            } else {

                tokens.add(
                        uiProduct
                );
            }
        }

        runOnUiThread(() -> {

            subscriptionsAdapter.submitList(
                    subscriptions
            );

            tokensAdapter.submitList(
                    tokens
            );

            binding.tvSubscriptionsTitle
                    .setVisibility(
                            subscriptions.isEmpty()
                                    ? View.GONE
                                    : View.VISIBLE
                    );

            binding.rvSubscriptions
                    .setVisibility(
                            subscriptions.isEmpty()
                                    ? View.GONE
                                    : View.VISIBLE
                    );

            binding.tvTokensTitle
                    .setVisibility(
                            tokens.isEmpty()
                                    ? View.GONE
                                    : View.VISIBLE
                    );

            binding.rvTokens
                    .setVisibility(
                            tokens.isEmpty()
                                    ? View.GONE
                                    : View.VISIBLE
                    );
        });
    }

    public void showPremiumInfo(
            @Nullable PremiumInfoResponse premium
    ) {

        runOnUiThread(() -> {

            if (premium == null) {

                showInactivePremiumHeader();

                return;
            }

            if (premium.isAutoRenew) {

                showAutoRenewPremiumHeader();

            } else {

                showCancelledPremiumHeader();
            }
        });
    }

    private void showInactivePremiumHeader() {

        binding.tvPremiumStatus.setText(
                R.string.premium_inactive
        );

        binding.ivAutoRenew.setVisibility(
                View.GONE
        );

        binding.btnPremiumDetails.setVisibility(
                View.GONE
        );

        binding.premiumHeader.setAlpha(
                0.78f
        );

        refreshPremiumHeaderTheme();
    }

    private void showAutoRenewPremiumHeader() {

        binding.premiumHeader.setAlpha(
                1f
        );

        binding.ivAutoRenew.setVisibility(
                View.VISIBLE
        );

        binding.btnPremiumDetails.setVisibility(
                View.VISIBLE
        );

        binding.ivAutoRenew.setImageResource(
                R.drawable.ic_check
        );

        binding.tvPremiumStatus.setText(
                R.string.auto_renew_enabled
        );

        refreshPremiumHeaderTheme();
    }

    private void showCancelledPremiumHeader() {

        binding.premiumHeader.setAlpha(
                1f
        );

        binding.ivAutoRenew.setVisibility(
                View.VISIBLE
        );

        binding.btnPremiumDetails.setVisibility(
                View.VISIBLE
        );

        binding.ivAutoRenew.setImageResource(
                R.drawable.ic_close
        );

        binding.tvPremiumStatus.setText(
                R.string.premium_subscription_cancelled
        );

        refreshPremiumHeaderTheme();
    }

    private void refreshPremiumHeaderTheme() {

        binding.premiumHeader.requestLayout();

        binding.premiumHeader.invalidate();

        PaymentScreensThemeBinder.applyProducts(
                this,
                binding
        );
    }

    private void openCheckout(
            UiProduct product
    ) {

        controller.chooseProduct(
                UUID.fromString(
                        product.getId()
                )
        );
    }
}
