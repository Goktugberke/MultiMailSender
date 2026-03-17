package com.yunussemree.multimailsender.repository;

import com.yunussemree.multimailsender.model.CompanyListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyListRepository extends JpaRepository<CompanyListItem, Long> {
    List<CompanyListItem> findByCityIgnoreCase(String city);
    List<CompanyListItem> findByCompanyNameContainingIgnoreCase(String name);
    boolean existsByCompanyMailIgnoreCase(String companyMail);
    List<CompanyListItem> findAllByOrderByCityAscCompanyNameAsc();
}
