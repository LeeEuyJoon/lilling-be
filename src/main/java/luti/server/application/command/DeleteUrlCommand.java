package luti.server.application.command;

public class DeleteUrlCommand {

	private final Long memberId;
	private final Long urlId;

	private DeleteUrlCommand(Long memberId, Long urlId) {
		this.memberId = memberId;
		this.urlId = urlId;
	}

	public static DeleteUrlCommand of(Long memberId, Long urlId) {
		return new DeleteUrlCommand(memberId, urlId);
	}

	public Long getMemberId() {
		return memberId;
	}

	public Long getUrlId() {
		return urlId;
	}
}
