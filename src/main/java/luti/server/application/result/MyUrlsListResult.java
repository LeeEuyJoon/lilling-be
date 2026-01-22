package luti.server.application.result;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import luti.server.domain.service.dto.MyUrlsListInfo;
import luti.server.domain.service.dto.RecentDailyStatisticsInfo;

public class MyUrlsListResult {
	private final List<MyUrlItemResult> urls;
	private final Long totalElements;
	private final Long totalPages;
	private final Long currentPage;
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

		List<MyUrlItemResult> items = urlsInfo.getUrls().stream()
										.map(item -> {
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

											// 통계 포함하여 MyUrlItemResult 생성
											return MyUrlItemResult.of(
												item.getId(),
												item.getShortUrl(),
												item.getOriginalUrl(),
												item.getDescription(),
												item.getCreatedAt(),
												item.getClickCount(),
												summaries
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
		private final Long id;
		private final String shortUrl;
		private final String originalUrl;
		private final String description;
		private final LocalDateTime createdAt;
		private final Long clickCount;
		private final List<DailyStatsSummaryResult> recentDailyStats;

		private MyUrlItemResult(Long id, String shortUrl, String originalUrl, String description, LocalDateTime createdAt,
						  Long clickCount, List<DailyStatsSummaryResult> recentDailyStats) {
			this.id = id;
			this.shortUrl = shortUrl;
			this.originalUrl = originalUrl;
			this.description = description;
			this.createdAt = createdAt;
			this.clickCount = clickCount;
			this.recentDailyStats = recentDailyStats;
		}

		public static MyUrlItemResult of(Long id, String shortUrl, String originalUrl, String description,
								   LocalDateTime createdAt, Long clickCount,
								   List<DailyStatsSummaryResult> recentDailyStats) {
			return new MyUrlItemResult(id, shortUrl, originalUrl, description, createdAt, clickCount, recentDailyStats);
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

		public static class DailyStatsSummaryResult {
			private final LocalDate date;
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
