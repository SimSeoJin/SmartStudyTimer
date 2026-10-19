package com.example.smartstudytimer.member.controller;

import com.example.smartstudytimer.member.Entity.Member;
import com.example.smartstudytimer.member.controller.dto.AuthResponse;
import com.example.smartstudytimer.member.controller.dto.GoogleTokenRequest;
import com.example.smartstudytimer.member.jwt.JwtProvider;
import com.example.smartstudytimer.member.service.GoogleService;
import com.example.smartstudytimer.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class GoogleController {

    private final GoogleService googleService;
    private final MemberService memberService;
    private final JwtProvider jwtProvider;

    @GetMapping("/login/oauth2/code/google")
    public ResponseEntity<AuthResponse> googleCallback(@RequestParam("code") String code) {
        String accessToken = googleService.getAccessToken(code);
        return ResponseEntity.ok(login(accessToken));
    }

    // 모바일 앱(구글 로그인 SDK)은 인가 코드가 아니라 구글 액세스 토큰을 직접 들고 있으므로,
    // code→token 교환 없이 그 토큰으로 바로 사용자 정보를 조회한다.
    @PostMapping("/auth/google")
    public ResponseEntity<AuthResponse> googleAppLogin(@RequestBody GoogleTokenRequest request) {
        return ResponseEntity.ok(login(request.getAccessToken()));
    }

    private AuthResponse login(String accessToken) {
        Map<String, String> userInfo = googleService.getUserInfo(accessToken);
        Member loginMember = memberService.processSocialLogin(
                userInfo.get("googleId"), userInfo.get("nickname"), "google");
        String jwt = jwtProvider.generateToken(loginMember.getMemberId(), loginMember.getId());
        return new AuthResponse(jwt, loginMember.getMemberId(), loginMember.getName());
    }
}
