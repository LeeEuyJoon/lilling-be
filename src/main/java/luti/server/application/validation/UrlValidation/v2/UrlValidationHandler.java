package luti.server.application.validation.UrlValidation.v2;

import luti.server.application.result.UrlVerifyResult;

/**
 * 싱글톤으로 안전하게 둘 수 있는 "무상태" 검증 핸들러
 * - next 없음
 * - 오직 자신의 검증만 수행하고
 * - 실패면 실패 결과, 성공이면 null 반환(= 계속 진행)
 */
public interface UrlValidationHandler {
	/**
	 * @return 실패면 UrlVerifyResult(실패/종료),
	 *         성공이면 null(다음 단계로 진행)
	 */
	UrlVerifyResult validate(UrlValidationContext context);
}
