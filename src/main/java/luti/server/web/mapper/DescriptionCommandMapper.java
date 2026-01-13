package luti.server.web.mapper;

import static luti.server.web.mapper.AuthExtractor.*;

import org.springframework.security.core.Authentication;

import luti.server.application.command.DescriptionCommand;
import luti.server.web.dto.request.DescriptionRequest;

public class DescriptionCommandMapper {
	public static DescriptionCommand toCommand(DescriptionRequest request, Authentication authentication) {
		Long memberId = extractMemberId(authentication);
		return DescriptionCommand.of(request.getUrlId(), memberId, request.getDescription());
	}
}
