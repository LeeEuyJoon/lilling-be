package luti.server.application.result;

public class RedirectResult {
	private final String originalUrl;

	private RedirectResult(String originalUrl) {
		this.originalUrl = originalUrl;
	}

	public String getOriginalUrl() {
		return originalUrl;
	}

	public static RedirectResult of(String originalUrl) {
		return new RedirectResult(originalUrl);
	}
}
