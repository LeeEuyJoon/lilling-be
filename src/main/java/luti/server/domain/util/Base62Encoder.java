package luti.server.domain.util;

import static luti.server.exception.ErrorCode.*;

import org.springframework.stereotype.Component;

import luti.server.exception.BusinessException;


@Component
public class Base62Encoder {

	private static final String CHARACTERS =
		"0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final int BASE = CHARACTERS.length();

	public String encode(Long number) {

		// 입력값 검증
		if (number == null) {
			throw new BusinessException(ENCODE_INPUT_NULL);
		}
		if (number < 0) {
			throw new BusinessException(ENCODE_INPUT_NEGATIVE);
		}
		if (number >= 3_521_614_606_208L) {
			throw new BusinessException(ENCODE_INPUT_OVERFLOW);
		}

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

	public Long decode(String encoded) {

		// 입력값 검증
		if (encoded == null || encoded.isEmpty()) {
			throw new BusinessException(DECODE_INPUT_EMPTY);
		}

		long result = 0;

		// 각 문자를 순회하면서 디코딩
		for (char c : encoded.toCharArray()) {
			int index = CHARACTERS.indexOf(c);

			// 유효하지 않은 문자 검증
			if (index == -1) {
				throw new BusinessException(DECODE_INVALID_CHARACTER);
			}

			// result = result * 62 + index
			result = result * BASE + index;
		}

		return result;
	}
}
