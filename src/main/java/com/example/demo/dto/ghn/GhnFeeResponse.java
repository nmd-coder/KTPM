package com.example.demo.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GhnFeeResponse(
    @JsonProperty("code") String code,
    @JsonProperty("message") String message,
    @JsonProperty("data") GhnFeeData data
) {}
