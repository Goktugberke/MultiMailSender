package com.yunussemree.multimailsender.model;

public enum ApplicationStatus {
    SENT,        // Mail gönderildi
    BOUNCED,     // Mail geri döndü (geçersiz adres)
    REPLIED,     // Cevap geldi
    EXAM,        // Sınav aşamasında
    INTERVIEW,   // Mülakat aşamasında
    REJECTED,    // Reddedildi
    ACCEPTED,    // Kabul edildi
    WITHDRAWN    // Başvuru geri çekildi
}
