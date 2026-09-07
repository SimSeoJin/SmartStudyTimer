package com.example.smartstudytimer.member.controller.dto;

import lombok.Data;

/**
 * 설정 화면의 회원정보 수정 요청. null 인 필드는 변경하지 않는다.
 */
@Data
public class MemberUpdateRequest {
    private String nickname;
    private String phoneNumber;
}
