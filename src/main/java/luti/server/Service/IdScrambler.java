package luti.server.Service;

import java.math.BigInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import jakarta.annotation.PostConstruct;

/**
 * ID 스크램블링 서비스 클래스
 * 입력값: 0 이상 3,521,614,606,208 미만의 Long 타입의 원본 ID (서비스에서는 1부터 시작하는 연속된 정수 ID)
 * 출력값: String 타입의 스크램블된 ID (3,521,614,606,208 미만의 고유한 정수)
 * 입력값 검증: 입력값이 null이거나 음수인 경우 IllegalArgumentException 예외 발생
 * 스크램블링 알고리즘: 선형식 + 모듈러 연산 -> ( A * id + B mod ) M (단, A와 M은 서로소, B는 0 이상 M 미만의 정수) x
 * -> 스크램블링 알고리즘 기존 선형식 기반에서 XORShift 연산으로 변경됨 (모듈러는 적용)
 * 출력값 검증: 0 이상 3,521,614,606,208 이하의 고유한 정수인지 확인 (DB 검증은 생략)
 */
@Service
public class IdScrambler {

	@Value("${SCRAMBLING_CONST_A}")
	private long A;

	@Value("${SCRAMBLING_CONST_B}")
	private long B;

	@Value("${SCRAMBLING_CONST_XOR1}")
	private long xorConst1;

	@Value("${SCRAMBLING_CONST_XOR2}")
	private long xorConst2;

	@Value("${SCRAMBLING_CONST_XOR3}")
	private long xorConst3;

	private static final long M = 3_521_614_606_208L;


	// XORShift 기반 스크램블링 알고리즘
	public Long scramble(Long id) {
		// 입력값 검증
		Assert.notNull(id, "ID must not be null");
		Assert.isTrue(id >= 0, "ID must be non-negative");
		Assert.isTrue(id < M, "ID must be less than " + M);

		// 스크램블링 알고리즘 적용
		Long x= id;
		x ^= (x << xorConst1);
		x ^= (x >>> xorConst2);
		x ^= (x << xorConst3);

		Long scrambledId = Math.floorMod(x, M);
		return scrambledId;
	}

	// 기존 선형식 기반 스크램블링 알고리즘 (혹시 몰라서 일단 보관)
	public Long scramble_linear(Long id) {

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

	/**
	 * 초기화 시 상수 A와 M이 서로소인지 검증
	 */
	// @PostConstruct
	public void validateConstants() {
		BigInteger bigA = BigInteger.valueOf(A);
		BigInteger bigM = BigInteger.valueOf(M);

		BigInteger gcd = bigA.gcd(bigM);
		Assert.isTrue(
			gcd.equals(BigInteger.ONE),
			String.format("Invalid constants: A (%d) and M (%d) are not coprime. gcd=%s", A, M, gcd)
		);
	}


}
