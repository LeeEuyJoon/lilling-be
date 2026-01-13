package luti.server.web.mapper;

import org.springframework.security.core.Authentication;

import luti.server.application.command.DeleteUrlCommand;

public class DeleteUrlCommandMapper {

	public static DeleteUrlCommand toCommand(Long urlId, Authentication authentication) {
		Long memberId = AuthExtractor.extractMemberId(authentication);
		return DeleteUrlCommand.of(memberId, urlId);
	}
}
