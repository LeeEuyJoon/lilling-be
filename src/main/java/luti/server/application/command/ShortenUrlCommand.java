package luti.server.application.command;

public class ShortenUrlCommand {
	private final Long memberId;
	private final String originalUrl;
	private final String keyword;

	private ShortenUrlCommand(Long memberId, String originalUrl, String keyword) {

		this.memberId = memberId;
		this.originalUrl = originalUrl;
		this.keyword = keyword;
	}

	public static ShortenUrlCommand of(Long memberId, String originalUrl, String keyword) {
		return new ShortenUrlCommand(memberId, originalUrl, keyword);
	}

	public Long getMemberId() {
		return memberId;
	}

	public String getOriginalUrl() {
		return originalUrl;
	}

	public String getKeyword() {
		return keyword;
	}
}
