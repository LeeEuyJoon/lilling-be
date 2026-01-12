package luti.server.service;

import static luti.server.exception.ErrorCode.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import luti.server.entity.UrlMapping;
import luti.server.exception.BusinessException;
import luti.server.repository.UrlMappingRepository;
import luti.server.service.dto.MyUrlsListInfo;
import luti.server.service.dto.UrlMappingInfo;

@Service
public class MyUrlService {

	private final UrlMappingRepository urlMappingRepository;

	public MyUrlService(UrlMappingRepository urlMappingRepository) {
		this.urlMappingRepository = urlMappingRepository;
	}

	@Transactional
	public void claimUrlMappingToMember(UrlMappingInfo urlMappingInfo, Long memberId) {
		if (urlMappingInfo.isHasOwner()) {
			throw new BusinessException(ALREADY_OWNED_URL);
		}
		urlMappingRepository.claimUrlMappingToMemberById(urlMappingInfo.getId(), memberId);
	}

	@Transactional(readOnly = true)
	public MyUrlsListInfo getMyUrls(Long memberId, Integer page, Integer size) {
		// PageRequest 생성
		Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

		// Repository 호출 -> Page<UrlMapping> 반환
		Page<UrlMapping> pageResult = urlMappingRepository.findByMember_IdOrderByCreatedAtDesc(memberId, pageable);

		// Page<UrlMapping> → MyUrlsListInfo 변환
		return MyUrlsListInfo.from(pageResult);
	}

}
