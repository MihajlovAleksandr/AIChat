package com.example.aichat.view.theme;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.aichat.databinding.ItemThemeBinding;
import com.example.aichat.model.entities.ThemeType;
import com.example.aichat.model.utils.theme.ThemeModel;
import com.example.aichat.R;
import java.util.ArrayList;
import java.util.List;

public class ThemeListAdapter extends RecyclerView.Adapter<ThemeListAdapter.ThemeViewHolder> {

    private final List<ThemeModel> themes;
    private final boolean myThemesSection;
    private final ThemeActionListener listener;

    private String selectedThemeId;
    private int cardColor = Color.TRANSPARENT;
    private int textColor = Color.WHITE;
    private int secondaryTextColor = Color.GRAY;

    public ThemeListAdapter(
            List<ThemeModel> themes,
            String selectedThemeId,
            boolean myThemesSection,
            ThemeActionListener listener
    ) {
        this.themes = themes != null ? themes : new ArrayList<>();
        this.selectedThemeId = selectedThemeId;
        this.myThemesSection = myThemesSection;
        this.listener = listener;

        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        ThemeModel theme = themes.get(position);

        if (theme == null || theme.getId() == null) {
            return RecyclerView.NO_ID;
        }

        return theme.getId().hashCode();
    }

    @NonNull
    @Override
    public ThemeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemThemeBinding binding = ItemThemeBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );

        return new ThemeViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ThemeViewHolder holder, int position) {
        holder.bind(themes.get(position));
    }

    @Override
    public int getItemCount() {
        return themes.size();
    }

    public void submitList(List<ThemeModel> newThemes) {
        themes.clear();

        if (newThemes != null) {
            themes.addAll(newThemes);
        }

        notifyDataSetChanged();
    }

    public void setSelectedThemeId(String selectedThemeId) {
        this.selectedThemeId = selectedThemeId;
        notifyDataSetChanged();
    }

    public void setThemeColors(int cardColor, int textColor, int secondaryTextColor) {
        this.cardColor = cardColor;
        this.textColor = textColor;
        this.secondaryTextColor = secondaryTextColor;

        notifyDataSetChanged();
    }

    public void setThemes(List<ThemeModel> newThemes) {
        submitList(newThemes);
    }

    class ThemeViewHolder extends RecyclerView.ViewHolder {

        private final ItemThemeBinding binding;

        ThemeViewHolder(ItemThemeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ThemeModel theme) {
            if (theme == null) {
                binding.getRoot().setVisibility(View.GONE);
                return;
            }

            binding.getRoot().setVisibility(View.VISIBLE);

            String themeId = theme.getId() != null
                    ? theme.getId().toString()
                    : null;

            boolean selected = selectedThemeId != null
                    && themeId != null
                    && selectedThemeId.equals(themeId);

            CardView cardView = (CardView) binding.getRoot();
            cardView.setCardBackgroundColor(cardColor);

            binding.tvThemeName.setText(theme.getName());
            binding.tvThemeName.setTextColor(textColor);
            binding.tvThemeName.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);

            binding.tvThemeSubtitle.setText(resolveSubtitle(theme, selected));
            binding.tvThemeSubtitle.setTextColor(secondaryTextColor);

            binding.viewThemeColor.setBackground(createCircleDrawable(theme.getColorPrimary()));

            binding.ivApplied.setVisibility(selected ? View.VISIBLE : View.GONE);
            binding.ivApplied.setColorFilter(textColor);

            binding.btnThemeMenu.setVisibility(View.VISIBLE);
            binding.btnThemeMenu.setColorFilter(textColor);

            binding.getRoot().setOnClickListener(v -> {
                if (selected) {
                    return;
                }

                if (listener != null) {
                    listener.onThemeClick(theme);
                }
            });

            binding.btnThemeMenu.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onThemeMenuClick(v, theme);
                }
            });
        }

        private String resolveSubtitle(ThemeModel theme, boolean selected) {
            String source;

            if (theme != null && theme.getSource() == ThemeType.System) {
                source = itemView.getContext().getString(R.string.theme_source_system);
            } else {
                source = itemView.getContext().getString(
                        myThemesSection ? R.string.theme_source_my : R.string.theme_source_library
                );
            }

            if (selected) {
                return itemView.getContext().getString(R.string.theme_selected_subtitle, source);
            }

            return source;
        }

        private GradientDrawable createCircleDrawable(String color) {
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(parseColor(color));
            drawable.setStroke(2, Color.parseColor("#22FFFFFF"));
            return drawable;
        }

        private int parseColor(String color) {
            try {
                return Color.parseColor(color);
            } catch (Exception ex) {
                return Color.parseColor("#20A39A");
            }
        }
    }

    public interface ThemeActionListener {
        void onThemeClick(ThemeModel theme);

        void onThemeMenuClick(View anchor, ThemeModel theme);
    }
}
