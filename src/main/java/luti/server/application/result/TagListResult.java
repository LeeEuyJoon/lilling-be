package luti.server.application.result;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import luti.server.domain.service.dto.TagInfo;

public class TagListResult {

	@JsonProperty("tags")
	private final List<CreateTagResult> tags;

	private TagListResult(List<CreateTagResult> tags) {
		this.tags = tags;
	}

	public static TagListResult from(List<TagInfo> tagInfos) {
		List<CreateTagResult> tags = tagInfos.stream().map(CreateTagResult::from).toList();
		return new TagListResult(tags);
	}

	public List<CreateTagResult> getTags() {
		return tags;
	}
}
