package luti.server.domain.service;

import static luti.server.exception.ErrorCode.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import luti.server.exception.ErrorCode;

@Service
public class TagService {

	private static final int TAG_LIMIT = 50;

	private final TagReader tagReader;
	private final TagStore tagStore;
	private final UrlTagReader urlTagReader;
	private final UrlTagStore urlTagStore;
	private final MemberReader memberReader;
	private final UrlMappingReader urlMappingReader;

	public TagService(TagReader tagReader, TagStore tagStore, UrlTagReader urlTagReader, UrlTagStore urlTagStore,
					  MemberReader memberReader, UrlMappingReader urlMappingReader) {
		this.tagReader = tagReader;
		this.tagStore = tagStore;
		this.urlTagReader = urlTagReader;
		this.urlTagStore = urlTagStore;
		this.memberReader = memberReader;
		this.urlMappingReader = urlMappingReader;
	}

	@Transactional(readOnly = true)
	public List<TagInfo> getTagsByMember(Long memberId) {
		return tagReader.findAllByMemberId(memberId).stream()
						.map(TagInfo::from).toList();
	}

	@Transactional
	public TagInfo createTag(Long memberId, String name) {

		// 개수 제한
		if (tagReader.countByMemberId(memberId) >= TAG_LIMIT) {
			throw new BusinessException(TAG_LIMIT_EXCEEDED);
		}

		// 태그 이름 중복 확인
		tagReader.findByMemberIdAndName(memberId, name)
			.ifPresent(t -> { throw new BusinessException(DUPLICATE_TAG_NAME); });

		// Member 조회 후 저장
		Member member = memberReader.findById(memberId)
									.orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

		Tag saved = tagStore.save(Tag.builder()
								   .member(member)
								   .name(name)
								   .build());

		return TagInfo.from(saved);
	}

	@Transactional
	public void updateTag(Long memberId, Long tagId, String name) {

		// 태그 조회
		Tag tag =  tagReader.findById(tagId).orElseThrow(() -> new BusinessException(TAG_NOT_FOUND));

		// 소유자 확인
		if (!tag.getMember().getId().equals(memberId)) {
			throw new BusinessException(NOT_TAG_OWNER);
		}

		// 태그 이름 중복 확인
		tagReader.findByMemberIdAndName(memberId, name)
			.filter(existing -> !existing.getId().equals(tagId))
			.ifPresent(t -> { throw new BusinessException(DUPLICATE_TAG_NAME); });

		tag.updateName(name);
		tagStore.save(tag);
	}

	@Transactional
	public void deleteTag(Long memberId, Long tagId) {

		// 태그 조회
		Tag tag = tagReader.findById(tagId).orElseThrow(() -> new BusinessException(TAG_NOT_FOUND));

		// 소유자 확인
		if (!tag.getMember().getId().equals(memberId)) {
			throw new BusinessException(NOT_TAG_OWNER);
		}

		// UrlTag 삭제 후 Tag 삭제
		urlTagStore.deleteByTagId(tagId);
		tagStore.deleteById(tagId);
	}

	@Transactional
	public void assignTags(Long memberId, Long urlId, List<Long> tagIds) {

		// // UrlMapping 조회
		UrlMapping urlMapping  = urlMappingReader.findById(urlId)
			.orElseThrow(() -> new BusinessException(SHORT_URL_NOT_FOUND));

		// 태그 조회 및 소유자 확인
		List<Tag> tags = tagIds.stream().map(tagId -> {
			Tag tag = tagReader.findById(tagId).orElseThrow(() -> new BusinessException(TAG_NOT_FOUND));
			if (!tag.getMember().getId().equals(memberId)) {
				throw new BusinessException(NOT_TAG_OWNER);
			}
			return tag;
		}).toList();

		// 이미 할당된 태그 제외
		List<Long> alreadyAssigned = urlTagReader.findByUrlMappingId(urlId).stream()
												 .map(ut -> ut.getTag().getId()).toList();


		List<UrlTag> newUrlTags = tags.stream()
			.filter(tag -> !alreadyAssigned.contains(tag.getId()))
			.map(tag -> UrlTag.of(urlMapping, tag))
			.toList();

		// 새로운 UrlTag 저장
		if (!newUrlTags.isEmpty()) {
			urlTagStore.saveAll(newUrlTags);
		}
	}

	@Transactional
	public void unassignTags(Long memberId, Long urlId, List<Long> tagIds) {

		// 태그 할당 해제
		urlTagStore.deleteByUrlMappingIdAndTagIdIn(urlId, tagIds);
	}

	@Transactional(readOnly = true)
	public Map<Long, List<TagInfo>> getTagsForUrls(List<Long> urlIds) {

		// URL ID 리스트가 비어있으면 빈 맵 반환
		if (urlIds == null || urlIds.isEmpty()) return Map.of();

		// URL ID 리스트에 해당하는 UrlTag 조회 후 URL ID별로 태그 정보 매핑
		Map<Long, List<TagInfo>> urlTags = urlTagReader.findByUrlMappingIdIn(urlIds).stream()
						   .collect(Collectors.groupingBy(
							   ut -> ut.getUrlMapping().getId(),
							   Collectors.mapping(ut -> TagInfo.from(ut.getTag()), Collectors.toList())
						   ));

		return urlTags;
	}

}
