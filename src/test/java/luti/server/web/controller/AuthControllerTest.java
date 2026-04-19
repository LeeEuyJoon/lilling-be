package luti.server.web.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import luti.server.exception.GlobalExceptionHandler;

/**
 * AuthController 슬라이스 테스트.
 *
 * AuthController는 Spring Security의 Authentication 파라미터를 직접 받는다.
 * @TestConfiguration으로 간단한 SecurityFilterChain을 등록하여
 * SecurityMockMvcRequestPostProcessors로 Authentication을 주입한다.
 */
@WebMvcTest(
    controllers = AuthController.class,
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration.class
    }
)
@Import({GlobalExceptionHandler.class, AuthControllerTest.TestSecurityConfig.class})
@DisplayName("AuthController - MockMvc 슬라이스 테스트")
class AuthControllerTest {

    @org.springframework.boot.test.context.TestConfiguration
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private SecurityContext buildSecurityContext(String username) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            username,
            null,
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        return context;
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/auth/me - 인증 상태 확인
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/auth/me - 인증 상태 확인")
    class IsAuthenticated {

        @Test
        @DisplayName("인증된 사용자 - isAuthenticated=true + memberId 반환")
        void 인증된사용자_true_memberId반환() throws Exception {
            // Given
            SecurityContext securityContext = buildSecurityContext("42");

            System.out.println("=== 인증된 사용자 인증 확인 ===");
            System.out.println("memberId: 42");

            // When & Then
            mockMvc.perform(get("/api/v1/auth/me")
                    .with(securityContext(securityContext)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isAuthenticated").value(true))
                .andExpect(jsonPath("$.memberId").value(42));

            System.out.println("isAuthenticated=true, memberId=42 확인");
        }

        @Test
        @DisplayName("비인증 사용자 - isAuthenticated=false + memberId null")
        void 비인증사용자_false_memberId_null() throws Exception {
            // Given: SecurityContext 주입 없음 (인증 없음)
            System.out.println("=== 비인증 사용자 인증 확인 ===");

            // When & Then
            mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isAuthenticated").value(false))
                .andExpect(jsonPath("$.memberId").isEmpty());

            System.out.println("isAuthenticated=false, memberId=null 확인");
        }

        @Test
        @DisplayName("응답 JSON 구조 검증 - isAuthenticated, memberId 필드 존재")
        void 응답JSON_구조_검증() throws Exception {
            // Given
            SecurityContext securityContext = buildSecurityContext("99");

            System.out.println("=== 응답 JSON 구조 검증 ===");

            // When & Then
            mockMvc.perform(get("/api/v1/auth/me")
                    .with(securityContext(securityContext)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isAuthenticated").exists())
                .andExpect(jsonPath("$.isAuthenticated").value(true))
                .andExpect(jsonPath("$.memberId").value(99));

            System.out.println("응답 JSON 구조 확인 완료");
        }

        @Test
        @DisplayName("숫자가 아닌 사용자명 - memberId가 null로 반환됨")
        void 숫자아닌_사용자명_memberId_null() throws Exception {
            // Given: 숫자가 아닌 사용자명 (AuthExtractor가 null 반환)
            SecurityContext securityContext = buildSecurityContext("not-a-number");

            System.out.println("=== 숫자 아닌 사용자명 테스트 ===");

            // When & Then: isAuthenticated=true이지만 memberId 파싱 실패로 null 반환
            mockMvc.perform(get("/api/v1/auth/me")
                    .with(securityContext(securityContext)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isAuthenticated").value(true))
                .andExpect(jsonPath("$.memberId").isEmpty());

            System.out.println("memberId=null 확인 (숫자가 아닌 사용자명)");
        }

        @Test
        @DisplayName("memberId=1 - 응답에 정확한 memberId 포함")
        void memberId_1_응답확인() throws Exception {
            // Given
            SecurityContext securityContext = buildSecurityContext("1");

            System.out.println("=== memberId=1 응답 검증 ===");

            // When & Then
            mockMvc.perform(get("/api/v1/auth/me")
                    .with(securityContext(securityContext)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isAuthenticated").value(true))
                .andExpect(jsonPath("$.memberId").value(1));

            System.out.println("memberId=1 응답 확인");
        }
    }
}
