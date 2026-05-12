package com.example.aichat.view.main.chatlist;

import android.content.Context;
import android.graphics.Rect;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.function.Consumer;

public class ChatSearchManager {

    public interface ChatSearchListener {
        void onSearch(String query);
    }

    private Consumer<Boolean> searchStateListener;

    public void setOnSearchStateChangedListener(Consumer<Boolean> listener) {
        this.searchStateListener = listener;
    }

    private final ImageButton btnSearch;
    private final ConstraintLayout searchLayout;
    private final ImageButton btnBack;
    private final EditText etSearch;

    private final RecyclerView recyclerView;
    private final ConstraintLayout rootLayout;
    private final ChatSearchListener listener;

    private boolean isSearchActive = false;
    private boolean isVisibleFromScroll = true;
    private int searchHeight = 0;
    private View itemDecorationView;

    private static final float HIDE_THRESHOLD_FACTOR = 0.2f;

    public ChatSearchManager(
            ImageButton btnSearch,
            ConstraintLayout searchLayout,
            ImageButton btnBack,
            EditText etSearch,
            RecyclerView recyclerView,
            ConstraintLayout rootLayout,
            ChatSearchListener listener
    ) {
        this.btnSearch = btnSearch;
        this.searchLayout = searchLayout;
        this.btnBack = btnBack;
        this.etSearch = etSearch;
        this.recyclerView = recyclerView;
        this.rootLayout = rootLayout;
        this.listener = listener;

        init();
    }

    private void init() {

        btnSearch.setOnLongClickListener(v -> {
            openSearch();
            return true;
        });

        btnBack.setOnClickListener(v -> {
            etSearch.setText("");
            listener.onSearch("");
            closeSearch();
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                listener.onSearch(etSearch.getText().toString().trim());
                return true;
            }
            return false;
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                listener.onSearch(query.isEmpty() ? "" : query);
            }

            @Override public void afterTextChanged(Editable s) {}
        });

        // Отключаем перехват touch events у searchLayout когда он скрыт
        setupSearchLayoutTouchInterceptor();

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView rv, int dx, int dy) {

                if (isSearchActive) return;
                if (btnSearch.getVisibility() != View.VISIBLE) return;

                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                if (lm == null) return;

                if (rv.getAdapter() != null && rv.getAdapter().getItemCount() == 0) {
                    showFromScroll(true);
                    return;
                }

                int firstVisible = lm.findFirstVisibleItemPosition();

                if (firstVisible == 0) {
                    RecyclerView.ViewHolder vh = rv.findViewHolderForAdapterPosition(0);
                    if (vh != null) {
                        int top = vh.itemView.getTop();
                        int threshold = (int) (vh.itemView.getHeight() * HIDE_THRESHOLD_FACTOR);

                        if (-top >= threshold) {
                            if (isVisibleFromScroll) showFromScroll(false);
                        } else {
                            if (!isVisibleFromScroll) showFromScroll(true);
                        }
                    }
                } else {
                    if (isVisibleFromScroll) showFromScroll(false);
                }
            }
        });

        searchLayout.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        searchLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                        searchHeight = searchLayout.getHeight();

                        // Используем ItemDecoration для отступа первого элемента
                        recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
                            @Override
                            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                                if (parent.getChildAdapterPosition(view) == 0) {
                                    outRect.top = searchHeight;
                                }
                            }
                        });

                        searchLayout.setTranslationY(0);
                        searchLayout.setAlpha(1f);
                        searchLayout.setVisibility(View.VISIBLE);

                        btnSearch.setVisibility(View.VISIBLE);
                        btnSearch.setAlpha(0f);
                        btnSearch.setTranslationY(-5000f);

                        isVisibleFromScroll = true;
                    }
                }
        );
    }

    private void setupSearchLayoutTouchInterceptor() {
        searchLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Если searchbar скрыт или не виден из-за скролла - пропускаем все события
                if (!isVisibleFromScroll && !isSearchActive) {
                    return true; // Потребляем событие, но не передаем дальше
                }

                // Если searchbar виден и активен - обрабатываем нормально
                if (isSearchActive) {
                    return false; // Передаем события дальше для обработки внутренними view
                }

                // Если searchbar частично виден при скролле - тоже пропускаем клики
                if (searchLayout.getTranslationY() < 0 && !isSearchActive) {
                    return true;
                }

                return false;
            }
        });

        // Также отключаем кликабельность для контейнера
        searchLayout.setClickable(false);
        searchLayout.setFocusable(false);

        // Убеждаемся, что внутренние элементы не блокируют клики когда не нужно
        btnBack.setClickable(true);
        etSearch.setClickable(true);
    }

    public void showFromScroll(boolean show) {
        if (show == isVisibleFromScroll) return;

        isVisibleFromScroll = show;

        float targetY = show ? 0 : -searchHeight;
        float targetAlpha = show ? 1f : 0f;

        searchLayout.animate().cancel();

        searchLayout.animate()
                .translationY(targetY)
                .alpha(targetAlpha)
                .setDuration(200)
                .withStartAction(() -> {
                    if (show) {
                        searchLayout.setVisibility(View.VISIBLE);
                    }
                })
                .withEndAction(() -> {
                    if (!show) {
                        searchLayout.setVisibility(View.GONE);
                        // Важно: после скрытия убеждаемся, что searchLayout не перехватывает клики
                        searchLayout.setTranslationY(-searchHeight);
                    }
                })
                .start();

        btnSearch.animate().cancel();
        btnSearch.setVisibility(View.VISIBLE);
        btnSearch.animate()
                .alpha(show ? 0f : 1f)
                .translationY(show ? -5000f : 0f)
                .setDuration(200)
                .start();
    }

    public void openSearch() {

        btnSearch.setVisibility(View.GONE);
        btnSearch.setEnabled(false);
        btnSearch.setClickable(false);
        btnSearch.setLongClickable(false);
        btnSearch.setFocusable(false);
        btnSearch.setFocusableInTouchMode(false);
        btnSearch.setOnTouchListener((v, e) -> true);

        if (searchStateListener != null) searchStateListener.accept(true);

        searchLayout.setAlpha(1f);
        searchLayout.setTranslationY(0);
        searchLayout.setVisibility(View.VISIBLE);
        // При открытии поиска включаем нормальную обработку кликов
        searchLayout.setClickable(true);

        btnBack.setVisibility(View.VISIBLE);

        isSearchActive = true;

        InputMethodManager imm = (InputMethodManager)
                etSearch.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null)
            imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT);
    }

    public void closeSearch() {

        btnSearch.setVisibility(View.VISIBLE);
        btnSearch.setEnabled(true);
        btnSearch.setClickable(true);
        btnSearch.setLongClickable(true);
        btnSearch.setFocusable(true);
        btnSearch.setFocusableInTouchMode(true);
        btnSearch.setOnTouchListener(null);

        if (searchStateListener != null) searchStateListener.accept(false);

        btnBack.setVisibility(View.GONE);

        isSearchActive = false;

        listener.onSearch("");

        showFromScroll(false);

        // После закрытия поиска снова отключаем кликабельность searchLayout
        searchLayout.setClickable(false);

        InputMethodManager imm = (InputMethodManager)
                etSearch.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null)
            imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
    }

    public boolean isSearchActive() {
        return isSearchActive;
    }

    public boolean isVisibleFromScroll() {
        return isVisibleFromScroll;
    }
}