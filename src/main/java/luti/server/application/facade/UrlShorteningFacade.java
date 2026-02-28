package luti.server.application.facade;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import luti.server.domain.model.Member;
import luti.server.exception.BusinessException;
import luti.server.exception.ErrorCode;
import luti.server.infrastructure.client.kgs.KeyBlockManager;
import luti.server.application.command.ShortenUrlCommand;
import luti.server.application.result.ShortenUrlResult;
import luti.server.domain.util.Base62Encoder;
import luti.server.domain.util.IdScrambler;
import luti.server.domain.service.UrlService;

@Component
public class UrlShorteningFacade {

	private static final Logger log = LoggerFactory.getLogger(UrlShorteningFacade.class);

	private final IdScrambler idScrambler;
	private final Base62Encoder base62Encoder;
	private final KeyBlockManager keyBlockManager;
	private final UrlService urlService;

	private static final int MAX_AUTO_RETRIES = 20;

	public UrlShorteningFacade(IdScrambler idScrambler, Base62Encoder base62Encoder, KeyBlockManager keyBlockManager,
							   UrlService urlService) {
		this.idScrambler = idScrambler;
		this.base62Encoder = base62Encoder;
		this.keyBlockManager = keyBlockManager;
		this.urlService = urlService;
	}

	/**
	 * URL 단축
	 * - keyword가 없는 경우: auto 로직 (KGS + ID Scrambling + Base62)
	 * - keyword가 있는 경우: keyword 로직 (검증 + 저장)
	 */
	public ShortenUrlResult shortenUrl(ShortenUrlCommand command) {

		log.info("URL 단축 요청: originalUrl={}", command.getOriginalUrl());

		urlService.validateOriginalUrl(command.getOriginalUrl());

		boolean hasKeyword = command.getKeyword() != null && !command.getKeyword().isBlank();

		String shortenedUrl = null;

		if (!hasKeyword) {

			Long nextId = null;
			String encodedValue = null;
			Member member = urlService.resolveMember(command.getMemberId());

			for (int attempt = 0; attempt < MAX_AUTO_RETRIES; attempt++) {
				nextId = keyBlockManager.getNextId();
				Long scrambledId = idScrambler.scramble(nextId);
				encodedValue = base62Encoder.encode(scrambledId);

				Optional<String> result = urlService.generateShortenedUrl(command.getOriginalUrl(), nextId, scrambledId,
																		  encodedValue, member);

				if (result.isPresent()) {
					shortenedUrl = result.get();
					break;
				}
			}

			if (shortenedUrl == null) {
				log.error("URL 단축 실패: auto 로직에서 최대 재시도 횟수 초과");
				throw new BusinessException(ErrorCode.AUTO_SHORTEN_FAILED);
			}

			log.info("URL 단축 성공 (auto): shortCode={}, kgsId={}, shortenedUrl={}", encodedValue, nextId, shortenedUrl);
		} else {
			shortenedUrl = urlService.generateShortenedUrlWithKeyword(command.getOriginalUrl(), command.getKeyword(),
																	  command.getMemberId());

			log.info("URL 단축 성공 (keyword): shortCode={}, shortenedUrl={}", command.getKeyword(), shortenedUrl);
		}

		return ShortenUrlResult.of(shortenedUrl);

	}

}
