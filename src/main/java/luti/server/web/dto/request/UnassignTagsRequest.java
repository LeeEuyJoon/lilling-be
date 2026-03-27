package luti.server.web.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public class UnassignTagsRequest {

	@NotNull(message = "URL ID는 필수입니다")
	private Long urlId;

	@NotEmpty(message = "태그 ID 목록은 비어있을 수 없습니다")
	private List<Long> tagIds;

	public Long getUrlId() {
		return urlId;
	}

	public List<Long> getTagIds() {
		return tagIds;
	}

}
