package luti.server.application.bus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import luti.server.application.handler.QueryHandler;
import luti.server.application.query.IQuery;
import luti.server.application.query.RedirectQuery;
import luti.server.application.result.RedirectResult;

@DisplayName("QueryBus 단위 테스트")
class QueryBusTest {

    // -------------------------------------------------------------------------
    // 내부 테스트용 Query/Handler 스텁 클래스
    // -------------------------------------------------------------------------

    static class AnotherQuery implements IQuery<String> {
        private final String value;

        AnotherQuery(String value) {
            this.value = value;
        }

        String getValue() {
            return value;
        }
    }

    static class AnotherQueryHandler implements QueryHandler<AnotherQuery, String> {

        @Override
        public String execute(AnotherQuery query) {
            return "result: " + query.getValue();
        }

        @Override
        public Class<AnotherQuery> getSupportedQueryType() {
            return AnotherQuery.class;
        }
    }

    static class UnregisteredQuery implements IQuery<String> {}

    // -------------------------------------------------------------------------
    // 라우팅 검증
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("핸들러 라우팅 검증")
    class HandlerRouting {

        @Test
        @DisplayName("RedirectQuery - 등록된 핸들러로 올바르게 라우팅됨")
        void RedirectQuery_등록된핸들러_라우팅() {
            // Given
            String shortCode = "abc1234";
            RedirectQuery query = new RedirectQuery(shortCode);
            RedirectResult expected = RedirectResult.of("https://www.example.com");

            @SuppressWarnings("unchecked")
            QueryHandler<RedirectQuery, RedirectResult> mockHandler =
                (QueryHandler<RedirectQuery, RedirectResult>) mock(QueryHandler.class);
            when(mockHandler.getSupportedQueryType()).thenReturn(RedirectQuery.class);
            when(mockHandler.execute(query)).thenReturn(expected);

            QueryBus queryBus = new QueryBus(List.of(mockHandler));

            System.out.println("=== RedirectQuery 라우팅 검증 ===");
            System.out.println("Query 타입: " + query.getClass().getSimpleName());

            // When
            RedirectResult result = queryBus.execute(query);

            // Then
            assertNotNull(result);
            assertEquals(expected.getOriginalUrl(), result.getOriginalUrl());

            System.out.println("라우팅 성공, originalUrl: " + result.getOriginalUrl());

            verify(mockHandler).execute(query);
        }

        @Test
        @DisplayName("AnotherQuery - 해당 타입의 핸들러로 올바르게 라우팅됨")
        void AnotherQuery_등록된핸들러_라우팅() {
            // Given
            AnotherQuery query = new AnotherQuery("test-value");
            AnotherQueryHandler anotherHandler = new AnotherQueryHandler();
            QueryBus queryBus = new QueryBus(List.of(anotherHandler));

            System.out.println("=== AnotherQuery 라우팅 검증 ===");
            System.out.println("Query 타입: " + query.getClass().getSimpleName());
            System.out.println("입력값: " + query.getValue());

            // When
            String result = queryBus.execute(query);

            // Then
            assertNotNull(result);
            assertEquals("result: test-value", result);

            System.out.println("라우팅 성공, 결과: " + result);
        }

        @Test
        @DisplayName("여러 핸들러 등록 - 각 Query가 올바른 핸들러로 라우팅됨")
        void 여러핸들러_각Query_올바른핸들러_라우팅() {
            // Given
            RedirectResult redirectResult = RedirectResult.of("https://www.example.com");

            @SuppressWarnings("unchecked")
            QueryHandler<RedirectQuery, RedirectResult> redirectHandler =
                (QueryHandler<RedirectQuery, RedirectResult>) mock(QueryHandler.class);
            when(redirectHandler.getSupportedQueryType()).thenReturn(RedirectQuery.class);
            when(redirectHandler.execute(any(RedirectQuery.class))).thenReturn(redirectResult);

            AnotherQueryHandler anotherHandler = new AnotherQueryHandler();

            QueryBus queryBus = new QueryBus(List.of(redirectHandler, anotherHandler));

            RedirectQuery redirectQuery = new RedirectQuery("abc1234");
            AnotherQuery anotherQuery = new AnotherQuery("hello");

            System.out.println("=== 여러 핸들러 라우팅 검증 ===");

            // When
            RedirectResult resultA = queryBus.execute(redirectQuery);
            String resultB = queryBus.execute(anotherQuery);

            // Then
            assertNotNull(resultA);
            assertEquals("https://www.example.com", resultA.getOriginalUrl());

            assertNotNull(resultB);
            assertEquals("result: hello", resultB);

            System.out.println("RedirectQuery 결과: " + resultA.getOriginalUrl());
            System.out.println("AnotherQuery 결과: " + resultB);

            verify(redirectHandler).execute(redirectQuery);
        }

        @Test
        @DisplayName("같은 Query 타입으로 여러 번 실행 - 매번 동일한 핸들러가 처리")
        void 같은Query_여러번실행_동일핸들러_처리() {
            // Given
            AnotherQueryHandler handler = spy(new AnotherQueryHandler());
            QueryBus queryBus = new QueryBus(List.of(handler));

            System.out.println("=== 같은 Query 타입 반복 실행 검증 ===");

            // When
            String result1 = queryBus.execute(new AnotherQuery("first"));
            String result2 = queryBus.execute(new AnotherQuery("second"));
            String result3 = queryBus.execute(new AnotherQuery("third"));

            // Then
            assertEquals("result: first", result1);
            assertEquals("result: second", result2);
            assertEquals("result: third", result3);

            System.out.println("결과1: " + result1);
            System.out.println("결과2: " + result2);
            System.out.println("결과3: " + result3);

            verify(handler, times(3)).execute(any(AnotherQuery.class));
        }
    }

    // -------------------------------------------------------------------------
    // 미등록 Query 예외 검증
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("미등록 Query 예외 검증")
    class UnregisteredQueryException {

        @Test
        @DisplayName("미등록 Query 실행 - IllegalArgumentException 발생")
        void 미등록Query_실행_예외발생() {
            // Given
            AnotherQueryHandler anotherHandler = new AnotherQueryHandler();
            QueryBus queryBus = new QueryBus(List.of(anotherHandler));

            UnregisteredQuery unregisteredQuery = new UnregisteredQuery();

            System.out.println("=== 미등록 Query 예외 검증 ===");
            System.out.println("미등록 Query 타입: " + unregisteredQuery.getClass().getSimpleName());

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> queryBus.execute(unregisteredQuery));

            assertNotNull(exception.getMessage());
            assertTrue(exception.getMessage().contains("UnregisteredQuery"),
                "예외 메시지에 Query 클래스명이 포함되어야 함. 실제: " + exception.getMessage());

            System.out.println("예외 발생: " + exception.getMessage());
        }

        @Test
        @DisplayName("핸들러 없이 생성된 QueryBus - 모든 Query에 대해 IllegalArgumentException 발생")
        void 핸들러없는_QueryBus_모든Query_예외발생() {
            // Given
            QueryBus emptyQueryBus = new QueryBus(List.of());

            RedirectQuery query = new RedirectQuery("abc1234");

            System.out.println("=== 빈 QueryBus 예외 검증 ===");

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> emptyQueryBus.execute(query));

            assertNotNull(exception.getMessage());
            assertTrue(exception.getMessage().contains("RedirectQuery"),
                "예외 메시지에 Query 클래스명이 포함되어야 함. 실제: " + exception.getMessage());

            System.out.println("예외 발생: " + exception.getMessage());
        }

        @Test
        @DisplayName("RedirectQuery 핸들러만 등록 - AnotherQuery 실행 시 IllegalArgumentException")
        void RedirectQuery핸들러만_등록_AnotherQuery_예외() {
            // Given
            @SuppressWarnings("unchecked")
            QueryHandler<RedirectQuery, RedirectResult> redirectHandler =
                (QueryHandler<RedirectQuery, RedirectResult>) mock(QueryHandler.class);
            when(redirectHandler.getSupportedQueryType()).thenReturn(RedirectQuery.class);

            QueryBus queryBus = new QueryBus(List.of(redirectHandler));

            AnotherQuery unregistered = new AnotherQuery("test");

            System.out.println("=== RedirectQuery 핸들러만 있을 때 AnotherQuery 예외 ===");

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> queryBus.execute(unregistered));

            assertNotNull(exception.getMessage());

            System.out.println("예외 발생: " + exception.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // QueryBus 생성 시 핸들러 등록 검증
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("QueryBus 생성 - 핸들러 등록 검증")
    class QueryBusConstruction {

        @Test
        @DisplayName("중복 타입의 핸들러 등록 - 마지막 핸들러로 덮어써짐")
        void 중복타입_핸들러_등록_마지막핸들러_덮어씀() {
            // Given
            AnotherQueryHandler handler1 = new AnotherQueryHandler() {
                @Override
                public String execute(AnotherQuery query) {
                    return "handler1: " + query.getValue();
                }
            };
            AnotherQueryHandler handler2 = new AnotherQueryHandler() {
                @Override
                public String execute(AnotherQuery query) {
                    return "handler2: " + query.getValue();
                }
            };

            // handler2가 나중에 등록되므로 handler1을 덮어씀
            QueryBus queryBus = new QueryBus(List.of(handler1, handler2));

            AnotherQuery query = new AnotherQuery("input");

            System.out.println("=== 중복 핸들러 등록 시 덮어쓰기 동작 검증 ===");

            // When
            String result = queryBus.execute(query);

            // Then
            assertEquals("handler2: input", result);

            System.out.println("실행된 핸들러 결과: " + result);
        }
    }
}
