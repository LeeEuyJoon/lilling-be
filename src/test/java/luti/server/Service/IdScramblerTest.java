package luti.server.Service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class IdScramblerTest {

	@Autowired
	private IdScrambler scrambler;

	@Test
	@DisplayName("ID 스크램블 테스트 입력값 검증 - null")
	void testScramble1() {
		Long originalId = null;

		IllegalArgumentException exception =
			assertThrows(
				IllegalArgumentException.class,
				() -> scrambler.scramble(originalId)
			);

		System.out.println("예외 메시지: " +
			exception.getMessage());
	}

	@Test
	@DisplayName("ID 스크램블 테스트 입력값 검증 - 음수")
	void testScramble2() {
		Long originalId = -123L;

		IllegalArgumentException exception =
			assertThrows(
				IllegalArgumentException.class,
				() -> scrambler.scramble(originalId)
			);

		System.out.println("예외 메시지: " +
			exception.getMessage());
	}

	@Test
	@DisplayName("ID 스크램블 테스트 입력값 검증 - M 초과")
	void testScramble3() {
		Long originalId = 4_521_614_606_208L;

		IllegalArgumentException exception =
			assertThrows(
				IllegalArgumentException.class,
				() -> scrambler.scramble(originalId)
			);

		System.out.println("예외 메시지: " +
			exception.getMessage());
	}

	@Test
	@DisplayName("ID 스크램블 테스트 정상 동작 - 작은 수")
	void testScramble4() {
		Long originalId1 = 1L;
		Long originalId2 = 123L;
		Long originalId3 = 9999L;

		Long scrambledId1 = scrambler.scramble(originalId1);
		Long scrambledId2 = scrambler.scramble(originalId2);
		Long scrambledId3 = scrambler.scramble(originalId3);

		System.out.println("원본 ID 1: " + originalId1);
		System.out.println("스크램블된 ID 1: " + scrambledId1);
		System.out.println("원본 ID 2: " + originalId2);
		System.out.println("스크램블된 ID 2: " + scrambledId2);
		System.out.println("원본 ID 3: " + originalId3);
		System.out.println("스크램블된 ID 3: " + scrambledId3);

		assertNotEquals(originalId1, scrambledId1);
		assertNotEquals(originalId2, scrambledId2);
		assertNotEquals(originalId3, scrambledId3);
		assertTrue(scrambledId1 >= 0 && scrambledId1 < 3_521_614_606_208L);
		assertTrue(scrambledId2 >= 0 && scrambledId2 < 3_521_614_606_208L);
		assertTrue(scrambledId3 >= 0 && scrambledId3 < 3_521_614_606_208L);
	}

	@Test
	@DisplayName("ID 스크램블 테스트 정상 동작 - 큰 수")
	void testScramble5() {
		Long originalId1 = 1_000_000_000L;
		Long originalId2 = 23_523_050_610L;
		Long originalId3 = 3_123_123_123_123L;

		Long scrambledId1 = scrambler.scramble(originalId1);
		Long scrambledId2 = scrambler.scramble(originalId2);
		Long scrambledId3 = scrambler.scramble(originalId3);

		System.out.println("원본 ID 1: " + originalId1);
		System.out.println("스크램블된 ID 1: " + scrambledId1);
		System.out.println("원본 ID 2: " + originalId2);
		System.out.println("스크램블된 ID 2: " + scrambledId2);
		System.out.println("원본 ID 3: " + originalId3);
		System.out.println("스크램블된 ID 3: " + scrambledId3);

		assertNotEquals(originalId1, scrambledId1);
		assertNotEquals(originalId2, scrambledId2);
		assertNotEquals(originalId3, scrambledId3);
		assertTrue(scrambledId1 >= 0 && scrambledId1 < 3_521_614_606_208L);
		assertTrue(scrambledId2 >= 0 && scrambledId2 < 3_521_614_606_208L);
		assertTrue(scrambledId3 >= 0 && scrambledId3 < 3_521_614_606_208L);
	}
}