package com.dohun.nfcreview.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * (2부) "그런 카드/가게 없어요". 이 예외가 컨트롤러 밖으로 나가면 스프링이 404 응답으로 바꿔줘요.
 * 브라우저에는 templates/error/404.html 화면이 보여요.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
