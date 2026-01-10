package luti.server.web.mapper;

import static luti.server.web.mapper.AuthExtractor.*;

import org.springframework.security.core.Authentication;

import luti.server.facade.command.ClaimUrlCommand;
import luti.server.web.dto.request.ClaimRequest;

public class ClaimUrlCommandMapper {

	public static ClaimUrlCommand toCommand(ClaimRequest request, Authentication authentication) {
		Long memberId = extractMemberId(authentication);
		return ClaimUrlCommand.of(memberId, request.getShortUrl());
	}
}
