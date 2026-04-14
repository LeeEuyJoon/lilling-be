package luti.server.web.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import luti.server.application.bus.CommandBus;
import luti.server.application.bus.QueryBus;
import luti.server.application.command.legacy.CreateTagCommand;
import luti.server.application.facade.TagFacade;
import luti.server.application.query.TagListQuery;
import luti.server.application.result.TagListResult;
import luti.server.web.dto.request.AssignTagsRequest;
import luti.server.web.dto.request.CreateTagRequest;
import luti.server.web.dto.request.UnassignTagsRequest;
import luti.server.web.dto.request.UpdateTagRequest;
import luti.server.web.dto.response.TagResponse;
import luti.server.web.mapper.TagCommandMapper;
import luti.server.web.resolver.ResolveQuery;

@RestController
@RequestMapping("/api/v1/tags")
public class TagController {

	private final TagFacade tagFacade;
	private final CommandBus commandBus;
	private final QueryBus queryBus;

	public TagController(TagFacade tagFacade, CommandBus commandBus, QueryBus queryBus) {
		this.tagFacade = tagFacade;
		this.commandBus = commandBus;
		this.queryBus = queryBus;
	}

	@GetMapping
	public TagListResult getTags(@ResolveQuery TagListQuery query) {

		return queryBus.execute(query);
	}

	@PostMapping
	public ResponseEntity<TagResponse> createTag(@Valid @RequestBody CreateTagRequest request,
												 Authentication authentication) {
		CreateTagCommand command = TagCommandMapper.toCreateCommand(request, authentication);
		TagResponse response = TagResponse.from(tagFacade.createTag(command));

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PatchMapping("/{tagId}")
	public ResponseEntity<Void> updateTag(@PathVariable Long tagId, @Valid @RequestBody UpdateTagRequest request,
										  Authentication authentication) {
		tagFacade.updateTag(TagCommandMapper.toUpdateCommand(tagId, request, authentication));
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{tagId}")
	public ResponseEntity<Void> deleteTag(@PathVariable Long tagId, Authentication authentication) {
		tagFacade.deleteTag(TagCommandMapper.toDeleteCommand(tagId, authentication));
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/assign")
	public ResponseEntity<Void> assignTags(@Valid @RequestBody AssignTagsRequest request,
										   Authentication authentication) {
		tagFacade.assignTags(TagCommandMapper.toAssignCommand(request, authentication));
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/unassign")
	public ResponseEntity<Void> unassignTags(@Valid @RequestBody UnassignTagsRequest request,
											 Authentication authentication) {
		tagFacade.unassignTags(TagCommandMapper.toUnassignCommand(request, authentication));
		return ResponseEntity.noContent().build();
	}
}
