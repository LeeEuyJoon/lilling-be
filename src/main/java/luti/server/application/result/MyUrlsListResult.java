package luti.server.application.result;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import luti.server.domain.service.dto.MyUrlsListInfo;
import luti.server.domain.service.dto.RecentDailyStatisticsInfo;
import luti.server.domain.service.dto.TagInfo;

public class MyUrlsListResult {

	@JsonProperty("urls")
	private final List<MyUrlItemResult> urls;

	@JsonProperty("totalElements")
	private final Long totalElements;

	@JsonProperty("totalPages")
	private final Long totalPages;

	@JsonProperty("currentPage")
	private final Long currentPage;

	@JsonProperty("pageSize")
	private final Long pageSize;

	private MyUrlsListResult(List<MyUrlItemResult> urls, Long totalElements, Long totalPages, Long currentPage,
							 Long pageSize) {
		this.urls = urls;
		this.totalElements = totalElements;
		this.totalPages = totalPages;
		this.currentPage = currentPage;
		this.pageSize = pageSize;
	}

	public static MyUrlsListResult from(MyUrlsListInfo urlsInfo, RecentDailyStatisticsInfo statsInfo) {
		return from(urlsInfo, Map.of(), statsInfo);
	}

	public static MyUrlsListResult from(MyUrlsListInfo urlsInfo, Map<Long, List<TagInfo>> tagsMap,
										RecentDailyStatisticsInfo statsInfo) {

		List<MyUrlItemResult> items = urlsInfo.getUrls().stream()
										.map(item -> {
											// 태그 조회
											List<TagInfo> tagInfos = tagsMap.getOrDefault(item.getId(), List.of());
											List<CreateTagResult> tags = tagInfos.stream().map(CreateTagResult::from).toList();

											// 해당 URL의 통계 조회
											List<RecentDailyStatisticsInfo.DailyStat> stats =
												statsInfo.getStatisticsForUrl(item.getId());

											// DailyStat → DailyStatsSummaryResult 변환
											List<MyUrlItemResult.DailyStatsSummaryResult> summaries = stats.stream()
																							   .map(
																								   stat -> MyUrlItemResult.DailyStatsSummaryResult
																									   .of(
																										   LocalDate
																											   .parse(
																												   stat
																													   .getDate()),
																										   stat
																											   .getClickCount()
																									   ))
																							   .toList();

											return MyUrlItemResult.of(
												item.getId(),
												item.getShortUrl(),
												item.getOriginalUrl(),
												item.getDescription(),
												item.getCreatedAt(),
												item.getClickCount(),
												summaries,
												tags
											);
										})
										.toList();

		return new MyUrlsListResult(
			items,
			urlsInfo.getTotalElements(),
			urlsInfo.getTotalPages(),
			urlsInfo.getCurrentPage(),
			urlsInfo.getPageSize()
		);
	}

	public static class MyUrlItemResult {

		@JsonProperty("id")
		private final Long id;

		@JsonProperty("shortUrl")
		private final String shortUrl;

		@JsonProperty("originalUrl")
		private final String originalUrl;

		@JsonProperty("description")
		private final String description;

		@JsonProperty("createdAt")
		private final LocalDateTime createdAt;

		@JsonProperty("clickCount")
		private final Long clickCount;

		@JsonProperty("recentDailyStats")
		private final List<DailyStatsSummaryResult> recentDailyStats;

		@JsonProperty("tags")
		private final List<CreateTagResult> tags;

		private MyUrlItemResult(Long id, String shortUrl, String originalUrl, String description, LocalDateTime createdAt,
						  Long clickCount, List<DailyStatsSummaryResult> recentDailyStats, List<CreateTagResult> tags) {
			this.id = id;
			this.shortUrl = shortUrl;
			this.originalUrl = originalUrl;
			this.description = description;
			this.createdAt = createdAt;
			this.clickCount = clickCount;
			this.recentDailyStats = recentDailyStats;
			this.tags = tags;
		}

		public static MyUrlItemResult of(Long id, String shortUrl, String originalUrl, String description,
								   LocalDateTime createdAt, Long clickCount,
								   List<DailyStatsSummaryResult> recentDailyStats, List<CreateTagResult> tags) {
			return new MyUrlItemResult(id, shortUrl, originalUrl, description, createdAt, clickCount, recentDailyStats, tags);
		}

		public Long getId() {
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

		public List<DailyStatsSummaryResult> getRecentDailyStats() {
			return recentDailyStats;
		}

		public List<CreateTagResult> getTags() {
			return tags;
		}

		public static class DailyStatsSummaryResult {

			@JsonProperty("date")
			private final LocalDate date;

			@JsonProperty("clickCount")
			private final Long clickCount;

			private DailyStatsSummaryResult(LocalDate date, Long clickCount) {
				this.date = date;
				this.clickCount = clickCount;
			}

			public static DailyStatsSummaryResult of(LocalDate date, Long clickCount) {
				return new DailyStatsSummaryResult(date, clickCount);
			}

			public LocalDate getDate() {
				return date;
			}

			public Long getClickCount() {
				return clickCount;
			}
		}
	}

	public List<MyUrlItemResult> getUrls() {
		return urls;
	}

	public Long getTotalElements() {
		return totalElements;
	}

	public Long getTotalPages() {
		return totalPages;
	}

	public Long getCurrentPage() {
		return currentPage;
	}

	public Long getPageSize() {
		return pageSize;
	}
}
