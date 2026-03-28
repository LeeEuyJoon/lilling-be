package luti.server.domain.service;

import static luti.server.exception.ErrorCode.*;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import luti.server.domain.model.UrlMapping;
import luti.server.domain.port.UrlMappingReader;
import luti.server.domain.port.UrlMappingStore;
import luti.server.exception.BusinessException;
import luti.server.domain.service.dto.MyUrlsListInfo;
import luti.server.domain.service.dto.UrlMappingInfo;

@Service
public class MyUrlService {

	private final UrlMappingReader urlMappingReader;
	private final UrlMappingStore urlMappingStore;

	public MyUrlService(UrlMappingReader urlMappingReader, UrlMappingStore urlMappingStore) {
		this.urlMappingReader = urlMappingReader;
		this.urlMappingStore = urlMappingStore;
	}

	@Transactional
	public void claimUrlMappingToMember(UrlMappingInfo urlMappingInfo, Long memberId) {
		if (urlMappingInfo.isHasOwner()) {
			throw new BusinessException(ALREADY_OWNED_URL);
		}
		urlMappingStore.claimToMember(urlMappingInfo.getId(), memberId);
	}

	@Transactional(readOnly = true)
	public MyUrlsListInfo getMyUrls(Long memberId, Integer page, Integer size, List<Long> tagIds) {
		Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

		Page<UrlMapping> pageResult;
		if (tagIds == null || tagIds.isEmpty()) {
			pageResult = urlMappingReader.findByMemberId(memberId, pageable);
		} else {
			pageResult = urlMappingReader.findByMemberIdAndTagIds(memberId, tagIds, pageable);
		}

		return MyUrlsListInfo.from(pageResult);
	}

	@Transactional
	public void updateUrlDescription(Long urlId, Long memberId, String description) {
		UrlMapping urlMapping = urlMappingReader.findById(urlId)
													.orElseThrow(() -> new BusinessException(URL_NOT_FOUND));

		// 소유자 확인
		if (!urlMapping.getMember().getId().equals(memberId)) {
			throw new BusinessException(NOT_URL_OWNER);
		}

		urlMappingStore.updateDescription(urlId, description);
	}

	@Transactional
	public void deleteUrlMapping(Long urlId, Long memberId) {

		UrlMapping urlMapping = urlMappingReader.findById(urlId)
													.orElseThrow(() -> new BusinessException(URL_NOT_FOUND));

		// 소유자 확인
		if (!urlMapping.getMember().getId().equals(memberId)) {
			throw new BusinessException(NOT_URL_OWNER);
		}

		urlMappingStore.deleteById(urlId);
	}
}
