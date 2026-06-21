package com.example.aichat.view.main.chat.helpers;

import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import com.example.aichat.model.entities.Message;
import com.example.aichat.R;
import com.example.aichat.view.main.chat.ChatFragment;
import java.util.List;

@androidx.media3.common.util.UnstableApi
public class ChatSearchUi {

    private final ChatFragment fragment;
    private final View root;

    private final View searchPanel;
    private final View resultPanel;

    private final EditText searchInput;
    private final TextView tvQuery;
    private final TextView tvCounter;

    private final ImageButton btnNext;
    private final ImageButton btnPrev;
    private final ImageButton btnBackSearch;
    private final ImageButton btnBackResult;

    private List<Message> foundMessages;
    private int currentIndex = 0;

    public ChatSearchUi(View root, ChatFragment fragment) {
        this.root = root;
        this.fragment = fragment;

        searchPanel = root.findViewById(R.id.search_layout);
        resultPanel = root.findViewById(R.id.searchResult_layout);

        searchInput = root.findViewById(R.id.et_search);
        tvQuery = root.findViewById(R.id.tv_title);
        tvCounter = root.findViewById(R.id.tv_searchResultCount);

        btnNext = root.findViewById(R.id.btn_bottom);
        btnPrev = root.findViewById(R.id.btn_top);
        btnBackSearch = root.findViewById(R.id.search_btn_back);
        btnBackResult = root.findViewById(R.id.searchResult_btn_back);

        initListeners();
    }

    private void initListeners() {

        // Поиск
        View btnSearch = root.findViewById(R.id.btn_search_action);
        if (btnSearch != null) {
            btnSearch.setOnClickListener(v -> {
                String query = searchInput.getText().toString().trim();
                if (!query.isEmpty()) {
                    fragment.onSearchQuery(query);
                }
            });
        }

        btnBackSearch.setOnClickListener(v -> fragment.onHideSearchPanel());
        btnBackResult.setOnClickListener(v -> fragment.onHideSearchResults());
        btnNext.setOnClickListener(v -> next());
        btnPrev.setOnClickListener(v -> prev());
    }
    public void showSearchPanel() {
        if (!fragment.isAdded()) return;

        UiAnimations.hideSearchPanelImmediately(resultPanel);
        UiAnimations.animateSearchPanelOpen(searchPanel, null);

        searchInput.setText("");
        foundMessages = null;
        currentIndex = 0;
        updateCounter();
    }

    public void hideSearchPanel() {
        if (!fragment.isAdded()) return;
        UiAnimations.animateSearchPanelClose(searchPanel, null);
    }

    public void showResults(List<Message> messages, String query) {
        if (!fragment.isAdded()) return;

        this.foundMessages = messages;
        this.currentIndex = 0;

        tvQuery.setText(query);
        updateCounter();

        UiAnimations.hideSearchPanelImmediately(searchPanel);
        UiAnimations.animateSearchPanelOpen(resultPanel, null);

        updateButtonsState();

        if (!messages.isEmpty()) {
            fragment.onHighlightMessage(messages.get(0));
        }
    }

    public void hideResults() {
        if (!fragment.isAdded()) return;

        UiAnimations.animateSearchPanelClose(resultPanel, () ->
                UiAnimations.animateSearchPanelOpen(searchPanel, null)
        );

        foundMessages = null;
        currentIndex = 0;
        updateCounter();
        updateButtonsState();
    }

    public void next() {
        if (!fragment.isAdded() || foundMessages == null || foundMessages.isEmpty()) return;

        if (currentIndex < foundMessages.size() - 1) {
            currentIndex++;
            updateCounter();
            updateButtonsState();
            fragment.onHighlightMessage(foundMessages.get(currentIndex));
        }
    }

    public void prev() {
        if (!fragment.isAdded() || foundMessages == null || foundMessages.isEmpty()) return;

        if (currentIndex > 0) {
            currentIndex--;
            updateCounter();
            updateButtonsState();
            fragment.onHighlightMessage(foundMessages.get(currentIndex));
        }
    }
    private void updateCounter() {
        if (!fragment.isAdded()) return;

        if (foundMessages == null || foundMessages.isEmpty()) {
            tvCounter.setText(fragment.getString(R.string.search_counter_empty));
        } else {
            tvCounter.setText(fragment.getString(
                    R.string.search_counter,
                    currentIndex + 1,
                    foundMessages.size()
            ));
        }
    }

    private void updateButtonsState() {
        boolean hasResults = foundMessages != null && !foundMessages.isEmpty();

        btnNext.setEnabled(hasResults && currentIndex < foundMessages.size() - 1);
        btnPrev.setEnabled(hasResults && currentIndex > 0);
    }
}
