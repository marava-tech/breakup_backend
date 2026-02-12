package com.breakupstories.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
    /** Cursor for next page (optional). When using cursor-based pagination, pass this as cursor param for next request. */
    private String nextCursor;

    public static <T> PagedResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean last = page >= totalPages - 1;
        return new PagedResponse<>(content, page, size, totalElements, totalPages, last, null);
    }

    public static <T> PagedResponse<T> ofWithCursor(List<T> content, int size, String nextCursor) {
        boolean last = nextCursor == null;
        return new PagedResponse<>(content, 0, size, content.size(), 1, last, nextCursor);
    }
} 