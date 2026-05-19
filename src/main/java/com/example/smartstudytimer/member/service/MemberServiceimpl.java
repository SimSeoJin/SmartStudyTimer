package com.example.smartstudytimer.member.service;

import com.example.smartstudytimer.member.Entity.Member;
import com.example.smartstudytimer.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor 
public class MemberServiceimpl implements MemberService{

    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public String join(String id, String name, String phoneNumber, String password) {
        Member member = Member.builder()
                .id(id)
                .name(name)
                .phoneNumber(phoneNumber)
                .password(password)
                .build();
        
        memberRepository.save(member);
        return "success";
    }
}
