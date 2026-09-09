package com.example.smartstudytimer.member.service;

import com.example.smartstudytimer.member.Entity.Member;
import com.example.smartstudytimer.member.controller.dto.MemberInfoResponse;
import com.example.smartstudytimer.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    // nickname 컬럼 길이 제한(varchar(45))
    private static final int NICKNAME_MAX_LENGTH = 45;
    private static final String DEFAULT_SOCIAL_NICKNAME = "카카오유저";

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public String join(String id, String name, String phoneNumber, String password) {
        if (memberRepository.findById(id).isPresent()) {
            throw new IllegalStateException("이미 사용 중인 아이디입니다.");
        }
        if (memberRepository.existsByName(name)) {
            throw new IllegalStateException("이미 사용 중인 닉네임입니다.");
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
                            .name(resolveUniqueNickname(nickname))
                            .oauthProvider(provider)
                            .oauthId(oauthId)
                            .build();

                    return memberRepository.saveAndFlush(newMember);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public MemberInfoResponse getMemberInfo(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        return toInfoResponse(member);
    }

    @Override
    @Transactional
    public MemberInfoResponse updateMember(Long memberId, String nickname, String phoneNumber) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        if (nickname != null && !nickname.isBlank() && !nickname.trim().equals(member.getName())) {
            if (memberRepository.existsByName(nickname.trim())) {
                throw new IllegalStateException("이미 사용 중인 닉네임입니다.");
            }
            member.setName(nickname.trim());
        }
        if (phoneNumber != null) {
            member.setPhoneNumber(phoneNumber.isBlank() ? null : phoneNumber.trim());
        }

        // @Transactional 안이라 dirty checking으로 flush 시점에 UPDATE 반영됨.
        return toInfoResponse(member);
    }

    private MemberInfoResponse toInfoResponse(Member member) {
        return MemberInfoResponse.builder()
                .memberId(member.getMemberId())
                .nickname(member.getName())
                .loginId(member.getId())
                .phoneNumber(member.getPhoneNumber())
                .oauthProvider(member.getOauthProvider())
                .build();
    }

    // 카카오에서 받아온 닉네임이 이미 다른 회원이 쓰고 있으면 그대로 저장 시 nickname UNIQUE 제약에 걸려
    // 500이 난다. 충돌 시 뒤에 숫자 suffix를 붙여 사용 가능한 닉네임을 만들어 반환한다.
    private String resolveUniqueNickname(String desired) {
        String base = (desired == null || desired.isBlank()) ? DEFAULT_SOCIAL_NICKNAME : desired.trim();
        if (base.length() > NICKNAME_MAX_LENGTH) {
            base = base.substring(0, NICKNAME_MAX_LENGTH);
        }
        if (!memberRepository.existsByName(base)) {
            return base;
        }

        for (int attempt = 0; attempt < 100; attempt++) {
            String suffix = "_" + ThreadLocalRandom.current().nextInt(1000, 10000);
            String prefix = base.length() + suffix.length() > NICKNAME_MAX_LENGTH
                    ? base.substring(0, NICKNAME_MAX_LENGTH - suffix.length())
                    : base;
            String candidate = prefix + suffix;
            if (!memberRepository.existsByName(candidate)) {
                return candidate;
            }
        }

        // 극히 드문 경우의 최후 수단.
        return (DEFAULT_SOCIAL_NICKNAME + "_" + System.currentTimeMillis());
    }
}
