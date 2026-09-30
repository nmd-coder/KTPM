package com.example.demo.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GhnDistrict(
    @JsonProperty("DistrictID") int districtId,
    @JsonProperty("ProvinceID") int provinceId,
    @JsonProperty("DistrictName") String districtName
) {}
