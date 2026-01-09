package luti.server.service;

import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * URL 형식 검증 및 shortCode 추출
 *
 * @return shortCode (검증 실패 시 Optional.empty())
 */
@Service
public class UrlVerifyService {

	private static final Logger log = LoggerFactory.getLogger(UrlVerifyService.class);

	private static final Pattern BASE62_PATTERN = Pattern.compile("^[a-zA-Z0-9]{1,7}$");

	@Value("${DOMAIN}")
	private String DOMAIN;

	public Optional<String> verifyAndExtractShortCode(String url) {
		log.debug("URL 형식 검증 및 shortCode 추출 시작: url={}", url);

		if (url == null || url.isBlank()) {
			return Optional.empty();
		}

		// 프로토콜 제거 (http://, https:// 제거)
		String urlWithoutProtocol = url.replaceFirst("^https?://", "");

		// DOMAIN으로 시작하지 않으면 실패
		if (!urlWithoutProtocol.startsWith(DOMAIN + "/")) {
			return Optional.empty();
		}

		// shortCode 추출
		String shortCode = urlWithoutProtocol.substring(DOMAIN.length() + 1);

		// Base62 형식 검증
		if (!BASE62_PATTERN.matcher(shortCode).matches()) {
			return Optional.empty();
		}

		log.debug("URL 형식 검증 성공: url={}, shortCode={}", url, shortCode);
		return Optional.of(shortCode);
	}
}
