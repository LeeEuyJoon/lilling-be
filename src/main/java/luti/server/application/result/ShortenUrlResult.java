package luti.server.application.result;

public class ShortenUrlResult {

	private final String shortenedUrl;

	private ShortenUrlResult(String shortenedUrl) {
		this.shortenedUrl = shortenedUrl;
	}

	public static ShortenUrlResult of(String shortenedUrl) {
		return new ShortenUrlResult(shortenedUrl);
	}

	public String getShortenedUrl() {
		return shortenedUrl;
	}
}
