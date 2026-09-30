package com.example.demo.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GhnProvince(
    @JsonProperty("ProvinceID") int provinceId,
    @JsonProperty("ProvinceName") String provinceName
) {}
