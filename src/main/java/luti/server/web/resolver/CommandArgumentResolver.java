package luti.server.web.resolver;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.servlet.http.HttpServletRequest;
import luti.server.web.mapper.AuthExtractor;

@Component
public class CommandArgumentResolver implements HandlerMethodArgumentResolver {

	private final ObjectMapper objectMapper;

	public CommandArgumentResolver(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(ResolveCommand.class);
	}

	@Override
	public Object resolveArgument(MethodParameter parameter,
								  ModelAndViewContainer mavContainer,
								  NativeWebRequest webRequest,
								  WebDataBinderFactory binderFactory) throws Exception {

		HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();

		// SecurityContext에서 Authentication 추출
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		Long memberId = AuthExtractor.extractMemberId(authentication);

		// JSON body 파싱
		JsonNode jsonNode = objectMapper.readTree(request.getInputStream());

		// memberId JSON에 병합
		if (jsonNode instanceof ObjectNode objectNode && memberId != null) {
			objectNode.put("memberId", memberId);
		}

		return objectMapper.treeToValue(jsonNode, parameter.getParameterType());
	}
}
