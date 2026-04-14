package luti.server.web.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import luti.server.application.bus.CommandBus;
import luti.server.application.command.ShortenUrlCommand;
import luti.server.application.result.ShortenUrlResult;
import luti.server.web.resolver.ResolveCommand;

@RestController
@RequestMapping("/api/v1/url")
public class UrlShorteningController {

	private final CommandBus commandBus;

	public UrlShorteningController(CommandBus commandBus) {
		this.commandBus = commandBus;
	}

	@PostMapping("/shorten")
	public ShortenUrlResult shortenUrl(@ResolveCommand ShortenUrlCommand command) {

		return commandBus.execute(command);
	}
}
