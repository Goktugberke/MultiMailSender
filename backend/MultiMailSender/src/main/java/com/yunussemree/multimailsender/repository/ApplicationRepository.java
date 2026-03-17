package com.yunussemree.multimailsender.repository;

import com.yunussemree.multimailsender.model.ApplicationRecord;
import com.yunussemree.multimailsender.model.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<ApplicationRecord, Long> {
    List<ApplicationRecord> findByStatus(ApplicationStatus status);
    List<ApplicationRecord> findByCompanyNameContainingIgnoreCase(String companyName);
    boolean existsByCompanyMailAndPositionTitle(String companyMail, String positionTitle);

    @Modifying
    @Transactional
    @Query("UPDATE ApplicationRecord a SET a.status = :status WHERE LOWER(a.companyMail) = :mail AND a.status = 'SENT'")
    int markBouncedByMail(@Param("mail") String mail, @Param("status") ApplicationStatus status);
}
