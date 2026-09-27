package com.dohun.nfcreview.controller;

import java.nio.charset.StandardCharsets;

/**
 * (2부) 리다이렉트할 주소를 HTTP 헤더(Location)에 그대로 넣어도 안전한 모양으로 바꿔요.
 *
 * 왜 필요한가요?
 *  1. HTTP 헤더에는 한글을 그대로 넣을 수 없어요. 사장님이 "...?query=빨간주막"처럼 한글이 든 주소를 붙여넣으면
 *     Tomcat이 Location 헤더를 통째로 지워버려요. 손님은 302만 받고 빈 화면에 멈추는데,
 *     CLICK은 정상 기록되니까 통계만 봐서는 아무도 눈치채지 못해요.
 *  2. 스프링의 "redirect:주소"는 {이름}을 URI 템플릿 변수로 해석해요.
 *     주소에 {foo}가 있으면 500 에러, {code}가 있으면 카드 코드로 몰래 바뀌어버려요.
 *
 * 그래서 주소에 그대로 써도 되는 글자(RFC 3986)는 남기고, 나머지(한글, 공백, 중괄호 등)만
 * UTF-8 퍼센트 인코딩(예: 빨 → %EB%B9%A8)해요. 이미 인코딩된 %XX는 그대로 두니까 두 번 인코딩되지 않아요.
 * 브라우저 주소창에 한글 주소를 붙여넣었을 때 브라우저가 하는 일과 같아요.
 */
final class RedirectUrls {

    // 영문자, 숫자, 주소에 쓰이는 기호들. %는 이미 인코딩된 부분(%EB 등)을 지키려고 넣었어요.
    private static final String ALLOWED =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~:/?#[]@!$&'()*+,;=%";
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private RedirectUrls() {
    }

    static String headerSafe(String url) {
        StringBuilder out = new StringBuilder(url.length());
        // 글자가 아니라 UTF-8 바이트 단위로 봐요. 한글 한 글자는 3바이트이고, 전부 128 이상이라 인코딩 대상이에요.
        for (byte b : url.getBytes(StandardCharsets.UTF_8)) {
            int c = b & 0xFF;
            if (c < 128 && ALLOWED.indexOf(c) >= 0) {
                out.append((char) c);
            } else {
                out.append('%').append(HEX[c >> 4]).append(HEX[c & 0x0F]);
            }
        }
        return out.toString();
    }
}
