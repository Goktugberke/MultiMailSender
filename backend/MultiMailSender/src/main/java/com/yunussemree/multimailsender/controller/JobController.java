package com.yunussemree.multimailsender.controller;

import com.yunussemree.multimailsender.model.JobListing;
import com.yunussemree.multimailsender.service.JobScraperService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobScraperService jobScraperService;

    @GetMapping
    public ResponseEntity<List<JobListing>> getJobs(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(jobScraperService.getAllJobs(q, category));
    }

    @PostMapping("/refresh")
    public ResponseEntity<List<JobListing>> refresh(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(jobScraperService.forceRefresh(q, category));
    }
}
