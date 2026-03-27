package luti.server.application.command;

public class UpdateTagCommand {

	private final Long MemberId;
	private final Long tagId;
	private final String name;

	private UpdateTagCommand(Long memberId, Long tagId, String name) {
		MemberId = memberId;
		this.tagId = tagId;
		this.name = name;
	}

	public static UpdateTagCommand of(Long memberId, Long tagId, String name) {
		return new UpdateTagCommand(memberId, tagId, name);
	}

	public Long getMemberId() {
		return MemberId;
	}

	public Long getTagId() {
		return tagId;
	}

	public String getName() {
		return name;
	}

}
