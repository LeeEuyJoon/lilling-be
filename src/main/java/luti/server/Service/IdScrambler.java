package luti.server.Service;

import java.math.BigInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * ID 스크램블링 서비스 클래스
 * 입력값: 0 이상 3,521,614,606,208 미만의 Long 타입의 원본 ID (서비스에서는 1부터 시작하는 연속된 정수 ID)
 * 출력값: String 타입의 스크램블된 ID (3,521,614,606,208 미만의 고유한 정수)
 * 입력값 검증: 입력값이 null이거나 음수인 경우 IllegalArgumentException 예외 발생
 * 스크램블링 알고리즘: 모듈러 연산 -> ( A * id + B mod ) M (단, A와 M은 서로소, B는 0 이상 M 미만의 정수)
 * 출력값 검증: 0 이상 3,521,614,606,208 이하의 고유한 정수인지 확인 (DB 검증은 생략)
 */
@Service
public class IdScrambler {

	@Value("${SCRAMBLING_CONST_A}")
	private long A;

	@Value("${SCRAMBLING_CONST_B}")
	private long B;

	private static final long M = 3_521_614_606_208L;

	public Long scramble(Long id) {

		// 입력값 검증
		Assert.notNull(id, "ID must not be null");
		Assert.isTrue(id >= 0, "ID must be non-negative");
		Assert.isTrue(id < M, "ID must be less than " + M);

		// 스크램블링 알고리즘 적용
		BigInteger bigA = BigInteger.valueOf(A);
		BigInteger bigB = BigInteger.valueOf(B);
		BigInteger bigM = BigInteger.valueOf(M);
		BigInteger bigId = BigInteger.valueOf(id);

		long scrambledId = bigA.multiply(bigId).add(bigB).mod(bigM).longValue();

		// 출력값 검증
		Assert.isTrue(scrambledId >= 0, "Scrambled ID must be non-negative");
		Assert.isTrue(scrambledId < M, "Scrambled ID must be less than " + M);

		return scrambledId;
	}

}
