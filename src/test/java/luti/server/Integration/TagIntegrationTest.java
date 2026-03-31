package luti.server.Integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import luti.server.domain.enums.Provider;
import luti.server.domain.model.Member;
import luti.server.domain.model.UrlMapping;
import luti.server.infrastructure.client.kgs.KeyBlock;
import luti.server.infrastructure.client.kgs.KgsClient;
import luti.server.infrastructure.persistence.MemberRepository;
import luti.server.infrastructure.persistence.TagRepository;
import luti.server.infrastructure.persistence.UrlMappingRepository;
import luti.server.infrastructure.persistence.UrlTagRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TagIntegrationTest {

	@Container
	static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
		.withDatabaseName("testdb")
		.withUsername("test")
		.withPassword("test");

	@Container
	static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
		.withExposedPorts(6379);

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", mysql::getJdbcUrl);
		registry.add("spring.datasource.username", mysql::getUsername);
		registry.add("spring.datasource.password", mysql::getPassword);
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");

		registry.add("spring.data.redis.host", redis::getHost);
		registry.add("spring.data.redis.port", redis::getFirstMappedPort);
		registry.add("redis.counter.host", redis::getHost);
		registry.add("redis.counter.port", redis::getFirstMappedPort);

		registry.add("JWT_SECRET_KEY", () -> "test-secret-key-for-jwt-signing-at-least-32-characters-long");
		registry.add("JWT_ACCESS_TTL_SECONDS", () -> "3600");
		registry.add("JWT_ISSUER", () -> "https://test.lill.ing");
		registry.add("JWT_AUDIENCE", () -> "api.lill.ing");

		registry.add("APP_ID", () -> "test-app");
		registry.add("DOMAIN", () -> "lill.ing");
		registry.add("SCRAMBLING_CONST_XOR1", () -> "13");
		registry.add("SCRAMBLING_CONST_XOR2", () -> "7");
		registry.add("SCRAMBLING_CONST_XOR3", () -> "17");
	}

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private KgsClient kgsClient;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private UrlMappingRepository urlMappingRepository;

	@Autowired
	private TagRepository tagRepository;

	@Autowired
	private UrlTagRepository urlTagRepository;

	@Autowired
	private ObjectMapper objectMapper;

	private Member savedMember;

	@BeforeEach
	void setUp() {
		urlTagRepository.deleteAll();
		tagRepository.deleteAll();
		urlMappingRepository.deleteAll();
		memberRepository.deleteAll();

		Member member = new Member(Provider.GOOGLE, "google-tag-test", "tag-test@example.com");
		savedMember = memberRepository.save(member);

		when(kgsClient.fetchNextBlock())
			.thenReturn(new KeyBlock(26001, 27000))
			.thenReturn(new KeyBlock(27001, 28000))
			.thenReturn(new KeyBlock(28001, 29000));
	}

	// -------------------------------------------------------------------------
	// POST /api/v1/tags - 태그 생성
	// -------------------------------------------------------------------------

	@Test
	@DisplayName("태그 생성 - 정상")
	void createTag_정상() throws Exception {
		// Given
		String requestBody = "{\"name\":\"개발\"}";

		System.out.println("=== 태그 생성 정상 테스트 ===");
		System.out.println("memberId: " + savedMember.getId());

		// When & Then
		mockMvc.perform(post("/api/v1/tags")
				.with(user(savedMember.getId().toString()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").exists())
			.andExpect(jsonPath("$.name").value("개발"));

		// DB 검증
		assertEquals(1, tagRepository.count());
		assertEquals("개발", tagRepository.findAll().get(0).getName());

		System.out.println("태그 생성 성공. DB 저장 확인 완료");
	}

	@Test
	@DisplayName("태그 생성 실패 - 중복 이름")
	void createTag_중복이름_409() throws Exception {
		// Given: 태그 먼저 생성
		mockMvc.perform(post("/api/v1/tags")
			.with(user(savedMember.getId().toString()))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"중복태그\"}"));

		System.out.println("=== 태그 생성 실패 - 중복 이름 테스트 ===");

		// When & Then: 동일한 이름으로 다시 생성 시도
		mockMvc.perform(post("/api/v1/tags")
				.with(user(savedMember.getId().toString()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"중복태그\"}"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("8002"));

		assertEquals(1, tagRepository.count());

		System.out.println("중복 태그 생성 실패: 409 Conflict");
	}

	@Test
	@DisplayName("태그 생성 실패 - 50개 초과")
	void createTag_50개초과_400() throws Exception {
		// Given: 50개 태그 생성
		for (int i = 1; i <= 50; i++) {
			mockMvc.perform(post("/api/v1/tags")
				.with(user(savedMember.getId().toString()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"태그" + i + "\"}"));
		}

		assertEquals(50, tagRepository.count());
		System.out.println("=== 태그 생성 실패 - 50개 초과 테스트 ===");
		System.out.println("현재 태그 수: 50");

		// When & Then: 51번째 태그 생성 시도
		mockMvc.perform(post("/api/v1/tags")
				.with(user(savedMember.getId().toString()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"태그51\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("8004"));

		assertEquals(50, tagRepository.count());

		System.out.println("51번째 태그 생성 실패: 400 Bad Request");
	}

	@Test
	@DisplayName("태그 생성 실패 - 인증 없음")
	void createTag_인증없음_401() throws Exception {
		// When & Then
		mockMvc.perform(post("/api/v1/tags")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"태그\"}"))
			.andExpect(status().isUnauthorized());

		assertEquals(0, tagRepository.count());

		System.out.println("인증 없이 태그 생성 실패: 401 Unauthorized");
	}

	// -------------------------------------------------------------------------
	// GET /api/v1/tags - 태그 목록 조회
	// -------------------------------------------------------------------------

	@Test
	@DisplayName("태그 목록 조회 - 정상")
	void getTags_정상() throws Exception {
		// Given: 태그 3개 생성
		for (int i = 1; i <= 3; i++) {
			mockMvc.perform(post("/api/v1/tags")
				.with(user(savedMember.getId().toString()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"태그" + i + "\"}"));
		}

		System.out.println("=== 태그 목록 조회 테스트 ===");

		// When & Then
		mockMvc.perform(get("/api/v1/tags")
				.with(user(savedMember.getId().toString())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tags").isArray())
			.andExpect(jsonPath("$.tags.length()").value(3));

		System.out.println("태그 목록 조회 성공: 3개");
	}

	@Test
	@DisplayName("태그 목록 조회 - 다른 멤버의 태그는 조회되지 않음")
	void getTags_다른멤버태그_분리() throws Exception {
		// Given: 다른 멤버 생성
		Member otherMember = new Member(Provider.KAKAO, "kakao-other", "other@example.com");
		Member savedOtherMember = memberRepository.save(otherMember);

		// 현재 멤버 태그 2개
		for (int i = 1; i <= 2; i++) {
			mockMvc.perform(post("/api/v1/tags")
				.with(user(savedMember.getId().toString()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"내태그" + i + "\"}"));
		}

		// 다른 멤버 태그 3개
		for (int i = 1; i <= 3; i++) {
			mockMvc.perform(post("/api/v1/tags")
				.with(user(savedOtherMember.getId().toString()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"남의태그" + i + "\"}"));
		}

		System.out.println("=== 태그 분리 조회 테스트 ===");

		// When & Then: 현재 멤버는 2개만 조회
		mockMvc.perform(get("/api/v1/tags")
				.with(user(savedMember.getId().toString())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tags.length()").value(2));

		System.out.println("태그 분리 조회 성공: 내 태그 2개만 조회");
	}

	@Test
	@DisplayName("태그 목록 조회 - 빈 리스트")
	void getTags_빈리스트() throws Exception {
		System.out.println("=== 태그 빈 목록 조회 테스트 ===");

		mockMvc.perform(get("/api/v1/tags")
				.with(user(savedMember.getId().toString())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tags").isArray())
			.andExpect(jsonPath("$.tags").isEmpty());

		System.out.println("빈 태그 목록 조회 성공");
	}

	// -------------------------------------------------------------------------
	// PATCH /api/v1/tags/{tagId} - 태그 이름 변경
	// -------------------------------------------------------------------------

	@Test
	@DisplayName("태그 이름 변경 - 정상")
	void updateTag_정상() throws Exception {
		// Given: 태그 생성
		Long tagId = createTagAndGetId("변경전이름");
		System.out.println("=== 태그 이름 변경 정상 테스트 ===");
		System.out.println("tagId: " + tagId);

		// When & Then
		mockMvc.perform(patch("/api/v1/tags/" + tagId)
				.with(user(savedMember.getId().toString()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"변경후이름\"}"))
			.andExpect(status().isNoContent());

		// DB 검증
		assertEquals("변경후이름", tagRepository.findById(tagId).orElseThrow().getName());

		System.out.println("태그 이름 변경 성공: 변경전이름 → 변경후이름");
	}

	@Test
	@DisplayName("태그 이름 변경 실패 - 소유자가 아닌 경우")
	void updateTag_소유자아닌경우_403() throws Exception {
		// Given: 다른 멤버 생성 후 태그 생성
		Member otherMember = new Member(Provider.KAKAO, "kakao-owner", "owner@example.com");
		Member savedOwner = memberRepository.save(otherMember);

		Long tagId = createTagAndGetId(savedOwner.getId(), "남의태그");

		System.out.println("=== 태그 이름 변경 실패 - 소유자 아님 테스트 ===");
		System.out.println("tagId: " + tagId + ", 요청자: " + savedMember.getId() + ", 소유자: " + savedOwner.getId());

		// When & Then: 다른 사람이 변경 시도
		mockMvc.perform(patch("/api/v1/tags/" + tagId)
				.with(user(savedMember.getId().toString()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"변경시도\"}"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("8003"));

		// DB 검증: 이름 그대로
		assertEquals("남의태그", tagRepository.findById(tagId).orElseThrow().getName());

		System.out.println("태그 이름 변경 실패: 403 Forbidden");
	}

	// -------------------------------------------------------------------------
	// DELETE /api/v1/tags/{tagId} - 태그 삭제
	// -------------------------------------------------------------------------

	@Test
	@DisplayName("태그 삭제 - 정상")
	void deleteTag_정상() throws Exception {
		// Given: 태그 생성
		Long tagId = createTagAndGetId("삭제할태그");
		assertEquals(1, tagRepository.count());

		System.out.println("=== 태그 삭제 정상 테스트 ===");
		System.out.println("tagId: " + tagId);

		// When & Then
		mockMvc.perform(delete("/api/v1/tags/" + tagId)
				.with(user(savedMember.getId().toString())))
			.andExpect(status().isNoContent());

		// DB 검증
		assertEquals(0, tagRepository.count());

		System.out.println("태그 삭제 성공. DB에서 제거 확인");
	}

	@Test
	@DisplayName("태그 삭제 실패 - 소유자가 아닌 경우")
	void deleteTag_소유자아닌경우_403() throws Exception {
		// Given: 다른 멤버 생성 후 태그 생성
		Member otherMember = new Member(Provider.KAKAO, "kakao-del-owner", "del-owner@example.com");
		Member savedOwner = memberRepository.save(otherMember);

		Long tagId = createTagAndGetId(savedOwner.getId(), "남의태그");

		System.out.println("=== 태그 삭제 실패 - 소유자 아님 테스트 ===");

		// When & Then
		mockMvc.perform(delete("/api/v1/tags/" + tagId)
				.with(user(savedMember.getId().toString())))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("8003"));

		// DB 검증: 태그 여전히 존재
		assertEquals(1, tagRepository.count());

		System.out.println("태그 삭제 실패: 403 Forbidden");
	}

	@Test
	@DisplayName("태그 삭제 - URL에 할당된 태그 삭제 시 UrlTag도 함께 삭제")
	void deleteTag_연결된UrlTag도삭제() throws Exception {
		// Given: URL 생성 후 태그 할당
		UrlMapping urlMapping = createUrlMapping();
		Long tagId = createTagAndGetId("삭제연동태그");

		// 태그 할당
		String assignBody = String.format("{\"urlId\":%d, \"tagIds\":[%d]}", urlMapping.getId(), tagId);
		mockMvc.perform(post("/api/v1/tags/assign")
			.with(user(savedMember.getId().toString()))
			.contentType(MediaType.APPLICATION_JSON)
			.content(assignBody));

		assertEquals(1, urlTagRepository.count());

		System.out.println("=== 태그 삭제 시 UrlTag도 삭제 테스트 ===");
		System.out.println("tagId: " + tagId + ", urlId: " + urlMapping.getId());

		// When: 태그 삭제
		mockMvc.perform(delete("/api/v1/tags/" + tagId)
				.with(user(savedMember.getId().toString())))
			.andExpect(status().isNoContent());

		// Then: UrlTag도 함께 삭제
		assertEquals(0, tagRepository.count());
		assertEquals(0, urlTagRepository.count());

		System.out.println("태그 삭제 시 UrlTag도 함께 삭제 확인");
	}

	// -------------------------------------------------------------------------
	// POST /api/v1/tags/assign - URL에 태그 할당
	// -------------------------------------------------------------------------

	@Test
	@DisplayName("태그 할당 - 정상")
	void assignTags_정상() throws Exception {
		// Given: URL 및 태그 생성
		UrlMapping urlMapping = createUrlMapping();
		Long tagId1 = createTagAndGetId("태그A");
		Long tagId2 = createTagAndGetId("태그B");

		String requestBody = String.format("{\"urlId\":%d, \"tagIds\":[%d, %d]}",
			urlMapping.getId(), tagId1, tagId2);

		System.out.println("=== 태그 할당 정상 테스트 ===");
		System.out.println("urlId: " + urlMapping.getId() + ", tagIds: [" + tagId1 + ", " + tagId2 + "]");

		// When & Then
		mockMvc.perform(post("/api/v1/tags/assign")
				.with(user(savedMember.getId().toString()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isNoContent());

		// DB 검증
		assertEquals(2, urlTagRepository.count());

		System.out.println("태그 할당 성공. UrlTag 2개 저장 확인");
	}

	@Test
	@DisplayName("태그 할당 실패 - URL 소유자가 아닌 경우")
	void assignTags_URL소유자아닌경우_403() throws Exception {
		// Given: 다른 멤버 소유의 URL 생성
		Member otherMember = new Member(Provider.KAKAO, "kakao-url-owner", "url-owner@example.com");
		Member savedOtherMember = memberRepository.save(otherMember);

		UrlMapping urlMapping = createUrlMapping(savedOtherMember);
		Long tagId = createTagAndGetId("태그");

		String requestBody = String.format("{\"urlId\":%d, \"tagIds\":[%d]}", urlMapping.getId(), tagId);

		System.out.println("=== 태그 할당 실패 - URL 소유자 아님 테스트 ===");
		System.out.println("URL 소유자: " + savedOtherMember.getId() + ", 요청자: " + savedMember.getId());

		// When & Then
		mockMvc.perform(post("/api/v1/tags/assign")
				.with(user(savedMember.getId().toString()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("6004"));

		assertEquals(0, urlTagRepository.count());

		System.out.println("태그 할당 실패: 403 Forbidden");
	}

	@Test
	@DisplayName("태그 할당 - 이미 할당된 태그 중복 요청 시 무시")
	void assignTags_중복할당_무시() throws Exception {
		// Given: URL 및 태그 생성
		UrlMapping urlMapping = createUrlMapping();
		Long tagId = createTagAndGetId("중복태그");

		String requestBody = String.format("{\"urlId\":%d, \"tagIds\":[%d]}", urlMapping.getId(), tagId);

		// 첫 번째 할당
		mockMvc.perform(post("/api/v1/tags/assign")
			.with(user(savedMember.getId().toString()))
			.contentType(MediaType.APPLICATION_JSON)
			.content(requestBody));

		assertEquals(1, urlTagRepository.count());

		System.out.println("=== 태그 할당 중복 무시 테스트 ===");

		// When: 동일한 태그를 다시 할당 시도
		mockMvc.perform(post("/api/v1/tags/assign")
				.with(user(savedMember.getId().toString()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isNoContent());

		// Then: UrlTag는 여전히 1개 (중복 저장 안 됨)
		assertEquals(1, urlTagRepository.count());

		System.out.println("중복 할당 무시 - UrlTag 여전히 1개");
	}

	// -------------------------------------------------------------------------
	// POST /api/v1/tags/unassign - URL에서 태그 해제
	// -------------------------------------------------------------------------

	@Test
	@DisplayName("태그 해제 - 정상")
	void unassignTags_정상() throws Exception {
		// Given: URL, 태그 생성 및 할당
		UrlMapping urlMapping = createUrlMapping();
		Long tagId1 = createTagAndGetId("해제태그A");
		Long tagId2 = createTagAndGetId("유지태그B");

		// 2개 할당
		String assignBody = String.format("{\"urlId\":%d, \"tagIds\":[%d, %d]}",
			urlMapping.getId(), tagId1, tagId2);
		mockMvc.perform(post("/api/v1/tags/assign")
			.with(user(savedMember.getId().toString()))
			.contentType(MediaType.APPLICATION_JSON)
			.content(assignBody));

		assertEquals(2, urlTagRepository.count());

		System.out.println("=== 태그 해제 정상 테스트 ===");
		System.out.println("urlId: " + urlMapping.getId() + ", 해제할 tagId: " + tagId1);

		// When: 태그1만 해제
		String unassignBody = String.format("{\"urlId\":%d, \"tagIds\":[%d]}",
			urlMapping.getId(), tagId1);
		mockMvc.perform(post("/api/v1/tags/unassign")
				.with(user(savedMember.getId().toString()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(unassignBody))
			.andExpect(status().isNoContent());

		// Then: UrlTag 1개만 남아있음
		assertEquals(1, urlTagRepository.count());

		System.out.println("태그 해제 성공. 남은 UrlTag: 1개");
	}

	@Test
	@DisplayName("태그 해제 실패 - URL 소유자가 아닌 경우")
	void unassignTags_URL소유자아닌경우_403() throws Exception {
		// Given: 다른 멤버의 URL에 태그 할당
		Member otherMember = new Member(Provider.KAKAO, "kakao-unassign-owner", "unassign-owner@example.com");
		Member savedOtherMember = memberRepository.save(otherMember);

		UrlMapping urlMapping = createUrlMapping(savedOtherMember);
		Long tagId = createTagAndGetId(savedOtherMember.getId(), "태그");

		// 태그 할당 (소유자로)
		String assignBody = String.format("{\"urlId\":%d, \"tagIds\":[%d]}", urlMapping.getId(), tagId);
		mockMvc.perform(post("/api/v1/tags/assign")
			.with(user(savedOtherMember.getId().toString()))
			.contentType(MediaType.APPLICATION_JSON)
			.content(assignBody));

		assertEquals(1, urlTagRepository.count());

		System.out.println("=== 태그 해제 실패 - URL 소유자 아님 테스트 ===");

		// When: 다른 사람이 해제 시도
		String unassignBody = String.format("{\"urlId\":%d, \"tagIds\":[%d]}", urlMapping.getId(), tagId);
		mockMvc.perform(post("/api/v1/tags/unassign")
				.with(user(savedMember.getId().toString()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(unassignBody))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("6004"));

		// DB 검증: 여전히 1개 남아있음
		assertEquals(1, urlTagRepository.count());

		System.out.println("태그 해제 실패: 403 Forbidden");
	}

	// -------------------------------------------------------------------------
	// GET /api/v1/my-urls/list?tagIds= - 태그 필터링 조회
	// -------------------------------------------------------------------------

	@Test
	@DisplayName("URL 목록 조회 - OR 필터링 (tagIds가 1개 이상인 경우 해당 태그 중 하나라도 포함)")
	void getMyUrls_OR필터링() throws Exception {
		// Given: URL 3개 생성, 각각 다른 태그 할당
		UrlMapping url1 = createUrlMapping();
		UrlMapping url2 = createUrlMapping();
		UrlMapping url3 = createUrlMapping();

		Long tagId1 = createTagAndGetId("태그알파");
		Long tagId2 = createTagAndGetId("태그베타");

		// url1에 tagId1 할당
		assignTag(url1.getId(), tagId1);

		// url2에 tagId2 할당
		assignTag(url2.getId(), tagId2);

		// url3에는 태그 없음

		System.out.println("=== OR 필터링 테스트 ===");
		System.out.println("tagId1: " + tagId1 + " (url1 할당), tagId2: " + tagId2 + " (url2 할당)");

		// When & Then: tagId1 OR tagId2 필터 → url1, url2 조회 (url3 제외)
		mockMvc.perform(get("/api/v1/my-urls/list")
				.param("tagIds", tagId1.toString())
				.param("tagIds", tagId2.toString())
				.param("filterMode", "or")
				.param("page", "0")
				.param("size", "10")
				.with(user(savedMember.getId().toString())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.urls.length()").value(2))
			.andExpect(jsonPath("$.totalElements").value(2));

		System.out.println("OR 필터링 성공: url1, url2 조회 (url3 제외)");
	}

	@Test
	@DisplayName("URL 목록 조회 - AND 필터링 (모든 태그를 포함하는 URL만 조회)")
	void getMyUrls_AND필터링() throws Exception {
		// Given: URL 3개 생성
		UrlMapping url1 = createUrlMapping();
		UrlMapping url2 = createUrlMapping();
		UrlMapping url3 = createUrlMapping();

		Long tagId1 = createTagAndGetId("태그감마");
		Long tagId2 = createTagAndGetId("태그델타");

		// url1에 tagId1, tagId2 모두 할당
		assignTag(url1.getId(), tagId1);
		assignTag(url1.getId(), tagId2);

		// url2에 tagId1만 할당
		assignTag(url2.getId(), tagId1);

		// url3에는 태그 없음

		System.out.println("=== AND 필터링 테스트 ===");
		System.out.println("tagId1: " + tagId1 + ", tagId2: " + tagId2);
		System.out.println("url1: 두 태그 모두, url2: tagId1만, url3: 없음");

		// When & Then: tagId1 AND tagId2 필터 → url1만 조회
		mockMvc.perform(get("/api/v1/my-urls/list")
				.param("tagIds", tagId1.toString())
				.param("tagIds", tagId2.toString())
				.param("filterMode", "and")
				.param("page", "0")
				.param("size", "10")
				.with(user(savedMember.getId().toString())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.urls.length()").value(1))
			.andExpect(jsonPath("$.totalElements").value(1));

		System.out.println("AND 필터링 성공: url1만 조회 (두 태그 모두 가진 URL)");
	}

	@Test
	@DisplayName("URL 목록 조회 - 단일 태그 OR 필터링")
	void getMyUrls_단일태그_OR필터링() throws Exception {
		// Given: URL 2개 생성, 1개에만 태그 할당
		UrlMapping url1 = createUrlMapping();
		UrlMapping url2 = createUrlMapping();

		Long tagId = createTagAndGetId("필터태그");

		// url1에만 태그 할당
		assignTag(url1.getId(), tagId);

		System.out.println("=== 단일 태그 OR 필터링 테스트 ===");

		// When & Then: tagId 필터 → url1만 조회
		mockMvc.perform(get("/api/v1/my-urls/list")
				.param("tagIds", tagId.toString())
				.param("filterMode", "or")
				.param("page", "0")
				.param("size", "10")
				.with(user(savedMember.getId().toString())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.urls.length()").value(1))
			.andExpect(jsonPath("$.totalElements").value(1));

		System.out.println("단일 태그 OR 필터링 성공");
	}

	@Test
	@DisplayName("URL 목록 조회 - 태그 필터 없이 전체 조회")
	void getMyUrls_태그필터없이_전체조회() throws Exception {
		// Given: URL 3개 생성
		createUrlMapping();
		createUrlMapping();
		createUrlMapping();

		System.out.println("=== 태그 필터 없이 전체 조회 테스트 ===");

		// When & Then: 태그 필터 없이 전체 조회
		mockMvc.perform(get("/api/v1/my-urls/list")
				.param("page", "0")
				.param("size", "10")
				.with(user(savedMember.getId().toString())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.urls.length()").value(3))
			.andExpect(jsonPath("$.totalElements").value(3));

		System.out.println("전체 조회 성공: 3개");
	}

	@Test
	@DisplayName("URL 목록 조회 - 응답에 tags 필드 포함 여부 확인")
	void getMyUrls_응답에_tags_포함() throws Exception {
		// Given: URL 생성, 태그 2개 할당
		UrlMapping urlMapping = createUrlMapping();
		Long tagId1 = createTagAndGetId("응답태그A");
		Long tagId2 = createTagAndGetId("응답태그B");

		assignTag(urlMapping.getId(), tagId1);
		assignTag(urlMapping.getId(), tagId2);

		System.out.println("=== URL 목록 응답에 tags 필드 포함 테스트 ===");

		// When & Then: tags 필드 포함 여부 확인
		mockMvc.perform(get("/api/v1/my-urls/list")
				.param("page", "0")
				.param("size", "10")
				.with(user(savedMember.getId().toString())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.urls[0].tags").isArray())
			.andExpect(jsonPath("$.urls[0].tags.length()").value(2));

		System.out.println("응답에 tags 필드 2개 포함 확인");
	}

	// -------------------------------------------------------------------------
	// 태그 전체 플로우 통합 테스트
	// -------------------------------------------------------------------------

	@Test
	@DisplayName("태그 전체 플로우 - 생성 → 할당 → 조회 → 해제 → 삭제")
	void tag_전체_플로우() throws Exception {
		// Step 1: 태그 생성
		Long tagId = createTagAndGetId("플로우태그");

		System.out.println("=== 태그 전체 플로우 테스트 ===");
		System.out.println("Step 1: 태그 생성 완료. tagId: " + tagId);

		// Step 2: URL 생성 및 태그 할당
		UrlMapping urlMapping = createUrlMapping();

		String assignBody = String.format("{\"urlId\":%d, \"tagIds\":[%d]}", urlMapping.getId(), tagId);
		mockMvc.perform(post("/api/v1/tags/assign")
			.with(user(savedMember.getId().toString()))
			.contentType(MediaType.APPLICATION_JSON)
			.content(assignBody));

		assertEquals(1, urlTagRepository.count());
		System.out.println("Step 2: 태그 할당 완료. UrlTag: 1개");

		// Step 3: 태그 필터링으로 URL 조회
		mockMvc.perform(get("/api/v1/my-urls/list")
				.param("tagIds", tagId.toString())
				.param("filterMode", "or")
				.param("page", "0")
				.param("size", "10")
				.with(user(savedMember.getId().toString())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(1));

		System.out.println("Step 3: 태그 필터링 조회 성공");

		// Step 4: 태그 이름 변경
		mockMvc.perform(patch("/api/v1/tags/" + tagId)
			.with(user(savedMember.getId().toString()))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"변경된플로우태그\"}"));

		assertEquals("변경된플로우태그", tagRepository.findById(tagId).orElseThrow().getName());
		System.out.println("Step 4: 태그 이름 변경 완료");

		// Step 5: 태그 해제
		String unassignBody = String.format("{\"urlId\":%d, \"tagIds\":[%d]}", urlMapping.getId(), tagId);
		mockMvc.perform(post("/api/v1/tags/unassign")
			.with(user(savedMember.getId().toString()))
			.contentType(MediaType.APPLICATION_JSON)
			.content(unassignBody));

		assertEquals(0, urlTagRepository.count());
		System.out.println("Step 5: 태그 해제 완료. UrlTag: 0개");

		// Step 6: 태그 삭제
		mockMvc.perform(delete("/api/v1/tags/" + tagId)
			.with(user(savedMember.getId().toString())));

		assertEquals(0, tagRepository.count());
		System.out.println("Step 6: 태그 삭제 완료");
		System.out.println("전체 플로우 완료");
	}

	// -------------------------------------------------------------------------
	// Helper 메서드
	// -------------------------------------------------------------------------

	/**
	 * 현재 멤버(savedMember)로 태그를 생성하고 생성된 태그 ID 반환
	 */
	private Long createTagAndGetId(String tagName) throws Exception {
		return createTagAndGetId(savedMember.getId(), tagName);
	}

	/**
	 * 지정한 멤버로 태그를 생성하고 생성된 태그 ID 반환
	 */
	private Long createTagAndGetId(Long memberId, String tagName) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/tags")
				.with(user(memberId.toString()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"" + tagName + "\"}"))
			.andExpect(status().isCreated())
			.andReturn();

		String responseBody = result.getResponse().getContentAsString();
		JsonNode json = objectMapper.readTree(responseBody);
		return Long.parseLong(json.get("id").asText());
	}

	/**
	 * 현재 멤버(savedMember)로 URL 단축 후 UrlMapping 반환
	 */
	private UrlMapping createUrlMapping() throws Exception {
		return createUrlMapping(savedMember);
	}

	/**
	 * 지정한 멤버로 URL 단축 후 UrlMapping 반환
	 */
	private UrlMapping createUrlMapping(Member member) throws Exception {
		String originalUrl = "https://example.com/" + System.nanoTime();
		mockMvc.perform(post("/api/v1/url/shorten")
			.with(user(member.getId().toString()))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"originalUrl\":\"" + originalUrl + "\"}"));

		return urlMappingRepository.findAll().stream()
			.filter(url -> url.getOriginalUrl().equals(originalUrl))
			.findFirst()
			.orElseThrow();
	}

	/**
	 * 현재 멤버로 URL에 태그 할당
	 */
	private void assignTag(Long urlId, Long tagId) throws Exception {
		String assignBody = String.format("{\"urlId\":%d, \"tagIds\":[%d]}", urlId, tagId);
		mockMvc.perform(post("/api/v1/tags/assign")
			.with(user(savedMember.getId().toString()))
			.contentType(MediaType.APPLICATION_JSON)
			.content(assignBody));
	}
}
