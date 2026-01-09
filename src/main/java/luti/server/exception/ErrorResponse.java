package luti.server.exception;

public class ErrorResponse {

	private final String code;
	private final String message;

	public ErrorResponse(String code, String message) {
		this.code = code;
		this.message = message;
	}

	public static ErrorResponse of(ErrorCode errorCode) {
		return new ErrorResponse(errorCode.name(), errorCode.getMessage());
	}

	public String getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}
}
