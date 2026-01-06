package luti.server.facade.dto;

public class CurrentMemberDto {

	private final Long memberId;

	public CurrentMemberDto(Long memberId) {
		this.memberId = memberId;
	}

	public Long getMemberId() {
		return memberId;
	}

}
