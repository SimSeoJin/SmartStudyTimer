package com.example.smartstudytimer.member.service;

import com.example.smartstudytimer.member.Entity.Member;

public interface MemberService {
    String join(String id, String name, String phoneNumber, String password);

    Member login(String id, String password);

    Member processSocialLogin(String oauthId, String nickname, String provider);
}