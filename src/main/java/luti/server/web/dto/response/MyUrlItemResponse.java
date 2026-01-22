package luti.server.web.dto.response;

import static luti.server.application.result.MyUrlsListResult.*;
import static luti.server.application.result.MyUrlsListResult.MyUrlItemResult.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import luti.server.application.result.MyUrlsListResult;

public class MyUrlItemResponse {

	private String id;
	private String shortUrl;
	private String originalUrl;
	private String description;
	private LocalDateTime createdAt;
	private Long clickCount;
	private List<DailyStatsSummaryResponse> recentDailyStats;


	public static MyUrlItemResponse from(MyUrlItemResult item) {
		MyUrlItemResponse response = new MyUrlItemResponse();
		response.id = item.getId().toString(); // Long -> String
		response.shortUrl = item.getShortUrl();
		response.originalUrl = item.getOriginalUrl();
		response.description = item.getDescription();
		response.createdAt = item.getCreatedAt();
		response.clickCount = item.getClickCount();
		response.recentDailyStats = item.getRecentDailyStats().stream()
										.map(DailyStatsSummaryResponse::from)
										.toList();
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

	public List<DailyStatsSummaryResponse> getRecentDailyStats() {
		return recentDailyStats;
	}

	public static class DailyStatsSummaryResponse {
		private LocalDate date;
		private Long clickCount;

		public static DailyStatsSummaryResponse from(DailyStatsSummaryResult summary) {
			DailyStatsSummaryResponse response = new DailyStatsSummaryResponse();
			response.date = summary.getDate();
			response.clickCount = summary.getClickCount();
			return response;
		}

		public LocalDate getDate() {
			return date;
		}

		public Long getClickCount() {
			return clickCount;
		}
	}
}
