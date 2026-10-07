package com.example.smartstudytimer.member.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

// URL 경로변수 memberId를 클라이언트가 임의로 바꿔서 다른 사람 데이터에 접근하지 못하도록,
// 토큰에서 인증된 memberId와 일치하는지 검사한다. 경로변수가 없는 요청은 그냥 통과시킨다.
@Component
public class MemberIdOwnershipInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Object pathVariables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (!(pathVariables instanceof Map<?, ?> variables)) {
            return true;
        }

        Object rawMemberId = variables.get("memberId");
        if (rawMemberId == null) {
            return true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long authenticatedMemberId = (authentication != null) ? (Long) authentication.getPrincipal() : null;

        if (authenticatedMemberId == null || !authenticatedMemberId.equals(Long.valueOf(rawMemberId.toString()))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                    objectMapper.writeValueAsString(Map.of("message", "본인 정보만 접근할 수 있습니다.")));
            return false;
        }

        return true;
    }
}
