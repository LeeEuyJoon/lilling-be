package luti.server.web.dto.response;

import java.time.LocalDateTime;

import luti.server.domain.enums.VerifyUrlStatus;
import luti.server.application.result.UrlVerifyResult;

public class VerifyUrlResponse {

	private final VerifyUrlStatus status;
	private final boolean valid;
	private final String originalUrl;
	private final String shortUrl;
	private final Long clickCount;
	private final LocalDateTime createdAt;

	private VerifyUrlResponse(VerifyUrlStatus status, boolean valid, String originalUrl, String shortUrl,
							  Long clickCount, LocalDateTime createdAt) {
		this.status = status;
		this.valid = valid;
		this.originalUrl = originalUrl;
		this.shortUrl = shortUrl;
		this.clickCount = clickCount;
		this.createdAt = createdAt;
	}

	public static VerifyUrlResponse from(UrlVerifyResult verifyResult) {
		return new VerifyUrlResponse(
			verifyResult.getStatus(),
			verifyResult.getStatus() == VerifyUrlStatus.OK,
			verifyResult.getOriginalUrl(),
			verifyResult.getShortUrl(),
			verifyResult.getClickCount(),
			verifyResult.getCreatedAt()
		);
	}

	public VerifyUrlStatus getStatus() {
		return status;
	}

	public boolean isValid() {
		return valid;
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

