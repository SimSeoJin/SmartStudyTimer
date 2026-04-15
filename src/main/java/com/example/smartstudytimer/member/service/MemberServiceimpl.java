package com.example.smartstudytimer.member.service;

import com.example.smartstudytimer.member.Entity.Member;
import com.example.smartstudytimer.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor // 생성자를 통해 의존성 주입 어노테이션
public class MemberServiceimpl implements MemberService{

    private final MemberRepository memberRepository;

    @Override
    public String join(String id, String name, String phoneNumber) {
        Member member = Member.builder().id(id).name(name).phoneNumber(phoneNumber).build();
        memberRepository.save(member);
        return "sucess";
    }
}
