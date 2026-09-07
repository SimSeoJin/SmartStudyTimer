package com.example.smartstudytimer.member.controller.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberInfoResponse {
    private Long memberId;
    private String nickname;
    private String loginId;
    private String phoneNumber;
    private String oauthProvider;
}
