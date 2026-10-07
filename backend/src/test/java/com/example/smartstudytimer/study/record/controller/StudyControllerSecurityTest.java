package com.example.smartstudytimer.study.record.controller;

import com.example.smartstudytimer.member.config.MemberIdOwnershipInterceptor;
import com.example.smartstudytimer.member.config.SecurityConfig;
import com.example.smartstudytimer.member.config.WebConfig;
import com.example.smartstudytimer.member.jwt.JwtAuthenticationEntryPoint;
import com.example.smartstudytimer.member.jwt.JwtProvider;
import com.example.smartstudytimer.study.record.controller.dto.StudyRecordRequest;
import com.example.smartstudytimer.study.record.service.StudyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudyController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtProvider.class,
        WebConfig.class, MemberIdOwnershipInterceptor.class})
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-for-jwt-auth-filter-tests-32bytes-min",
        "jwt.expiration-ms=3600000"
})
class StudyControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private StudyService studyService;

    @Test
    void bodyMemberIdIsIgnoredInFavorOfAuthenticatedMemberId() throws Exception {
        String token = jwtProvider.generateToken(1L, "tester");
        when(studyService.recordStudy(any())).thenReturn("success");

        String body = """
                {"memberId":999,"startTime":"%s","endTime":"%s","studyMinutes":30}
                """.formatted(LocalDateTime.now().minusMinutes(30), LocalDateTime.now());

        mockMvc.perform(post("/study/record")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        var captor = org.mockito.ArgumentCaptor.forClass(StudyRecordRequest.class);
        verify(studyService).recordStudy(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(1L, captor.getValue().getMemberId());
    }

    @Test
    void withoutTokenReturns401() throws Exception {
        mockMvc.perform(post("/study/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
