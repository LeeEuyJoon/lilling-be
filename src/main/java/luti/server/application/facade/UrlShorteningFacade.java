package luti.server.application.facade;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import luti.server.exception.BusinessException;
import luti.server.exception.ErrorCode;
import luti.server.infrastructure.client.kgs.KeyBlockManager;
import luti.server.application.command.ShortenUrlCommand;
import luti.server.application.result.ShortenUrlResult;
import luti.server.domain.util.Base62Encoder;
import luti.server.domain.util.IdScrambler;
import luti.server.domain.service.UrlShorteningService;

@Component
public class UrlShorteningFacade {

	private static final Logger log = LoggerFactory.getLogger(UrlShorteningFacade.class);

	private final IdScrambler idScrambler;
	private final Base62Encoder base62Encoder;
	private final KeyBlockManager keyBlockManager;
	private final UrlShorteningService urlShorteningService;

	private static final int MAX_AUTO_RETRIES = 20;

	public UrlShorteningFacade(IdScrambler idScrambler, Base62Encoder base62Encoder, KeyBlockManager keyBlockManager,
							   UrlShorteningService urlShorteningService) {
		this.idScrambler = idScrambler;
		this.base62Encoder = base62Encoder;
		this.keyBlockManager = keyBlockManager;
		this.urlShorteningService = urlShorteningService;
	}

	/**
	 * URL 단축
	 * - keyword가 없는 경우: auto 로직 (KGS + ID Scrambling + Base62)
	 * - keyword가 있는 경우: keyword 로직 (검증 + 저장)
	 */
	public ShortenUrlResult shortenUrl(ShortenUrlCommand command) {
		log.info("URL 단축 요청: originalUrl={}", command.getOriginalUrl());
		urlShorteningService.validateOriginalUrl(command.getOriginalUrl());

		String shortenedUrl;

		if (hasKeyword(command)) {
			shortenedUrl = shortenWithKeyword(command);
			ShortenUrlResult result = ShortenUrlResult.of(shortenedUrl);

			return result;
		}

		if (!hasKeyword(command)) {
			shortenedUrl = shortenAuto(command);
			ShortenUrlResult result = ShortenUrlResult.of(shortenedUrl);

			return result;
		}

		throw new IllegalStateException("unreachable");
	}

	private boolean hasKeyword(ShortenUrlCommand command) {
		return command.getKeyword() != null && !command.getKeyword().isBlank();
	}

	private String shortenAuto(ShortenUrlCommand command) {
		Long nextId = null;
		String encodedValue = null;

		for (int attempt = 0; attempt < MAX_AUTO_RETRIES; attempt++) {
			nextId = keyBlockManager.getNextId();
			Long scrambledId = idScrambler.scramble(nextId);
			encodedValue = base62Encoder.encode(scrambledId);

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
