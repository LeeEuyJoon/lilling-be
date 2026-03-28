package luti.server.application.command;

import java.util.List;

public class MyUrlsCommand {
	private final Integer page;
	private final Integer size;
	private final Long memberId;
	private final List<Long> tagIds;
	private final String filterMode;

	private MyUrlsCommand(Integer page, Integer size, Long memberId, List<Long> tagIds, String filterMode) {
		this.page = page;
		this.size = size;
		this.memberId = memberId;
		this.tagIds = tagIds;
		this.filterMode = filterMode;
	}

	public static MyUrlsCommand of(Integer page, Integer size, Long memberId, List<Long> tagIds, String filterMode) {
		return new MyUrlsCommand(page, size, memberId, tagIds, filterMode);
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
