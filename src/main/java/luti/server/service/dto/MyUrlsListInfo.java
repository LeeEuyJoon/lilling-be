package luti.server.service.dto;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;

import luti.server.entity.UrlMapping;

public class MyUrlsListInfo {
	private final List<MyUrlItemInfo> urls;
	private final Long totalElements;
	private final Long totalPages;
	private final Long currentPage;
	private final Long pageSize;

	private MyUrlsListInfo(List<MyUrlItemInfo> urls, Long totalElements, Long totalPages,
						   Long currentPage, Long pageSize) {
		this.urls = urls;
		this.totalElements = totalElements;
		this.totalPages = totalPages;
		this.currentPage = currentPage;
		this.pageSize = pageSize;
	}

	public static MyUrlsListInfo from(Page<UrlMapping> page) {
		List<MyUrlItemInfo> items = page.getContent().stream()
										.map(MyUrlItemInfo::from)
										.toList();

		return new MyUrlsListInfo(
			items,
			page.getTotalElements(),
			(long) page.getTotalPages(),
			(long) page.getNumber(),
			(long) page.getSize()
		);
	}

	public List<MyUrlItemInfo> getUrls() {
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

	public static class MyUrlItemInfo {
		private final Long id;
		private final String shortUrl;
		private final String originalUrl;
		private final String description;
		private final LocalDateTime createdAt;
		private final Long clickCount;

		private MyUrlItemInfo(Long id, String shortUrl, String originalUrl,
							  String description, LocalDateTime createdAt, Long clickCount) {
			this.id = id;
			this.shortUrl = shortUrl;
			this.originalUrl = originalUrl;
			this.description = description;
			this.createdAt = createdAt;
			this.clickCount = clickCount;
		}

		public static MyUrlItemInfo from(UrlMapping entity) {
			return new MyUrlItemInfo(
				entity.getId(),
				entity.getShortUrl(),
				entity.getOriginalUrl(),
				entity.getDescription(),
				entity.getCreatedAt(),
				entity.getClickCount()
			);
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
}
