package com.dohun.nfcreview.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * (5부) 카드 코드 생성기. 예: "k7Pq2xMz"
 *
 * SecureRandom: 다음 값을 예측할 수 없는 난수예요. 일반 Random은 규칙을 알면 다음 값을 맞힐 수 있어서,
 * 누가 남의 가게 카드 주소를 알아내 통계를 부풀릴 수 있어요.
 * 사람이 헷갈리는 글자(0/O/o, 1/l/I/i)는 뺐어요. 55가지 글자 × 8자리 ≈ 84조 가지.
 */
@Component
public class CardCodeGenerator {

    static final String ALPHABET = "23456789abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";
    static final int LENGTH = 8;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder code = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
