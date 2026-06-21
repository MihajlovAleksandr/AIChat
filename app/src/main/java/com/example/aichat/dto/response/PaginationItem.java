package com.example.aichat.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PaginationItem<T> {
    public final T[] data;
    public final int totalCount;
    public final int currentPage;
    public final int totalPages;
    public final int pageSize;

    public PaginationItem(
            @JsonProperty("data") T[] data,
            @JsonProperty("totalCount") int totalCount,
            @JsonProperty("currentPage") int currentPage,
            @JsonProperty("totalPages") int totalPages,
            @JsonProperty("pageSize") int pageSize){
        this.data = data;
        this.totalCount = totalCount;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.pageSize = pageSize;
    }
}
