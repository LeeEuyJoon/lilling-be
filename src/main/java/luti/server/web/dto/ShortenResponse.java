package luti.server.web.dto;

public class ShortenResponse {
	private final String shortUrl;

	public ShortenResponse(String shortUrl) {
		this.shortUrl = shortUrl;
	}

	public String getShortUrl() {
		return shortUrl;
	}

	public static ShortenResponse of(String shortUrl) {
		return new ShortenResponse(shortUrl);
	}
}
