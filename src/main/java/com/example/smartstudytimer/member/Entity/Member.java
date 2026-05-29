package com.example.smartstudytimer.member.Entity;

import jakarta.persistence.*;
import lombok.*;

@NoArgsConstructor
@Builder
@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "member")
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    
    @Column(name = "member_id") 
    private Long memberId;

    @Column(name = "oauth_provider")
    private String oauthProvider;

    @Column(name = "oauth_id")
    private String oauthId;

    @Column(name = "login_id", unique = true) 
    private String id;

    @Column(name = "nickname", nullable = false, unique = true) 
    private String name;

    @Column(name = "phone_number") 
    private String phoneNumber;

    @Column(name = "password") 
    private String password;

    
}