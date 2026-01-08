package luti.server.web.dto;

import java.time.LocalDateTime;

import luti.server.enums.VerifyUrlStatus;
import luti.server.facade.dto.UrlVerifyResult;

public class VerifyUrlResponse {

	private final boolean valid;
	private final String originalUrl;
	private final String shortUrl;
	private final Long clickCount;
	private final LocalDateTime createdAt;

	private VerifyUrlResponse(boolean valid, String originalUrl, String shortUrl,
							  Long clickCount, LocalDateTime createdAt) {
		this.valid = valid;
		this.originalUrl = originalUrl;
		this.shortUrl = shortUrl;
		this.clickCount = clickCount;
		this.createdAt = createdAt;
	}

	public static VerifyUrlResponse from(UrlVerifyResult verifyResult) {
		return new VerifyUrlResponse(
			verifyResult.getStatus() == VerifyUrlStatus.OK,
			verifyResult.getOriginalUrl(),
			verifyResult.getShortUrl(),
			verifyResult.getClickCount(),
			verifyResult.getCreatedAt()
		);
	}
}

