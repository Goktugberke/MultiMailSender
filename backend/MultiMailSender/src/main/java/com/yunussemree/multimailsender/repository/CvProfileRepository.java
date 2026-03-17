package com.yunussemree.multimailsender.repository;

import com.yunussemree.multimailsender.model.CvProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CvProfileRepository extends JpaRepository<CvProfile, Long> {
    // En son yüklenen CV profilini al (tek profil sistemi)
    Optional<CvProfile> findTopByOrderByUploadedAtDesc();
}
