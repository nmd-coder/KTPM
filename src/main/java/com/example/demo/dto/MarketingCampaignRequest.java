package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketingCampaignRequest {
    private String segment; // "VIP", "NEW", "AT_RISK"
    private String campaignName; // VD: "Nhớ Bạn", "Chào Mừng"
    private String voucherCode; // VD: "COMEBACK15"
    private BigDecimal discountValue;
    private String discountType; // "PERCENTAGE" hoặc "FIXED"
}
