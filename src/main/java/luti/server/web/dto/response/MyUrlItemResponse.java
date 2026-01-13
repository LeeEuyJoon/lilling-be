package luti.server.web.dto.response;

import java.time.LocalDateTime;

import luti.server.application.result.MyUrlsListResult;

public class MyUrlItemResponse {

	private String id;
	private String shortUrl;
	private String originalUrl;
	private String description;
	private LocalDateTime createdAt;
	private Long clickCount;

	public static MyUrlItemResponse from(MyUrlsListResult.MyUrlItem item) {
		MyUrlItemResponse response = new MyUrlItemResponse();
		response.id = item.getId().toString(); // Long -> String
		response.shortUrl = item.getShortUrl();
		response.originalUrl = item.getOriginalUrl();
		response.description = item.getDescription();
		response.createdAt = item.getCreatedAt();
		response.clickCount = item.getClickCount();
		return response;
	}

	public String getId() {
		return id;
	}

	public String getShortUrl() {
		return shortUrl;
	}

	public String getOriginalUrl() {
		return originalUrl;
	}

	public String getDescription() {
		return description;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public Long getClickCount() {
		return clickCount;
	}
}
