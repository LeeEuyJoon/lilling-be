package luti.server.application.command.legacy;

public class CreateTagCommand {

	private final Long memberId;
	private final String name;

	private CreateTagCommand(Long memberId, String name) {
		this.memberId = memberId;
		this.name = name;
	}

	public static CreateTagCommand of(Long memberId, String name) {
		return new CreateTagCommand(memberId, name);
	}

	public Long getMemberId() {
		return memberId;
	}

	public String getName() {
		return name;
	}


}
