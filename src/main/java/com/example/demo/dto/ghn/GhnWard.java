package com.example.demo.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GhnWard(
    @JsonProperty("WardCode") String wardCode,
    @JsonProperty("DistrictID") int districtId,
    @JsonProperty("WardName") String wardName
) {}
