package luti.server.web.mapper;

import static luti.server.web.mapper.AuthExtractor.*;

import org.springframework.security.core.Authentication;

import luti.server.application.command.legacy.AssignTagsCommand;
import luti.server.application.command.legacy.CreateTagCommand;
import luti.server.application.command.legacy.DeleteTagCommand;
import luti.server.application.command.legacy.UnassignTagsCommand;
import luti.server.application.command.legacy.UpdateTagCommand;
import luti.server.web.dto.request.AssignTagsRequest;
import luti.server.web.dto.request.CreateTagRequest;
import luti.server.web.dto.request.UnassignTagsRequest;
import luti.server.web.dto.request.UpdateTagRequest;

public class TagCommandMapper {
	public static CreateTagCommand toCreateCommand(CreateTagRequest request, Authentication authentication) {
		Long memberId = extractMemberId(authentication);
		return CreateTagCommand.of(memberId, request.getName());
	}
	public static UpdateTagCommand toUpdateCommand(Long tagId, UpdateTagRequest request, Authentication authentication) {
		return UpdateTagCommand.of(extractMemberId(authentication), tagId, request.getName());
	}
	public static DeleteTagCommand toDeleteCommand(Long tagId, Authentication authentication) {
		return DeleteTagCommand.of(extractMemberId(authentication), tagId);
	}
	public static AssignTagsCommand toAssignCommand(AssignTagsRequest request, Authentication authentication) {
		return AssignTagsCommand.of(extractMemberId(authentication), request.getUrlId(), request.getTagIds());
	}
	public static UnassignTagsCommand toUnassignCommand(UnassignTagsRequest request, Authentication authentication) {
		return UnassignTagsCommand.of(extractMemberId(authentication), request.getUrlId(), request.getTagIds());
	}
}
