package com.example.smartstudytimer.member.controller;

import com.example.smartstudytimer.member.Entity.Member;
import com.example.smartstudytimer.member.controller.dto.AuthResponse;
import com.example.smartstudytimer.member.controller.dto.NaverTokenRequest;
import com.example.smartstudytimer.member.jwt.JwtProvider;
import com.example.smartstudytimer.member.service.MemberService;
import com.example.smartstudytimer.member.service.NaverService;
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
public class NaverController {

    private final NaverService naverService;
    private final MemberService memberService;
    private final JwtProvider jwtProvider;

    @GetMapping("/login/oauth2/code/naver")
    public ResponseEntity<AuthResponse> naverCallback(@RequestParam("code") String code,
                                                        @RequestParam("state") String state) {
        String accessToken = naverService.getAccessToken(code, state);
        return ResponseEntity.ok(login(accessToken));
    }

    // 모바일 앱(네이버 로그인 SDK)은 인가 코드가 아니라 네이버 액세스 토큰을 직접 들고 있으므로,
    // code→token 교환 없이 그 토큰으로 바로 사용자 정보를 조회한다.
    @PostMapping("/auth/naver")
    public ResponseEntity<AuthResponse> naverAppLogin(@RequestBody NaverTokenRequest request) {
        return ResponseEntity.ok(login(request.getAccessToken()));
    }

    private AuthResponse login(String accessToken) {
        Map<String, String> userInfo = naverService.getUserInfo(accessToken);
        Member loginMember = memberService.processSocialLogin(
                userInfo.get("naverId"), userInfo.get("nickname"), "naver");
        String jwt = jwtProvider.generateToken(loginMember.getMemberId(), loginMember.getId());
        return new AuthResponse(jwt, loginMember.getMemberId(), loginMember.getName());
    }
}
