package com.example.demo.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record GhnProvinceResponse(
    @JsonProperty("code") String code,
    @JsonProperty("message") String message,
    @JsonProperty("data") List<GhnProvince> data
) {}
