package luti.server.Service;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * Base62 인코딩 서비스 클래스
 * long 타입의 숫자를 Base62 문자열로 변환합니다.
 * 입력값 : 0 이상 3,521,614,606,208 미만의 long 숫자 (IdScrambler의 결과)
 * 출력값 : Base62로 인코딩된 문자열 (7자리 이하)
 */
@Service
public class Base62Encoder {

	private static final String CHARACTERS =
		"0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final int BASE = CHARACTERS.length();

	public String encode(Long number) {

		// 입력값 검증
		Assert.notNull(number, "Number not be null");
		Assert.isTrue(number >= 0, "Number must be non-negative");
		Assert.isTrue(number < 3_521_614_606_208L, "Number must be less than 3,521,614,606,208");

		// 입력값이 0이면 "0"을 반환
		if (number == 0) {
			return String.valueOf(CHARACTERS.charAt(0));
		}

		StringBuilder sb = new StringBuilder();

		// number가 0보다 큰 동안 반복
		while (number > 0) {
			// 62로 나눈 나머지를 구함 (0 ~ 61)
			int remainder = (int) (number % BASE);

			// 나머지에 해당하는 문자를 sb에 추가
			sb.append(CHARACTERS.charAt(remainder));

			// number를 62로 나눔
			number /= BASE;
		}

		// 뒤집어서 리턴
		return sb.reverse().toString();
	}
}
