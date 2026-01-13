package luti.server.application.command;

public class ShortenUrlCommand {
	private final Long memberId;
	private final String originalUrl;

	private ShortenUrlCommand(Long memberId, String originalUrl) {

		this.memberId = memberId;
		this.originalUrl = originalUrl;
	}

	public static ShortenUrlCommand of(Long memberId, String originalUrl) {
		return new ShortenUrlCommand(memberId, originalUrl);
	}

	public Long getMemberId() {
		return memberId;
	}

	public String getOriginalUrl() {
		return originalUrl;
	}
}
