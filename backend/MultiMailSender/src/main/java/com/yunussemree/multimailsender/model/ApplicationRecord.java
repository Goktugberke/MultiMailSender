package com.yunussemree.multimailsender.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@Table(name = "application_records")
public class ApplicationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String companyName;
    private String companyMail;
    private String positionTitle;

    private LocalDateTime sentAt;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status = ApplicationStatus.SENT;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private LocalDateTime examDate;
    private LocalDateTime interviewDate;
    private LocalDateTime followUpDate;

    public ApplicationRecord(String companyName, String companyMail, String positionTitle) {
        this.companyName = companyName;
        this.companyMail = companyMail;
        this.positionTitle = positionTitle;
        this.sentAt = LocalDateTime.now();
        this.status = ApplicationStatus.SENT;
    }
}
