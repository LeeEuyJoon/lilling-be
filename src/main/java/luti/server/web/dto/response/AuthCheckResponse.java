package luti.server.web.dto.response;

public class AuthCheckResponse {
	private Boolean isAuthenticated;

	private AuthCheckResponse(Boolean isAuthenticated) {
		this.isAuthenticated = isAuthenticated;
	}

	public static AuthCheckResponse of(Boolean isAuthenticated) {
		return new AuthCheckResponse(isAuthenticated);
	}

	public Boolean getIsAuthenticated() {
		return isAuthenticated;
	}
}
