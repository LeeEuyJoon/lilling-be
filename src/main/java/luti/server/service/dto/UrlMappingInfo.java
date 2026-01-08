package luti.server.service.dto;

import java.time.LocalDateTime;

import luti.server.entity.UrlMapping;

public class UrlMappingInfo {
	private final String originalUrl;
	private final String shortUrl;
	private final Long clickCount;
	private final LocalDateTime createdAt;
	private final boolean hasOwner;

	public UrlMappingInfo(String originalUrl, String shortUrl, Long clickCount, LocalDateTime createdAt, boolean b) {
		this.originalUrl = originalUrl;
		this.shortUrl = shortUrl;
		this.clickCount = clickCount;
		this.createdAt = createdAt;
		this.hasOwner = b;
	}

	public static UrlMappingInfo from(UrlMapping entity) {
		return new UrlMappingInfo(
			entity.getOriginalUrl(),
			entity.getShortUrl(),
			entity.getClickCount(),
			entity.getCreatedAt(),
			entity.getMember() != null
		);
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

	public boolean isHasOwner() {
		return hasOwner;
	}
}
