package com.example.smartstudytimer.member.controller;

import com.example.smartstudytimer.member.Entity.Member;
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

    @GetMapping("/login/oauth2/code/kakao")
    public ResponseEntity<String> kakaoCallback(@RequestParam("code") String code) {
        String accessToken = kakaoService.getAccessToken(code);
        Map<String, String> userInfo = kakaoService.getUserInfo(accessToken);
        String kakaoId = userInfo.get("kakaoId");
        String realNickname = userInfo.get("nickname");

        Member loginMember = memberService.processSocialLogin(kakaoId, realNickname, "kakao");

        System.out.println("=========================================");
        System.out.println("소셜 로그인 성공 / 회원 번호: " + loginMember.getMemberId());
        System.out.println("사용자 이름: " + loginMember.getName());
        System.out.println("=========================================");

        return ResponseEntity.ok("카카오 로그인 최종 완료 / 회원 일련번호: " + loginMember.getMemberId() + ", 이름: " + loginMember.getName());
    }
}