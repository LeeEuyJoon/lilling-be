package luti.server.application.command;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DeleteTagCommand implements ICommand<Void> {

	private final Long memberId;
	private final Long tagId;

	@JsonCreator
	public DeleteTagCommand(
						@JsonProperty("memberId") Long memberId,
						@JsonProperty("tagId") Long tagId) {
		this.memberId = memberId;
		this.tagId = tagId;
	}

	public Long getMemberId() {
		return memberId;
	}

	public Long getTagId() {
		return tagId;
	}
}
