package com.dohun.nfcreview.repository;

import com.dohun.nfcreview.domain.Store;
import org.springframework.data.jpa.repository.JpaRepository;

// (5부) 상속만 하면 save, findById, findAll 같은 기본 기능을 스프링이 만들어줘요
public interface StoreRepository extends JpaRepository<Store, Long> {
}
