package luti.server.application.command;

import java.util.List;

import luti.server.domain.service.dto.TagInfo;

public class TagListResult {

	private final List<TagResult> tags;

	public TagListResult(List<TagResult> tags) {
		this.tags = tags;
	}

	public static TagListResult from(List<TagInfo> tagInfos) {
		List<TagResult> tags = tagInfos.stream().map(TagResult::from).toList();
		return new TagListResult(tags);
	}

	public List<TagResult> getTags() {
		return tags;
	}

}
