package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "momo")
public class MoMoProperties {

	private String baseUrl;
	private String partnerCode;
	private String accessKey;
	private String secretKey;
	private String returnUrl;
	private String notifyUrl;
	private String requestType;
	private String endpointCreate;
	private String endpointQuery;
	private String endpointRefund;

	public String getBaseUrl() { return baseUrl; }
	public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

	public String getPartnerCode() { return partnerCode; }
	public void setPartnerCode(String partnerCode) { this.partnerCode = partnerCode; }

	public String getAccessKey() { return accessKey; }
	public void setAccessKey(String accessKey) { this.accessKey = accessKey; }

	public String getSecretKey() { return secretKey; }
	public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

	public String getReturnUrl() { return returnUrl; }
	public void setReturnUrl(String returnUrl) { this.returnUrl = returnUrl; }

	public String getNotifyUrl() { return notifyUrl; }
	public void setNotifyUrl(String notifyUrl) { this.notifyUrl = notifyUrl; }

	public String getRequestType() { return requestType; }
	public void setRequestType(String requestType) { this.requestType = requestType; }

	public String getEndpointCreate() { return endpointCreate; }
	public void setEndpointCreate(String endpointCreate) { this.endpointCreate = endpointCreate; }

	public String getEndpointQuery() { return endpointQuery; }
	public void setEndpointQuery(String endpointQuery) { this.endpointQuery = endpointQuery; }

	public String getEndpointRefund() { return endpointRefund; }
	public void setEndpointRefund(String endpointRefund) { this.endpointRefund = endpointRefund; }
}
