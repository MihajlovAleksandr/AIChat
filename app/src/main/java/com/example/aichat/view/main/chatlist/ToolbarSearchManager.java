package com.example.aichat.view.main.chatlist;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

public class ToolbarSearchManager {

    public interface ToolbarSearchListener {
        void onSearch(String query);
        void onCloseSearch();
    }

    private final TextView tvAppName;
    private final EditText etToolbarSearch;
    private final ImageButton btnToolbarSearch;
    private final ImageButton btnToolbarClose;

    private final ToolbarSearchListener listener;

    private boolean isSearchActive = false;

    public ToolbarSearchManager(
            TextView tvAppName,
            EditText etToolbarSearch,
            ImageButton btnToolbarSearch,
            ImageButton btnToolbarClose,
            ToolbarSearchListener listener
    ) {
        this.tvAppName = tvAppName;
        this.etToolbarSearch = etToolbarSearch;
        this.btnToolbarSearch = btnToolbarSearch;
        this.btnToolbarClose = btnToolbarClose;
        this.listener = listener;

        init();
    }

    private void init() {

        // Отключаем long‑press полностью
        btnToolbarSearch.setLongClickable(false);
        btnToolbarSearch.setOnLongClickListener(v -> true);

        btnToolbarSearch.setOnClickListener(v -> {
            if (!isSearchActive) {
                openSearch();
            } else {
                if (listener != null)
                    listener.onSearch(etToolbarSearch.getText().toString().trim());
            }
        });

        btnToolbarClose.setOnClickListener(v -> closeSearch());

        etToolbarSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH && listener != null) {
                listener.onSearch(etToolbarSearch.getText().toString().trim());
                return true;
            }
            return false;
        });

        etToolbarSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isSearchActive && listener != null)
                    listener.onSearch(s.toString().trim());
            }

            @Override public void afterTextChanged(Editable s) {}
        });
    }

    public void openSearch() {
        if (isSearchActive) return;
        isSearchActive = true;

        tvAppName.setVisibility(View.GONE);

        etToolbarSearch.setText("");
        etToolbarSearch.setVisibility(View.VISIBLE);
        etToolbarSearch.requestFocus();

        btnToolbarClose.setVisibility(View.VISIBLE);
        btnToolbarSearch.setVisibility(View.GONE);

        InputMethodManager imm = (InputMethodManager)
                etToolbarSearch.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(etToolbarSearch, InputMethodManager.SHOW_IMPLICIT);
    }

    public void closeSearch() {
        if (!isSearchActive) return;
        isSearchActive = false;

        etToolbarSearch.setText("");
        etToolbarSearch.setVisibility(View.GONE);

        tvAppName.setVisibility(View.VISIBLE);

        btnToolbarClose.setVisibility(View.GONE);
        btnToolbarSearch.setVisibility(View.VISIBLE);

        InputMethodManager imm = (InputMethodManager)
                etToolbarSearch.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etToolbarSearch.getWindowToken(), 0);

        if (listener != null) listener.onCloseSearch();
    }

    public boolean isSearchActive() {
        return isSearchActive;
    }
}
