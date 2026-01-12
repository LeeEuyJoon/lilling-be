package luti.server.facade.result;

import java.time.LocalDateTime;
import java.util.List;

import luti.server.service.dto.MyUrlsListInfo;

public class MyUrlsListResult {
	private final List<MyUrlItem> urls;
	private final Long totalElements;
	private final Long totalPages;
	private final Long currentPage;
	private final Long pageSize;

	private MyUrlsListResult(List<MyUrlItem> urls, Long totalElements, Long totalPages, Long currentPage,
							Long pageSize) {
		this.urls = urls;
		this.totalElements = totalElements;
		this.totalPages = totalPages;
		this.currentPage = currentPage;
		this.pageSize = pageSize;
	}

	public static MyUrlsListResult from(MyUrlsListInfo info) {

		List<MyUrlItem> items = info.getUrls().stream()
				.map(item -> MyUrlItem.of(
						item.getId(),
						item.getShortUrl(),
						item.getOriginalUrl(),
						item.getDescription(),
						item.getCreatedAt(),
						item.getClickCount()
				))
				.toList();

		return new MyUrlsListResult(
				items,
				info.getTotalElements(),
				info.getTotalPages(),
				info.getCurrentPage(),
				info.getPageSize()
		);

	}

	public static class MyUrlItem {
		private final Long id;
		private final String shortUrl;
		private final String originalUrl;
		private final String description;
		private final LocalDateTime createdAt;
		private final Long clickCount;

		private MyUrlItem(Long id, String shortUrl, String originalUrl, String description, LocalDateTime createdAt,
						 Long clickCount) {
			this.id = id;
			this.shortUrl = shortUrl;
			this.originalUrl = originalUrl;
			this.description = description;
			this.createdAt = createdAt;
			this.clickCount = clickCount;
		}

		public static MyUrlItem of(Long id, String shortUrl, String originalUrl, String description,
									   LocalDateTime createdAt, Long clickCount) {
			return new MyUrlItem(id, shortUrl, originalUrl, description, createdAt, clickCount);
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
	}

	public List<MyUrlItem> getUrls() {
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
