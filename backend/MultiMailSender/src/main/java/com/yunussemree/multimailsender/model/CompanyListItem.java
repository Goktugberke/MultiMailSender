package com.yunussemree.multimailsender.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "company_list_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyListItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String city;

    @Column(nullable = false)
    private String companyName;

    private String companyMail;
    private String companyWebsite;
    private String companyNumber;

    @Column(length = 1000)
    private String notes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime addedAt;

    @PrePersist
    void prePersist() {
        if (addedAt == null) addedAt = LocalDateTime.now();
    }
}
