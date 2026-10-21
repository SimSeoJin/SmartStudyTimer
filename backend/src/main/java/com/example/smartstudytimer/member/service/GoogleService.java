package com.example.smartstudytimer.member.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
public class GoogleService {

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String TOKEN_INFO_URL = "https://oauth2.googleapis.com/tokeninfo";
    private static final String USER_INFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    @Value("${google.client-id}")
    private String clientId;

    @Value("${google.client-secret}")
    private String clientSecret;

    @Value("${google.redirect-uri}")
    private String redirectUri;

    // 앱(안드로이드) 클라이언트 ID처럼 웹 클라이언트 외에 이 서비스가 허용할 클라이언트 ID들(쉼표로 구분).
    @Value("${google.allowed-client-ids:}")
    private String allowedClientIds;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = createRestTemplate();

    // 구글 서버가 응답하지 않을 때 요청 스레드가 무한정 묶이지 않도록 타임아웃을 둔다.
    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(5).toMillis());
        return new RestTemplate(factory);
    }

    public String getAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        JsonNode body = call(TOKEN_URL, HttpMethod.POST, new HttpEntity<>(params, headers));

        JsonNode accessToken = body.get("access_token");
        if (accessToken == null || accessToken.isNull()) {
            throw unauthorized("구글 인가 코드가 유효하지 않습니다.");
        }
        return accessToken.asText();
    }

    public Map<String, String> getUserInfo(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw unauthorized("구글 액세스 토큰이 없습니다.");
        }

        // 다른 앱에 발급된 유효한 구글 토큰으로 이 서비스에 로그인하는 것을 막는다.
        verifyAudience(accessToken);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);

        JsonNode profile = call(USER_INFO_URL, HttpMethod.GET, new HttpEntity<>(headers));

        // sub: 구글 계정 고유 ID (변하지 않음). 이메일은 바뀔 수 있어 식별자로 쓰지 않는다.
        if (!profile.hasNonNull("sub")) {
            throw unauthorized("구글 사용자 정보를 가져오지 못했습니다.");
        }
        String googleId = profile.get("sub").asText();

        String nickname = "구글유저";
        if (profile.hasNonNull("name")) {
            nickname = profile.get("name").asText();
        } else if (profile.hasNonNull("given_name")) {
            nickname = profile.get("given_name").asText();
        }

        Map<String, String> userInfo = new HashMap<>();
        userInfo.put("googleId", googleId);
        userInfo.put("nickname", nickname);
        return userInfo;
    }

    // tokeninfo의 aud(또는 azp)가 허용한 클라이언트 ID가 아니면 거부한다.
    // 허용 목록이 비어 있으면 검증할 수 없으므로 모두 거부한다(설정 누락 시 열려 버리는 것을 방지).
    private void verifyAudience(String accessToken) {
        Set<String> allowed = allowedAudiences();
        if (allowed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "구글 클라이언트 ID가 서버에 설정되어 있지 않습니다.");
        }

        String url = UriComponentsBuilder.fromUriString(TOKEN_INFO_URL)
                .queryParam("access_token", accessToken)
                .build().toUriString();
        JsonNode info = call(url, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()));

        boolean valid = (info.hasNonNull("aud") && allowed.contains(info.get("aud").asText()))
                || (info.hasNonNull("azp") && allowed.contains(info.get("azp").asText()));
        if (!valid) {
            throw unauthorized("이 서비스용으로 발급된 구글 토큰이 아닙니다.");
        }
    }

    private Set<String> allowedAudiences() {
        Set<String> allowed = new HashSet<>();
        if (clientId != null && !clientId.isBlank() && !clientId.startsWith("REPLACE_WITH_")) {
            allowed.add(clientId.trim());
        }
        Arrays.stream(allowedClientIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(allowed::add);
        return allowed;
    }

    // 구글 호출 공통 처리: 4xx는 인증 실패(401), 네트워크 문제는 502로 구분해서 던진다.
    // 응답 원문에는 토큰/개인정보가 섞일 수 있어 로그에 남기지 않는다.
    private JsonNode call(String url, HttpMethod method, HttpEntity<?> request) {
        try {
            String responseBody = restTemplate.exchange(url, method, request, String.class).getBody();
            if (responseBody == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "구글 응답이 비어 있습니다.");
            }
            return objectMapper.readTree(responseBody);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().is4xxClientError()) {
                throw unauthorized("구글 인증에 실패했습니다.");
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "구글 서버 오류입니다.", e);
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "구글 서버에 연결하지 못했습니다.", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "구글 응답을 해석하지 못했습니다.", e);
        }
    }

    private ResponseStatusException unauthorized(String message) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
    }
}
