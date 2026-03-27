package luti.server.web.dto.response;

import java.util.List;

import luti.server.application.result.TagListResult;

public class TagListResponse {

	private List<TagResponse> tags;

	public static TagListResponse from(TagListResult result) {
		TagListResponse response = new TagListResponse();
		response.tags = result.getTags().stream().map(TagResponse::from).toList();
		return response;
	}

	public List<TagResponse> getTags() {
		return tags;
	}

}
