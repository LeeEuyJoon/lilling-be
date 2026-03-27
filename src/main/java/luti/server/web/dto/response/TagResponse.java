package luti.server.web.dto.response;

import luti.server.application.result.TagResult;

public class TagResponse {

	private String id;
	private String name;

	public static TagResponse from(TagResult tagResult) {
		TagResponse tagResponse = new TagResponse();
		tagResponse.id = tagResult.getId().toString();
		tagResponse.name = tagResult.getName();
		return tagResponse;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

}
