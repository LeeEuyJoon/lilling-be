package luti.server.FeistelCipherUniqueness;

import static org.assertj.core.api.Assertions.*;

import java.util.HashSet;
import java.util.List;

import net.jqwik.api.*;

import luti.server.Service.IdScrambler;

public class PropertyBasedTest {

	private static final long M = 3_521_614_606_208L;

	// 리스트 크기를 파라미터로 받아서 Arbitrary 생성
	Arbitrary<List<Long>> idListOfSize(int size) {
		return Arbitraries.longs()
			.between(1, M - 1)		// 1 이상 M-1 이하의 랜덤한 값을
			.list()					// 리스트로
			.ofSize(size)			// size 크기만큼
			.uniqueElements();		// 고유한 값으로 생성
	}

	@Group
	class ScramblePropertyTests {
		// 프로바이더 정의
		@Provide Arbitrary<List<Long>> ids_hundred() { return idListOfSize(100); }				// ID 100개
		@Provide Arbitrary<List<Long>> ids_10_thousand() { return idListOfSize(10_000); }		// ID 1만개
		// @Provide Arbitrary<List<Long>> ids_million() { return idListOfSize(1_000_000); }		// ID 100만개 -> 안돌아감 ... ㅋㅋ

		@Property(tries = 10000)
		@Label("XORShift 고유성 테스트 - 100개 ID 10000번")
		void testScrambleWith100Ids(@ForAll("ids_hundred") List<Long> ids) {
			runScrambleTest(ids);
		}

		@Property(tries = 100)
		@Label("XORShift 고유성 테스트 - 1만개 ID 100번")
		void testScrambleWith10000Ids(@ForAll("ids_10_thousand") List<Long> ids) {
			runScrambleTest(ids);
		}

		// @Property(tries = 1)
		// @Label("XORShift 고유성 테스트 - 100만개 ID 1번")
		// void testScrambleWith1MillionIds(@ForAll("ids_million") List<Long> ids) {
		// 	runScrambleTest(ids);
		// }


	}

	// 공통 테스트 로직 메서드
	void runScrambleTest(List<Long> ids) {
		IdScrambler scrambler = new IdScrambler(M, 13, 7, 17);
		HashSet<Long> seen = new HashSet<>();

		for (Long id : ids) {
			Long scrambled = scrambler.scramble(id);

			assertThat(scrambled)
				.isGreaterThanOrEqualTo(0L)
				.isLessThan(M);

			assertThat(seen).doesNotContain(scrambled);
			seen.add(scrambled);
		}
	}
}
