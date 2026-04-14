package luti.server.application.command;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import luti.server.application.result.CreateTagResult;

public class CreateTagCommand implements ICommand<CreateTagResult> {

	private final Long memberId;
	private final String name;

	@JsonCreator
	public CreateTagCommand(
						@JsonProperty("memberId") Long memberId,
						@JsonProperty("name") String name) {

		this.memberId = memberId;
		this.name = name;
	}

	public Long getMemberId() {
		return memberId;
	}

	public String getName() {
		return name;
	}

}
