package luti.server.web.dto.response;

import java.time.LocalDate;
import java.util.List;

import luti.server.application.result.MyUrlsListResult;

public class MyUrlsListResponse {

	private List<MyUrlItemResponse> urls;
	private Long totalElements;
	private Long totalPages;
	private Long currentPage;
	private Long pageSize;

	public static MyUrlsListResponse from(MyUrlsListResult result) {
		MyUrlsListResponse response = new MyUrlsListResponse();
		response.urls = result.getUrls().stream()
				.map(MyUrlItemResponse::from)
				.toList();
		response.totalElements = result.getTotalElements();
		response.totalPages = result.getTotalPages();
		response.currentPage = result.getCurrentPage();
		response.pageSize = result.getPageSize();

		return response;
	}

	public List<MyUrlItemResponse> getUrls() {
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
