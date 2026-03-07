package luti.server.domain.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class KeywordSuffixScrambler {

	private final FeistelBijection[] bijections;

	public KeywordSuffixScrambler(
		@Value("${SCRAMBLING_CONST_XOR1}") long xorConst1,
		@Value("${SCRAMBLING_CONST_XOR2}") long xorConst2,
		@Value("${SCRAMBLING_CONST_XOR3}") long xorConst3
	) {
		bijections = new FeistelBijection[7];
		long M = 62L;
		for (int k = 1; k <= 6; k++) {
			bijections[k] = new FeistelBijection(M, xorConst1, xorConst2, xorConst3);
			M *= 62L;
		}
	}

	public long scramble(long i, int suffixMaxLen) {
		return bijections[suffixMaxLen].permute(i);
	}

	public long suffixSpace(int suffixMaxLen) {
		return bijections[suffixMaxLen].getM();
	}

}
