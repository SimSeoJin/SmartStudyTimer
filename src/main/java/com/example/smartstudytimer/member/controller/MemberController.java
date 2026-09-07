package com.example.smartstudytimer.member.controller;

import com.example.smartstudytimer.member.Entity.Member;
import com.example.smartstudytimer.member.controller.dto.AuthResponse;
import com.example.smartstudytimer.member.controller.dto.ErrorResponse;
import com.example.smartstudytimer.member.controller.dto.JoinRequest;
import com.example.smartstudytimer.member.controller.dto.LoginRequest;
import com.example.smartstudytimer.member.controller.dto.MemberUpdateRequest;
import com.example.smartstudytimer.member.jwt.JwtProvider;
import com.example.smartstudytimer.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final JwtProvider jwtProvider;

    @GetMapping("/hello")
    public String getHello(){
        return "hello";
    }

    @PostMapping("/join")
    public ResponseEntity<?> join(@RequestBody JoinRequest joinRequest){
        try {
            memberService.join(
                joinRequest.getId(),
                joinRequest.getName(),
                joinRequest.getPhoneNumber(),
                joinRequest.getPassword()
            );
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            Member member = memberService.login(loginRequest.getId(), loginRequest.getPassword());
            String accessToken = jwtProvider.generateToken(member.getMemberId(), member.getId());
            return ResponseEntity.ok(new AuthResponse(accessToken, member.getMemberId(), member.getName()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<?> getMemberInfo(@PathVariable Long memberId) {
        try {
            return ResponseEntity.ok(memberService.getMemberInfo(memberId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
        }
    }

    @PatchMapping("/member/{memberId}")
    public ResponseEntity<?> updateMemberInfo(@PathVariable Long memberId,
                                             @RequestBody MemberUpdateRequest request) {
        try {
            return ResponseEntity.ok(
                    memberService.updateMember(memberId, request.getNickname(), request.getPhoneNumber()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
        }
    }

}
