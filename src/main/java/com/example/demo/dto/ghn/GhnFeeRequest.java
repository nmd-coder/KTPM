package com.example.demo.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GhnFeeRequest(
    @JsonProperty("service_type_id") Integer serviceTypeId,
    @JsonProperty("service_id") Integer serviceId,
    @JsonProperty("from_district_id") Integer fromDistrictId,
    @JsonProperty("from_ward_code") String fromWardCode,
    @JsonProperty("to_district_id") Integer toDistrictId,
    @JsonProperty("to_ward_code") String toWardCode,
    @JsonProperty("weight") Integer weight,
    @JsonProperty("length") Integer length,
    @JsonProperty("width") Integer width,
    @JsonProperty("height") Integer height,
    @JsonProperty("insurance_value") Integer insuranceValue,
    @JsonProperty("cod_value") Integer codValue,
    @JsonProperty("coupon") String coupon
) {}
