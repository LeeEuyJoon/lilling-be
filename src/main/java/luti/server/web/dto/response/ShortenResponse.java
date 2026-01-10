package luti.server.web.dto.response;

import luti.server.facade.result.ShortenUrlResult;

public class ShortenResponse {

	private final String shortUrl;

	private ShortenResponse(String shortUrl) {
		this.shortUrl = shortUrl;
	}

	public String getShortUrl() {
		return shortUrl;
	}

	public static ShortenResponse from(ShortenUrlResult result) {
		return new ShortenResponse(result.getShortenedUrl());
	}
}
