package com.example.demo.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MarketingCampaignResponse {
    private boolean success;
    private String message;
    private int recipientCount;
    private String voucherCode;
    private int emailsSent;
    private int emailsFailed;
}
