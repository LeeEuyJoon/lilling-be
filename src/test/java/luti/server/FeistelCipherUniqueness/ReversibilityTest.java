package luti.server.FeistelCipherUniqueness;

import static org.assertj.core.api.AssertionsForClassTypes.*;

import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.LongRange;

import luti.server.util.IdScrambler;

public class ReversibilityTest {

	private static final long M = 3_521_614_606_208L;

	private final IdScrambler scrambler = new IdScrambler(M, 13, 7, 17);

	@Property(tries = 10_000)
	@Label("Feistel Cipher 가역성 테스트 - ID 복원 검증")
	void testReversibility(
		@ForAll @LongRange(min = 0, max = M - 1) long originalId
	) {

		// 알고리즘 실행
		Long scrambled = scrambler.scramble(originalId);
		Long unscrambled = scrambler.unscramble(scrambled);

		// 가역성 검증
		assertThat(unscrambled)
			.as("원본 ID: %d, 스크램블된 값: %d, 복원된 값: %d",
				originalId, scrambled, unscrambled)
			.isEqualTo(originalId);

	}

}
