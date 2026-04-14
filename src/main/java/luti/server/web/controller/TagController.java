package luti.server.web.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import luti.server.application.bus.CommandBus;
import luti.server.application.bus.QueryBus;
import luti.server.application.command.AssignTagsCommand;
import luti.server.application.command.CreateTagCommand;
import luti.server.application.command.DeleteTagCommand;
import luti.server.application.command.UnassignTagsCommand;
import luti.server.application.command.UpdateTagCommand;
import luti.server.application.query.TagListQuery;
import luti.server.application.result.CreateTagResult;
import luti.server.application.result.TagListResult;
import luti.server.web.resolver.ResolveCommand;
import luti.server.web.resolver.ResolveQuery;

@RestController
@RequestMapping("/api/v1/tags")
public class TagController {

	private final CommandBus commandBus;
	private final QueryBus queryBus;

	public TagController(CommandBus commandBus, QueryBus queryBus) {
		this.commandBus = commandBus;
		this.queryBus = queryBus;
	}

	@GetMapping
	public TagListResult getTags(@ResolveQuery TagListQuery query) {

		return queryBus.execute(query);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CreateTagResult createTag(@ResolveCommand CreateTagCommand command) {

		return commandBus.execute(command);
	}

	@PatchMapping("/{tagId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void updateTag(@ResolveCommand UpdateTagCommand command) {

		commandBus.execute(command);
	}

	@DeleteMapping("/{tagId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteTag(@ResolveCommand DeleteTagCommand command) {

		commandBus.execute(command);
	}

	@PostMapping("/assign")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void assignTags(@ResolveCommand AssignTagsCommand command) {

		commandBus.execute(command);
	}

	@PostMapping("/unassign")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void unassignTags(@ResolveCommand UnassignTagsCommand command) {

		commandBus.execute(command);
	}
}
