package luti.server.Facade;

import org.springframework.stereotype.Component;

import luti.server.Service.Base62Encoder;
import luti.server.Service.IdScrambler;
import luti.server.Web.Dto.ShortenRequest;

@Component
public class ApiFacade {

	private final IdScrambler idScrambler;
	private final Base62Encoder base62Encoder;

	public ApiFacade(IdScrambler idScrambler, Base62Encoder base62Encoder) {
		this.idScrambler = idScrambler;
		this.base62Encoder = base62Encoder;
	}

	public String shortenUrl(ShortenRequest request) {

		// DB I/O를 두 번 해야한다? 객체를 저장해야 PK가 뜨고 그걸 이용해 모듈러 연산을 하는거니까?
		// 근데 이러면 요청량 많아질때 DB 부하가 너무 심해질 것 같은데
		// 근데 또 그 문제가 되는 DB 부하를 해결하는 과정도 의미가 있을 것 같긴 하고
		// 흠 ....
		// KGS 도입 ..?


		return "shortenedUrl";
	}

}
