package luti.server.web.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
import luti.server.application.command.AssignTagsCommand;
import luti.server.application.command.CreateTagCommand;
import luti.server.application.command.DeleteTagCommand;
import luti.server.application.command.UnassignTagsCommand;
import luti.server.application.command.UpdateTagCommand;
import luti.server.application.query.TagListQuery;
import luti.server.application.result.CreateTagResult;
import luti.server.application.result.TagListResult;
import luti.server.domain.service.dto.TagInfo;
import luti.server.exception.BusinessException;
import luti.server.exception.ErrorCode;
import luti.server.exception.GlobalExceptionHandler;
import luti.server.web.resolver.CommandArgumentResolver;
import luti.server.web.resolver.QueryArgumentResolver;

@WebMvcTest(
    controllers = TagController.class,
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
@DisplayName("TagController - MockMvc 슬라이스 테스트")
class TagControllerTest {

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
    // GET /api/v1/tags - 태그 목록 조회
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/tags - 태그 목록 조회")
    class GetTags {

        @Test
        @DisplayName("태그 목록 조회 성공 - 태그 배열 응답 반환")
        void 태그목록_조회성공() throws Exception {
            // Given
            Long memberId = 1L;

            TagListResult mockResult = buildTagListResult(List.of(
                buildTagInfo(1L, "개발"),
                buildTagInfo(2L, "스터디")
            ));
            when(queryBus.execute(any(TagListQuery.class))).thenReturn(mockResult);

            System.out.println("=== 태그 목록 조회 성공 ===");
            System.out.println("memberId: " + memberId);

            // When & Then
            mockMvc.perform(get("/api/v1/tags")
                    .with(user(memberId.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags").isArray())
                .andExpect(jsonPath("$.tags.length()").value(2))
                .andExpect(jsonPath("$.tags[0].id").value(1))
                .andExpect(jsonPath("$.tags[0].name").value("개발"))
                .andExpect(jsonPath("$.tags[1].id").value(2))
                .andExpect(jsonPath("$.tags[1].name").value("스터디"));

            System.out.println("태그 목록 조회 응답 확인: 2개");

            verify(queryBus).execute(any(TagListQuery.class));
        }

        @Test
        @DisplayName("태그 목록 조회 - 빈 배열 응답")
        void 태그목록_빈배열() throws Exception {
            // Given
            Long memberId = 1L;

            TagListResult emptyResult = buildTagListResult(List.of());
            when(queryBus.execute(any(TagListQuery.class))).thenReturn(emptyResult);

            System.out.println("=== 태그 빈 목록 조회 ===");

            // When & Then
            mockMvc.perform(get("/api/v1/tags")
                    .with(user(memberId.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags").isArray())
                .andExpect(jsonPath("$.tags").isEmpty());

            System.out.println("빈 태그 목록 확인");
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/tags - 태그 생성
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/tags - 태그 생성")
    class CreateTag {

        @Test
        @DisplayName("태그 생성 성공 - 201 + 생성된 태그 정보 반환")
        void 태그생성_성공_201반환() throws Exception {
            // Given
            Long memberId = 1L;
            String requestBody = "{\"name\":\"개발\"}";

            CreateTagResult mockResult = buildCreateTagResult(1L, "개발");
            when(commandBus.execute(any(CreateTagCommand.class))).thenReturn(mockResult);

            System.out.println("=== 태그 생성 성공 ===");
            System.out.println("memberId: " + memberId);

            // When & Then
            mockMvc.perform(post("/api/v1/tags")
                    .with(user(memberId.toString()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("개발"));

            System.out.println("201 Created + 태그 정보 확인");

            verify(commandBus).execute(any(CreateTagCommand.class));
        }

        @Test
        @DisplayName("중복 태그 이름 - 409 + 에러코드 8002 반환")
        void 중복태그이름_409반환() throws Exception {
            // Given
            Long memberId = 1L;
            String requestBody = "{\"name\":\"중복태그\"}";

            when(commandBus.execute(any(CreateTagCommand.class)))
                .thenThrow(new BusinessException(ErrorCode.DUPLICATE_TAG_NAME));

            System.out.println("=== 태그 생성 실패 - 중복 이름 ===");

            // When & Then
            mockMvc.perform(post("/api/v1/tags")
                    .with(user(memberId.toString()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("8002"));

            System.out.println("409 Conflict + 코드 8002 확인");
        }

        @Test
        @DisplayName("태그 개수 초과 - 400 + 에러코드 8004 반환")
        void 태그개수초과_400반환() throws Exception {
            // Given
            Long memberId = 1L;
            String requestBody = "{\"name\":\"태그51\"}";

            when(commandBus.execute(any(CreateTagCommand.class)))
                .thenThrow(new BusinessException(ErrorCode.TAG_LIMIT_EXCEEDED));

            System.out.println("=== 태그 생성 실패 - 50개 초과 ===");

            // When & Then
            mockMvc.perform(post("/api/v1/tags")
                    .with(user(memberId.toString()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("8004"));

            System.out.println("400 Bad Request + 코드 8004 확인");
        }
    }

    // -------------------------------------------------------------------------
    // PATCH /api/v1/tags/{tagId} - 태그 이름 변경
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("PATCH /api/v1/tags/{tagId} - 태그 이름 변경")
    class UpdateTag {

        @Test
        @DisplayName("태그 이름 변경 성공 - 204 No Content 반환")
        void 태그이름변경_성공_204반환() throws Exception {
            // Given
            Long memberId = 1L;
            Long tagId = 10L;
            String requestBody = "{\"name\":\"변경된이름\"}";

            when(commandBus.execute(any(UpdateTagCommand.class))).thenReturn(null);

            System.out.println("=== 태그 이름 변경 성공 ===");
            System.out.println("tagId: " + tagId);

            // When & Then
            mockMvc.perform(patch("/api/v1/tags/" + tagId)
                    .with(user(memberId.toString()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isNoContent());

            System.out.println("204 No Content 확인");

            verify(commandBus).execute(any(UpdateTagCommand.class));
        }

        @Test
        @DisplayName("태그 소유자 아님 - 403 + 에러코드 8003 반환")
        void 태그소유자아님_403반환() throws Exception {
            // Given
            Long memberId = 2L;
            Long tagId = 10L;
            String requestBody = "{\"name\":\"변경시도\"}";

            when(commandBus.execute(any(UpdateTagCommand.class)))
                .thenThrow(new BusinessException(ErrorCode.NOT_TAG_OWNER));

            System.out.println("=== 태그 이름 변경 실패 - 소유자 아님 ===");

            // When & Then
            mockMvc.perform(patch("/api/v1/tags/" + tagId)
                    .with(user(memberId.toString()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("8003"));

            System.out.println("403 Forbidden + 코드 8003 확인");
        }

        @Test
        @DisplayName("존재하지 않는 태그 - 404 + 에러코드 8001 반환")
        void 존재하지않는태그_404반환() throws Exception {
            // Given
            Long memberId = 1L;
            Long tagId = 99999L;
            String requestBody = "{\"name\":\"변경시도\"}";

            when(commandBus.execute(any(UpdateTagCommand.class)))
                .thenThrow(new BusinessException(ErrorCode.TAG_NOT_FOUND));

            System.out.println("=== 태그 이름 변경 실패 - 존재하지 않음 ===");

            // When & Then
            mockMvc.perform(patch("/api/v1/tags/" + tagId)
                    .with(user(memberId.toString()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("8001"));

            System.out.println("404 Not Found + 코드 8001 확인");
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/tags/{tagId} - 태그 삭제
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("DELETE /api/v1/tags/{tagId} - 태그 삭제")
    class DeleteTag {

        @Test
        @DisplayName("태그 삭제 성공 - 204 No Content 반환")
        void 태그삭제_성공_204반환() throws Exception {
            // Given
            Long memberId = 1L;
            Long tagId = 10L;

            when(commandBus.execute(any(DeleteTagCommand.class))).thenReturn(null);

            System.out.println("=== 태그 삭제 성공 ===");
            System.out.println("tagId: " + tagId);

            // When & Then
            // CommandArgumentResolver는 body stream을 읽으므로, DELETE 요청에 빈 body를 전달한다.
            mockMvc.perform(delete("/api/v1/tags/" + tagId)
                    .with(user(memberId.toString()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isNoContent());

            System.out.println("204 No Content 확인");

            verify(commandBus).execute(any(DeleteTagCommand.class));
        }

        @Test
        @DisplayName("태그 소유자 아님 - 403 + 에러코드 8003 반환")
        void 태그소유자아님_403반환() throws Exception {
            // Given
            Long memberId = 2L;
            Long tagId = 10L;

            when(commandBus.execute(any(DeleteTagCommand.class)))
                .thenThrow(new BusinessException(ErrorCode.NOT_TAG_OWNER));

            System.out.println("=== 태그 삭제 실패 - 소유자 아님 ===");

            // When & Then
            mockMvc.perform(delete("/api/v1/tags/" + tagId)
                    .with(user(memberId.toString()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("8003"));

            System.out.println("403 Forbidden + 코드 8003 확인");
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/tags/assign - 태그 할당
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/tags/assign - 태그 할당")
    class AssignTags {

        @Test
        @DisplayName("태그 할당 성공 - 204 No Content 반환")
        void 태그할당_성공_204반환() throws Exception {
            // Given
            Long memberId = 1L;
            String requestBody = "{\"urlId\":1,\"tagIds\":[1,2]}";

            when(commandBus.execute(any(AssignTagsCommand.class))).thenReturn(null);

            System.out.println("=== 태그 할당 성공 ===");

            // When & Then
            mockMvc.perform(post("/api/v1/tags/assign")
                    .with(user(memberId.toString()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isNoContent());

            System.out.println("204 No Content 확인");

            verify(commandBus).execute(any(AssignTagsCommand.class));
        }

        @Test
        @DisplayName("URL 소유자 아님 태그 할당 - 403 + 에러코드 6004 반환")
        void URL소유자아님_태그할당_403반환() throws Exception {
            // Given
            Long memberId = 2L;
            String requestBody = "{\"urlId\":1,\"tagIds\":[1]}";

            when(commandBus.execute(any(AssignTagsCommand.class)))
                .thenThrow(new BusinessException(ErrorCode.NOT_URL_OWNER));

            System.out.println("=== 태그 할당 실패 - URL 소유자 아님 ===");

            // When & Then
            mockMvc.perform(post("/api/v1/tags/assign")
                    .with(user(memberId.toString()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("6004"));

            System.out.println("403 Forbidden + 코드 6004 확인");
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/tags/unassign - 태그 해제
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/tags/unassign - 태그 해제")
    class UnassignTags {

        @Test
        @DisplayName("태그 해제 성공 - 204 No Content 반환")
        void 태그해제_성공_204반환() throws Exception {
            // Given
            Long memberId = 1L;
            String requestBody = "{\"urlId\":1,\"tagIds\":[1,2]}";

            when(commandBus.execute(any(UnassignTagsCommand.class))).thenReturn(null);

            System.out.println("=== 태그 해제 성공 ===");

            // When & Then
            mockMvc.perform(post("/api/v1/tags/unassign")
                    .with(user(memberId.toString()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isNoContent());

            System.out.println("204 No Content 확인");

            verify(commandBus).execute(any(UnassignTagsCommand.class));
        }

        @Test
        @DisplayName("URL 소유자 아님 태그 해제 - 403 + 에러코드 6004 반환")
        void URL소유자아님_태그해제_403반환() throws Exception {
            // Given
            Long memberId = 2L;
            String requestBody = "{\"urlId\":1,\"tagIds\":[1]}";

            when(commandBus.execute(any(UnassignTagsCommand.class)))
                .thenThrow(new BusinessException(ErrorCode.NOT_URL_OWNER));

            System.out.println("=== 태그 해제 실패 - URL 소유자 아님 ===");

            // When & Then
            mockMvc.perform(post("/api/v1/tags/unassign")
                    .with(user(memberId.toString()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("6004"));

            System.out.println("403 Forbidden + 코드 6004 확인");
        }
    }

    // -------------------------------------------------------------------------
    // CommandBus 호출 검증
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("CommandBus/QueryBus 호출 검증")
    class BusInteraction {

        @Test
        @DisplayName("태그 생성 요청 - CommandBus.execute가 정확히 1번 호출됨")
        void 태그생성_CommandBus_1번_호출() throws Exception {
            // Given
            Long memberId = 1L;
            String requestBody = "{\"name\":\"태그\"}";

            CreateTagResult mockResult = buildCreateTagResult(1L, "태그");
            when(commandBus.execute(any(CreateTagCommand.class))).thenReturn(mockResult);

            System.out.println("=== CommandBus.execute 호출 횟수 검증 ===");

            // When
            mockMvc.perform(post("/api/v1/tags")
                .with(user(memberId.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

            // Then
            verify(commandBus, times(1)).execute(any(CreateTagCommand.class));

            System.out.println("CommandBus.execute 1번 호출 확인");
        }

        @Test
        @DisplayName("태그 목록 조회 - QueryBus.execute가 정확히 1번 호출됨")
        void 태그목록조회_QueryBus_1번_호출() throws Exception {
            // Given
            Long memberId = 1L;

            TagListResult emptyResult = buildTagListResult(List.of());
            when(queryBus.execute(any(TagListQuery.class))).thenReturn(emptyResult);

            System.out.println("=== QueryBus.execute 호출 횟수 검증 ===");

            // When
            mockMvc.perform(get("/api/v1/tags")
                .with(user(memberId.toString())));

            // Then
            verify(queryBus, times(1)).execute(any(TagListQuery.class));

            System.out.println("QueryBus.execute 1번 호출 확인");
        }
    }

    // -------------------------------------------------------------------------
    // Helper 메서드
    // -------------------------------------------------------------------------

    private TagInfo buildTagInfo(Long id, String name) {
        TagInfo tagInfo = mock(TagInfo.class);
        when(tagInfo.getId()).thenReturn(id);
        when(tagInfo.getName()).thenReturn(name);
        return tagInfo;
    }

    private TagListResult buildTagListResult(List<TagInfo> tagInfos) {
        return TagListResult.from(tagInfos);
    }

    private CreateTagResult buildCreateTagResult(Long id, String name) {
        TagInfo tagInfo = buildTagInfo(id, name);
        return CreateTagResult.from(tagInfo);
    }
}
