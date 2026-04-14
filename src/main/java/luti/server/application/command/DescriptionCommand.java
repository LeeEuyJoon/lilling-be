package luti.server.application.command;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DescriptionCommand implements ICommand<Void> {

	private final Long urlId;
	private final Long memberId;
	private final String description;

	@JsonCreator
	public DescriptionCommand(
						@JsonProperty("urlId") Long urlId,
						@JsonProperty("memberId") Long memberId,
						@JsonProperty("description") String description) {
		this.urlId = urlId;
		this.memberId = memberId;
		this.description = description;
	}

	public Long getUrlId() {
		return urlId;
	}

	public Long getMemberId() {
		return memberId;
	}

	public String getDescription() {
		return description;
	}
}
