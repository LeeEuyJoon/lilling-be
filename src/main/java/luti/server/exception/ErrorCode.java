package luti.server.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

	/**
	 * 0000: 알 수 없는 서버 오류
	 * 2xxx: 리다이렉트 관련 오류
	 * 3xxx: shortening 관련 오류
	 * 4xxx: KGS 관련 오류
	 */

	DEFAULT_BUSINESS_ERROR("0000", HttpStatus.BAD_REQUEST, "서버 오류 발생"),


	URL_NOT_FOUND("2001", HttpStatus.NOT_FOUND, "존재하지 않는 URL 입니다."),


	ENCODE_INPUT_NULL("3101", HttpStatus.BAD_REQUEST, "인코딩 입력값은 null일 수 없습니다"),
	ENCODE_INPUT_NEGATIVE("3102", HttpStatus.BAD_REQUEST, "인코딩 입력값은 음수일 수 없습니다"),
	ENCODE_INPUT_OVERFLOW("3103", HttpStatus.BAD_REQUEST, "인코딩 입력값이 허용 범위를 초과했습니다"),
	DECODE_INPUT_EMPTY("3104", HttpStatus.BAD_REQUEST, "디코딩 입력값이 비어있습니다"),
	DECODE_INVALID_CHARACTER("3105", HttpStatus.BAD_REQUEST, "디코딩 시 유효하지 않은 문자가 포함되어 있습니다"),

	SCRAMBLE_INPUT_NULL("3201", HttpStatus.BAD_REQUEST, "스크램블링 입력값은 null일 수 없습니다"),
	SCRAMBLE_INPUT_NEGATIVE("3202", HttpStatus.BAD_REQUEST, "스크램블링 입력값은 음수일 수 없습니다"),
	SCRAMBLE_INPUT_OVERFLOW("3203", HttpStatus.BAD_REQUEST, "스크램블링 입력값이 허용 범위를 초과했습니다"),
	UNSCRAMBLE_INPUT_NULL("3204", HttpStatus.BAD_REQUEST, "언스크램블링 입력값은 null일 수 없습니다"),
	UNSCRAMBLE_INPUT_NEGATIVE("3205", HttpStatus.BAD_REQUEST, "언스크램블링 입력값은 음수일 수 없습니다"),
	UNSCRAMBLE_INPUT_OVERFLOW("3206", HttpStatus.BAD_REQUEST, "언스크램블링 입력값이 허용 범위를 초과했습니다"),


	KGS_CONNECTION_FAILED("4001", HttpStatus.SERVICE_UNAVAILABLE, "KGS 서버 연결에 실패했습니다"),
	KGS_NULL_RESPONSE("4002", HttpStatus.INTERNAL_SERVER_ERROR, "KGS 서버로부터 null 응답을 받았습니다"),
	KGS_INVALID_RESPONSE("4003", HttpStatus.INTERNAL_SERVER_ERROR, "KGS 서버로부터 유효하지 않은 응답을 받았습니다"),
	KGS_TIMEOUT("4004", HttpStatus.GATEWAY_TIMEOUT, "KGS 서버 요청 시간이 초과되었습니다"),
	KGS_UNAVAILABLE("4005", HttpStatus.SERVICE_UNAVAILABLE, "KGS 서비스를 사용할 수 없습니다");




	private final String code;
	private final HttpStatus httpStatus;
	private final String message;

	ErrorCode(String code, HttpStatus httpStatus, String message) {
		this.code = code;
		this.httpStatus = httpStatus;
		this.message = message;
	}

	public String getMessage() {
		return "[" + this.code + "] " + this.message;
	}


	public HttpStatus getHttpStatus() {
		return this.httpStatus;
	}

}
