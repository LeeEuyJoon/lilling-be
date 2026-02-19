package luti.server.application.validation.UrlValidation.v2;

import luti.server.application.result.UrlVerifyResult;

/**
 * CoR의 "연결(next)"을 담당하는 체인 노드.
 * - 매 요청마다 새로 만들어지는 객체라 스레드 안전
 */
public class UrlValidationChainNode implements UrlValidator {

	private final UrlValidationHandler handler;
	private UrlValidator next;

	public UrlValidationChainNode(UrlValidationHandler handler) {
		this.handler = handler;
	}

	@Override
	public UrlVerifyResult validate(UrlValidationContext context) {
		UrlVerifyResult failOrNull = handler.validate(context);

		// 실패면 즉시 종료
		if (failOrNull != null) {
			return failOrNull;
		}

		// 성공이면 다음으로
		if (next != null) {
			return next.validate(context);
		}

		// 체인의 끝까지 성공한 경우: 여기서 최종 OK 생성
		return UrlVerifyResult.ok(context.getUrlMappingInfo());
	}

	@Override
	public void setNext(UrlValidator next) {
		this.next = next;
	}
}
