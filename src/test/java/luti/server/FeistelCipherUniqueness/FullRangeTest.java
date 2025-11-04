package luti.server.FeistelCipherUniqueness;

import static org.assertj.core.api.AssertionsForClassTypes.*;

import java.util.HashSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import luti.server.Service.IdScrambler;

public class FullRangeTest {

	@Test
	@DisplayName("XORShift 고유성 증명 - M=1,000,000 전체 범위 테스트")
	void fullRangeTest() {

		long smallM = 1_000_000L; // 100만

		IdScrambler scrambler = new IdScrambler(smallM, 13, 7, 17);

		HashSet<Long> set = new HashSet<>((int) smallM);

		for (long id = 0; id < smallM; id++) {

			// 알고리즘 적용
			Long scrambled = scrambler.scramble(id);

			// 출력 범위 검증
			assertThat(scrambled)
				.as("ID: %d, 스크램블된 값이 범위를 벗어났습니다: %d", id, scrambled)
				.isGreaterThanOrEqualTo(0L)
				.isLessThan(smallM);


			// 고유성 검증
			if (!set.add(scrambled)) {
				fail("중복된 스크램블된 ID가 발견되었습니다. ID: " + id + ", 스크램블된 값: " + scrambled);
			}

		}

		// 5. 최종 검증: 루프가 끝난 후, 100만 개의 고유한 ID가 나왔는지 확인
		assertThat(set.size())
			.as("스크램블된 ID의 개수가 일치하지 않습니다.")
			.isEqualTo(smallM);
	}
}