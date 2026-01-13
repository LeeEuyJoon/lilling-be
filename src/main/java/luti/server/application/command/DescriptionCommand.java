package luti.server.application.command;

public class DescriptionCommand {
	private final Long urlId;
	private final Long memberId;
	private final String description;

	private DescriptionCommand(Long urlId, Long memberId, String description) {
		this.urlId = urlId;
		this.memberId = memberId;
		this.description = description;
	}

	public static DescriptionCommand of(Long urlId, Long memberId, String description) {
		return new DescriptionCommand(urlId, memberId, description);
	}

	public Long getUrlId() {
		return urlId;
	}

	public Long getMemberId() {
		return memberId;
	}

	public String getDescription() {
		return description;
	}
}
