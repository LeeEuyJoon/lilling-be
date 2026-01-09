package luti.server.service;

import static luti.server.exception.ErrorCode.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import luti.server.entity.Member;
import luti.server.exception.BusinessException;
import luti.server.repository.UrlMappingRepository;
import luti.server.service.dto.UrlMappingInfo;

@Service
public class MyUrlService {

	private final UrlMappingRepository urlMappingRepository;

	public MyUrlService(UrlMappingRepository urlMappingRepository) {
		this.urlMappingRepository = urlMappingRepository;
	}

	@Transactional
	public void claimUrlMappingToMember(UrlMappingInfo urlMappingInfo, Member member) {
		if (urlMappingInfo.isHasOwner()) {
			throw new BusinessException(ALREADY_OWNED_URL);
		}
		urlMappingRepository.claimUrlMappingToMemberById(urlMappingInfo.getId(), member.getId());
	}

}
