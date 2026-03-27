package luti.server.domain.service.dto;

import luti.server.domain.model.Tag;

public class TagInfo {
	private final Long id;
	private final String name;

	private TagInfo(Long id, String name) {
		this.id = id;
		this.name = name;
	}

	public static TagInfo from(Tag tag) {
		return new TagInfo(tag.getId(), tag.getName());
	}

	public Long getId() { return id; }
	public String getName() { return name; }
}
