package luti.server.domain.service;

import static luti.server.exception.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import luti.server.domain.enums.Provider;
import luti.server.domain.model.Member;
import luti.server.domain.model.Tag;
import luti.server.domain.model.UrlMapping;
import luti.server.domain.model.UrlTag;
import luti.server.domain.port.MemberReader;
import luti.server.domain.port.TagReader;
import luti.server.domain.port.TagStore;
import luti.server.domain.port.UrlMappingReader;
import luti.server.domain.port.UrlTagReader;
import luti.server.domain.port.UrlTagStore;
import luti.server.domain.service.dto.TagInfo;
import luti.server.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
@DisplayName("TagService 단위 테스트")
class TagServiceTest {

	@Mock
	private TagReader tagReader;

	@Mock
	private TagStore tagStore;

	@Mock
	private UrlTagReader urlTagReader;

	@Mock
	private UrlTagStore urlTagStore;

	@Mock
	private MemberReader memberReader;

	@Mock
	private UrlMappingReader urlMappingReader;

	private TagService tagService;

	private Member testMember;
	private Member otherMember;

	@BeforeEach
	void setUp() {
		tagService = new TagService(tagReader, tagStore, urlTagReader, urlTagStore, memberReader, urlMappingReader);

		testMember = new Member(Provider.GOOGLE, "google-123", "test@example.com");
		ReflectionTestUtils.setField(testMember, "id", 1L);

		otherMember = new Member(Provider.KAKAO, "kakao-456", "other@example.com");
		ReflectionTestUtils.setField(otherMember, "id", 2L);
	}

	// -------------------------------------------------------------------------
	// 태그 생성
	// -------------------------------------------------------------------------

	@Nested
	@DisplayName("태그 생성")
	class CreateTag {

		@Test
		@DisplayName("태그 생성 정상 - TagInfo 반환")
		void createTag_정상() {
			// Given
			Long memberId = 1L;
			String tagName = "개발";

			Tag savedTag = Tag.builder().member(testMember).name(tagName).build();
			ReflectionTestUtils.setField(savedTag, "id", 10L);

			when(tagReader.countByMemberId(memberId)).thenReturn(0L);
			when(tagReader.findByMemberIdAndName(memberId, tagName)).thenReturn(Optional.empty());
			when(memberReader.findById(memberId)).thenReturn(Optional.of(testMember));
			when(tagStore.save(any(Tag.class))).thenReturn(savedTag);

			System.out.println("=== 태그 생성 정상 테스트 ===");
			System.out.println("memberId: " + memberId + ", tagName: " + tagName);

			// When
			TagInfo result = tagService.createTag(memberId, tagName);

			// Then
			assertNotNull(result);
			assertEquals(10L, result.getId());
			assertEquals(tagName, result.getName());

			System.out.println("생성된 태그 ID: " + result.getId() + ", 이름: " + result.getName());

			verify(tagStore).save(any(Tag.class));
		}

		@Test
		@DisplayName("태그 생성 실패 - 중복 이름")
		void createTag_중복이름_예외() {
			// Given
			Long memberId = 1L;
			String tagName = "중복태그";

			Tag existingTag = Tag.builder().member(testMember).name(tagName).build();
			ReflectionTestUtils.setField(existingTag, "id", 5L);

			when(tagReader.countByMemberId(memberId)).thenReturn(1L);
			when(tagReader.findByMemberIdAndName(memberId, tagName)).thenReturn(Optional.of(existingTag));

			System.out.println("=== 태그 생성 실패 - 중복 이름 테스트 ===");
			System.out.println("tagName: " + tagName);

			// When & Then
			BusinessException exception = assertThrows(BusinessException.class,
				() -> tagService.createTag(memberId, tagName));

			assertEquals(DUPLICATE_TAG_NAME, exception.getErrorCode());

			System.out.println("예외 발생: " + exception.getMessage());

			verify(tagStore, never()).save(any(Tag.class));
		}

		@Test
		@DisplayName("태그 생성 실패 - 50개 초과")
		void createTag_50개초과_예외() {
			// Given
			Long memberId = 1L;
			String tagName = "새태그";

			when(tagReader.countByMemberId(memberId)).thenReturn(50L);

			System.out.println("=== 태그 생성 실패 - 50개 초과 테스트 ===");
			System.out.println("현재 태그 수: 50");

			// When & Then
			BusinessException exception = assertThrows(BusinessException.class,
				() -> tagService.createTag(memberId, tagName));

			assertEquals(TAG_LIMIT_EXCEEDED, exception.getErrorCode());

			System.out.println("예외 발생: " + exception.getMessage());

			verify(tagStore, never()).save(any(Tag.class));
		}

		@Test
		@DisplayName("태그 생성 - 정확히 49개일 때 성공 (경계값)")
		void createTag_49개_성공() {
			// Given
			Long memberId = 1L;
			String tagName = "50번째태그";

			Tag savedTag = Tag.builder().member(testMember).name(tagName).build();
			ReflectionTestUtils.setField(savedTag, "id", 50L);

			when(tagReader.countByMemberId(memberId)).thenReturn(49L);
			when(tagReader.findByMemberIdAndName(memberId, tagName)).thenReturn(Optional.empty());
			when(memberReader.findById(memberId)).thenReturn(Optional.of(testMember));
			when(tagStore.save(any(Tag.class))).thenReturn(savedTag);

			System.out.println("=== 태그 생성 경계값 - 49개 → 50개 테스트 ===");

			// When
			TagInfo result = tagService.createTag(memberId, tagName);

			// Then
			assertNotNull(result);
			assertEquals(tagName, result.getName());

			System.out.println("50번째 태그 생성 성공: " + result.getName());
		}
	}

	// -------------------------------------------------------------------------
	// 태그 조회
	// -------------------------------------------------------------------------

	@Nested
	@DisplayName("태그 조회")
	class GetTags {

		@Test
		@DisplayName("태그 목록 조회 - 정상")
		void getTagsByMember_정상() {
			// Given
			Long memberId = 1L;

			Tag tag1 = Tag.builder().member(testMember).name("태그1").build();
			Tag tag2 = Tag.builder().member(testMember).name("태그2").build();
			ReflectionTestUtils.setField(tag1, "id", 1L);
			ReflectionTestUtils.setField(tag2, "id", 2L);

			when(tagReader.findAllByMemberId(memberId)).thenReturn(List.of(tag1, tag2));

			System.out.println("=== 태그 목록 조회 테스트 ===");

			// When
			List<TagInfo> result = tagService.getTagsByMember(memberId);

			// Then
			assertEquals(2, result.size());
			assertEquals("태그1", result.get(0).getName());
			assertEquals("태그2", result.get(1).getName());

			System.out.println("조회된 태그 수: " + result.size());
		}

		@Test
		@DisplayName("태그 목록 조회 - 빈 리스트")
		void getTagsByMember_빈리스트() {
			// Given
			Long memberId = 1L;

			when(tagReader.findAllByMemberId(memberId)).thenReturn(List.of());

			System.out.println("=== 태그 목록 조회 빈 리스트 테스트 ===");

			// When
			List<TagInfo> result = tagService.getTagsByMember(memberId);

			// Then
			assertTrue(result.isEmpty());

			System.out.println("빈 태그 목록 조회 성공");
		}
	}

	// -------------------------------------------------------------------------
	// 태그 이름 변경
	// -------------------------------------------------------------------------

	@Nested
	@DisplayName("태그 이름 변경")
	class UpdateTag {

		@Test
		@DisplayName("태그 이름 변경 - 정상")
		void updateTag_정상() {
			// Given
			Long memberId = 1L;
			Long tagId = 10L;
			String newName = "새이름";

			Tag tag = Tag.builder().member(testMember).name("기존이름").build();
			ReflectionTestUtils.setField(tag, "id", tagId);

			when(tagReader.findById(tagId)).thenReturn(Optional.of(tag));
			when(tagReader.findByMemberIdAndName(memberId, newName)).thenReturn(Optional.empty());
			when(tagStore.save(any(Tag.class))).thenReturn(tag);

			System.out.println("=== 태그 이름 변경 정상 테스트 ===");
			System.out.println("tagId: " + tagId + ", newName: " + newName);

			// When & Then (예외 없이 완료)
			assertDoesNotThrow(() -> tagService.updateTag(memberId, tagId, newName));

			verify(tagStore).save(tag);

			System.out.println("태그 이름 변경 성공: " + newName);
		}

		@Test
		@DisplayName("태그 이름 변경 실패 - 소유자가 아닌 경우")
		void updateTag_소유자아닌경우_예외() {
			// Given
			Long memberId = 1L;
			Long tagId = 10L;
			String newName = "새이름";

			Tag tag = Tag.builder().member(otherMember).name("기존이름").build();
			ReflectionTestUtils.setField(tag, "id", tagId);

			when(tagReader.findById(tagId)).thenReturn(Optional.of(tag));

			System.out.println("=== 태그 이름 변경 실패 - 소유자 아님 테스트 ===");
			System.out.println("요청자 memberId: " + memberId + ", 소유자 memberId: " + otherMember.getId());

			// When & Then
			BusinessException exception = assertThrows(BusinessException.class,
				() -> tagService.updateTag(memberId, tagId, newName));

			assertEquals(NOT_TAG_OWNER, exception.getErrorCode());

			System.out.println("예외 발생: " + exception.getMessage());

			verify(tagStore, never()).save(any(Tag.class));
		}

		@Test
		@DisplayName("태그 이름 변경 실패 - 중복 이름")
		void updateTag_중복이름_예외() {
			// Given
			Long memberId = 1L;
			Long tagId = 10L;
			Long otherTagId = 20L;
			String duplicateName = "이미있는이름";

			Tag tag = Tag.builder().member(testMember).name("기존이름").build();
			ReflectionTestUtils.setField(tag, "id", tagId);

			Tag existingTag = Tag.builder().member(testMember).name(duplicateName).build();
			ReflectionTestUtils.setField(existingTag, "id", otherTagId);

			when(tagReader.findById(tagId)).thenReturn(Optional.of(tag));
			when(tagReader.findByMemberIdAndName(memberId, duplicateName)).thenReturn(Optional.of(existingTag));

			System.out.println("=== 태그 이름 변경 실패 - 중복 이름 테스트 ===");

			// When & Then
			BusinessException exception = assertThrows(BusinessException.class,
				() -> tagService.updateTag(memberId, tagId, duplicateName));

			assertEquals(DUPLICATE_TAG_NAME, exception.getErrorCode());

			System.out.println("예외 발생: " + exception.getMessage());

			verify(tagStore, never()).save(any(Tag.class));
		}

		@Test
		@DisplayName("태그 이름 변경 실패 - 존재하지 않는 태그")
		void updateTag_존재하지않는태그_예외() {
			// Given
			Long memberId = 1L;
			Long nonExistentTagId = 999L;

			when(tagReader.findById(nonExistentTagId)).thenReturn(Optional.empty());

			System.out.println("=== 태그 이름 변경 실패 - 존재하지 않는 태그 ===");

			// When & Then
			BusinessException exception = assertThrows(BusinessException.class,
				() -> tagService.updateTag(memberId, nonExistentTagId, "새이름"));

			assertEquals(TAG_NOT_FOUND, exception.getErrorCode());

			System.out.println("예외 발생: " + exception.getMessage());
		}
	}

	// -------------------------------------------------------------------------
	// 태그 삭제
	// -------------------------------------------------------------------------

	@Nested
	@DisplayName("태그 삭제")
	class DeleteTag {

		@Test
		@DisplayName("태그 삭제 - 정상")
		void deleteTag_정상() {
			// Given
			Long memberId = 1L;
			Long tagId = 10L;

			Tag tag = Tag.builder().member(testMember).name("삭제할태그").build();
			ReflectionTestUtils.setField(tag, "id", tagId);

			when(tagReader.findById(tagId)).thenReturn(Optional.of(tag));

			System.out.println("=== 태그 삭제 정상 테스트 ===");
			System.out.println("tagId: " + tagId);

			// When & Then
			assertDoesNotThrow(() -> tagService.deleteTag(memberId, tagId));

			verify(urlTagStore).deleteByTagId(tagId);
			verify(tagStore).deleteById(tagId);

			System.out.println("태그 삭제 성공 - UrlTag 먼저 삭제 후 Tag 삭제");
		}

		@Test
		@DisplayName("태그 삭제 실패 - 소유자가 아닌 경우")
		void deleteTag_소유자아닌경우_예외() {
			// Given
			Long memberId = 1L;
			Long tagId = 10L;

			Tag tag = Tag.builder().member(otherMember).name("태그").build();
			ReflectionTestUtils.setField(tag, "id", tagId);

			when(tagReader.findById(tagId)).thenReturn(Optional.of(tag));

			System.out.println("=== 태그 삭제 실패 - 소유자 아님 테스트 ===");

			// When & Then
			BusinessException exception = assertThrows(BusinessException.class,
				() -> tagService.deleteTag(memberId, tagId));

			assertEquals(NOT_TAG_OWNER, exception.getErrorCode());

			System.out.println("예외 발생: " + exception.getMessage());

			verify(urlTagStore, never()).deleteByTagId(any());
			verify(tagStore, never()).deleteById(any());
		}

		@Test
		@DisplayName("태그 삭제 실패 - 존재하지 않는 태그")
		void deleteTag_존재하지않는태그_예외() {
			// Given
			Long memberId = 1L;
			Long nonExistentTagId = 999L;

			when(tagReader.findById(nonExistentTagId)).thenReturn(Optional.empty());

			System.out.println("=== 태그 삭제 실패 - 존재하지 않는 태그 ===");

			// When & Then
			BusinessException exception = assertThrows(BusinessException.class,
				() -> tagService.deleteTag(memberId, nonExistentTagId));

			assertEquals(TAG_NOT_FOUND, exception.getErrorCode());

			System.out.println("예외 발생: " + exception.getMessage());
		}
	}

	// -------------------------------------------------------------------------
	// 태그 할당
	// -------------------------------------------------------------------------

	@Nested
	@DisplayName("태그 할당")
	class AssignTags {

		@Test
		@DisplayName("태그 할당 - 정상")
		void assignTags_정상() {
			// Given
			Long memberId = 1L;
			Long urlId = 100L;
			Long tagId1 = 10L;
			Long tagId2 = 20L;
			List<Long> tagIds = List.of(tagId1, tagId2);

			UrlMapping urlMapping = buildUrlMapping(urlId, testMember);
			Tag tag1 = buildTag(tagId1, testMember, "태그1");
			Tag tag2 = buildTag(tagId2, testMember, "태그2");

			when(urlMappingReader.findById(urlId)).thenReturn(Optional.of(urlMapping));
			when(tagReader.findById(tagId1)).thenReturn(Optional.of(tag1));
			when(tagReader.findById(tagId2)).thenReturn(Optional.of(tag2));
			when(urlTagReader.findByUrlMappingId(urlId)).thenReturn(List.of());

			System.out.println("=== 태그 할당 정상 테스트 ===");
			System.out.println("urlId: " + urlId + ", tagIds: " + tagIds);

			// When & Then
			assertDoesNotThrow(() -> tagService.assignTags(memberId, urlId, tagIds));

			verify(urlTagStore).saveAll(argThat(list -> list.size() == 2));

			System.out.println("태그 할당 성공 - 2개 UrlTag 저장");
		}

		@Test
		@DisplayName("태그 할당 실패 - 태그 소유자가 아닌 경우")
		void assignTags_태그소유자아닌경우_예외() {
			// Given
			Long memberId = 1L;
			Long urlId = 100L;
			Long tagId = 10L;
			List<Long> tagIds = List.of(tagId);

			// URL은 testMember 소유, 태그는 otherMember 소유
			UrlMapping urlMapping = buildUrlMapping(urlId, testMember);
			Tag otherMemberTag = buildTag(tagId, otherMember, "남의태그");

			when(urlMappingReader.findById(urlId)).thenReturn(Optional.of(urlMapping));
			when(tagReader.findById(tagId)).thenReturn(Optional.of(otherMemberTag));

			System.out.println("=== 태그 할당 실패 - 태그 소유자 아님 테스트 ===");
			System.out.println("요청자 memberId: " + memberId + ", 태그 소유자 memberId: " + otherMember.getId());
			System.out.println("실제 assignTags는 URL 소유권이 아닌 태그 소유권을 체크함");

			// When & Then
			BusinessException exception = assertThrows(BusinessException.class,
				() -> tagService.assignTags(memberId, urlId, tagIds));

			assertEquals(NOT_TAG_OWNER, exception.getErrorCode());

			System.out.println("예외 발생: " + exception.getMessage());

			verify(urlTagStore, never()).saveAll(any());
		}

		@Test
		@DisplayName("태그 할당 - 이미 할당된 태그 중복 처리 (새 태그만 저장)")
		void assignTags_이미할당된태그_중복처리() {
			// Given
			Long memberId = 1L;
			Long urlId = 100L;
			Long existingTagId = 10L;
			Long newTagId = 20L;
			List<Long> tagIds = List.of(existingTagId, newTagId);

			UrlMapping urlMapping = buildUrlMapping(urlId, testMember);
			Tag existingTag = buildTag(existingTagId, testMember, "이미있는태그");
			Tag newTag = buildTag(newTagId, testMember, "새태그");

			UrlTag existingUrlTag = UrlTag.of(urlMapping, existingTag);

			when(urlMappingReader.findById(urlId)).thenReturn(Optional.of(urlMapping));
			when(tagReader.findById(existingTagId)).thenReturn(Optional.of(existingTag));
			when(tagReader.findById(newTagId)).thenReturn(Optional.of(newTag));
			when(urlTagReader.findByUrlMappingId(urlId)).thenReturn(List.of(existingUrlTag));

			System.out.println("=== 태그 할당 - 이미 할당된 태그 중복 처리 테스트 ===");
			System.out.println("기존 태그 ID: " + existingTagId + ", 새 태그 ID: " + newTagId);

			// When
			assertDoesNotThrow(() -> tagService.assignTags(memberId, urlId, tagIds));

			// Then: 이미 할당된 태그를 제외한 1개만 저장
			verify(urlTagStore).saveAll(argThat(list -> list.size() == 1));

			System.out.println("중복 태그 제외하고 새 태그 1개만 저장 성공");
		}

		@Test
		@DisplayName("태그 할당 - 모두 이미 할당된 경우 저장 안 함")
		void assignTags_모두이미할당됨_저장안함() {
			// Given
			Long memberId = 1L;
			Long urlId = 100L;
			Long existingTagId = 10L;
			List<Long> tagIds = List.of(existingTagId);

			UrlMapping urlMapping = buildUrlMapping(urlId, testMember);
			Tag existingTag = buildTag(existingTagId, testMember, "이미있는태그");
			UrlTag existingUrlTag = UrlTag.of(urlMapping, existingTag);

			when(urlMappingReader.findById(urlId)).thenReturn(Optional.of(urlMapping));
			when(tagReader.findById(existingTagId)).thenReturn(Optional.of(existingTag));
			when(urlTagReader.findByUrlMappingId(urlId)).thenReturn(List.of(existingUrlTag));

			System.out.println("=== 태그 할당 - 모두 이미 할당된 경우 ===");

			// When
			assertDoesNotThrow(() -> tagService.assignTags(memberId, urlId, tagIds));

			// Then: 모두 중복이므로 saveAll 호출 안 함
			verify(urlTagStore, never()).saveAll(any());

			System.out.println("모두 중복 - saveAll 호출 없음");
		}
	}

	// -------------------------------------------------------------------------
	// 태그 해제
	// -------------------------------------------------------------------------

	@Nested
	@DisplayName("태그 해제")
	class UnassignTags {

		@Test
		@DisplayName("태그 해제 - 정상")
		void unassignTags_정상() {
			// Given
			Long memberId = 1L;
			Long urlId = 100L;
			Long tagId = 10L;
			List<Long> tagIds = List.of(tagId);

			System.out.println("=== 태그 해제 정상 테스트 ===");
			System.out.println("urlId: " + urlId + ", tagId: " + tagId);

			// When & Then
			assertDoesNotThrow(() -> tagService.unassignTags(memberId, urlId, tagIds));

			verify(urlTagStore).deleteByUrlMappingIdAndTagIdIn(urlId, tagIds);

			System.out.println("태그 해제 성공");
		}

		@Test
		@DisplayName("태그 해제 - 소유자가 아닌 멤버도 정상 실행 (소유권 체크 없음)")
		void unassignTags_소유자아닌경우_정상실행() {
			// Given
			Long memberId = 1L;
			Long urlId = 100L;
			List<Long> tagIds = List.of(10L);

			System.out.println("=== 태그 해제 - 소유권 체크 없음 테스트 ===");
			System.out.println("memberId: " + memberId + ", urlId: " + urlId);
			System.out.println("실제 unassignTags 구현에 소유권 체크 로직 없음 - 예외 발생하지 않음");

			// When & Then: 소유권 체크가 없으므로 예외 없이 정상 실행
			assertDoesNotThrow(() -> tagService.unassignTags(memberId, urlId, tagIds));

			verify(urlTagStore).deleteByUrlMappingIdAndTagIdIn(urlId, tagIds);

			System.out.println("소유권 체크 없이 정상 실행 확인");
		}
	}

	// -------------------------------------------------------------------------
	// URL에 연결된 태그 맵 조회
	// -------------------------------------------------------------------------

	@Nested
	@DisplayName("URL별 태그 목록 조회")
	class GetTagsForUrls {

		@Test
		@DisplayName("URL 목록에 해당하는 태그 맵 조회 - 정상")
		void getTagsForUrls_정상() {
			// Given
			Long urlId1 = 100L;
			Long urlId2 = 200L;
			List<Long> urlIds = List.of(urlId1, urlId2);

			UrlMapping urlMapping1 = buildUrlMapping(urlId1, testMember);
			UrlMapping urlMapping2 = buildUrlMapping(urlId2, testMember);

			Tag tag1 = buildTag(10L, testMember, "태그A");
			Tag tag2 = buildTag(20L, testMember, "태그B");
			Tag tag3 = buildTag(30L, testMember, "태그C");

			UrlTag urlTag1 = UrlTag.of(urlMapping1, tag1);
			UrlTag urlTag2 = UrlTag.of(urlMapping1, tag2);
			UrlTag urlTag3 = UrlTag.of(urlMapping2, tag3);

			when(urlTagReader.findByUrlMappingIdIn(urlIds)).thenReturn(List.of(urlTag1, urlTag2, urlTag3));

			System.out.println("=== URL별 태그 목록 조회 테스트 ===");
			System.out.println("urlIds: " + urlIds);

			// When
			var result = tagService.getTagsForUrls(urlIds);

			// Then
			assertEquals(2, result.size());
			assertEquals(2, result.get(urlId1).size());
			assertEquals(1, result.get(urlId2).size());

			System.out.println("urlId1 태그 수: " + result.get(urlId1).size());
			System.out.println("urlId2 태그 수: " + result.get(urlId2).size());
		}

		@Test
		@DisplayName("URL 목록이 비어있으면 빈 맵 반환")
		void getTagsForUrls_빈목록_빈맵반환() {
			// Given
			List<Long> emptyUrlIds = List.of();

			System.out.println("=== URL 목록이 빈 경우 테스트 ===");

			// When
			var result = tagService.getTagsForUrls(emptyUrlIds);

			// Then
			assertTrue(result.isEmpty());

			verifyNoInteractions(urlTagReader);

			System.out.println("빈 맵 반환 성공");
		}

		@Test
		@DisplayName("null URL 목록 - 빈 맵 반환")
		void getTagsForUrls_null목록_빈맵반환() {
			// Given
			System.out.println("=== null URL 목록 테스트 ===");

			// When
			var result = tagService.getTagsForUrls(null);

			// Then
			assertTrue(result.isEmpty());

			verifyNoInteractions(urlTagReader);

			System.out.println("null 입력 시 빈 맵 반환 성공");
		}
	}

	// -------------------------------------------------------------------------
	// Helper 메서드
	// -------------------------------------------------------------------------

	private UrlMapping buildUrlMapping(Long id, Member member) {
		UrlMapping urlMapping = UrlMapping.builder()
			.scrambledId(id * 100)
			.kgsId(id * 10)
			.originalUrl("https://example.com/" + id)
			.shortUrl("https://lill.ing/test" + id)
			.appId("test-app")
			.member(member)
			.build();
		ReflectionTestUtils.setField(urlMapping, "id", id);
		return urlMapping;
	}

	private Tag buildTag(Long id, Member member, String name) {
		Tag tag = Tag.builder().member(member).name(name).build();
		ReflectionTestUtils.setField(tag, "id", id);
		return tag;
	}
}
