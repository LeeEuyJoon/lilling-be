package luti.server.application.command;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UpdateTagCommand implements ICommand<Void> {

	private final Long memberId;
	private final Long tagId;
	private final String name;

	@JsonCreator
	public UpdateTagCommand(
						@JsonProperty("memberId") Long memberId,
						@JsonProperty("tagId") Long tagId,
						@JsonProperty("name") String name) {
		this.memberId = memberId;
		this.tagId = tagId;
		this.name = name;
	}

	public Long getMemberId() {
		return memberId;
	}

	public Long getTagId() {
		return tagId;
	}

	public String getName() {
		return name;
	}

}
