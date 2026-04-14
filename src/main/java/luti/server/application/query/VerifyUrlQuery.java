package luti.server.application.query;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import luti.server.application.result.UrlVerifyResult;

public class VerifyUrlQuery implements IQuery<UrlVerifyResult> {

	private final String shortUrl;

	@JsonCreator
	public VerifyUrlQuery(@JsonProperty("shortUrl") String shortUrl) {
		this.shortUrl = shortUrl;
	}

	public String getShortUrl() {
		return shortUrl;
	}
}
