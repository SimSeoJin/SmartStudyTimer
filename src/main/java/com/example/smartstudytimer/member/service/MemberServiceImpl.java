package com.example.smartstudytimer.member.service;

import com.example.smartstudytimer.member.Entity.Member;
import com.example.smartstudytimer.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public String join(String id, String name, String phoneNumber, String password) {
        if (memberRepository.findById(id).isPresent()) {
            throw new IllegalStateException("이미 사용 중인 아이디입니다.");
        }

        Member member = Member.builder()
                .id(id)
                .name(name)
                .phoneNumber(phoneNumber)
                .password(passwordEncoder.encode(password))
                .build();

        memberRepository.save(member);
        return "success";
    }

    @Override
    public Member login(String id, String password) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이디입니다."));

        if (member.getPassword() == null || !passwordEncoder.matches(password, member.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        return member;
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