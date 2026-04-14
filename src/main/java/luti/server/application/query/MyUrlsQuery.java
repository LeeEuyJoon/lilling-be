package luti.server.application.query;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import luti.server.application.result.MyUrlsListResult;

public class MyUrlsQuery implements IQuery<MyUrlsListResult> {

	private final Integer page;
	private final Integer size;
	private final Long memberId;
	private final List<Long> tagIds;
	private final String filterMode;

	@JsonCreator
	public MyUrlsQuery(
					@JsonProperty("page") Integer page,
					@JsonProperty("size") Integer size,
					@JsonProperty("memberId") Long memberId,
					@JsonProperty("tagIds") List<Long> tagIds,
					@JsonProperty("filterMode") String filterMode) {
		this.page = page != null ? page : 0;
		this.size = size != null ? size : 10;
		this.memberId = memberId;
		this.tagIds = tagIds;
		this.filterMode = filterMode != null ? filterMode : "or";
	}

	public boolean isAndMode() {
		return "and".equalsIgnoreCase(filterMode);
	}

	public Integer getPage() {
		return page;
	}

	public Integer getSize() {
		return size;
	}

	public Long getMemberId() {
		return memberId;
	}

	public List<Long> getTagIds() {
		return tagIds;
	}
}
