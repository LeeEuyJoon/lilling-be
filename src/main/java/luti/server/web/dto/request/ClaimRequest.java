package luti.server.web.dto.request;

public class ClaimRequest {
	private String shortUrl;

	public ClaimRequest(String shortUrl) {
		this.shortUrl = shortUrl;
	}

	public String getShortUrl() {
		return shortUrl;
	}
}
