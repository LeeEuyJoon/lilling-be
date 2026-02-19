package luti.server.application.validation.UrlValidation.v2;

import org.springframework.stereotype.Component;

@Component
public class UrlValidationChainBuilder {

	private final UrlFormatValidationHandler formatHandler;
	private final UrlExistenceValidationHandler existenceHandler;
	private final UrlOwnershipValidationHandler ownershipHandler;

	public UrlValidationChainBuilder(
			UrlFormatValidationHandler formatHandler,
			UrlExistenceValidationHandler existenceHandler,
			UrlOwnershipValidationHandler ownershipHandler) {
		this.formatHandler = formatHandler;
		this.existenceHandler = existenceHandler;
		this.ownershipHandler = ownershipHandler;
	}

	public UrlValidator buildVerifyChain() {
		UrlValidationChainNode formatNode = new UrlValidationChainNode(formatHandler);
		UrlValidationChainNode existenceNode = new UrlValidationChainNode(existenceHandler);
		UrlValidationChainNode ownershipNode = new UrlValidationChainNode(ownershipHandler);

		formatNode.setNext(existenceNode);
		existenceNode.setNext(ownershipNode);

		return formatNode;
	}

	public UrlValidator buildClaimChain() {
		UrlValidationChainNode formatNode = new UrlValidationChainNode(formatHandler);
		UrlValidationChainNode existenceNode = new UrlValidationChainNode(existenceHandler);

		formatNode.setNext(existenceNode);

		return formatNode;
	}
}
