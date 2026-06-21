package com.example.aichat.view.payment;

import android.os.Bundle;
import android.view.View;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.util.Pair;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.aichat.controller.payment.CheckoutController;
import com.example.aichat.controller.payment.PaymentResultHandler;
import com.example.aichat.databinding.ActivityCheckoutBinding;
import com.example.aichat.dto.response.ProductResponse;
import com.example.aichat.model.payment.PaymentType;
import com.example.aichat.R;
import com.example.aichat.view.main.BaseActivity;
import com.example.aichat.view.theme.binders.PaymentScreensThemeBinder;
import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;
import java.util.UUID;

public class CheckoutActivity
        extends BaseActivity {

    public static final String EXTRA_PRODUCT_ID =
            "product_id";

    private ActivityCheckoutBinding binding;

    private CheckoutController controller;

    private PaymentSheet paymentSheet;

    private PaymentResultHandler paymentResultHandler;

    private ProductResponse currentProduct;

    private ProductUiMapper productUiMapper;

    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState
    ) {

        super.onCreate(savedInstanceState);

        binding =
                ActivityCheckoutBinding.inflate(
                        getLayoutInflater()
                );

        setContentView(
                binding.getRoot()
        );

        productUiMapper =
                new ProductUiMapper(
                        this
                );

        applyRuntimeTheme();

        setupInsets();

        initializePaymentSheet();

        initializeController();

        initializeListeners();

        setupBackPressedHandler();

        loadProduct();
    }

    private void applyRuntimeTheme() {

        PaymentScreensThemeBinder.applyCheckout(
                this,
                binding
        );
    }

    private void setupInsets() {

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(
                        android.R.id.content
                ),
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

    private void initializePaymentSheet() {

        paymentSheet =
                new PaymentSheet(
                        this,
                        this::handlePaymentResult
                );

        paymentResultHandler =
                new PaymentResultHandler(
                        this,
                        paymentSheet
                );
    }

    private void initializeController() {

        controller =
                new CheckoutController(
                        this,
                        paymentSheet
                );
    }

    private void initializeListeners() {

        binding.btnBack.setOnClickListener(
                v -> finish()
        );

        binding.payButton.setOnClickListener(
                v -> startPayment()
        );
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
                .addCallback(
                        this,
                        callback
                );
    }

    private void loadProduct() {

        String productIdString =
                getIntent()
                        .getStringExtra(
                                EXTRA_PRODUCT_ID
                        );

        if (productIdString == null) {

            paymentResultHandler.showError(
                    getString(
                            R.string.product_not_found
                    )
            );

            finish();

            return;
        }

        controller.loadProduct(
                UUID.fromString(
                        productIdString
                )
        );
    }

    public void showProduct(
            ProductResponse product
    ) {

        runOnUiThread(() -> {

            currentProduct = product;

            String formattedPrice =
                    productUiMapper.formatPrice(
                            product.price,
                            product.currency
                    );

            String[] priceParts =
                    splitPrice(
                            formattedPrice
                    );

            binding.productNameText.setText(
                    productUiMapper.resolveTitle(
                            product
                    )
            );

            String description =
                    productUiMapper.resolveDescription(
                            product
                    );

            if ((description == null || description.trim().isEmpty())
                    && (product.paymentType == PaymentType.Payment
                    || product.paymentType == PaymentType.SingleItem)) {

                description =
                        getString(
                                R.string.currency_package_description
                        );
            }

            binding.productDescriptionText.setText(
                    description
            );

            binding.productCurrencyText.setText(
                    priceParts[0]
            );

            binding.productPriceText.setText(
                    priceParts[1]
            );

            binding.ivProductIcon.setImageResource(
                    productUiMapper.resolveIcon(
                            product
                    )
            );

            applyProductVisualColor(
                    product
            );

            String badge =
                    productUiMapper.resolveBadge(
                            product
                    );

            if (badge == null || badge.isEmpty()) {

                binding.productBadgeText
                        .setVisibility(
                                View.GONE
                        );

            } else {

                binding.productBadgeText
                        .setVisibility(
                                View.VISIBLE
                        );

                binding.productBadgeText
                        .setText(
                                badge
                        );
            }

            if (product.paymentType == PaymentType.Subscription) {

                binding.titleText.setText(
                        R.string.subscription
                );

                binding.quantityContainer
                        .setVisibility(
                                View.GONE
                        );

            } else if (product.paymentType == PaymentType.SingleItem) {

                binding.titleText.setText(
                        R.string.currency
                );

                binding.quantityContainer
                        .setVisibility(
                                View.GONE
                        );

            } else {

                binding.titleText.setText(
                        R.string.currency
                );

                binding.quantityContainer
                        .setVisibility(
                                View.VISIBLE
                        );
            }

            PaymentScreensThemeBinder.applyCheckout(
                    this,
                    binding
            );

            applyProductVisualColor(
                    product
            );
        });
    }

    private void applyProductVisualColor(ProductResponse product) {

        if (product == null) {
            return;
        }

        binding.iconCard
                .setCardBackgroundColor(
                        productUiMapper
                                .resolveColorByName(
                                        product.name
                                )
                );
    }

    @SuppressWarnings("unchecked")
    private void startPayment() {

        if (currentProduct == null) {

            paymentResultHandler.showError(
                    getString(
                            R.string.product_not_loaded
                    )
            );

            return;
        }

        int quantity = 1;

        if (currentProduct.paymentType == PaymentType.Payment
                && binding.quantityContainer.getVisibility() == View.VISIBLE) {

            try {

                quantity =
                        Integer.parseInt(
                                binding.quantityEditText
                                        .getText()
                                        .toString()
                                        .trim()
                        );

            } catch (Exception ex) {

                binding.quantityEditText
                        .setError(
                                getString(
                                        R.string.invalid_quantity
                                )
                        );

                return;
            }

            if (quantity <= 0) {

                binding.quantityEditText
                        .setError(
                                getString(
                                        R.string.invalid_quantity
                                )
                        );

                return;
            }
        }

        Pair<UUID, Integer>[] items =
                new Pair[]{
                        new Pair<>(
                                currentProduct.id,
                                quantity
                        )
                };

        controller.createPayment(
                items,
                currentProduct.paymentType
        );
    }

    private void handlePaymentResult(
            PaymentSheetResult paymentResult
    ) {

        if (paymentResult
                instanceof PaymentSheetResult.Completed) {

            String productName =
                    currentProduct != null
                            ? productUiMapper
                            .resolveTitle(
                                    currentProduct
                            )
                            : getString(
                            R.string.payment_success
                    );

            runOnUiThread(() ->
                    paymentResultHandler.showSuccess(
                            productName
                    )
            );

            return;
        }

        if (paymentResult
                instanceof PaymentSheetResult.Canceled) {

            runOnUiThread(() ->
                    paymentResultHandler.showError(
                            getString(
                                    R.string.payment_canceled
                            )
                    )
            );

            return;
        }

        if (paymentResult
                instanceof PaymentSheetResult.Failed) {

            PaymentSheetResult.Failed failed =
                    (PaymentSheetResult.Failed)
                            paymentResult;

            String errorMessage =
                    failed.getError() != null
                            ? failed.getError()
                            .getLocalizedMessage()
                            : getString(
                            R.string.payment_failed
                    );

            runOnUiThread(() ->
                    paymentResultHandler.showError(
                            errorMessage
                    )
            );
        }
    }

    private String[] splitPrice(
            String value
    ) {

        int index = 0;

        while (index < value.length()
                && !Character.isDigit(
                value.charAt(index)
        )) {

            index++;
        }

        String currency =
                value.substring(
                        0,
                        index
                );

        String price =
                value.substring(
                        index
                );

        return new String[]{
                currency.trim(),
                price.trim()
        };
    }
}
