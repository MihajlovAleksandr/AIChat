package com.example.aichat.view.payment;

import android.content.Context;
import android.graphics.Color;
import androidx.annotation.DrawableRes;
import com.example.aichat.dto.response.ProductResponse;
import com.example.aichat.R;
import java.math.BigDecimal;

import static com.example.aichat.model.payment.PaymentType.Payment;
import static com.example.aichat.model.payment.PaymentType.SingleItem;
import static com.example.aichat.model.payment.PaymentType.Subscription;

public class ProductUiMapper {

    private final Context context;

    public ProductUiMapper(
            Context context
    ) {

        this.context = context;
    }

    public UiProduct map(
            ProductResponse response
    ) {

        return new UiProduct(
                response.id.toString(),
                response.name,
                resolveTitle(response),
                resolveDescription(response),
                formatPrice(
                        response.price,
                        response.currency
                ),
                resolveBadge(response),
                resolveIcon(response),
                resolveColorByName(
                        response.name
                )
        );
    }

    public String resolveTitle(
            ProductResponse response
    ) {

        if (response.paymentType == Subscription) {

            switch (response.name) {

                case "sub_month_title":
                    return context.getString(
                            R.string.subscription_month
                    );

                case "sub_3_month_title":
                    return context.getString(
                            R.string.subscription_3_months
                    );

                case "sub_year_title":
                    return context.getString(
                            R.string.subscription_year
                    );

                default:
                    return context.getString(
                            R.string.subscription
                    );
            }
        }

        if (response.paymentType == Payment) {

            switch (response.name) {

                case "point_100_title":
                    return context.getString(
                            R.string.currency_100
                    );

                case "point_500_title":
                    return context.getString(
                            R.string.currency_500
                    );

                case "point_1000_title":
                    return context.getString(
                            R.string.currency_1000
                    );

                default:
                    return context.getString(
                            R.string.currency_package
                    );
            }
        }

        if (response.paymentType == SingleItem) {

            switch (response.name) {

                case "models_ollamaqwen3_4B_title":
                    return context.getString(
                            R.string.models_ollamaqwen3_4B_title
                    );

                case "models_ollamallama3_title":
                    return context.getString(
                            R.string.models_ollamallama3_title
                    );

                case "models_ollamamistral_title":
                    return context.getString(
                            R.string.models_ollamamistral_title
                    );

                case "models_ollamagemma4е_title":
                    return context.getString(
                            R.string.models_ollamagemma4е_title
                    );

                default:
                    return context.getString(
                            R.string.single_item
                    );
            }
        }

        return response.name;
    }

    public String resolveDescription(
            ProductResponse response
    ) {

        if (response.paymentType == Subscription) {

            switch (response.name) {

                case "sub_month_title":
                    return context.getString(
                            R.string.subscription_month_description
                    );

                case "sub_3_month_title":
                    return context.getString(
                            R.string.subscription_3_months_description
                    );

                case "sub_year_title":
                    return context.getString(
                            R.string.subscription_year_description
                    );

                default:
                    return "";
            }
        }

        if (response.paymentType == Payment) {

            switch (response.name) {

                case "point_100_title":
                    return context.getString(
                            R.string.currency_100_description
                    );

                case "point_500_title":
                    return context.getString(
                            R.string.currency_500_description
                    );

                case "point_1000_title":
                    return context.getString(
                            R.string.currency_1000_description
                    );

                default:
                    return "";
            }
        }

        if (response.paymentType == SingleItem) {

            switch (response.name) {

                case "models_ollamaqwen3_4B_title":
                    return context.getString(
                            R.string.models_ollamaqwen3_4B_description
                    );

                case "models_ollamallama3_title":
                    return context.getString(
                            R.string.models_ollamallama3_description
                    );

                case "models_ollamamistral_title":
                    return context.getString(
                            R.string.models_ollamamistral_description
                    );

                case "models_ollamagemma4е_title":
                    return context.getString(
                            R.string.models_ollamagemma4е_description
                    );

                default:
                    return "";
            }
        }

        return "";
    }

    public String resolveBadge(
            ProductResponse response
    ) {

        if (response.name.equals(
                "sub_month_title"
        ) || response.name.equals(
                "point_100_title"
        )) {

            return context.getString(
                    R.string.badge_popular
            );
        }

        if (response.name.equals(
                "point_1000_title"
        )) {

            return context.getString(
                    R.string.badge_best_value
            );
        }

        return "";
    }

    @DrawableRes
    public int resolveIcon(
            ProductResponse response
    ) {

        if (response.name != null
                && response.name.startsWith("sub_")) {

            return R.drawable.ic_premium_crown;
        }

        if (response.name != null
                && response.name.startsWith("models_")) {

            return R.drawable.ic_ai;
        }

        return R.drawable.ic_currency;
    }

    public int resolveColorByName(
            String name
    ) {

        switch (name) {

            case "sub_month_title":
                return Color.parseColor(
                        "#159B97"
                );

            case "sub_3_month_title":
                return Color.parseColor(
                        "#5E72E4"
                );

            case "sub_year_title":
                return Color.parseColor(
                        "#F59E0B"
                );

            case "point_100_title":
                return Color.parseColor(
                        "#10B981"
                );

            case "point_500_title":
                return Color.parseColor(
                        "#8B5CF6"
                );

            case "point_1000_title":
                return Color.parseColor(
                        "#EF4444"
                );

            case "models_ollamaqwen3_4B_title":
                return Color.parseColor(
                        "#3B82F6"
                );

            case "models_ollamallama3_title":
                return Color.parseColor(
                        "#8B5CF6"
                );

            case "models_ollamamistral_title":
                return Color.parseColor(
                        "#EC4899"
                );

            case "models_ollamagemma4е_title":
                return Color.parseColor(
                        "#F59E0B"
                );

            default:
                return Color.parseColor(
                        "#159B97"
                );
        }
    }

    public String formatPrice(
            BigDecimal price,
            String currency
    ) {

        return currency + " "
                + price.toPlainString();
    }
}
