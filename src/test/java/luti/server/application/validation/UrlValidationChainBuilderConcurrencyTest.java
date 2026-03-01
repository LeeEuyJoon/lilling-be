package luti.server.application.validation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import luti.server.application.result.UrlVerifyResult;
import luti.server.application.validation.UrlValidation.v1.UrlExistenceValidationHandler;
import luti.server.application.validation.UrlValidation.v1.UrlFormatValidationHandler;
import luti.server.application.validation.UrlValidation.v1.UrlOwnershipValidationHandler;
import luti.server.application.validation.UrlValidation.v1.UrlValidationChainBuilder;
import luti.server.application.validation.UrlValidation.v1.UrlValidationContext;
import luti.server.application.validation.UrlValidation.v1.UrlValidationHandler;
import luti.server.domain.enums.VerifyUrlStatus;
import luti.server.domain.service.UrlQueryService;
import luti.server.domain.service.dto.UrlMappingInfo;
import luti.server.domain.util.Base62Encoder;

class UrlValidationChainBuilderConcurrencyTest {

	private UrlValidationChainBuilder chainBuilder;
	private UrlFormatValidationHandler formatValidator;
	private UrlExistenceValidationHandler existenceValidator;
	private UrlOwnershipValidationHandler ownershipValidator;

	private UrlQueryService urlQueryService;
	private Base62Encoder base62Encoder;

	@BeforeEach
	void setUp() {
		urlQueryService = mock(UrlQueryService.class);
		base62Encoder = mock(Base62Encoder.class);

		formatValidator = new UrlFormatValidationHandler(urlQueryService);
		existenceValidator = new UrlExistenceValidationHandler(base62Encoder, urlQueryService);
		ownershipValidator = new UrlOwnershipValidationHandler();

		chainBuilder = new UrlValidationChainBuilder(formatValidator, existenceValidator, ownershipValidator);

		when(urlQueryService.verifyAndExtractShortCode(anyString()))
			.thenReturn(Optional.of("abc123"));
		when(base62Encoder.decode(anyString()))
			.thenReturn(123L);
		when(urlQueryService.findByDecodedId(anyLong()))
			.thenReturn(Optional.of(createMockUrlMappingInfo(false)));
	}

	@Test
	@DisplayName("단일 스레드 환경 - 정상 동작 확인")
	void testSingleThread() {
		UrlValidationContext context = new UrlValidationContext("https://short.url/abc123");
		UrlValidationHandler chain = chainBuilder.buildVerifyChain();

		UrlVerifyResult result = chain.validate(context);

		assertEquals(VerifyUrlStatus.OK, result.getStatus());
	}


	@RepeatedTest(10)
	@DisplayName("멀티스레드 환경 - 소유권 검증 누락 확인")
	void testOwnershipValidationSkipped() throws Exception {
		int verifyThreads = 30;
		int claimThreads = 30;
		int totalThreads = verifyThreads + claimThreads;

		ExecutorService executorService = Executors.newFixedThreadPool(totalThreads);
		CyclicBarrier barrier = new CyclicBarrier(totalThreads);
		CountDownLatch doneLatch = new CountDownLatch(totalThreads);

		AtomicInteger correctOwnershipCheck = new AtomicInteger(0);
		AtomicInteger skippedOwnershipCheck = new AtomicInteger(0);
		List<Throwable> exceptions = new ArrayList<>();

		// Mock: 소유자가 있는 URL → Verify는 ALREADY_OWNED 반환해야 함
		when(urlQueryService.findByDecodedId(anyLong()))
			.thenReturn(Optional.of(createMockUrlMappingInfo(true)));

		// Verify 스레드들
		for (int i = 0; i < verifyThreads; i++) {
			executorService.submit(() -> {
				try {
					UrlValidationContext context = new UrlValidationContext("https://short.url/abc123");
					UrlValidationHandler chain = chainBuilder.buildVerifyChain();

					barrier.await(); // 30개의 Verify 스레드와 30개의 Claim 스레드가 동시에 시작하도록 대기

					UrlVerifyResult result = chain.validate(context);

					if (result.getStatus() == VerifyUrlStatus.ALREADY_OWNED) {
						correctOwnershipCheck.incrementAndGet();
					} else if (result.getStatus() == VerifyUrlStatus.OK) {
						skippedOwnershipCheck.incrementAndGet();
						System.err.println("Verify 체인에서 소유권 검증 누락 (OK 반환)");
					}
				} catch (Throwable e) {
					synchronized (exceptions) {
						exceptions.add(e);
					}
				} finally {
					doneLatch.countDown();
				}
			});
		}

		// Claim 스레드들 - 계속 체인을 null로 변경
		for (int i = 0; i < claimThreads; i++) {
			executorService.submit(() -> {
				try {
					barrier.await(); // 30개의 Verify 스레드와 30개의 Claim 스레드가 동시에 시작하도록 대기

					// 반복적으로 Claim 체인 호출 → next를 null로 덮어씀
					for (int j = 0; j < 10; j++) {
						chainBuilder.buildClaimChain();
						Thread.sleep(1); // 타이밍 조정
					}
				} catch (Throwable e) {
					synchronized (exceptions) {
						exceptions.add(e);
					}
				} finally {
					doneLatch.countDown();
				}
			});
		}

		assertTrue(doneLatch.await(20, TimeUnit.SECONDS), "타임아웃");
		executorService.shutdown();

		System.out.println("=== 테스트 결과 ===");
		System.out.println("소유권 검증 정상 수행: " + correctOwnershipCheck.get() + " / " + verifyThreads);
		System.out.println("소유권 검증 누락 (race condition): " + skippedOwnershipCheck.get());
		System.out.println("예외 발생: " + exceptions.size());

		if (skippedOwnershipCheck.get() > 0) {
			System.err.println("\n⚠️ Race condition 확인됨!");
			System.err.println("Verify 체인 실행 중 Claim 체인 build가 next를 null로 덮어써서 소유권 검증 누락");
			fail("Race condition 발생: " + skippedOwnershipCheck.get() + "개의 Verify 요청에서 소유권 검증 누락");
		}
	}

	private UrlMappingInfo createMockUrlMappingInfo(boolean hasOwner) {
		try {
			var constructor = UrlMappingInfo.class.getDeclaredConstructor(
				Long.class, String.class, String.class, Long.class, java.time.LocalDateTime.class, boolean.class
			);
			constructor.setAccessible(true);
			return constructor.newInstance(
				1L,
				"https://example.com",
				"https://short.url/abc123",
				0L,
				java.time.LocalDateTime.now(),
				hasOwner
			);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create UrlMappingInfo", e);
		}
	}
}
