package com.yunussemree.multimailsender.service;

import com.yunussemree.multimailsender.model.ApplicationRecord;
import com.yunussemree.multimailsender.model.ApplicationStatus;
import com.yunussemree.multimailsender.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository repository;

    public ApplicationRecord createFromMailSend(String companyName, String companyMail, String positionTitle) {
        ApplicationRecord record = new ApplicationRecord(companyName, companyMail, positionTitle);
        return repository.save(record);
    }

    public List<ApplicationRecord> getAll() {
        return repository.findAll();
    }

    public ApplicationRecord getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found: " + id));
    }

    public ApplicationRecord update(Long id, ApplicationRecord patch) {
        ApplicationRecord existing = getById(id);
        if (patch.getStatus() != null) existing.setStatus(patch.getStatus());
        if (patch.getNotes() != null) existing.setNotes(patch.getNotes());
        if (patch.getExamDate() != null) existing.setExamDate(patch.getExamDate());
        if (patch.getInterviewDate() != null) existing.setInterviewDate(patch.getInterviewDate());
        if (patch.getFollowUpDate() != null) existing.setFollowUpDate(patch.getFollowUpDate());
        if (patch.getPositionTitle() != null) existing.setPositionTitle(patch.getPositionTitle());
        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public List<ApplicationRecord> getByStatus(ApplicationStatus status) {
        return repository.findByStatus(status);
    }

    public Map<String, Long> getStats() {
        List<ApplicationRecord> all = repository.findAll();
        Map<String, Long> stats = all.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getStatus().name(),
                        Collectors.counting()
                ));
        stats.put("TOTAL", (long) all.size());

        // Yaklasan takip tarihleri (3 gun icinde)
        long upcomingFollowUps = all.stream()
                .filter(r -> r.getFollowUpDate() != null
                        && r.getFollowUpDate().isAfter(LocalDateTime.now())
                        && r.getFollowUpDate().isBefore(LocalDateTime.now().plusDays(3)))
                .count();
        stats.put("UPCOMING_FOLLOW_UPS", upcomingFollowUps);

        long upcomingExams = all.stream()
                .filter(r -> r.getExamDate() != null
                        && r.getExamDate().isAfter(LocalDateTime.now())
                        && r.getExamDate().isBefore(LocalDateTime.now().plusDays(7)))
                .count();
        stats.put("UPCOMING_EXAMS", upcomingExams);

        return stats;
    }
}
