package com.example.smartstudytimer.member.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

@Service
public class KakaoService {

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    public String getAccessToken(String code) {
        String tokenUrl = "https://kauth.kakao.com/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> kakaoTokenRequest = new HttpEntity<>(params, headers);

        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> response = rt.exchange(
                tokenUrl,
                HttpMethod.POST,
                kakaoTokenRequest,
                String.class
        );

        try {
            String responseBody = response.getBody();
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            
            String accessToken = jsonNode.get("access_token").asText();
            return accessToken;
            
        } catch (Exception e) {
            throw new RuntimeException("토큰 파싱 실패", e);
        }
    }

    public Map<String, String> getUserInfo(String accessToken) {
        String userInfoUrl = "https://kapi.kakao.com/v2/user/me";

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        HttpEntity<MultiValueMap<String, String>> kakaoProfileRequest = new HttpEntity<>(headers);

        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> response = rt.exchange(
                userInfoUrl,
                HttpMethod.POST,
                kakaoProfileRequest,
                String.class
        );

        try {
            String responseBody = response.getBody();
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(responseBody);

            // 1. 카카오 고유 ID 파싱
            String kakaoId = jsonNode.get("id").asText();

            // 2. 카카오 실제 닉네임 파싱 (기존에 만들어 둔 안전한 방어 코드)
            String nickname = "카카오유저"; 
            if (jsonNode.has("properties") && jsonNode.get("properties").has("nickname")) {
                nickname = jsonNode.get("properties").get("nickname").asText();
            } else if (jsonNode.has("kakao_account") && jsonNode.get("kakao_account").has("profile")) {
                JsonNode profile = jsonNode.get("kakao_account").get("profile");
                if (profile.has("nickname")) {
                    nickname = profile.get("nickname").asText();
                }
            }

            System.out.println("====== 카카오에서 받아온 유저 정보 ======");
            System.out.println("카카오 고유 ID: " + kakaoId);
            System.out.println("카카오 실제 닉네임: " + nickname);
            System.out.println("=======================================");

            // 💡 [수정] ID와 닉네임을 Map에 담아서 둘 다 반환합니다.
            Map<String, String> userInfo = new HashMap<>();
            userInfo.put("kakaoId", kakaoId);
            userInfo.put("nickname", nickname);

            return userInfo; 

        } catch (Exception e) {
            System.out.println("카카오 응답 원본 JSON: " + response.getBody());
            throw new RuntimeException("카카오 유저 정보 파싱 실패", e);
        }
    }
}

