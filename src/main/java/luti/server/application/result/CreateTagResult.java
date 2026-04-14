package luti.server.application.result;

import luti.server.domain.service.dto.TagInfo;

public class CreateTagResult {
	private final Long id;
	private final String name;

	private CreateTagResult(Long id, String name) {
		this.id = id;
		this.name = name;
	}

	public static CreateTagResult from(TagInfo tagInfo) {
		return new CreateTagResult(tagInfo.getId(), tagInfo.getName());
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}
}
