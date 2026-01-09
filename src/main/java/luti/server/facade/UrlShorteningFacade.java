package luti.server.facade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import luti.server.client.KeyBlockManager;
import luti.server.entity.Member;
import luti.server.service.AuthService;
import luti.server.service.Base62Encoder;
import luti.server.service.IdScrambler;
import luti.server.service.UrlService;

@Component
public class UrlShorteningFacade {

	private static final Logger log = LoggerFactory.getLogger(UrlShorteningFacade.class);

	private final IdScrambler idScrambler;
	private final Base62Encoder base62Encoder;
	private final KeyBlockManager keyBlockManager;
	private final UrlService urlService;
	private final AuthService authService;

	public UrlShorteningFacade(IdScrambler idScrambler, Base62Encoder base62Encoder, KeyBlockManager keyBlockManager,
							   UrlService urlService, AuthService authService) {
		this.idScrambler = idScrambler;
		this.base62Encoder = base62Encoder;
		this.keyBlockManager = keyBlockManager;
		this.urlService = urlService;
		this.authService = authService;
	}

	public String shortenUrl(String originalUrl, Authentication authentication) {
		log.info("URL 단축 요청: originalUrl={}", originalUrl);

		Member member = authService.getMemberFromAuthentication(authentication); // 으 패서드 레이어에서 엔티티 사용하고 있었네, 일단 두고 나중에 고치자
		Long nextId = keyBlockManager.getNextId();
		Long scrambledId = idScrambler.scramble(nextId);
		String encodedValue = base62Encoder.encode(scrambledId);
		String shortenedUrl =
			urlService.generateShortenedUrl(originalUrl, nextId, scrambledId, encodedValue, member);

		log.info("URL 단축 성공: shortCode={}, kgsId={}, shortenedUrl={}", encodedValue, nextId, shortenedUrl);
		return shortenedUrl;

	}

}
