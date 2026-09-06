package com.example.smartstudytimer.member.controller;

import com.example.smartstudytimer.member.Entity.Member;
import com.example.smartstudytimer.member.controller.dto.AuthResponse;
import com.example.smartstudytimer.member.jwt.JwtProvider;
import com.example.smartstudytimer.member.service.KakaoService;
import com.example.smartstudytimer.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
}