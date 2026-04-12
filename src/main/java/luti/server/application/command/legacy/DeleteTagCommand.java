package luti.server.application.command.legacy;

public class DeleteTagCommand {

	private final Long memberId;
	private final Long tagId;

	private DeleteTagCommand(Long memberId, Long tagId) {
		this.memberId = memberId;
		this.tagId = tagId;
	}

	public static DeleteTagCommand of(Long memberId, Long tagId) {
		return new DeleteTagCommand(memberId, tagId);
	}

	public Long getMemberId() {
		return memberId;
	}

	public Long getTagId() {
		return tagId;
	}
}
