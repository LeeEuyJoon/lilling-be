package luti.server.security.dto;

import luti.server.enums.Role;

public class ProvisionedMemberDto {

	private final Long memberId;
	private final Role role;

	private ProvisionedMemberDto(Long memberId, Role role) {
		this.memberId = memberId;
		this.role = role;
	}

	public Long getMemberId() {
		return memberId;
	}

	public Role getRole() {
		return role;
	}

	public static ProvisionedMemberDto of(Long memberId, Role role) {
		return new ProvisionedMemberDto(memberId, role);
	}
}
