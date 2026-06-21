package com.example.aichat.view.payment;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.aichat.controller.payment.PaymentDetailsController;
import com.example.aichat.databinding.ActivityPaymentDetailsBinding;
import com.example.aichat.dto.response.PaymentItemResponse;
import com.example.aichat.dto.response.PaymentResponse;
import com.example.aichat.dto.response.ProductResponse;
import com.example.aichat.model.payment.PaymentStatuses;
import com.example.aichat.model.payment.PaymentType;
import com.example.aichat.R;
import com.example.aichat.view.main.BaseActivity;
import com.example.aichat.view.theme.binders.PaymentScreensThemeBinder;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class PaymentDetailsActivity
        extends BaseActivity {

    public static final String EXTRA_PAYMENT_ID =
            "payment_id";
    public static final String EXTRA_PREMIUM_ID =
            "premium_id";

    private ActivityPaymentDetailsBinding binding;

    private PaymentDetailsController controller;

    private ProductAdapter itemsAdapter;

    private ProductUiMapper productUiMapper;

    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState
    ) {

        super.onCreate(savedInstanceState);

        binding =
                ActivityPaymentDetailsBinding.inflate(
                        getLayoutInflater()
                );

        setContentView(
                binding.getRoot()
        );

        PaymentScreensThemeBinder.applyPaymentDetails(
                this,
                binding
        );

        productUiMapper =
                new ProductUiMapper(this);

        controller =
                new PaymentDetailsController(this);

        setupInsets();

        setupRecyclerView();

        setupListeners();

        setupBackPressedHandler();

        String paymentId =
                getIntent().getStringExtra(
                        EXTRA_PAYMENT_ID
                );

        if (paymentId != null) {

            controller.getPaymentById(
                    paymentId
            );
        }
        else{
            String premiumId =
                    getIntent().getStringExtra(
                            EXTRA_PREMIUM_ID
                    );
            if(premiumId!=null)
                controller.getPaymentByPremiumId(
                        premiumId
                );
        }
    }

    @Override
    protected void onResume() {

        super.onResume();

        PaymentScreensThemeBinder.applyPaymentDetails(
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

    private void setupRecyclerView() {

        itemsAdapter =
                new ProductAdapter(
                        product -> {
                        },
                        false
                );

        binding.rvItems.setLayoutManager(
                new LinearLayoutManager(this)
        );

        binding.rvItems.setAdapter(
                itemsAdapter
        );
    }

    private void setupListeners() {

        binding.btnBack.setOnClickListener(
                v -> finish()
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
                .addCallback(this, callback);
    }

    public void renderPayment(
            PaymentResponse payment
    ) {

        runOnUiThread(() -> {

            binding.tvPaymentId.setText(
                    String.valueOf(
                            payment.id
                    )
            );

            binding.tvAmount.setText(
                    productUiMapper.formatPrice(
                            payment.amount,
                            payment.currency
                    )
            );

            binding.tvCurrency.setText(
                    payment.currency
            );

            setupStatus(
                    payment.status
            );

            if (payment.createdAt != null) {

                binding.tvCreatedAt.setText(
                        payment.createdAt
                );
            }

            List<UiProduct> items =
                    new ArrayList<>();

            if (payment.items != null) {

                for (PaymentItemResponse item
                        : payment.items) {

                    ProductResponse product =
                            item.product;

                    if (product == null) {
                        continue;
                    }

                    String productName =
                            product.name;

                    if (product.paymentType
                            == PaymentType.Payment
                            && item.quantity > 0) {

                        productName =
                                product.name
                                        + " x"
                                        + item.quantity;
                    }

                    ProductResponse mappedProduct =
                            new ProductResponse(
                                    product.id,
                                    productName,
                                    product.name != null
                                            && product.name.startsWith(
                                            "sub_"
                                    )
                                            ? PaymentType.Subscription
                                            : PaymentType.Payment,
                                    product.price != null
                                            ? product.price
                                            : BigDecimal.ZERO,
                                    product.currency,
                                    product.description
                            );

                    items.add(
                            productUiMapper.map(
                                    mappedProduct
                            )
                    );
                }
            }

            itemsAdapter.submitList(
                    items
            );

            if (payment.stripeInvoiceUrl != null
                    && !payment.stripeInvoiceUrl.isEmpty()) {

                binding.btnOpenInvoice.setVisibility(
                        android.view.View.VISIBLE
                );

                binding.btnOpenInvoice
                        .setOnClickListener(v -> {

                            Intent intent =
                                    new Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse(
                                                    payment.stripeInvoiceUrl
                                            )
                                    );

                            startActivity(
                                    intent
                            );
                        });

            } else {

                binding.btnOpenInvoice.setVisibility(
                        android.view.View.GONE
                );
            }

            PaymentScreensThemeBinder.applyPaymentDetails(
                    this,
                    binding
            );
        });
    }

    private void setupStatus(
            PaymentStatuses status
    ) {

        binding.tvStatus.setTextColor(
                Color.WHITE
        );

        if (status == null) {

            binding.tvStatus.setText(
                    R.string.payment_status_pending
            );

            binding.statusContainer
                    .setBackgroundResource(
                            R.drawable.bg_payment_status_pending
                    );

            return;
        }

        if (status == PaymentStatuses.Confirmed){

            binding.tvStatus.setText(
                    R.string.payment_status_confirmed
            );

            binding.statusContainer
                    .setBackgroundResource(
                            R.drawable.bg_payment_status_confirmed
                    );

            return;
        }

        if (status == PaymentStatuses.Pending) {

            binding.tvStatus.setText(
                    R.string.payment_status_pending
            );

            binding.statusContainer
                    .setBackgroundResource(
                            R.drawable.bg_payment_status_pending
                    );

            return;
        }

        if (status == PaymentStatuses.Failed) {

            binding.tvStatus.setText(
                    R.string.payment_status_failed
            );

            binding.statusContainer
                    .setBackgroundResource(
                            R.drawable.bg_payment_status_failed
                    );

            return;
        }

        if (status == PaymentStatuses.Expired) {

            binding.tvStatus.setText(
                    R.string.payment_status_expired
            );

            binding.statusContainer
                    .setBackgroundResource(
                            R.drawable.bg_payment_status_failed
                    );

            return;
        }

        binding.tvStatus.setText(
                R.string.payment_status_pending
        );

        binding.statusContainer
                .setBackgroundResource(
                        R.drawable.bg_payment_status_pending
                );
    }

    public void showError(
            String message
    ) {

        runOnUiThread(() ->
                Toast.makeText(
                        this,
                        message,
                        Toast.LENGTH_SHORT
                ).show()
        );
    }
}
