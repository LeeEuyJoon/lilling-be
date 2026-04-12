package luti.server.application.command;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import luti.server.application.result.ShortenUrlResult;

public class ShortenUrlCommand implements ICommand<ShortenUrlResult> {

	private final Long memberId;
	private final String originalUrl;
	private final String keyword;

	@JsonCreator
	public ShortenUrlCommand(
						@JsonProperty("memberId") Long memberId,
						@JsonProperty("originalUrl") String originalUrl,
						@JsonProperty("keyword") String keyword) {

		this.memberId = memberId;
		this.originalUrl = originalUrl;
		this.keyword = keyword;
	}

	public Long getMemberId() {
		return memberId;
	}
	public String getOriginalUrl() {
		return originalUrl;
	}
	public String getKeyword() {
		return keyword;
	}
}
