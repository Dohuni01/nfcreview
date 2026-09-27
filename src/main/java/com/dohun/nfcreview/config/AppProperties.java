package com.dohun.nfcreview.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

// (4부) application.yml의 app.* 값을 자바 객체로 받아요.
//   app.base-url       → baseUrl()
//   app.admin.username → admin().username()
// 검증 규칙에 안 맞으면(빈 값, 8자 미만 비밀번호) 앱이 아예 안 켜져요.
// 운영에서 틀린 값으로 조용히 켜지는 것보다 훨씬 안전해요.
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(@NotBlank String baseUrl, @NotNull @Valid Admin admin) {

    public AppProperties {
        // "https://도메인/"처럼 끝에 /를 붙여도 "https://도메인//t/..."가 되지 않게 잘라요
        if (baseUrl != null && baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
    }

    public record Admin(@NotBlank String username, @NotBlank @Size(min = 8) String password) {
    }
}
