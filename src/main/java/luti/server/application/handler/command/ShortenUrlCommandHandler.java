package luti.server.application.handler.command;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import luti.server.application.command.ShortenUrlCommand;
import luti.server.application.handler.CommandHandler;
import luti.server.application.result.ShortenUrlResult;
import luti.server.domain.service.UrlShorteningService;
import luti.server.domain.util.Base62Encoder;
import luti.server.domain.util.IdScrambler;
import luti.server.exception.BusinessException;
import luti.server.exception.ErrorCode;
import luti.server.infrastructure.client.kgs.KeyBlockManager;

@Component
public class ShortenUrlCommandHandler implements CommandHandler<ShortenUrlCommand, ShortenUrlResult>{

	private static final Logger log = LoggerFactory.getLogger(ShortenUrlCommandHandler.class);
	private static final int MAX_AUTO_RETIRES = 20;

	private final IdScrambler idScrambler;
	private final Base62Encoder base62Encoder;
	private final KeyBlockManager keyBlockManager;
	private final UrlShorteningService urlShorteningService;

	public ShortenUrlCommandHandler(IdScrambler idScrambler, Base62Encoder base62Encoder,
									KeyBlockManager keyBlockManager, UrlShorteningService urlShorteningService) {
		this.idScrambler = idScrambler;
		this.base62Encoder = base62Encoder;
		this.keyBlockManager = keyBlockManager;
		this.urlShorteningService = urlShorteningService;
	}

	@Override
	public ShortenUrlResult execute(ShortenUrlCommand command) {
		log.info("URL 단축 요청: originalUrl={}", command.getOriginalUrl());
		urlShorteningService.validateOriginalUrl(command.getOriginalUrl());

		String shortenedUrl;
		ShortenUrlResult result;

		if (hasKeyword(command)) {
			shortenedUrl = shortenWithKeyword(command);
			result = ShortenUrlResult.of(shortenedUrl);

			return result;
		}

		if (!hasKeyword(command)) {
			shortenedUrl = shortenAuto(command);
			result = ShortenUrlResult.of(shortenedUrl);

			return result;
		}

		throw new IllegalStateException("unreachable");
	}

	@Override
	public Class<ShortenUrlCommand> getSupportedCommandType() {
		return ShortenUrlCommand.class;
	}

	private boolean hasKeyword(ShortenUrlCommand command) {
		return command.getKeyword() != null && !command.getKeyword().isBlank();
	}

	private String shortenAuto(ShortenUrlCommand command) {
		for (int attempt = 0; attempt < MAX_AUTO_RETIRES; attempt++) {
			Long nextId = keyBlockManager.getNextId();
			Long scrambledId = idScrambler.scramble(nextId);
			String encodedValue = base62Encoder.encode(scrambledId);

			Optional<String> result = urlShorteningService.generateShortenedUrl(
				command.getOriginalUrl(), nextId, scrambledId, encodedValue, command.getMemberId());

			if (result.isPresent()) {
				log.info("URL 단축 성공 (auto): shortCode={}, kgsId={}, shortenedUrl={}", encodedValue, nextId, result.get());
				return result.get();
			}
		}

		log.error("URL 단축 실패: auto 로직에서 최대 재시도 횟수 초과");
		throw new BusinessException(ErrorCode.AUTO_SHORTEN_FAILED);
	}

	private String shortenWithKeyword(ShortenUrlCommand command) {
		String shortenedUrl = urlShorteningService.generateShortenedUrlWithKeyword(
			command.getOriginalUrl(), command.getKeyword(), command.getMemberId());

		log.info("URL 단축 성공 (keyword): shortCode={}, shortenedUrl={}", command.getKeyword(), shortenedUrl);
		return shortenedUrl;
	}

}
