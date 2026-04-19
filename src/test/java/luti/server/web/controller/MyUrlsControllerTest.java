package luti.server.web.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import luti.server.application.bus.CommandBus;
import luti.server.application.bus.QueryBus;
import luti.server.application.command.ClaimUrlCommand;
import luti.server.application.command.DeleteUrlCommand;
import luti.server.application.command.DescriptionCommand;
import luti.server.application.query.MyUrlsQuery;
import luti.server.application.query.UrlAnalyticsQuery;
import luti.server.application.query.VerifyUrlQuery;
import luti.server.application.result.MyUrlsListResult;
import luti.server.application.result.UrlAnalyticsResult;
import luti.server.application.result.UrlVerifyResult;
import luti.server.domain.enums.VerifyUrlStatus;
import luti.server.domain.service.dto.UrlMappingInfo;
import luti.server.exception.BusinessException;
import luti.server.exception.ErrorCode;
import luti.server.exception.GlobalExceptionHandler;
import luti.server.web.resolver.CommandArgumentResolver;
import luti.server.web.resolver.QueryArgumentResolver;

@WebMvcTest(
    controllers = MyUrlsController.class,
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
		org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration.class,
		org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration.class
    }
)
@AutoConfigureMockMvc(addFilters = false)
@Import({CommandArgumentResolver.class, QueryArgumentResolver.class, GlobalExceptionHandler.class})
@DisplayName("MyUrlsController - MockMvc 슬라이스 테스트")
class MyUrlsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockitoBean
    private CommandBus commandBus;

    @MockitoBean
    private QueryBus queryBus;

    @BeforeEach
    void setUp() {
        reset(commandBus, queryBus);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/my-urls/verify
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/my-urls/verify - URL 검증")
    class VerifyUrl {

        @Test
        @DisplayName("유효한 URL - valid=true 응답 반환")
        void 유효한URL_valid_true() throws Exception {
            // Given
            String shortUrl = "lill.ing/abc1234";
            Long memberId = 1L;

            UrlMappingInfo urlMappingInfo = buildUrlMappingInfo();
            UrlVerifyResult verifyResult = UrlVerifyResult.ok(urlMappingInfo);

            when(queryBus.execute(any(VerifyUrlQuery.class))).thenReturn(verifyResult);

            System.out.println("=== URL 검증 - 유효한 URL ===");
            System.out.println("shortUrl: " + shortUrl);

            // When & Then
            mockMvc.perform(get("/api/v1/my-urls/verify")
                    .param("shortUrl", shortUrl)
                    .with(user(memberId.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.originalUrl").exists())
                .andExpect(jsonPath("$.shortUrl").exists());

            System.out.println("valid=true 응답 확인");

            verify(queryBus).execute(any(VerifyUrlQuery.class));
        }

        @Test
        @DisplayName("소유자 있는 URL - valid=false 응답 반환")
        void 소유자있는URL_valid_false() throws Exception {
            // Given
            String shortUrl = "lill.ing/already1";
            Long memberId = 1L;

            UrlVerifyResult verifyResult = UrlVerifyResult.alreadyOwned();

            when(queryBus.execute(any(VerifyUrlQuery.class))).thenReturn(verifyResult);

            System.out.println("=== URL 검증 - 이미 소유자 있음 ===");

            // When & Then
            mockMvc.perform(get("/api/v1/my-urls/verify")
                    .param("shortUrl", shortUrl)
                    .with(user(memberId.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));

            System.out.println("valid=false 응답 확인 (소유자 있음)");
        }

        @Test
        @DisplayName("존재하지 않는 URL - valid=false 응답 반환")
        void 존재하지않는URL_valid_false() throws Exception {
            // Given
            String shortUrl = "lill.ing/ZZZZZZZ";
            Long memberId = 1L;

            UrlVerifyResult verifyResult = UrlVerifyResult.notFound();

            when(queryBus.execute(any(VerifyUrlQuery.class))).thenReturn(verifyResult);

            System.out.println("=== URL 검증 - 존재하지 않는 URL ===");

            // When & Then
            mockMvc.perform(get("/api/v1/my-urls/verify")
                    .param("shortUrl", shortUrl)
                    .with(user(memberId.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));

            System.out.println("valid=false 응답 확인 (존재하지 않음)");
        }

        @Test
        @DisplayName("잘못된 형식 URL - valid=false 응답 반환")
        void 잘못된형식URL_valid_false() throws Exception {
            // Given
            String shortUrl = "invalid-format";
            Long memberId = 1L;

            UrlVerifyResult verifyResult = UrlVerifyResult.invalidFormat();

            when(queryBus.execute(any(VerifyUrlQuery.class))).thenReturn(verifyResult);

            System.out.println("=== URL 검증 - 잘못된 형식 ===");

            // When & Then
            mockMvc.perform(get("/api/v1/my-urls/verify")
                    .param("shortUrl", shortUrl)
                    .with(user(memberId.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));

            System.out.println("valid=false 응답 확인 (잘못된 형식)");
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/my-urls/claim
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/my-urls/claim - URL 클레임")
    class ClaimUrl {

        @Test
        @DisplayName("정상 클레임 - 204 No Content 반환")
        void 정상클레임_204반환() throws Exception {
            // Given
            Long memberId = 1L;
            String shortUrl = "lill.ing/abc1234";
            String requestBody = "{\"shortUrl\":\"" + shortUrl + "\"}";

            when(commandBus.execute(any(ClaimUrlCommand.class))).thenReturn(null);

            System.out.println("=== URL 클레임 성공 ===");
            System.out.println("memberId: " + memberId);
            System.out.println("shortUrl: " + shortUrl);

            // When & Then
            mockMvc.perform(post("/api/v1/my-urls/claim")
                    .with(user(memberId.toString()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isNoContent());

            System.out.println("204 No Content 확인");

            verify(commandBus).execute(any(ClaimUrlCommand.class));
        }

        @Test
        @DisplayName("INVALID_SHORT_URL_FORMAT - 400 + 에러코드 6001 반환")
        void INVALID_SHORT_URL_FORMAT_400반환() throws Exception {
            // Given
            Long memberId = 1L;
            String requestBody = "{\"shortUrl\":\"invalid-url\"}";

            when(commandBus.execute(any(ClaimUrlCommand.class)))
                .thenThrow(new BusinessException(ErrorCode.INVALID_SHORT_URL_FORMAT));

            System.out.println("=== URL 클레임 실패 - 잘못된 URL 형식 ===");

            // When & Then
            mockMvc.perform(post("/api/v1/my-urls/claim")
                    .with(user(memberId.toString()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("6001"));

            System.out.println("400 Bad Request + 코드 6001 확인");
        }

        @Test
        @DisplayName("ALREADY_OWNED_URL - 409 + 에러코드 6003 반환")
        void ALREADY_OWNED_URL_409반환() throws Exception {
            // Given
            Long memberId = 1L;
            String requestBody = "{\"shortUrl\":\"lill.ing/abc1234\"}";

            when(commandBus.execute(any(ClaimUrlCommand.class)))
                .thenThrow(new BusinessException(ErrorCode.ALREADY_OWNED_URL));

            System.out.println("=== URL 클레임 실패 - 이미 소유된 URL ===");

            // When & Then
            mockMvc.perform(post("/api/v1/my-urls/claim")
                    .with(user(memberId.toString()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("6003"));

            System.out.println("409 Conflict + 코드 6003 확인");
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/my-urls/list
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/my-urls/list - URL 목록 조회")
    class GetMyUrls {

        @Test
        @DisplayName("목록 조회 성공 - 페이지네이션 응답 구조 검증")
        void 목록조회_성공_페이지네이션응답() throws Exception {
            // Given
            Long memberId = 1L;

            MyUrlsListResult mockResult = buildMyUrlsListResult(0);
            when(queryBus.execute(any(MyUrlsQuery.class))).thenReturn(mockResult);

            System.out.println("=== URL 목록 조회 성공 ===");

            // When & Then
            mockMvc.perform(get("/api/v1/my-urls/list")
                    .param("page", "0")
                    .param("size", "10")
                    .with(user(memberId.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urls").isArray())
                .andExpect(jsonPath("$.totalElements").exists())
                .andExpect(jsonPath("$.totalPages").exists())
                .andExpect(jsonPath("$.currentPage").exists())
                .andExpect(jsonPath("$.pageSize").exists());

            System.out.println("목록 조회 응답 구조 확인");

            verify(queryBus).execute(any(MyUrlsQuery.class));
        }

        @Test
        @DisplayName("빈 목록 조회 - 빈 배열과 0 카운트 응답")
        void 빈목록_조회() throws Exception {
            // Given
            Long memberId = 1L;

            MyUrlsListResult emptyResult = buildMyUrlsListResult(0);
            when(queryBus.execute(any(MyUrlsQuery.class))).thenReturn(emptyResult);

            System.out.println("=== 빈 URL 목록 조회 ===");

            // When & Then
            mockMvc.perform(get("/api/v1/my-urls/list")
                    .param("page", "0")
                    .param("size", "10")
                    .with(user(memberId.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urls").isArray())
                .andExpect(jsonPath("$.urls").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));

            System.out.println("빈 목록 응답 확인");
        }

        @Test
        @DisplayName("tagIds 파라미터 포함 요청 - QueryBus에 전달됨")
        void tagIds_파라미터_포함_요청() throws Exception {
            // Given
            Long memberId = 1L;

            MyUrlsListResult mockResult = buildMyUrlsListResult(0);
            when(queryBus.execute(any(MyUrlsQuery.class))).thenReturn(mockResult);

            System.out.println("=== tagIds 파라미터 포함 요청 ===");

            // When & Then
            mockMvc.perform(get("/api/v1/my-urls/list")
                    .param("page", "0")
                    .param("size", "10")
                    .param("tagIds", "1", "2", "3")
                    .param("filterMode", "and")
                    .with(user(memberId.toString())))
                .andExpect(status().isOk());

            System.out.println("tagIds 파라미터 요청 처리 확인");

            verify(queryBus).execute(any(MyUrlsQuery.class));
        }
    }

    // -------------------------------------------------------------------------
    // PATCH /api/v1/my-urls/description
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("PATCH /api/v1/my-urls/description - 설명 수정")
    class UpdateDescription {

        @Test
        @DisplayName("설명 수정 성공 - 204 No Content 반환")
        void 설명수정_성공_204반환() throws Exception {
            // Given
            Long memberId = 1L;
            String requestBody = "{\"urlId\":1,\"description\":\"새로운 설명\"}";

            when(commandBus.execute(any(DescriptionCommand.class))).thenReturn(null);

            System.out.println("=== URL 설명 수정 성공 ===");

            // When & Then
            mockMvc.perform(patch("/api/v1/my-urls/description")
                    .with(user(memberId.toString()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isNoContent());

            System.out.println("204 No Content 확인");

            verify(commandBus).execute(any(DescriptionCommand.class));
        }

        @Test
        @DisplayName("존재하지 않는 URL 설명 수정 - 404 반환")
        void 존재하지않는URL_설명수정_404반환() throws Exception {
            // Given
            Long memberId = 1L;
            String requestBody = "{\"urlId\":99999,\"description\":\"설명\"}";

            when(commandBus.execute(any(DescriptionCommand.class)))
                .thenThrow(new BusinessException(ErrorCode.SHORT_URL_NOT_FOUND));

            System.out.println("=== URL 설명 수정 실패 - 존재하지 않는 URL ===");

            // When & Then
            mockMvc.perform(patch("/api/v1/my-urls/description")
                    .with(user(memberId.toString()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("6002"));

            System.out.println("404 Not Found + 코드 6002 확인");
        }

        @Test
        @DisplayName("소유자가 아닌 설명 수정 - 403 반환")
        void 소유자아닌_설명수정_403반환() throws Exception {
            // Given
            Long memberId = 2L;
            String requestBody = "{\"urlId\":1,\"description\":\"설명\"}";

            when(commandBus.execute(any(DescriptionCommand.class)))
                .thenThrow(new BusinessException(ErrorCode.NOT_URL_OWNER));

            System.out.println("=== URL 설명 수정 실패 - 소유자 아님 ===");

            // When & Then
            mockMvc.perform(patch("/api/v1/my-urls/description")
                    .with(user(memberId.toString()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("6004"));

            System.out.println("403 Forbidden + 코드 6004 확인");
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/my-urls/{urlId}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("DELETE /api/v1/my-urls/{urlId} - URL 삭제")
    class DeleteUrl {

        @Test
        @DisplayName("URL 삭제 성공 - 204 No Content 반환")
        void URL삭제_성공_204반환() throws Exception {
            // Given
            Long memberId = 1L;
            Long urlId = 100L;

            when(commandBus.execute(any(DeleteUrlCommand.class))).thenReturn(null);

            System.out.println("=== URL 삭제 성공 ===");
            System.out.println("memberId: " + memberId + ", urlId: " + urlId);

            // When & Then
            // CommandArgumentResolver는 body stream을 읽으므로, DELETE 요청에 빈 body를 전달한다.
            mockMvc.perform(delete("/api/v1/my-urls/" + urlId)
                    .with(user(memberId.toString()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isNoContent());

            System.out.println("204 No Content 확인");

            verify(commandBus).execute(any(DeleteUrlCommand.class));
        }

        @Test
        @DisplayName("존재하지 않는 URL 삭제 - 404 반환")
        void 존재하지않는URL_삭제_404반환() throws Exception {
            // Given
            Long memberId = 1L;
            Long urlId = 999999L;

            when(commandBus.execute(any(DeleteUrlCommand.class)))
                .thenThrow(new BusinessException(ErrorCode.SHORT_URL_NOT_FOUND));

            System.out.println("=== URL 삭제 실패 - 존재하지 않는 URL ===");

            // When & Then
            mockMvc.perform(delete("/api/v1/my-urls/" + urlId)
                    .with(user(memberId.toString()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("6002"));

            System.out.println("404 Not Found + 코드 6002 확인");
        }

        @Test
        @DisplayName("소유자가 아닌 사용자 URL 삭제 - 403 반환")
        void 소유자아닌_URL삭제_403반환() throws Exception {
            // Given
            Long memberId = 2L;
            Long urlId = 100L;

            when(commandBus.execute(any(DeleteUrlCommand.class)))
                .thenThrow(new BusinessException(ErrorCode.NOT_URL_OWNER));

            System.out.println("=== URL 삭제 실패 - 소유자 아님 ===");

            // When & Then
            mockMvc.perform(delete("/api/v1/my-urls/" + urlId)
                    .with(user(memberId.toString()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("6004"));

            System.out.println("403 Forbidden + 코드 6004 확인");
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/my-urls/{urlId}/analytics
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/my-urls/{urlId}/analytics - URL 통계 조회")
    class GetUrlAnalytics {

        @Test
        @DisplayName("통계 조회 성공 - 응답 구조 검증")
        void 통계조회_성공_응답구조() throws Exception {
            // Given
            Long memberId = 1L;
            Long urlId = 100L;

            UrlAnalyticsResult mockResult = buildUrlAnalyticsResult();
            when(queryBus.execute(any(UrlAnalyticsQuery.class))).thenReturn(mockResult);

            System.out.println("=== URL 통계 조회 성공 ===");
            System.out.println("urlId: " + urlId);

            // When & Then
            mockMvc.perform(get("/api/v1/my-urls/" + urlId + "/analytics")
                    .with(user(memberId.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hourly").exists())
                .andExpect(jsonPath("$.daily").exists())
                .andExpect(jsonPath("$.weekly").exists())
                .andExpect(jsonPath("$.monthly").exists());

            System.out.println("통계 조회 응답 구조 확인");

            verify(queryBus).execute(any(UrlAnalyticsQuery.class));
        }

        @Test
        @DisplayName("존재하지 않는 URL 통계 - 404 반환")
        void 존재하지않는URL_통계_404반환() throws Exception {
            // Given
            Long memberId = 1L;
            Long urlId = 999999L;

            when(queryBus.execute(any(UrlAnalyticsQuery.class)))
                .thenThrow(new BusinessException(ErrorCode.SHORT_URL_NOT_FOUND));

            System.out.println("=== URL 통계 조회 실패 - 존재하지 않는 URL ===");

            // When & Then
            mockMvc.perform(get("/api/v1/my-urls/" + urlId + "/analytics")
                    .with(user(memberId.toString())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("6002"));

            System.out.println("404 Not Found + 코드 6002 확인");
        }

        @Test
        @DisplayName("소유자가 아닌 URL 통계 조회 - 403 반환")
        void 소유자아닌_URL통계_403반환() throws Exception {
            // Given
            Long memberId = 2L;
            Long urlId = 100L;

            when(queryBus.execute(any(UrlAnalyticsQuery.class)))
                .thenThrow(new BusinessException(ErrorCode.NOT_URL_OWNER));

            System.out.println("=== URL 통계 조회 실패 - 소유자 아님 ===");

            // When & Then
            mockMvc.perform(get("/api/v1/my-urls/" + urlId + "/analytics")
                    .with(user(memberId.toString())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("6004"));

            System.out.println("403 Forbidden + 코드 6004 확인");
        }
    }

    // -------------------------------------------------------------------------
    // Helper 메서드
    // -------------------------------------------------------------------------

    private UrlMappingInfo buildUrlMappingInfo() {
        UrlMappingInfo urlMappingInfo = mock(UrlMappingInfo.class);
        when(urlMappingInfo.getId()).thenReturn(1L);
        when(urlMappingInfo.getOriginalUrl()).thenReturn("https://www.example.com");
        when(urlMappingInfo.getShortUrl()).thenReturn("lill.ing/abc1234");
        when(urlMappingInfo.getClickCount()).thenReturn(10L);
        when(urlMappingInfo.getCreatedAt()).thenReturn(LocalDateTime.now());
        return urlMappingInfo;
    }

    private MyUrlsListResult buildMyUrlsListResult(int urlCount) {
        MyUrlsListResult result = mock(MyUrlsListResult.class);
        when(result.getUrls()).thenReturn(List.of());
        when(result.getTotalElements()).thenReturn((long) urlCount);
        when(result.getTotalPages()).thenReturn(0L);
        when(result.getCurrentPage()).thenReturn(0L);
        when(result.getPageSize()).thenReturn(10L);
        return result;
    }

    private UrlAnalyticsResult buildUrlAnalyticsResult() {
        UrlAnalyticsResult result = mock(UrlAnalyticsResult.class);
        when(result.getHourly()).thenReturn(UrlAnalyticsResult.TimeSeriesResult.of("24h", List.of()));
        when(result.getDaily()).thenReturn(UrlAnalyticsResult.TimeSeriesResult.of("30d", List.of()));
        when(result.getWeekly()).thenReturn(UrlAnalyticsResult.TimeSeriesResult.of("12w", List.of()));
        when(result.getMonthly()).thenReturn(UrlAnalyticsResult.TimeSeriesResult.of("12m", List.of()));
        return result;
    }
}
