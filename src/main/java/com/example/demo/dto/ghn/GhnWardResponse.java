package com.example.demo.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record GhnWardResponse(
    @JsonProperty("code") String code,
    @JsonProperty("message") String message,
    @JsonProperty("data") List<GhnWard> data
) {}
