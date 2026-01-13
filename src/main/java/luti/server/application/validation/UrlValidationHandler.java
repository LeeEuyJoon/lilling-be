package luti.server.application.validation;

import luti.server.application.result.UrlVerifyResult;

public interface UrlValidationHandler {
	UrlVerifyResult validate(UrlValidationContext context);
	void setNext(UrlValidationHandler next);
}
