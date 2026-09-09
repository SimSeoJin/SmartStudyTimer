package com.example.smartstudytimer.member.repository;

import com.example.smartstudytimer.member.Entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findById(String id);
    Optional<Member> findByOauthProviderAndOauthId(String oauthProvider, String oauthId);

    // Member.name 필드는 DB의 nickname 컬럼에 매핑된다(닉네임 중복 확인용).
    boolean existsByName(String name);
}