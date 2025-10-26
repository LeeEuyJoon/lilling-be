package luti.server.Web.Dto;

public class ShortenResponse {
	private final String shortUrl;

	public ShortenResponse(String shortUrl) {
		this.shortUrl = shortUrl;
	}

	public String getShortUrl() {
		return shortUrl;
	}
}
