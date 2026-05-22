package com.hospital.portal.repository.jpa;

import com.hospital.portal.domain.entity.PortalUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PortalUserRepository extends JpaRepository<PortalUser, Long> {

    Optional<PortalUser> findByPhoneNumber(String phoneNumber);

    Optional<PortalUser> findByPatientUid(String patientUid);

    Optional<PortalUser> findByPersonId(int personId);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByPersonId(int personId);
}
