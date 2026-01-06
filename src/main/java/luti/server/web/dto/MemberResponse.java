package luti.server.web.dto;

import luti.server.enums.Role;

public class MemberResponse {

	private Long id;
	private String email;
	private Role role;

	public static MemberResponse of(Long id, String email, Role role) {
		MemberResponse response = new MemberResponse();
		response.id = id;
		response.email = email;
		response.role = role;
		return response;
	}
}
