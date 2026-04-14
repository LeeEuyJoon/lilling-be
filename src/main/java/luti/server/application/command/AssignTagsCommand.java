package luti.server.application.command;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AssignTagsCommand implements ICommand<Void> {

	private final Long memberId;
	private final Long urlId;
	private final List<Long> tagIds;

	@JsonCreator
	public AssignTagsCommand(
						@JsonProperty("memberId") Long memberId,
						@JsonProperty("urlId") Long urlId,
						@JsonProperty("tagIds") List<Long> tagIds) {
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
