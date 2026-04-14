package luti.server.application.query;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import luti.server.application.result.UrlAnalyticsResult;

public class UrlAnalyticsQuery implements IQuery<UrlAnalyticsResult> {

	private final Long urlId;
	private final Long memberId;

	@JsonCreator
	public UrlAnalyticsQuery(
					@JsonProperty("urlId") Long urlId,
					@JsonProperty("memberId") Long memberId) {
		this.urlId = urlId;
		this.memberId = memberId;
	}

	public Long getUrlId() {
		return urlId;
	}

	public Long getMemberId() {
		return memberId;
	}
}
