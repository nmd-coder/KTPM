package com.example.demo.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO: kết quả tạo đánh giá hàng loạt
 */
public class BulkReviewResponse {

    private List<ReviewResponse> created = new ArrayList<>();
    private List<Long> skippedItemIds = new ArrayList<>();

    public List<ReviewResponse> getCreated() {
        return created;
    }

    public void setCreated(List<ReviewResponse> created) {
        this.created = created;
    }

    public List<Long> getSkippedItemIds() {
        return skippedItemIds;
    }

    public void setSkippedItemIds(List<Long> skippedItemIds) {
        this.skippedItemIds = skippedItemIds;
    }
}


