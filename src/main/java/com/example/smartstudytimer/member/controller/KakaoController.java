package com.example.smartstudytimer.member.controller;

import com.example.smartstudytimer.member.Entity.Member;
import com.example.smartstudytimer.member.controller.dto.AuthResponse;
import com.example.smartstudytimer.member.controller.dto.KakaoTokenRequest;
import com.example.smartstudytimer.member.jwt.JwtProvider;
import com.example.smartstudytimer.member.service.KakaoService;
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
public class KakaoController {

    private final KakaoService kakaoService;
    private final MemberService memberService;
    private final JwtProvider jwtProvider;

    @GetMapping("/login/oauth2/code/kakao")
    public ResponseEntity<AuthResponse> kakaoCallback(@RequestParam("code") String code) {
        String accessToken = kakaoService.getAccessToken(code);
        Map<String, String> userInfo = kakaoService.getUserInfo(accessToken);
        String kakaoId = userInfo.get("kakaoId");
        String realNickname = userInfo.get("nickname");

        Member loginMember = memberService.processSocialLogin(kakaoId, realNickname, "kakao");
        String jwt = jwtProvider.generateToken(loginMember.getMemberId(), loginMember.getId());

        System.out.println("=========================================");
        System.out.println("소셜 로그인 성공 / 회원 번호: " + loginMember.getMemberId());
        System.out.println("사용자 이름: " + loginMember.getName());
        System.out.println("=========================================");

        return ResponseEntity.ok(new AuthResponse(jwt, loginMember.getMemberId(), loginMember.getName()));
    }

    // 모바일 앱(카카오 네이티브 SDK)은 인가 코드가 아니라 카카오 액세스 토큰을 직접 들고 있으므로,
    // code→token 교환 없이 그 토큰으로 바로 사용자 정보를 조회한다.
    @PostMapping("/auth/kakao")
    public ResponseEntity<AuthResponse> kakaoAppLogin(@RequestBody KakaoTokenRequest request) {
        Map<String, String> userInfo = kakaoService.getUserInfo(request.getAccessToken());
        String kakaoId = userInfo.get("kakaoId");
        String realNickname = userInfo.get("nickname");

        Member loginMember = memberService.processSocialLogin(kakaoId, realNickname, "kakao");
        String jwt = jwtProvider.generateToken(loginMember.getMemberId(), loginMember.getId());

        return ResponseEntity.ok(new AuthResponse(jwt, loginMember.getMemberId(), loginMember.getName()));
    }
}