package com.example.aichat.view.payment;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.aichat.dto.response.PremiumInfoResponse;
import com.example.aichat.dto.response.UserPremiumResponse;
import com.example.aichat.model.connection.ConnectionDispatcher;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.HttpClient;
import com.example.aichat.R;
import com.example.aichat.view.main.BaseActivity;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.LocalDateTime;
import java.util.List;

public class PremiumListActivity extends BaseActivity {

    private ConnectionDispatcher dispatcher;

    private LinearLayout premiumHeader;
    private LinearLayout noPremiumLayout;
    private TextView tvPremiumActivePeriod;
    private TextView tvNextPayment;
    private TextView tvAutoRenewText;
    private TextView tvCancelSubscription;
    private ImageView ivAutoRenew;
    private RecyclerView rvPremiumList;
    private PremiumListAdapter adapter;

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy");
    private DateTimeFormatter dateFormatterShort = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private PremiumInfoResponse currentPremium;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_premium_list);

        dispatcher = ConnectionSingleton.getInstance().getConnectionDispatcher();

        setupInsets();
        initViews();
        setupClickListeners();
        loadPremiumData();
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

    private void initViews() {
        premiumHeader = findViewById(R.id.premium_header);
        noPremiumLayout = findViewById(R.id.no_premium_layout);
        tvPremiumActivePeriod = findViewById(R.id.tv_premium_active_period);
        tvNextPayment = findViewById(R.id.tv_next_payment);
        tvAutoRenewText = findViewById(R.id.tv_auto_renew_text);
        tvCancelSubscription = findViewById(R.id.tv_cancel_subscription);
        ivAutoRenew = findViewById(R.id.iv_auto_renew);
        rvPremiumList = findViewById(R.id.rv_premium_list);

        rvPremiumList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PremiumListAdapter();
        rvPremiumList.setAdapter(adapter);
    }

    private void setupClickListeners() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        Button btnActivatePremium = findViewById(R.id.btn_activate_premium);

        btnBack.setOnClickListener(v -> finish());

        btnActivatePremium.setOnClickListener(v -> finish());

        // Cancel subscription click listener
        tvCancelSubscription.setOnClickListener(v -> showCancelSubscriptionDialog());
    }

    private void showCancelSubscriptionDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.cancel_subscription_dialog_title)
                .setMessage(R.string.cancel_subscription_dialog_message)
                .setPositiveButton(R.string.cancel_subscription_confirm,
                        (dialog, which) -> cancelSubscription())
                .setNegativeButton(R.string.keep_subscription, null)
                .setCancelable(true)
                .show();
    }

    private void cancelSubscription() {
        tvCancelSubscription.setEnabled(false);
        tvCancelSubscription.setAlpha(0.45f);

        dispatcher.sendHttpRequestAsync(
                "/api/payments/cancel-subscription",
                HttpClient.HTTPMethod.POST,
                null,
                true
        ).thenAccept(cmd -> {
            runOnUiThread(() -> {
                tvCancelSubscription.setEnabled(true);
                tvCancelSubscription.setAlpha(1f);

                if (cmd.getCode() == 200) {
                    Toast.makeText(this, R.string.subscription_cancelled_success, Toast.LENGTH_SHORT).show();
                    updateUIAfterCancellation();
                } else {
                    Toast.makeText(this, R.string.subscription_cancel_error, Toast.LENGTH_SHORT).show();
                }
            });
        }).exceptionally(throwable -> {
            runOnUiThread(() -> {
                tvCancelSubscription.setEnabled(true);
                tvCancelSubscription.setAlpha(1f);
                Toast.makeText(this, R.string.connection_error, Toast.LENGTH_SHORT).show();
            });
            return null;
        });
    }

    private void updateUIAfterCancellation() {
        if (currentPremium != null) {
            currentPremium =
                    new PremiumInfoResponse(
                            currentPremium.startTime,
                            currentPremium.endTime,
                            false
                    );

            showActivePremium(currentPremium);
        } else {
            ivAutoRenew.setImageResource(R.drawable.ic_close);
            tvAutoRenewText.setText(R.string.premium_autorenew_off);
            tvNextPayment.setVisibility(View.GONE);
            tvCancelSubscription.setVisibility(View.GONE);
        }

        loadPremiumHistory();
    }

    private void loadPremiumData() {
        loadActivePremium();
        loadPremiumHistory();
    }

    private void loadActivePremium() {
        dispatcher.sendHttpRequestAsync(
                "/api/user/premium",
                HttpClient.HTTPMethod.GET,
                null,
                true
        ).thenAccept(cmd -> {
            if (cmd.getCode() == 204) {
                runOnUiThread(() -> showNoPremium(true));
                return;
            }

            if (!cmd.isSuccess()) {
                runOnUiThread(() -> showNoPremium(false));
                return;
            }

            PremiumInfoResponse premium = cmd.getData(PremiumInfoResponse.class);
            currentPremium = premium;
            runOnUiThread(() -> showActivePremium(premium));

        }).exceptionally(throwable -> {
            runOnUiThread(() -> showNoPremium(false));
            return null;
        });
    }

    private void loadPremiumHistory() {
        dispatcher.sendHttpRequestAsync(
                "/api/user/premium/history",
                HttpClient.HTTPMethod.GET,
                null,
                true
        ).thenAccept(cmd -> {
            if (!cmd.isSuccess()) return;

            UserPremiumResponse[] history = cmd.getData(UserPremiumResponse[].class);
            runOnUiThread(() -> {
                if (history != null && history.length > 0) {
                    adapter.setPremiumList(List.of(history));
                }
            });
        });
    }

    private void showActivePremium(PremiumInfoResponse premium) {
        premiumHeader.setVisibility(View.VISIBLE);
        noPremiumLayout.setVisibility(View.GONE);

        LocalDateTime start = toLocalDateTime(premium.startTime);
        LocalDateTime end = toLocalDateTime(premium.endTime);

        String startDate = (start == null) ? "—" : start.format(dateFormatter);
        String endDate = (end == null) ? "—" : end.format(dateFormatter);

        if (premium.isAutoRenew) {
            tvPremiumActivePeriod.setText(
                    getString(
                            R.string.premium_active_period_format,
                            startDate,
                            endDate
                    )
            );

            ivAutoRenew.setImageResource(R.drawable.ic_check);
            tvAutoRenewText.setText(R.string.premium_autorenew_on);
            tvCancelSubscription.setVisibility(View.VISIBLE);
            tvCancelSubscription.setEnabled(true);
            tvCancelSubscription.setAlpha(1f);

            String nextPaymentDate = (end == null) ? "—" : end.format(dateFormatter);
            tvNextPayment.setText(
                    getString(
                            R.string.premium_next_payment_format,
                            nextPaymentDate
                    )
            );
            tvNextPayment.setVisibility(View.VISIBLE);

        } else {
            tvPremiumActivePeriod.setText(
                    getString(
                            R.string.premium_access_until_after_cancel,
                            endDate
                    )
            );

            ivAutoRenew.setImageResource(R.drawable.ic_close);
            tvAutoRenewText.setText(R.string.premium_autorenew_off);
            tvCancelSubscription.setVisibility(View.GONE);
            tvNextPayment.setVisibility(View.GONE);
        }
    }

    private void showNoPremium(boolean hasAutoRenewChecked) {
        premiumHeader.setVisibility(View.GONE);
        noPremiumLayout.setVisibility(View.VISIBLE);

        if (!hasAutoRenewChecked) {
            Toast.makeText(this, R.string.premium_load_error, Toast.LENGTH_SHORT).show();
        }
    }

    private class PremiumListAdapter extends RecyclerView.Adapter<PremiumListAdapter.PremiumViewHolder> {

        private List<UserPremiumResponse> premiumList;

        public void setPremiumList(List<UserPremiumResponse> list) {
            this.premiumList = list;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public PremiumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_premium, parent, false);
            return new PremiumViewHolder(view);
        }

        @Override
        public void onBindViewHolder(PremiumViewHolder holder, int position) {
            holder.bind(premiumList.get(position));
        }

        @Override
        public int getItemCount() {
            return premiumList == null ? 0 : premiumList.size();
        }

        class PremiumViewHolder extends RecyclerView.ViewHolder {

            private TextView tvPremiumId;
            private TextView tvPremiumPeriod;
            private TextView tvAutoRenewStatus;
            private Button btnViewPayment;

            public PremiumViewHolder(View itemView) {
                super(itemView);
                tvPremiumId = itemView.findViewById(R.id.tv_premium_id);
                tvPremiumPeriod = itemView.findViewById(R.id.tv_premium_period);
                tvAutoRenewStatus = itemView.findViewById(R.id.tv_auto_renew_status);
                btnViewPayment = itemView.findViewById(R.id.btn_view_payment);
            }

            public void bind(UserPremiumResponse premium) {

                LocalDateTime start = toLocalDateTime(premium.startTime);
                LocalDateTime end = toLocalDateTime(premium.endTime);

                String startDate = (start == null) ? "—" : start.format(dateFormatterShort);
                String endDate = (end == null) ? "—" : end.format(dateFormatterShort);

                tvPremiumPeriod.setText(startDate + " - " + endDate);

                tvAutoRenewStatus.setText(
                        premium.isAutoRenew
                                ? R.string.premium_history_autorenew_on
                                : R.string.premium_history_autorenew_off
                );

                String shortId = premium.id.toString().substring(0, 8);
                tvPremiumId.setText(
                        getString(
                                R.string.premium_history_item_title,
                                shortId
                        )
                );

                btnViewPayment.setOnClickListener(v ->
                        loadPaymentDetails(premium.id.toString())
                );
            }
        }
    }

    private void loadPaymentDetails(String premiumId) {
        dispatcher.sendHttpRequestAsync(
                "/api/payments/premium/" + premiumId,
                HttpClient.HTTPMethod.GET,
                null,
                true
        ).thenAccept(cmd -> {
            if (!cmd.isSuccess()) {
                runOnUiThread(() ->
                        Toast.makeText(this, R.string.payment_load_error, Toast.LENGTH_SHORT).show()
                );
                return;
            }

            runOnUiThread(() -> {
                Intent intent = new Intent(this, PaymentDetailsActivity.class);
                intent.putExtra("premium_id", premiumId);
                startActivity(intent);
            });
        }).exceptionally(throwable -> {
            runOnUiThread(() ->
                    Toast.makeText(this, R.string.connection_error, Toast.LENGTH_SHORT).show()
            );
            return null;
        });
    }

    private LocalDateTime toLocalDateTime(String dateTime) {
        if (dateTime == null || dateTime.isEmpty()) {
            return null;
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            return LocalDateTime.parse(dateTime, formatter);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
