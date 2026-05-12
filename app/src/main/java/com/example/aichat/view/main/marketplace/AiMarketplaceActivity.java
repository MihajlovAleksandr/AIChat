package com.example.aichat.view.main.marketplace;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aichat.R;
import com.example.aichat.view.FullScreenHelper;
import com.example.aichat.view.main.chatlist.FabScrollManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class AiMarketplaceActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AiMarketplaceAdapter adapter;
    private FloatingActionButton fab;
    private ImageView btnBack;
    private ImageView menu;
    private ImageView search;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.Theme_AIChat);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_ai_marketplace);

        // FULLSCREEN режим
        FullScreenHelper.enableFullScreen(getWindow());

        applyInsets();

        initViews();
        initListeners();
        initRecycler();
        initFab();
        setupFabPadding();
        setupFabScrollManager();
        loadMockData();
    }

    /**
     * Адаптация под статус бар и вырезы (Pixel 9 Pro и др.)
     */
    private void applyInsets() {
        View root = findViewById(android.R.id.content);

        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int topInset = insets.getInsets(WindowInsets.Type.statusBars()).top;
            int bottomInset = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;

            // Применяем отступы только для корневого layout
            findViewById(R.id.root_constraint_layout).setPadding(0, topInset, 0, bottomInset);

            return insets;
        });
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerMarketplace);
        fab = findViewById(R.id.fabAdd);
        btnBack = findViewById(R.id.btnBack);
        menu = findViewById(R.id.menu);
        search = findViewById(R.id.search);
    }

    private void initListeners() {
        // Кнопка "Назад" - закрыть активити и вернуться в список чатов
        btnBack.setOnClickListener(v -> finish());

        // Кнопка меню - показать дополнительное меню
        menu.setOnClickListener(v -> {
            // TODO: открыть боковое меню или показать PopupMenu
            Toast.makeText(this, "Меню (в разработке)", Toast.LENGTH_SHORT).show();
        });

        // Кнопка поиска - активировать поиск
        search.setOnClickListener(v -> {
            // TODO: открыть поиск через overlay_search_bar
            Toast.makeText(this, "Поиск (в разработке)", Toast.LENGTH_SHORT).show();
        });
    }

    private void initRecycler() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AiMarketplaceAdapter();
        recyclerView.setAdapter(adapter);
    }

    private void initFab() {
        fab.setOnClickListener(v -> {
            // TODO: экран создания AI модели
            Toast.makeText(this, "Создание AI модели (в разработке)", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Настройка правильного отступа под FAB
     * RecyclerView знает высоту FAB и адаптируется под неё
     */
    private void setupFabPadding() {
        recyclerView.post(() -> {
            int fabHeight = fab.getHeight();
            int margin = ((ViewGroup.MarginLayoutParams) fab.getLayoutParams()).bottomMargin;
            int extra = dpToPx(16); // небольшой дополнительный отступ

            int bottomPadding = fabHeight + margin + extra;

            recyclerView.setPadding(
                    recyclerView.getPaddingLeft(),
                    recyclerView.getPaddingTop(),
                    recyclerView.getPaddingRight(),
                    bottomPadding
            );

            recyclerView.setClipToPadding(false);
        });
    }

    /**
     * Подключение FabScrollManager для скрытия FAB при скролле
     */
    private void setupFabScrollManager() {
        new FabScrollManager(fab, recyclerView);
    }

    /**
     * Конвертация dp в px
     */
    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    /**
     * Временные данные (пока нет backend)
     */
    private void loadMockData() {
        List<AiModel> list = new ArrayList<>();

        list.add(new AiModel(
                "GPT-4 Vision Pro",
                "Мультимодальная модель",
                "₽ 14.90 / мес.",
                "4.8",
                "128",
                "12.4K"
        ));

        list.add(new AiModel(
                "AI Writer Pro",
                "Генерация текста",
                "₽ 9.90 / мес.",
                "4.6",
                "98",
                "8.1K"
        ));

        list.add(new AiModel(
                "Image Gen X",
                "Генерация изображений",
                "₽ 19.90 / мес.",
                "4.9",
                "210",
                "15.7K"
        ));

        list.add(new AiModel(
                "Claude 3 Sonnet",
                "Анализ документов",
                "₽ 12.90 / мес.",
                "4.7",
                "156",
                "9.8K"
        ));

        list.add(new AiModel(
                "Midjourney Pro",
                "Генерация искусства",
                "₽ 24.90 / мес.",
                "4.9",
                "342",
                "28.3K"
        ));

        adapter.setItems(list);
    }
}