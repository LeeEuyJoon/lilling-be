package luti.server.application.command.legacy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import luti.server.application.command.ICommand;

public class ClaimUrlCommand implements ICommand<Void> {

	private final Long memberId;
	private final String shortUrl;

	@JsonCreator
	public ClaimUrlCommand(
						@JsonProperty("memberId") Long memberId,
						@JsonProperty("shortUrl") String shortUrl) {
		this.memberId = memberId;
		this.shortUrl = shortUrl;
	}

	public Long getMemberId() {
		return memberId;
	}

	public String getShortUrl() {
		return shortUrl;
	}
}
