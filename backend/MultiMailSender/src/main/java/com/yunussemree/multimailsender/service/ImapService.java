package com.yunussemree.multimailsender.service;

import com.yunussemree.multimailsender.model.ApplicationStatus;
import com.yunussemree.multimailsender.repository.ApplicationRepository;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.search.OrTerm;
import jakarta.mail.search.SubjectTerm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ImapService {

    private final ApplicationRepository applicationRepository;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}", Pattern.CASE_INSENSITIVE);

    /**
     * Gmail INBOX'ini tarayarak geri dönen mailleri bulur,
     * ilgili ApplicationRecord'un statusunu BOUNCED olarak günceller.
     *
     * @return Güncellenen kayıt sayısı
     */
    public int checkBounces(String email, String appPassword) {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", "imap.gmail.com");
        props.put("mail.imaps.port", "993");
        props.put("mail.imaps.ssl.enable", "true");
        props.put("mail.imaps.connectiontimeout", "10000");
        props.put("mail.imaps.timeout", "10000");

        int updatedCount = 0;

        try {
            Session session = Session.getInstance(props);
            Store store = session.getStore("imaps");
            store.connect("imap.gmail.com", email, appPassword);

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            // Geri dönme subject pattern'leri
            SearchTerm searchTerm = new OrTerm(new SearchTerm[]{
                    new SubjectTerm("Delivery Status Notification"),
                    new SubjectTerm("Mail delivery failed"),
                    new SubjectTerm("Undeliverable"),
                    new SubjectTerm("Delivery Failure"),
                    new SubjectTerm("Returned mail"),
                    new SubjectTerm("failure notice"),
                    new SubjectTerm("Undelivered Mail"),
                    new SubjectTerm("Message not delivered"),
                    new SubjectTerm("Mail Delivery System")
            });

            Message[] bounceMessages = inbox.search(searchTerm);

            for (Message msg : bounceMessages) {
                try {
                    String bodyText = getTextFromMessage(msg);
                    Set<String> foundEmails = extractEmails(bodyText);

                    for (String foundEmail : foundEmails) {
                        // Kendi adresimiz değilse ve DB'de kayıt varsa BOUNCED yap
                        if (!foundEmail.equalsIgnoreCase(email)) {
                            int updated = applicationRepository.markBouncedByMail(
                                    foundEmail.toLowerCase(), ApplicationStatus.BOUNCED);
                            updatedCount += updated;
                        }
                    }
                } catch (Exception ignored) {
                    // Tek mesaj parse hatası tüm işlemi durdurmaz
                }
            }

            inbox.close(false);
            store.close();

        } catch (AuthenticationFailedException e) {
            throw new RuntimeException("Gmail kimlik doğrulama hatası. App Password doğru mu?", e);
        } catch (Exception e) {
            throw new RuntimeException("IMAP bağlantı hatası: " + e.getMessage(), e);
        }

        return updatedCount;
    }

    private Set<String> extractEmails(String text) {
        Set<String> emails = new LinkedHashSet<>();
        if (text == null) return emails;
        Matcher m = EMAIL_PATTERN.matcher(text);
        while (m.find()) {
            emails.add(m.group().toLowerCase());
        }
        return emails;
    }

    private String getTextFromMessage(Message msg) throws Exception {
        if (msg.isMimeType("text/plain")) {
            return (String) msg.getContent();
        }
        if (msg.isMimeType("multipart/*")) {
            return getTextFromMultipart((Multipart) msg.getContent());
        }
        return "";
    }

    private String getTextFromMultipart(Multipart mp) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mp.getCount(); i++) {
            BodyPart bp = mp.getBodyPart(i);
            if (bp.isMimeType("text/plain")) {
                sb.append((String) bp.getContent());
            } else if (bp.isMimeType("multipart/*")) {
                sb.append(getTextFromMultipart((Multipart) bp.getContent()));
            }
        }
        return sb.toString();
    }
}
