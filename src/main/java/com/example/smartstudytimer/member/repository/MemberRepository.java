package com.example.smartstudytimer.member.repository;

import com.example.smartstudytimer.member.Entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

}
