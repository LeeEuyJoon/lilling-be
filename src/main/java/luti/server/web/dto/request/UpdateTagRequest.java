package luti.server.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateTagRequest {

	@NotBlank(message = "태그 이름은 필수입니다.")
	@Size(min = 1, max = 50, message = "태그 이름은 1자 이상 50자 이하로 입력해야 합니다.")
	private String name;

	public String getName() {
		return name;
	}
}
