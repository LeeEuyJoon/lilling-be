package luti.server.application.command;

import luti.server.domain.service.dto.TagInfo;

public class TagResult {
	private final Long id;
	private final String name;

	public TagResult(Long id, String name) {
		this.id = id;
		this.name = name;
	}

	public static TagResult from(TagInfo tagInfo) {
		return new TagResult(tagInfo.getId(), tagInfo.getName());
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}
}
