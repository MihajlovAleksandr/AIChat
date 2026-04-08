package com.example.aichat.view.main.chatlist;

import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class FabScrollManager {

    private final FloatingActionButton fab;
    private final RecyclerView recyclerView;
    private boolean isVisibleFromScroll = true;
    private int fabHeight = 0;

    private static final float HIDE_THRESHOLD_FACTOR = 0.35f;

    public FabScrollManager(FloatingActionButton fab, RecyclerView recyclerView) {
        this.fab = fab;
        this.recyclerView = recyclerView;
        init();
    }

    private void init() {
        fab.post(() -> fabHeight = fab.getHeight());

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                if (lm == null) return;

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

        fab.setAlpha(1f);
        fab.setTranslationY(0f);
    }

    public void showFromScroll(boolean show) {
        if (show == isVisibleFromScroll) return;
        isVisibleFromScroll = show;

        float targetY = show ? 0f : fabHeight + 32f;
        float targetAlpha = show ? 1f : 0f;

        fab.animate().cancel();
        fab.animate()
                .translationY(targetY)
                .alpha(targetAlpha)
                .setDuration(200)
                .start();
    }

    public boolean isVisibleFromScroll() { return isVisibleFromScroll; }
}