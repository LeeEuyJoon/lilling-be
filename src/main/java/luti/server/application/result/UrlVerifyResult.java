package luti.server.application.result;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import luti.server.domain.enums.VerifyUrlStatus;
import luti.server.domain.service.dto.UrlMappingInfo;

public class UrlVerifyResult {

	@JsonProperty("status")
	private final VerifyUrlStatus status;

	@JsonProperty("originalUrl")
	private final String originalUrl;

	@JsonProperty("shortUrl")
	private final String shortUrl;

	@JsonProperty("clickCount")
	private final Long clickCount;

	@JsonProperty("createdAt")
	private final LocalDateTime createdAt;

	private UrlVerifyResult(VerifyUrlStatus status, String originalUrl, String shortUrl, Long clickCount,
						   LocalDateTime createdAt) {
		this.status = status;
		this.originalUrl = originalUrl;
		this.shortUrl = shortUrl;
		this.clickCount = clickCount;
		this.createdAt = createdAt;
	}

	public static UrlVerifyResult ok(UrlMappingInfo urlMappingInfo) {
		return new UrlVerifyResult(
			VerifyUrlStatus.OK,
			urlMappingInfo.getOriginalUrl(),
			urlMappingInfo.getShortUrl(),
			urlMappingInfo.getClickCount(),
			urlMappingInfo.getCreatedAt()
		);
	}

	public static UrlVerifyResult notFound() {
		return new UrlVerifyResult(VerifyUrlStatus.NOT_FOUND, null, null, null, null);
	}

	public static UrlVerifyResult alreadyOwned() {
		return new UrlVerifyResult(VerifyUrlStatus.ALREADY_OWNED, null, null, null, null);
	}

	public static UrlVerifyResult invalidFormat() {
		return new UrlVerifyResult(VerifyUrlStatus.INVALID_FORMAT, null, null, null, null);
	}

	public VerifyUrlStatus getStatus() {
		return status;
	}

	@JsonProperty("valid")
	public boolean isValid() {
		return status == VerifyUrlStatus.OK;
	}

	public String getOriginalUrl() {
		return originalUrl;
	}

	public String getShortUrl() {
		return shortUrl;
	}

	public Long getClickCount() {
		return clickCount;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	@JsonIgnore
	public boolean isOk() {
		return status == VerifyUrlStatus.OK;
	}
}
