package com.example.smartstudytimer.member.repository;

import com.example.smartstudytimer.member.Entity.Member;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class MemberReposiotryTest {

    // 컨테이너에서 레파지토리 bean을 의존성 주입
    @Autowired
    private MemberRepository memberRepository;

    @Test
    public void crudTest(){
            Member member = Member.builder().name("seojin").phoneNumber("010").build();

        // 회원 생성 테스트
        memberRepository.save(member);

        // get test
        Member foundMember = memberRepository.findById(1L).get();

    }


}
