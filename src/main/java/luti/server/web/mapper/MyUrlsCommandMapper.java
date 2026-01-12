package luti.server.web.mapper;

import static luti.server.web.mapper.AuthExtractor.*;

import org.springframework.security.core.Authentication;

import luti.server.facade.command.MyUrlsCommand;

public class MyUrlsCommandMapper {
	public static MyUrlsCommand toCommand(Integer page, Integer size, Authentication authentication) {
		Long memberId = extractMemberId(authentication);
		return MyUrlsCommand.of(page, size, memberId);
	}
}
