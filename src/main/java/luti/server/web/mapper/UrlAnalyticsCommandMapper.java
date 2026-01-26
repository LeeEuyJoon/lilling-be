package luti.server.web.mapper;

import static luti.server.web.mapper.AuthExtractor.*;

import org.springframework.security.core.Authentication;

import luti.server.application.command.UrlAnalyticsCommand;

public class UrlAnalyticsCommandMapper {

	public static UrlAnalyticsCommand toCommand(Long id, Authentication authentication) {
		Long memberId = extractMemberId(authentication);
		return UrlAnalyticsCommand.of(id, memberId);
	}
}
