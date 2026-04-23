package com.example.smartstudytimer.member.Entity;

import jakarta.persistence.*;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;
import java.util.List;

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
    private Long index;

    @Column(name = "login_id", nullable = false, unique = true) 
    private String id;

    @Column(name = "nickname", nullable = false, unique = true) 
    private String name;

    @Column(name = "phone_number") 
    private String phoneNumber;

    @Column(name = "password", nullable = false) 
    private String password;


}
