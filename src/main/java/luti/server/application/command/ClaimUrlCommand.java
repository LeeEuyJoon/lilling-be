package luti.server.application.command;

public class ClaimUrlCommand {

	private final Long memberId;
	private final String shortUrl;

	private ClaimUrlCommand(Long memberId, String shortUrl) {
		this.memberId = memberId;
		this.shortUrl = shortUrl;
	}

	public static ClaimUrlCommand of(Long memberId, String shortUrl) {
		return new ClaimUrlCommand(memberId, shortUrl);
	}

	public Long getMemberId() {
		return memberId;
	}

	public String getShortUrl() {
		return shortUrl;
	}
}
