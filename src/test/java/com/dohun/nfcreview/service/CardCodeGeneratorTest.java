package com.dohun.nfcreview.service;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** (7부) 카드 코드 규칙 검사 */
class CardCodeGeneratorTest {

    private final CardCodeGenerator generator = new CardCodeGenerator();

    @Test
    void 코드는_8자리이고_헷갈리는_글자가_없다() {
        for (int i = 0; i < 1_000; i++) {
            // 2~9, 소문자(i, l, o 제외), 대문자(I, O 제외)만
            assertThat(generator.generate()).matches("[2-9a-hjkmnp-zA-HJ-NP-Z]{8}");
        }
    }

    @Test
    void 만_개를_만들어도_겹치지_않는다() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            codes.add(generator.generate());
        }
        assertThat(codes).hasSize(10_000);
    }
}
