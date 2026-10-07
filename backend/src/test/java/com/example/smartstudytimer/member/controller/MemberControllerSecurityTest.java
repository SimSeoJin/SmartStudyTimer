package com.example.smartstudytimer.member.controller;

import com.example.smartstudytimer.member.config.MemberIdOwnershipInterceptor;
import com.example.smartstudytimer.member.config.SecurityConfig;
import com.example.smartstudytimer.member.config.WebConfig;
import com.example.smartstudytimer.member.jwt.JwtAuthenticationEntryPoint;
import com.example.smartstudytimer.member.jwt.JwtProvider;
import com.example.smartstudytimer.member.service.MemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtProvider.class,
        WebConfig.class, MemberIdOwnershipInterceptor.class})
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-for-jwt-auth-filter-tests-32bytes-min",
        "jwt.expiration-ms=3600000"
})
class MemberControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private MemberService memberService;

    @Test
    void publicEndpointDoesNotRequireToken() throws Exception {
        mockMvc.perform(get("/hello"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpointWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/member/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithInvalidTokenReturns401() throws Exception {
        mockMvc.perform(get("/member/1").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithValidTokenIsAuthenticated() throws Exception {
        String token = jwtProvider.generateToken(1L, "tester");

        mockMvc.perform(get("/member/1").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void tokenForAnotherMemberCannotAccessSomeoneElsesPath() throws Exception {
        String token = jwtProvider.generateToken(1L, "tester");

        mockMvc.perform(get("/member/2").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
