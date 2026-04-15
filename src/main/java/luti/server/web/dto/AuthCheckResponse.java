package luti.server.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthCheckResponse {

	@JsonProperty("isAuthenticated")
	private final boolean isAuthenticated;

	@JsonProperty("memberId")
	private final Long memberId;

	@JsonProperty("email")
	private final String email;

	private AuthCheckResponse(boolean isAuthenticated, Long memberId, String email) {
		this.isAuthenticated = isAuthenticated;
		this.memberId = memberId;
		this.email = email;
	}

	public static AuthCheckResponse of(boolean isAuthenticated, Long memberId, String email) {
		return new AuthCheckResponse(isAuthenticated, memberId, email);
	}

	public boolean isAuthenticated() {
		return isAuthenticated;
	}

	public Long getMemberId() {
		return memberId;
	}

	public String getEmail() {
		return email;
	}
}
