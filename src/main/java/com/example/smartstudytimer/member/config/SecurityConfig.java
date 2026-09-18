package com.example.smartstudytimer.member.config;

import com.example.smartstudytimer.member.jwt.JwtAuthenticationEntryPoint;
import com.example.smartstudytimer.member.jwt.JwtAuthenticationFilter;
import com.example.smartstudytimer.member.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // 로그인 전 단계라 토큰이 없는 엔드포인트만 예외로 둔다.
    private static final String[] PUBLIC_PATHS = {
            "/hello", "/join", "/login",
            "/login/oauth2/code/kakao", "/auth/kakao"
    };

    private final JwtProvider jwtProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
