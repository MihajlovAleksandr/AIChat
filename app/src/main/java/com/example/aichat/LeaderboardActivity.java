package com.example.aichat;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.example.aichat.view.main.BaseActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.aichat.controller.LeaderboardController;
import com.example.aichat.dto.response.LeaderboardItemResponse;
import com.example.aichat.dto.response.PaginationItem;
import com.example.aichat.dto.response.UserDataResponse;
import com.example.aichat.dto.response.UserInfoResponse;
import com.example.aichat.model.entities.Gender;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LeaderboardActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private LeaderboardAdapter adapter;
    private TextView paginationInfo;
    private ImageButton btnFirst, btnPrevious, btnNext, btnLast;
    private Spinner pageSizeSpinner;
    private ProgressBar paginationProgress;
    private LinearLayout emptyState;
    private LinearLayout loadingState;
    private LeaderboardController controller;

    private List<LeaderboardItemResponse> currentPageItems = new ArrayList<>();

    private int currentPage = 1;
    private int pageSize = 10;
    private int totalPages = 1;
    private int totalItems = 0;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        initViews();
        setupRecyclerView();
        setupPaginationControls();
        setupPageSizeSpinner();

        controller = new LeaderboardController(this);
        loadLeaderboardData();
    }

    private void initViews() {
        ImageButton backButton = findViewById(R.id.back_button);
        recyclerView = findViewById(R.id.leaders_recycler_view);
        paginationInfo = findViewById(R.id.pagination_info);
        btnFirst = findViewById(R.id.pagination_first);
        btnPrevious = findViewById(R.id.pagination_previous);
        btnNext = findViewById(R.id.pagination_next);
        btnLast = findViewById(R.id.pagination_last);
        pageSizeSpinner = findViewById(R.id.page_size_spinner);
        paginationProgress = findViewById(R.id.pagination_progress);
        emptyState = findViewById(R.id.empty_state);
        loadingState = findViewById(R.id.loading_state);
        backButton.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new LeaderboardAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupPaginationControls() {
        btnFirst.setOnClickListener(v -> {
            if (currentPage != 1 && !isLoading) {
                currentPage = 1;
                loadLeaderboardData();
            }
        });

        btnPrevious.setOnClickListener(v -> {
            if (currentPage > 1 && !isLoading) {
                currentPage--;
                loadLeaderboardData();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (currentPage < totalPages && !isLoading) {
                currentPage++;
                loadLeaderboardData();
            }
        });

        btnLast.setOnClickListener(v -> {
            if (currentPage != totalPages && !isLoading) {
                currentPage = totalPages;
                loadLeaderboardData();
            }
        });
    }

    private void setupPageSizeSpinner() {
        Integer[] pageSizes = {1, 5, 10, 15, 20, 25, 50};
        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, pageSizes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        pageSizeSpinner.setAdapter(adapter);
        pageSizeSpinner.setSelection(2);

        pageSizeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isLoading) {
                    int newPageSize = (Integer) parent.getItemAtPosition(position);
                    if (pageSize != newPageSize) {
                        pageSize = newPageSize;
                        currentPage = 1;
                        loadLeaderboardData();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadLeaderboardData() {
        if (isLoading) return;

        showLoading(true);
        isLoading = true;

        controller.getThemes(pageSize, currentPage);
    }

    public void setLeaderboard(PaginationItem<LeaderboardItemResponse> paginationItem) {
        isLoading = false;
        showLoading(false);

        if (paginationItem == null || paginationItem.data == null || paginationItem.data.length == 0) {
            if (currentPage == 1) {
                showEmptyState(true);
                currentPageItems.clear();
                adapter.updateData(currentPageItems);
                totalItems = 0;
                totalPages = 1;
            } else {
                Toast.makeText(this, "Нет данных на этой странице", Toast.LENGTH_SHORT).show();
                if (currentPage > 1) {
                    currentPage--;
                    loadLeaderboardData();
                }
            }
            updatePaginationInfo();
            updatePaginationButtons();
            return;
        }

        showEmptyState(false);

        currentPageItems = Arrays.asList(paginationItem.data);
        adapter.updateData(currentPageItems);
        totalItems = paginationItem.totalCount;
        totalPages = paginationItem.totalPages;
        currentPage = paginationItem.currentPage;
        if (pageSize != paginationItem.pageSize) {
            pageSize = paginationItem.pageSize;
            updatePageSizeSpinnerSelection(pageSize);
        }

        updatePaginationInfo();
        updatePaginationButtons();
    }

    private void updatePageSizeSpinnerSelection(int newPageSize) {
        Integer[] pageSizes = {1, 5, 10, 15, 20, 25, 50};
        for (int i = 0; i < pageSizes.length; i++) {
            if (pageSizes[i] == newPageSize) {
                pageSizeSpinner.setSelection(i, false);
                break;
            }
        }
    }

    public void setLeaderboardError(String errorMessage) {
        isLoading = false;
        showLoading(false);
        Toast.makeText(this, "Ошибка загрузки: " + errorMessage, Toast.LENGTH_SHORT).show();

        if (currentPage == 1) {
            showEmptyState(true);
        }

        updatePaginationButtons();
    }

    private void updatePaginationInfo() {
        if (totalItems == 0) {
            paginationInfo.setText("0 / 0");
            return;
        }

        int startItem = (currentPage - 1) * pageSize + 1;
        int endItem = Math.min(currentPage * pageSize, totalItems);

        paginationInfo.setText(String.format("%d-%d / %d", startItem, endItem, totalItems));
    }

    private void updatePaginationButtons() {
        btnFirst.setEnabled(currentPage != 1 && !isLoading);
        btnPrevious.setEnabled(currentPage > 1 && !isLoading);
        btnNext.setEnabled(currentPage < totalPages && !isLoading);
        btnLast.setEnabled(currentPage != totalPages && !isLoading);

        float activeAlpha = 1.0f;
        float inactiveAlpha = 0.3f;

        btnFirst.setAlpha(btnFirst.isEnabled() ? activeAlpha : inactiveAlpha);
        btnPrevious.setAlpha(btnPrevious.isEnabled() ? activeAlpha : inactiveAlpha);
        btnNext.setAlpha(btnNext.isEnabled() ? activeAlpha : inactiveAlpha);
        btnLast.setAlpha(btnLast.isEnabled() ? activeAlpha : inactiveAlpha);
    }

    private void showLoading(boolean show) {
        if (loadingState != null) {
            loadingState.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
        if (paginationProgress != null) {
            paginationProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void showEmptyState(boolean show) {
        if (emptyState != null) {
            emptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

        private List<LeaderboardItemResponse> items = new ArrayList<>();

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_leader, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            LeaderboardItemResponse item = items.get(position);
            if (item != null && item.userInfo != null && item.userInfo.userData != null) {
                String nameWithAge = String.format("%s, %d",
                        item.userInfo.userData.name,
                        item.userInfo.userData.age);
                holder.name.setText(nameWithAge);

                holder.age.setText(String.format("%d лет", item.userInfo.userData.age));

                String region = item.userInfo.region != null ? item.userInfo.region : "Не указан";
                holder.region.setText(region);

                holder.points.setText(String.valueOf(item.pointsCount));
            } else {
                holder.name.setText("Неизвестно");
                holder.age.setText("--");
                holder.region.setText("Не указан");
                holder.points.setText("0");
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public void updateData(List<LeaderboardItemResponse> newItems) {
            this.items = new ArrayList<>(newItems);
            notifyDataSetChanged();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView name;
            TextView age;
            TextView region;
            TextView points;

            ViewHolder(View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.leader_name);
                age = itemView.findViewById(R.id.leader_age);
                region = itemView.findViewById(R.id.leader_region);
                points = itemView.findViewById(R.id.leader_points);
            }
        }
    }
}
