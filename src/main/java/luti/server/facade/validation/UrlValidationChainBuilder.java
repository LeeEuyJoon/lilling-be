package luti.server.facade.validation;

import org.springframework.stereotype.Component;

@Component
public class UrlValidationChainBuilder {

	private final UrlFormatValidationHandler formatValidator;
	private final UrlExistenceValidationHandler existenceValidator;
	private final UrlOwnershipValidationHandler ownershipValidator;

	public UrlValidationChainBuilder(
			UrlFormatValidationHandler formatValidator,
			UrlExistenceValidationHandler existenceValidator,
			UrlOwnershipValidationHandler ownershipValidator) {
		this.formatValidator = formatValidator;
		this.existenceValidator = existenceValidator;
		this.ownershipValidator = ownershipValidator;
	}

	public UrlValidationHandler buildVerifyChain() {
		formatValidator.setNext(existenceValidator);
		existenceValidator.setNext(ownershipValidator);
		return formatValidator;
	}

	public UrlValidationHandler buildClaimChain() {
		formatValidator.setNext(existenceValidator);
		existenceValidator.setNext(null);
		return formatValidator;
	}
}
