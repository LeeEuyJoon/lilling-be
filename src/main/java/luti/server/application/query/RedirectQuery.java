package luti.server.application.query;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import luti.server.application.result.RedirectResult;

public class RedirectQuery implements IQuery<RedirectResult> {

	private final String shortCode;

	@JsonCreator
	public RedirectQuery(@JsonProperty("shortCode") String shortCode) {
		this.shortCode = shortCode;
	}

	public String getShortCode() {
		return shortCode;
	}
}
