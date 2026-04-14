package luti.server.application.command.legacy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import luti.server.application.command.ICommand;

public class DeleteUrlCommand implements ICommand<Void> {

	private final Long memberId;
	private final Long urlId;

	@JsonCreator
	public DeleteUrlCommand(
						@JsonProperty("memberId") Long memberId,
						@JsonProperty("urlId") Long urlId) {
		this.memberId = memberId;
		this.urlId = urlId;
	}

	public Long getMemberId() {
		return memberId;
	}

	public Long getUrlId() {
		return urlId;
	}
}
