package luti.server.facade.validation;

import luti.server.facade.result.UrlVerifyResult;

public interface UrlValidationHandler {
	UrlVerifyResult validate(UrlValidationContext context);
	void setNext(UrlValidationHandler next);
}
