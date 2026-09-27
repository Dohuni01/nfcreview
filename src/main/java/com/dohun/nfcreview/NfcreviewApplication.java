package com.dohun.nfcreview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan // (4부) AppProperties 같은 설정 클래스를 자동으로 찾아 등록해요
public class NfcreviewApplication {

    public static void main(String[] args) {
        SpringApplication.run(NfcreviewApplication.class, args);
    }
}
