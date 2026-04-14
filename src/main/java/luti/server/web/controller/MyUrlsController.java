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
import luti.server.application.command.ClaimUrlCommand;
import luti.server.application.command.DeleteUrlCommand;
import luti.server.application.command.DescriptionCommand;
import luti.server.application.query.MyUrlsQuery;
import luti.server.application.query.UrlAnalyticsQuery;
import luti.server.application.query.VerifyUrlQuery;
import luti.server.application.result.MyUrlsListResult;
import luti.server.application.result.UrlAnalyticsResult;
import luti.server.application.result.UrlVerifyResult;
import luti.server.web.resolver.ResolveCommand;
import luti.server.web.resolver.ResolveQuery;

@RestController
@RequestMapping("/api/v1/my-urls")
public class MyUrlsController {

	private final CommandBus commandBus;
	private final QueryBus queryBus;

	public MyUrlsController(CommandBus commandBus, QueryBus queryBus) {
		this.commandBus = commandBus;
		this.queryBus = queryBus;
	}

	@GetMapping("/verify")
	public UrlVerifyResult verify(@ResolveQuery VerifyUrlQuery query) {
		return queryBus.execute(query);
	}

	@PostMapping("/claim")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void claimUrl(@ResolveCommand ClaimUrlCommand command) {
		commandBus.execute(command);
	}

	@GetMapping("/list")
	public MyUrlsListResult getMyUrls(@ResolveQuery MyUrlsQuery query) {
		return queryBus.execute(query);
	}

	@PatchMapping("/description")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void updateDescription(@ResolveCommand DescriptionCommand command) {
		commandBus.execute(command);
	}

	@DeleteMapping("/{urlId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteUrl(@ResolveCommand DeleteUrlCommand command) {
		commandBus.execute(command);
	}

	@GetMapping("/{urlId}/analytics")
	public UrlAnalyticsResult getUrlAnalytics(@ResolveQuery UrlAnalyticsQuery query) {
		return queryBus.execute(query);
	}
}
