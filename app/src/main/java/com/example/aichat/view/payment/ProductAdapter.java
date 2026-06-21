package com.example.aichat.view.payment;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.aichat.model.utils.theme.ThemeAttrResolver;
import com.example.aichat.R;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

public class ProductAdapter
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnProductClickListener {

        void onProductClick(UiProduct product);
    }

    private final List<UiProduct> products =
            new ArrayList<>();

    private final OnProductClickListener listener;

    private final boolean showButton;

    public ProductAdapter(
            OnProductClickListener listener,
            boolean showButton
    ) {

        this.listener = listener;

        this.showButton = showButton;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view =
                LayoutInflater
                        .from(parent.getContext())
                        .inflate(
                                R.layout.item_product,
                                parent,
                                false
                        );

        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder,
            int position
    ) {

        ((ProductViewHolder) holder).bind(
                products.get(position),
                listener,
                showButton
        );
    }

    @Override
    public int getItemCount() {

        return products.size();
    }

    public void submitList(
            List<UiProduct> list
    ) {

        products.clear();

        products.addAll(list);

        notifyDataSetChanged();
    }

    private static class ProductViewHolder
            extends RecyclerView.ViewHolder {

        private final CardView iconCard;

        private final ImageView ivProductIcon;

        private final TextView tvTitle;

        private final TextView tvDescription;

        private final TextView tvCurrency;

        private final TextView tvPrice;

        private final TextView tvBadge;

        private final MaterialButton btnContinue;

        public ProductViewHolder(
                @NonNull View itemView
        ) {

            super(itemView);

            iconCard =
                    itemView.findViewById(
                            R.id.iconCard
                    );

            ivProductIcon =
                    itemView.findViewById(
                            R.id.iv_product_icon
                    );

            tvTitle =
                    itemView.findViewById(
                            R.id.tv_product_title
                    );

            tvDescription =
                    itemView.findViewById(
                            R.id.tv_product_description
                    );

            tvCurrency =
                    itemView.findViewById(
                            R.id.tv_product_currency
                    );

            tvPrice =
                    itemView.findViewById(
                            R.id.tv_product_price
                    );

            tvBadge =
                    itemView.findViewById(
                            R.id.tv_product_badge
                    );

            btnContinue =
                    itemView.findViewById(
                            R.id.btn_continue
                    );
        }

        public void bind(
                UiProduct product,
                OnProductClickListener listener,
                boolean showButton
        ) {

            tvTitle.setText(
                    product.getTitle()
            );

            setupProductVisuals(product);

            applyRuntimeTheme();

            String description =
                    product.getDescription();

            if ((description == null || description.isEmpty())
                    && isCurrencyProduct(product)) {

                description =
                        itemView.getContext().getString(
                                R.string.currency_package_description
                        );
            }

            if (description == null
                    || description.isEmpty()) {

                tvDescription.setVisibility(
                        View.GONE
                );

            } else {

                tvDescription.setVisibility(
                        View.VISIBLE
                );

                tvDescription.setText(
                        description
                );
            }

            String[] priceParts =
                    splitPrice(
                            product.getPrice()
                    );

            tvCurrency.setText(
                    priceParts[0]
            );

            tvPrice.setText(
                    priceParts[1]
            );

            String badge =
                    product.getBadge();

            if (badge == null
                    || badge.isEmpty()) {

                tvBadge.setVisibility(
                        View.GONE
                );

            } else {

                tvBadge.setVisibility(
                        View.VISIBLE
                );

                tvBadge.setText(
                        badge
                );
            }

            if (showButton) {

                btnContinue.setVisibility(
                        View.VISIBLE
                );

                btnContinue.setOnClickListener(v ->
                        listener.onProductClick(
                                product
                        )
                );

            } else {

                btnContinue.setVisibility(
                        View.GONE
                );
            }
        }

        private boolean isCurrencyProduct(@NonNull UiProduct product) {
            String name =
                    product.getName();

            if (name == null) {
                return false;
            }

            return !name.startsWith("sub_");
        }

        private void setupProductVisuals(
                UiProduct product
        ) {

            ivProductIcon.setImageResource(
                    product.getIconRes()
            );

            /*
             * Цвет товара назначается только из Java.
             * Runtime custom theme не должен перетирать iconCard на colorPrimary.
             */
            iconCard.setCardBackgroundColor(
                    product.getAccentColor()
            );

            ivProductIcon.setImageTintList(
                    ColorStateList.valueOf(
                            Color.WHITE
                    )
            );
        }

        private void applyRuntimeTheme() {

            Context context =
                    itemView.getContext();

            int primary =
                    resolveThemeColor(
                            context,
                            com.google.android.material.R.attr.colorPrimary,
                            0xFF20A39A
                    );

            int onPrimary =
                    resolveThemeColor(
                            context,
                            com.google.android.material.R.attr.colorOnPrimary,
                            Color.WHITE
                    );

            int onSurface =
                    resolveThemeColor(
                            context,
                            com.google.android.material.R.attr.colorOnSurface,
                            0xFF1A1A1A
                    );

            int onSurfaceVariant =
                    resolveThemeColor(
                            context,
                            com.google.android.material.R.attr.colorOnSurfaceVariant,
                            0xFF666666
                    );

            tvTitle.setTextColor(
                    onSurface
            );

            tvCurrency.setTextColor(
                    onSurface
            );

            tvPrice.setTextColor(
                    onSurface
            );

            tvDescription.setTextColor(
                    onSurfaceVariant
            );

            tvBadge.setTextColor(
                    onSurface
            );

            btnContinue.setBackgroundTintList(
                    ColorStateList.valueOf(
                            primary
                    )
            );

            btnContinue.setTextColor(
                    onPrimary
            );
        }

        private int resolveThemeColor(
                Context context,
                int attr,
                int fallback
        ) {
            try {
                return ThemeAttrResolver.resolveColor(
                        context,
                        attr
                );
            } catch (Exception ignored) {
                return fallback;
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
                    value.substring(0, index);

            String price =
                    value.substring(index);

            return new String[]{
                    currency,
                    price
            };
        }
    }
}
