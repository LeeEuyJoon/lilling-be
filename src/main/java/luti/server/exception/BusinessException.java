package luti.server.exception;

public class BusinessException extends RuntimeException {

  private final ErrorCode errorCode;

  public BusinessException(ErrorCode code) {
    super(code.getMessage());
    this.errorCode = code;
  }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
