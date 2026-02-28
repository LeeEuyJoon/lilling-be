package luti.server.domain.util;

import static luti.server.exception.ErrorCode.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import luti.server.exception.BusinessException;

/**
 * ID 스크램블링 서비스 클래스
 * 입력값: 0 이상 3,521,614,606,208 미만의 Long 타입의 원본 ID (서비스에서는 1부터 시작하는 연속된 정수 ID)
 * 출력값: String 타입의 스크램블된 ID (3,521,614,606,208 미만의 고유한 정수)
 * 입력값 검증: 입력값이 null이거나 음수인 경우 IllegalArgumentException 예외 발생
 *
 * 스크램블링 알고리즘: 선형식 + 모듈러 연산 -> ( A * id + B mod ) M (단, A와 M은 서로소, B는 0 이상 M 미만의 정수) x
 * -> 스크램블링 알고리즘 기존 선형식 기반에서 XORShift 연산으로 변경됨 (모듈러는 적용) x
 * -> XORShift 기반 스크램블링 알고리즘에서 파이스텔 네트워크 구조로 변경 (자세한건 문서에 기재)
 *
 * 출력값 검증: 0 이상 3,521,614,606,208 이하의 고유한 정수인지 확인
 */
@Component
public class IdScrambler {

	@Value("${SCRAMBLING_CONST_XOR1}")
	private long xorConst1;

	@Value("${SCRAMBLING_CONST_XOR2}")
	private long xorConst2;

	@Value("${SCRAMBLING_CONST_XOR3}")
	private long xorConst3;

	/**
	 * 최대값 M = 62^7 -> 보장 가능한 7자리 이하의 고유한 ID 개수
	 */
	private final long M;

	/**
	 * M의 제곱근 -> 파이스텔 L, R을 나누는 기준 크기
	 * (전체 공간 M ≈ m × m)
	 */
	private final long m;

	/**
	 * 기본 생성자 -> Spring Container 용
	 */
	public IdScrambler() {
		this.M = 3_521_614_606_208L;
		this.m = (long) Math.ceil(Math.sqrt(M));
	}

	/**
	 * Property Based Testing 용
	 */
	public IdScrambler(long M, long xorConst1, long xorConst2, long xorConst3) {
		this.M = M;
		this.m = (long) Math.ceil(Math.sqrt(M));
		this.xorConst1 = xorConst1;
		this.xorConst2 = xorConst2;
		this.xorConst3 = xorConst3;
	}

	/**
	 * 파이스텔 네트워크 연산에 사용되는 F 함수
	 * 무작위성을 부여하는 비선형 혼합기
	 * XORShift 연산 사용
	 */
	private long F(long x) {
		long val = x;
		val ^= (val << xorConst1);
		val ^= (val >>> xorConst2);
		val ^= (val << xorConst3);

		// 결과값을 [0, m-1] 범위로 축소
		// 파이스텔 네트워크에서 노드의 크기는 최대 m이어야 함
		return Math.floorMod(val, m);
	}

	/**
	 * 파이스텔 네트워크 기반 ID 스크램블링 메서드
	 */
	public Long scramble(Long id) {
		// 입력값 검증
		if (id == null) {
			throw new BusinessException(SCRAMBLE_INPUT_NULL);
		}
		if (id < 0) {
			throw new BusinessException(SCRAMBLE_INPUT_NEGATIVE);
		}
		if (id >= M) {
			throw new BusinessException(SCRAMBLE_INPUT_OVERFLOW);
		}

		long currentId = id;

		/**
		 * 각 라운드 구조
		 * L1 = R0
		 * R1 = (L0 + F(R0)) mod m
		 */
		while (true) {

			long L = currentId / m;
			long R = currentId % m;

			long F1 = F(L);
			R = Math.floorMod(R + F1, m);

			long F2 = F(R);
			L = Math.floorMod(L + F2, m);

			long F3 = F(L);
			R = Math.floorMod(R + F3, m);

			long F4 = F(R);
			L = Math.floorMod(L + F4, m);

			currentId = L * m + R;

			//
			if (currentId < M) {
				return currentId;
			}
		}
	}

	/**
	 * 파이스텔 네트워크 기반 ID 복호화 메서드
	 * 비즈니스 로직에서는 사용되지 않음
	 * 파이스텔 네트워크 구조의 가역성을 검증하기 위한 용도
	 * -> 2026.02.28: keyword 기반 shortCode 생성 기능 추가하면서, 비즈니스 로직에서도 사용됨
	 */
	public Long descramble(Long scrambledId) {
		// 입력값 검증
		if (scrambledId == null) {
			throw new BusinessException(UNSCRAMBLE_INPUT_NULL);
		}
		if (scrambledId < 0) {
			throw new BusinessException(UNSCRAMBLE_INPUT_NEGATIVE);
		}
		if (scrambledId >= M) {
			throw new BusinessException(UNSCRAMBLE_INPUT_OVERFLOW);
		}

		long currentId = scrambledId;

		/**
		 * 각 라운드 구조 (역순)
		 * R0 = L1
		 * L0 = (R1 - F(L1)) mod m
		 */
		while (true) {

			long L = currentId / m;
			long R = currentId % m;

			long F4 = F(R);
			L = Math.floorMod(L - F4, m);

			long F3 = F(L);
			R = Math.floorMod(R - F3, m);

			long F2 = F(R);
			L = Math.floorMod(L - F2, m);

			long F1 = F(L);
			R = Math.floorMod(R - F1, m);

			currentId = L * m + R;

			if (currentId < M) {
				return currentId;
			}
		}

	}
}
