package luti.server.application.command;

import java.util.List;

public class UnassignTagsCommand {

	private final Long memberId;
	private final Long urlId;
	private final List<Long> tagIds;

	public UnassignTagsCommand(Long memberId, Long urlId, List<Long> tagIds) {
		this.memberId = memberId;
		this.urlId = urlId;
		this.tagIds = tagIds;
	}

	public Long getMemberId() {
		return memberId;
	}

	public Long getUrlId() {
		return urlId;
	}

	public List<Long> getTagIds() {
		return tagIds;
	}

}
