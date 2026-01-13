package luti.server.domain.service.dto;

import java.time.LocalDateTime;

import luti.server.domain.model.UrlMapping;

public class UrlMappingInfo {
	private final Long id;
	private final String originalUrl;
	private final String shortUrl;
	private final Long clickCount;
	private final LocalDateTime createdAt;
	private final boolean hasOwner;

	private UrlMappingInfo(Long id, String originalUrl, String shortUrl, Long clickCount, LocalDateTime createdAt, boolean hasOwner) {
		this.id = id;
		this.originalUrl = originalUrl;
		this.shortUrl = shortUrl;
		this.clickCount = clickCount;
		this.createdAt = createdAt;
		this.hasOwner = hasOwner;
	}

	public static UrlMappingInfo from(UrlMapping entity) {
		return new UrlMappingInfo(
			entity.getId(),
			entity.getOriginalUrl(),
			entity.getShortUrl(),
			entity.getClickCount(),
			entity.getCreatedAt(),
			entity.getMember() != null
		);
	}
	public Long getId() {
		return id;
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
