package luti.server.web.mapper;

import static luti.server.web.mapper.AuthExtractor.*;

import org.springframework.security.core.Authentication;

import luti.server.application.command.ShortenUrlCommand;
import luti.server.web.dto.request.ShortenRequest;

public class ShortenUrlCommandMapper {

	public static ShortenUrlCommand toCommand(ShortenRequest request, Authentication authentication) {
		Long memberId = extractMemberId(authentication);
		return ShortenUrlCommand.of(memberId, request.getOriginalUrl(), request.getKeyword());
	}

}
