package luti.server.facade;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import luti.server.entity.Member;
import luti.server.exception.BusinessException;
import luti.server.exception.ErrorCode;
import luti.server.facade.dto.UrlVerifyResult;
import luti.server.service.AuthService;
import luti.server.service.Base62Encoder;
import luti.server.service.MyUrlService;
import luti.server.service.UrlService;
import luti.server.service.UrlVerifyService;
import luti.server.service.dto.UrlMappingInfo;

@Component
public class MyUrlsFacade {

	private static final Logger log = LoggerFactory.getLogger(MyUrlsFacade.class);

	private final UrlVerifyService urlVerifyService;
	private final Base62Encoder base62Encoder;
	private final UrlService urlService;
	private final AuthService authService;
	private final MyUrlService myUrlService;

	public MyUrlsFacade(UrlVerifyService urlVerifyService, Base62Encoder base62Encoder, UrlService urlService,
						AuthService authService, MyUrlService myUrlService) {
		this.urlVerifyService = urlVerifyService;
		this.base62Encoder = base62Encoder;
		this.urlService = urlService;
		this.authService = authService;
		this.myUrlService = myUrlService;
	}

	public UrlVerifyResult verify(String shortUrl) {

		// 1. url 형식 검증 (형식이 잘못되었으면 즉시 INVALID_FORMAT 반환, 형식이 올바르면 다음 단계)
		// 2. 단축 URL 존재 여부 확인 (존재하지 않으면 NOT_FOUND 반환, 존재하면 다음 단계)
		// 3. 단축 URL 주인 확인 (주인이 있으면 ALREADY_OWNED 반환, 주인이 없으면 다음 단계)
		// 4. 필드 채우고 OK 반환

		log.info("단축 URL 추가 가능 검증 요청: shortUrl={}", shortUrl);

		Optional<String> shortCode =  urlVerifyService.verifyAndExtractShortCode(shortUrl);

		if (shortCode.isEmpty()) {
			return UrlVerifyResult.invalidFormat();
		}

		Long decodedId = base62Encoder.decode(shortCode.get());

		Optional<UrlMappingInfo> urlInfo = urlService.findByDecodedId(decodedId);

		if (urlInfo.isEmpty()) {
			return UrlVerifyResult.notFound();
		}

		if (urlInfo.get().isHasOwner()) {
			return UrlVerifyResult.alreadyOwned();
		}

		return UrlVerifyResult.ok(urlInfo.get());
	}

	public void claimUrlsToMember(String shortUrl, Authentication authentication) {

		// 1. 인증 객체에서 멤버 정보 추출
		// 2. 포맷 검증 ('http://' 'https://' 포함하고있으면 제거하고 shortCode만 추출)
		// 3. shortCode -> id 디코딩
		// 4. id로 urlMapping 조회
		// 5. urlMapping 주인 없으면 현재 멤버로 주인 설정

		Member member = authService.getMemberFromAuthentication(authentication); // 이것도 claim api 구현 이후 수정하기

		Optional<String> shortCode =  urlVerifyService.verifyAndExtractShortCode(shortUrl);

		if (shortCode.isEmpty()) {
			throw new BusinessException(ErrorCode.INVALID_SHORT_URL_FORMAT);
		}

		Long decodedId = base62Encoder.decode(shortCode.get());
		UrlMappingInfo urlMappingInfo = urlService.findByDecodedId(decodedId)
			.orElseThrow(() -> new BusinessException(ErrorCode.SHORT_URL_NOT_FOUND));

		myUrlService.claimUrlMappingToMember(urlMappingInfo, member);

	}
}
