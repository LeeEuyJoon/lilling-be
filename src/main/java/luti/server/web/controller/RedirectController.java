package luti.server.web.controller;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import luti.server.application.bus.QueryBus;
import luti.server.application.query.RedirectQuery;
import luti.server.application.result.RedirectResult;
import luti.server.web.resolver.ResolveQuery;

@RestController
@RequestMapping("/")
public class RedirectController {

	private final QueryBus queryBus;

	public RedirectController(QueryBus queryBus) {
		this.queryBus = queryBus;
	}

	@GetMapping("/{shortCode}")
	public ResponseEntity<Void> redirect(@ResolveQuery RedirectQuery query) {

		RedirectResult result = queryBus.execute(query);

		return ResponseEntity
			.status(HttpStatus.FOUND)
			.location(URI.create(result.getOriginalUrl()))
			.build();
	}

}
