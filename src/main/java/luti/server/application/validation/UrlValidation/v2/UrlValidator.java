package luti.server.application.validation.UrlValidation.v2;

import luti.server.application.result.UrlVerifyResult;

public interface UrlValidator {
	UrlVerifyResult validate(UrlValidationContext context);
	void setNext(UrlValidator next);
}
