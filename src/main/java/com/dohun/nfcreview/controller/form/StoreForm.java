package com.dohun.nfcreview.controller.form;

import com.dohun.nfcreview.domain.LandingMode;
import com.dohun.nfcreview.domain.Store;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * (5부) 가게 등록/수정 폼의 입력값.
 * 엔티티(Store)를 폼에 직접 쓰지 않는 이유: 화면 입력 규칙과 DB 규칙은 따로 바뀌고,
 * 엔티티에 setter를 열면 아무 데서나 값이 바뀔 수 있어요.
 * th:field는 getter/setter로 값을 읽고 쓰기 때문에 record가 아니라 일반 클래스로 만들었어요.
 */
public class StoreForm {

    @NotBlank(message = "가게 이름을 적어주세요.")
    @Size(max = 100, message = "가게 이름은 100자 이내로 적어주세요.")
    private String name;

    @NotBlank(message = "네이버 리뷰 주소를 붙여넣어 주세요.")
    @Size(max = 1000, message = "주소가 너무 길어요.")
    @Pattern(regexp = "https://\\S+", message = "https://로 시작하는 주소를 공백 없이 넣어주세요.")
    private String reviewUrl;

    @NotNull(message = "랜딩 모드를 선택해주세요.")
    private LandingMode landingMode;

    @Size(max = 1000, message = "주소가 너무 길어요.")
    private String landingUrl;

    public static StoreForm from(Store store) {
        StoreForm form = new StoreForm();
        form.setName(store.getName());
        form.setReviewUrl(store.getReviewUrl());
        form.setLandingMode(store.getLandingMode());
        form.setLandingUrl(store.getLandingUrl());
        return form;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getReviewUrl() { return reviewUrl; }
    public void setReviewUrl(String reviewUrl) { this.reviewUrl = reviewUrl; }

    public LandingMode getLandingMode() { return landingMode; }
    public void setLandingMode(LandingMode landingMode) { this.landingMode = landingMode; }

    public String getLandingUrl() { return landingUrl; }
    public void setLandingUrl(String landingUrl) { this.landingUrl = landingUrl; }
}
