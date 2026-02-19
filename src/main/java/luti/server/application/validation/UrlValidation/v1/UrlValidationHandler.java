package luti.server.application.validation.UrlValidation.v1;

import luti.server.application.result.UrlVerifyResult;

public interface UrlValidationHandler {
	UrlVerifyResult validate(UrlValidationContext context);
	void setNext(UrlValidationHandler next);
}
