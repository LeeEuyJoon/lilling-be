package luti.server.web.resolver;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import luti.server.web.mapper.AuthExtractor;

@Component
public class QueryArgumentResolver implements HandlerMethodArgumentResolver {

	private final ObjectMapper objectMapper;

	public QueryArgumentResolver(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(ResolveQuery.class);
	}

	@Override
	public Object resolveArgument(MethodParameter parameter,
								  ModelAndViewContainer mavContainer,
								  NativeWebRequest webRequest,
								  WebDataBinderFactory binderFactory) throws Exception {

		HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();

		Map<String, Object> params = new HashMap<>();

		// query params 수집
		request.getParameterMap().forEach((k, v) ->
			params.put(k, v.length == 1 ? v[0] : Arrays.asList(v)));

		// path variables 수집
		@SuppressWarnings("unchecked")
		Map<String, String> pathVars = (Map<String, String>)
			request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
		if (pathVars != null) {
			params.putAll(pathVars);
		}

		// memberId 추가
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		Long memberId = AuthExtractor.extractMemberId(authentication);
		if (memberId != null) {
			params.put("memberId", memberId);
		}

		return objectMapper.convertValue(params, parameter.getParameterType());
	}
}
