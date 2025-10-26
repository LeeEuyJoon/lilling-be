package luti.server.Service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class Base62EncoderTest {

	@Autowired
	private Base62Encoder base62Encoder;

	@Test
	@DisplayName("Base62 인코딩 입력값 검증 - null")
	void testEncoding1() {

		Long number = null;

		IllegalArgumentException exception =
			assertThrows(
				IllegalArgumentException.class,
				() -> base62Encoder.encode(number)
			);

		System.out.println("예외 메시지: " +
			exception.getMessage());

	}

	@Test
	@DisplayName("Base62 인코딩 입력값 검증 - 음수")
	void testEncoding2() {

		Long number = -123L;

		IllegalArgumentException exception =
			assertThrows(
				IllegalArgumentException.class,
				() -> base62Encoder.encode(number)
			);

		System.out.println("예외 메시지: " +
			exception.getMessage());

	}

	@Test
	@DisplayName("Base62 인코딩 입력값 검증 - M 초과")
	void testEncoding3() {

		Long number = 4_521_614_606_208L;

		IllegalArgumentException exception =
			assertThrows(
				IllegalArgumentException.class,
				() -> base62Encoder.encode(number)
			);

		System.out.println("예외 메시지: " +
			exception.getMessage());

	}

	@Test
	@DisplayName("Base62 인코딩 정상 동작 테스트 - 0 입력")
	void testEncoding4() {

		Long number = 0L;

		String encoded = base62Encoder.encode(number);

		System.out.println("input : " + number);
		System.out.println("encoded : " + encoded);

		assertEquals("0", encoded);
	}

	@Test
	@DisplayName("Base62 인코딩 정상 동작 테스트 - 작은 수")
	void testEncoding5() {

		Long number1 = 15L;
		Long number2 = 333L;
		Long number3 = 9999L;

		String encoded1 = base62Encoder.encode(number1);
		String encoded2 = base62Encoder.encode(number2);
		String encoded3 = base62Encoder.encode(number3);

		System.out.println("input1 : " + number1);
		System.out.println("encoded1 : " + encoded1);
		System.out.println("input2 : " + number2);
		System.out.println("encoded2 : " + encoded2);
		System.out.println("input3 : " + number3);
		System.out.println("encoded3 : " + encoded3);

		assertTrue(encoded1.length() <= 7);
		assertTrue(encoded2.length() <= 7);
		assertTrue(encoded3.length() <= 7);
	}

	@Test
	@DisplayName("Base62 인코딩 정상 동작 테스트 - 큰 수")
	void testEncoding6() {

		Long number1 = 51_614_606_208L;
		Long number2 = 2_521_614_606_208L;
		Long number3 = 3_521_614_606_208L - 1;

		String encoded1 = base62Encoder.encode(number1);
		String encoded2 = base62Encoder.encode(number2);
		String encoded3 = base62Encoder.encode(number3);

		System.out.println("input1 : " + number1);
		System.out.println("encoded1 : " + encoded1);
		System.out.println("input2 : " + number2);
		System.out.println("encoded2 : " + encoded2);
		System.out.println("input3 : " + number3);
		System.out.println("encoded3 : " + encoded3);

		assertTrue(encoded1.length() <= 7);
		assertTrue(encoded2.length() <= 7);
		assertTrue(encoded3.length() <= 7);
	}
}