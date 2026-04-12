package luti.server.application.command.legacy;

public class UpdateTagCommand {

	private final Long memberId;
	private final Long tagId;
	private final String name;

	private UpdateTagCommand(Long memberId, Long tagId, String name) {
		this.memberId = memberId;
		this.tagId = tagId;
		this.name = name;
	}

	public static UpdateTagCommand of(Long memberId, Long tagId, String name) {
		return new UpdateTagCommand(memberId, tagId, name);
	}

	public Long getMemberId() {
		return memberId;
	}

	public Long getTagId() {
		return tagId;
	}

	public String getName() {
		return name;
	}

}
