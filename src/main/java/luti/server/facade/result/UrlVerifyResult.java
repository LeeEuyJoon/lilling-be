package luti.server.facade.result;

import java.time.LocalDateTime;

import luti.server.enums.VerifyUrlStatus;
import luti.server.service.dto.UrlMappingInfo;

public class UrlVerifyResult {

	private final VerifyUrlStatus status;
	private final String originalUrl;
	private final String shortUrl;
	private final Long clickCount;
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
}
