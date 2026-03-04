package luti.server.domain.util;

public class FeistelBijection {

	private final long M;
	private final long m;
	private final long xorConst1;
	private final long xorConst2;
	private final long xorConst3;

	public FeistelBijection(long M, long xorConst1, long xorConst2, long xorConst3) {
		this.M = M;
		this.m = (long) Math.ceil(Math.sqrt(M));
		this.xorConst1 = xorConst1;
		this.xorConst2 = xorConst2;
		this.xorConst3 = xorConst3;
	}

	/**
	 * 파이스텔 네트워크 연산에 사용되는 F 함수
	 * 무작위성을 부여하는 비선형 혼합기
	 * XORShift 연산 사용
	 */
	private long F(long x) {
		long val = x;
		val ^= (val << xorConst1);
		val ^= (val >>> xorConst2);
		val ^= (val << xorConst3);
		return Math.floorMod(val, m);
	}


	public long permute(long id) {

		long currentId = id;

		/**
		 * 각 라운드 구조
		 * L1 = R0
		 * R1 = (L0 + F(R0)) mod m
		 */
		while (true) {

			long L = currentId / m;
			long R = currentId % m;

			long F1 = F(L);
			R = Math.floorMod(R + F1, m);

			long F2 = F(R);
			L = Math.floorMod(L + F2, m);

			long F3 = F(L);
			R = Math.floorMod(R + F3, m);

			long F4 = F(R);
			L = Math.floorMod(L + F4, m);

			currentId = L * m + R;

			if (currentId < M) {
				return currentId;
			}
		}
	}

	public long inversePermute(long id) {
		long currentId = id;

		/**
		 * 각 라운드 구조 (역순)
		 * R0 = L1
		 * L0 = (R1 - F(L1)) mod m
		 */
		while (true) {

			long L = currentId / m;
			long R = currentId % m;

			long F4 = F(R);
			L = Math.floorMod(L - F4, m);

			long F3 = F(L);
			R = Math.floorMod(R - F3, m);

			long F2 = F(R);
			L = Math.floorMod(L - F2, m);

			long F1 = F(L);
			R = Math.floorMod(R - F1, m);

			currentId = L * m + R;

			if (currentId < M) {
				return currentId;
			}
		}
	}


}
