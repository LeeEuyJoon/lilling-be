package luti.server.domain.validator;

import org.springframework.stereotype.Component;

import luti.server.domain.model.UrlMapping;
import luti.server.exception.BusinessException;
import luti.server.exception.ErrorCode;

/**
 * URL 소유권 검증자
 * URL이 특정 회원에게 속해있는지 검증하는 책임을 가진 컴포넌트
 */
@Component
public class UrlOwnershipValidator {

	/**
	 * URL 소유권 검증
	 * URL이 해당 회원에게 속해있는지 확인
	 *
	 * @param urlMapping 검증할 URL 매핑
	 * @param memberId 회원 ID
	 * @throws BusinessException URL이 클레임되지 않았거나 소유자가 아닌 경우
	 */
	public void validateOwnership(UrlMapping urlMapping, Long memberId) {
		if (urlMapping.getMember() == null || !urlMapping.getMember().getId().equals(memberId)) {
			throw new BusinessException(ErrorCode.NOT_URL_OWNER);
		}
	}
}
