package com.hospital.portal.repository.jpa;

import com.hospital.portal.domain.entity.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    Optional<OtpToken> findTopByPhoneNumberAndUsedFalseOrderByCreatedAtDesc(String phoneNumber);

    @Modifying
    @Transactional
    @Query("UPDATE OtpToken o SET o.used = true WHERE o.phoneNumber = :phone AND o.used = false")
    void invalidateAllForPhone(@Param("phone") String phoneNumber);

    @Modifying
    @Transactional
    @Query("DELETE FROM OtpToken o WHERE o.expiresAt < :cutoff")
    void deleteExpiredBefore(@Param("cutoff") LocalDateTime cutoff);
}
