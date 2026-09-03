package com.example.smartstudytimer.member.service;

import com.example.smartstudytimer.member.Entity.Member;
import com.example.smartstudytimer.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;

    @Override
    public String join(String id, String name, String phoneNumber, String password) {
        return "success";
    }

    @Override
    @Transactional
    public Member processSocialLogin(String oauthId, String nickname, String provider) {
        return memberRepository.findByOauthProviderAndOauthId(provider, oauthId)
                .orElseGet(() -> {
                    System.out.println("신규 소셜 회원 감지: 자동 회원가입 진행");

                    Member newMember = Member.builder()
                            .name(nickname)
                            .oauthProvider(provider)
                            .oauthId(oauthId)
                            .build();

                    return memberRepository.saveAndFlush(newMember);
                });
    }
}