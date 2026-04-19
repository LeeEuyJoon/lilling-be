package luti.server.web.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import luti.server.application.bus.QueryBus;
import luti.server.application.query.RedirectQuery;
import luti.server.application.result.RedirectResult;
import luti.server.exception.BusinessException;
import luti.server.exception.ErrorCode;
import luti.server.exception.GlobalExceptionHandler;
import luti.server.web.resolver.QueryArgumentResolver;

@WebMvcTest(
    controllers = RedirectController.class,
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
		org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration.class,
		org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration.class
    }
)
@AutoConfigureMockMvc(addFilters = false)
@Import({QueryArgumentResolver.class, GlobalExceptionHandler.class})
@DisplayName("RedirectController - MockMvc 슬라이스 테스트")
class RedirectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockitoBean
    private QueryBus queryBus;

    @BeforeEach
    void setUp() {
        reset(queryBus);
    }

    // -------------------------------------------------------------------------
    // GET /{shortCode} - 리다이렉트 성공
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /{shortCode} - 리다이렉트 성공")
    class RedirectSuccess {

        @Test
        @DisplayName("유효한 shortCode - 302 Found + Location 헤더 반환")
        void 유효한shortCode_302_Location반환() throws Exception {
            // Given
            String shortCode = "abc1234";
            String originalUrl = "https://www.example.com";

            when(queryBus.execute(any(RedirectQuery.class)))
                .thenReturn(RedirectResult.of(originalUrl));

            System.out.println("=== 리다이렉트 성공 ===");
            System.out.println("shortCode: " + shortCode);
            System.out.println("originalUrl: " + originalUrl);

            // When & Then
            mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", originalUrl));

            System.out.println("302 Found + Location: " + originalUrl + " 확인");

            verify(queryBus).execute(any(RedirectQuery.class));
        }

        @Test
        @DisplayName("긴 originalUrl - 302 Found + 정확한 Location 헤더 반환")
        void 긴URL_302_Location반환() throws Exception {
            // Given
            String shortCode = "xyz9876";
            String originalUrl = "https://www.example.com/very/long/path/to/a/resource?param1=value1&param2=value2";

            when(queryBus.execute(any(RedirectQuery.class)))
                .thenReturn(RedirectResult.of(originalUrl));

            System.out.println("=== 긴 URL 리다이렉트 ===");
            System.out.println("shortCode: " + shortCode);
            System.out.println("originalUrl: " + originalUrl);

            // When & Then
            mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", originalUrl));

            System.out.println("302 Found + 긴 URL Location 확인");
        }

        @Test
        @DisplayName("QueryBus가 반환한 originalUrl이 Location 헤더에 정확히 포함됨")
        void QueryBus_반환URL_Location_정확히_포함() throws Exception {
            // Given
            String shortCode = "testXYZ";
            String expectedUrl = "https://www.naver.com/path?q=spring";

            when(queryBus.execute(any(RedirectQuery.class)))
                .thenReturn(RedirectResult.of(expectedUrl));

            System.out.println("=== QueryBus 반환값 검증 ===");
            System.out.println("expectedUrl: " + expectedUrl);

            // When & Then
            mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", expectedUrl));

            System.out.println("Location 헤더 일치 확인");
        }
    }

    // -------------------------------------------------------------------------
    // GET /{shortCode} - 예외 처리
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /{shortCode} - 비즈니스 예외 전파")
    class RedirectException {

        @Test
        @DisplayName("존재하지 않는 shortCode - 404 + 에러코드 2001 반환")
        void 존재하지않는shortCode_404_반환() throws Exception {
            // Given
            String shortCode = "ZZZZZZZ";

            when(queryBus.execute(any(RedirectQuery.class)))
                .thenThrow(new BusinessException(ErrorCode.URL_NOT_FOUND));

            System.out.println("=== 존재하지 않는 URL 리다이렉트 ===");
            System.out.println("shortCode: " + shortCode);
            System.out.println("기대 상태: 404, 기대 코드: 2001");

            // When & Then
            mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("2001"))
                .andExpect(jsonPath("$.message").exists());

            System.out.println("404 Not Found + 코드 2001 확인");

            verify(queryBus).execute(any(RedirectQuery.class));
        }

        @Test
        @DisplayName("잘못된 Base62 인코딩 shortCode - 400 + GlobalExceptionHandler 처리")
        void 잘못된Base62_shortCode_400반환() throws Exception {
            // Given
            String invalidShortCode = "!!!";

            when(queryBus.execute(any(RedirectQuery.class)))
                .thenThrow(new BusinessException(ErrorCode.DECODE_INVALID_CHARACTER));

            System.out.println("=== 잘못된 shortCode 리다이렉트 ===");
            System.out.println("invalidShortCode: " + invalidShortCode);

            // When & Then
            mockMvc.perform(get("/" + invalidShortCode))
                .andExpect(status().isBadRequest());

            System.out.println("400 Bad Request 확인");
        }
    }

    // -------------------------------------------------------------------------
    // QueryBus 호출 검증
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("QueryBus 호출 검증")
    class QueryBusInteraction {

        @Test
        @DisplayName("단일 요청 - QueryBus.execute가 정확히 1번 호출됨")
        void 단일요청_QueryBus_1번_호출() throws Exception {
            // Given
            String shortCode = "abc1234";

            when(queryBus.execute(any(RedirectQuery.class)))
                .thenReturn(RedirectResult.of("https://www.example.com"));

            System.out.println("=== QueryBus.execute 호출 횟수 검증 ===");

            // When
            mockMvc.perform(get("/" + shortCode));

            // Then
            verify(queryBus, times(1)).execute(any(RedirectQuery.class));

            System.out.println("QueryBus.execute 1번 호출 확인");
        }

        @Test
        @DisplayName("3번 요청 - QueryBus.execute가 3번 호출됨")
        void 세번요청_QueryBus_3번_호출() throws Exception {
            // Given
            when(queryBus.execute(any(RedirectQuery.class)))
                .thenReturn(RedirectResult.of("https://www.example.com"));

            System.out.println("=== QueryBus.execute 3번 호출 검증 ===");

            // When
            for (int i = 0; i < 3; i++) {
                mockMvc.perform(get("/abc123" + i));
            }

            // Then
            verify(queryBus, times(3)).execute(any(RedirectQuery.class));

            System.out.println("QueryBus.execute 3번 호출 확인");
        }
    }
}
