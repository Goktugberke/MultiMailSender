package com.yunussemree.multimailsender.controller;

import com.yunussemree.multimailsender.model.ApplicationRecord;
import com.yunussemree.multimailsender.model.ApplicationStatus;
import com.yunussemree.multimailsender.model.ApiResponse;
import com.yunussemree.multimailsender.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @GetMapping
    public ResponseEntity<List<ApplicationRecord>> getAll(
            @RequestParam(required = false) String status) {
        if (status != null && !status.isBlank()) {
            try {
                ApplicationStatus s = ApplicationStatus.valueOf(status.toUpperCase());
                return ResponseEntity.ok(applicationService.getByStatus(s));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }
        return ResponseEntity.ok(applicationService.getAll());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        return ResponseEntity.ok(applicationService.getStats());
    }

    @PostMapping
    public ResponseEntity<ApplicationRecord> create(@RequestBody ApplicationRecord record) {
        ApplicationRecord created = applicationService.createFromMailSend(
                record.getCompanyName(),
                record.getCompanyMail(),
                record.getPositionTitle()
        );
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApplicationRecord> update(
            @PathVariable Long id,
            @RequestBody ApplicationRecord patch) {
        try {
            return ResponseEntity.ok(applicationService.update(id, patch));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id) {
        applicationService.delete(id);
        return ResponseEntity.ok(new ApiResponse("Deleted", null));
    }
}
