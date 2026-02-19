package luti.server.application.validation.UrlValidation.v2;

import luti.server.domain.service.dto.UrlMappingInfo;

public class UrlValidationContext {
	private final String shortUrl;
	private String shortCode;
	private Long decodedId;
	private UrlMappingInfo urlMappingInfo;

	public UrlValidationContext(String shortUrl) {
		this.shortUrl = shortUrl;
	}

	public String getShortUrl() {
		return shortUrl;
	}

	public String getShortCode() {
		return shortCode;
	}

	public void setShortCode(String shortCode) {
		this.shortCode = shortCode;
	}

	public Long getDecodedId() {
		return decodedId;
	}

	public void setDecodedId(Long decodedId) {
		this.decodedId = decodedId;
	}

	public UrlMappingInfo getUrlMappingInfo() {
		return urlMappingInfo;
	}

	public void setUrlMappingInfo(UrlMappingInfo urlMappingInfo) {
		this.urlMappingInfo = urlMappingInfo;
	}
}
