package luti.server.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex) {
		ErrorResponse response = ErrorResponse.of(ErrorCode.DEFAULT_BUSINESS_ERROR);

		return ResponseEntity
			.status(ErrorCode.DEFAULT_BUSINESS_ERROR.getHttpStatus())
			.body(response);
	}


	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
		ErrorResponse response = ErrorResponse.of(ex.getErrorCode());

		return ResponseEntity
			.status(ex.getErrorCode().getHttpStatus())
			.body(response);
	}

}
