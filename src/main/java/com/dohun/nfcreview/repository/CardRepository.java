package com.dohun.nfcreview.repository;

import com.dohun.nfcreview.domain.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {

    /**
     * (2부) 손님용: 켜져 있는 카드를 코드로 찾아요.
     * join fetch: 카드를 가져올 때 가게 정보까지 쿼리 한 번에 같이 가져와요.
     * open-in-view를 꺼뒀기 때문에, 미리 가져오지 않으면 나중에 가게 이름을 꺼낼 때 에러가 나요.
     */
    @Query("select c from Card c join fetch c.store where c.code = :code and c.active = true")
    Optional<Card> findActiveByCode(@Param("code") String code);

    // (5부) 관리자용: 메서드 이름만으로 쿼리가 만들어져요 → where store_id = ? order by id asc
    List<Card> findByStoreIdOrderByIdAsc(Long storeId);

    // (5부) 새 카드 코드가 이미 쓰이고 있는지
    boolean existsByCode(String code);
}
