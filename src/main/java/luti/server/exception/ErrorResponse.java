package luti.server.exception;

public class ErrorResponse {

	private final String code;
	private final String name;
	private final String message;

	public ErrorResponse(String code, String name, String message) {
		this.code = code;
		this.name = name;
		this.message = message;
	}

	public static ErrorResponse of(ErrorCode errorCode) {
		return new ErrorResponse(errorCode.getNumericCode(), errorCode.name(), errorCode.getMessage());
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public String getMessage() {
		return message;
	}
}
