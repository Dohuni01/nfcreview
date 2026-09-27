package com.dohun.nfcreview.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * (4부) 누가 어디에 들어올 수 있는지 정하는 곳.
 * 요청은 컨트롤러에 닿기 전에 이 규칙(필터 체인)을 먼저 통과해야 해요.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 규칙은 위에서부터 차례로 검사해요. 그래서 "예외(로그인 화면)"를 먼저 적어요.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/login").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().permitAll())            // 손님 주소(/t, /go), css, 헬스체크는 누구나
                .formLogin(form -> form
                        .loginPage("/admin/login")            // 우리가 만든 로그인 화면 (GET)
                        .loginProcessingUrl("/admin/login")   // 로그인 폼 제출 (POST) → 스프링 시큐리티가 처리
                        .defaultSuccessUrl("/admin/stores", true)
                        .failureUrl("/admin/login?error"))
                .logout(logout -> logout
                        .logoutUrl("/admin/logout")           // POST로만 동작해요 (CSRF 보호)
                        .logoutSuccessUrl("/admin/login?logout"));
        // CSRF 보호는 기본으로 켜져 있어요. Thymeleaf의 th:action이 폼에 토큰을 자동으로 넣어줘요.
        return http.build();
    }

    // 비밀번호를 BCrypt로 해시해요. 결과는 {bcrypt}$2a$10$... 모양이고, 원래 비밀번호로 되돌릴 수 없어요.
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    // 관리자는 나 한 명 → DB 테이블 대신 설정값(환경변수)으로 계정 하나를 메모리에 만들어요.
    @Bean
    UserDetailsService userDetailsService(AppProperties appProperties, PasswordEncoder passwordEncoder) {
        AppProperties.Admin admin = appProperties.admin();
        UserDetails user = User.withUsername(admin.username())
                .password(passwordEncoder.encode(admin.password()))
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(user);
    }
}
