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

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class NaverService {

    private static final String TOKEN_URL = "https://nid.naver.com/oauth2.0/token";
    private static final String USER_INFO_URL = "https://openapi.naver.com/v1/nid/me";

    @Value("${naver.client-id}")
    private String clientId;

    @Value("${naver.client-secret}")
    private String clientSecret;

    @Value("${naver.redirect-uri}")
    private String redirectUri;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = createRestTemplate();

    // 네이버 서버가 응답하지 않을 때 요청 스레드가 무한정 묶이지 않도록 타임아웃을 둔다.
    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(5).toMillis());
        return new RestTemplate(factory);
    }

    public String getAccessToken(String code, String state) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);
        params.add("state", state);

        JsonNode body = call(TOKEN_URL, HttpMethod.POST, new HttpEntity<>(params, headers));

        // 네이버는 인가 코드가 틀려도 200으로 응답하고 본문에 error를 담기도 한다.
        JsonNode accessToken = body.get("access_token");
        if (accessToken == null || accessToken.isNull()) {
            throw unauthorized("네이버 인가 코드가 유효하지 않습니다.");
        }
        return accessToken.asText();
    }

    public Map<String, String> getUserInfo(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw unauthorized("네이버 액세스 토큰이 없습니다.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);

        JsonNode body = call(USER_INFO_URL, HttpMethod.GET, new HttpEntity<>(headers));

        JsonNode profile = body.get("response");
        if (profile == null || profile.isNull() || !profile.hasNonNull("id")) {
            throw unauthorized("네이버 사용자 정보를 가져오지 못했습니다.");
        }

        String naverId = profile.get("id").asText();

        String nickname = "네이버유저";
        if (profile.hasNonNull("nickname")) {
            nickname = profile.get("nickname").asText();
        } else if (profile.hasNonNull("name")) {
            nickname = profile.get("name").asText();
        }

        Map<String, String> userInfo = new HashMap<>();
        userInfo.put("naverId", naverId);
        userInfo.put("nickname", nickname);
        return userInfo;
    }

    // 네이버 호출 공통 처리: 4xx는 인증 실패(401), 네트워크 문제는 502로 구분해서 던진다.
    // 응답 원문에는 토큰/개인정보가 섞일 수 있어 로그에 남기지 않는다.
    private JsonNode call(String url, HttpMethod method, HttpEntity<?> request) {
        try {
            String responseBody = restTemplate.exchange(url, method, request, String.class).getBody();
            if (responseBody == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "네이버 응답이 비어 있습니다.");
            }
            return objectMapper.readTree(responseBody);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().is4xxClientError()) {
                throw unauthorized("네이버 인증에 실패했습니다.");
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "네이버 서버 오류입니다.", e);
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "네이버 서버에 연결하지 못했습니다.", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "네이버 응답을 해석하지 못했습니다.", e);
        }
    }

    private ResponseStatusException unauthorized(String message) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
    }
}
