package luti.server.domain.service;

import static org.springframework.transaction.annotation.Propagation.*;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import luti.server.domain.model.Member;
import luti.server.domain.model.UrlMapping;
import luti.server.domain.port.MemberReader;
import luti.server.domain.port.UrlMappingStore;
import luti.server.domain.util.Base62Encoder;
import luti.server.domain.util.IdScrambler;

@Component
public class KeywordUrlInserter {

	private final UrlMappingStore urlMappingStore;
	private final Base62Encoder base62Encoder;
	private final IdScrambler idScrambler;
	private final MemberReader memberReader;


	@Value("${DOMAIN}")
	private String DOMAIN;

	@Value("${APP_ID}")
	private String APP_ID;

	public KeywordUrlInserter(UrlMappingStore urlMappingStore, Base62Encoder base62Encoder, IdScrambler idScrambler,
							  MemberReader memberReader) {
		this.urlMappingStore = urlMappingStore;
		this.base62Encoder = base62Encoder;
		this.idScrambler = idScrambler;
		this.memberReader = memberReader;
	}

	@Transactional(propagation = REQUIRES_NEW)
	public String tryInsertKeywordMapping(String originalUrl, String candidateShortCode, Long memberId) {
		Member member = Optional.ofNullable(memberId)
								.flatMap(memberReader::findById)
								.orElse(null);

		String shortUrl = DOMAIN + "/" + candidateShortCode;

		Long scrambledId = base62Encoder.decode(candidateShortCode);
		Long kgsId = idScrambler.descramble(scrambledId);

		UrlMapping urlMapping = UrlMapping.builder()
										  .scrambledId(scrambledId)
										  .kgsId(kgsId)
										  .originalUrl(originalUrl)
										  .shortUrl(shortUrl)
										  .appId(APP_ID)
										  .member(member)
										  .build();

		try {
			urlMappingStore.saveAndFlush(urlMapping);
			return urlMapping.getShortUrl();
		} catch (Exception e) {
			return null;
		}
	}
}
