package luti.server.application.query;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import luti.server.application.result.TagListResult;

public class TagListQuery implements IQuery<TagListResult> {

	private final Long memberId;

	@JsonCreator
	public TagListQuery(@JsonProperty("memberId") Long memberId) {
		this.memberId = memberId;
	}

	public Long getMemberId() {
		return memberId;
	}
}
