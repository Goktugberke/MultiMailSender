package com.yunussemree.multimailsender.controller;

import com.yunussemree.multimailsender.model.ApiResponse;
import com.yunussemree.multimailsender.model.CvProfile;
import com.yunussemree.multimailsender.service.AiService;
import com.yunussemree.multimailsender.service.CvService;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/cv")
@RequiredArgsConstructor
public class CvController {

    private final CvService cvService;
    private final AiService aiService;

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        Optional<CvProfile> profile = cvService.getCurrentProfile();
        return profile.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(new ApiResponse("No CV uploaded yet", null)));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestPart("file") MultipartFile file) {
        try {
            CvProfile profile = cvService.uploadAndAnalyze(file);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse("Upload failed: " + e.getMessage(), null));
        }
    }

    @PostMapping("/reanalyze/{id}")
    public ResponseEntity<?> reanalyze(@PathVariable Long id) {
        try {
            CvProfile profile = cvService.reanalyze(id);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse("Reanalysis failed: " + e.getMessage(), null));
        }
    }

    @PostMapping("/generate-mail")
    public ResponseEntity<ApiResponse> generateMail(@RequestBody Map<String, String> body) {
        String companyName = body.getOrDefault("companyName", "");
        String positionHint = body.getOrDefault("positionHint", "Staj/İş Başvurusu");
        String cvAnalysis = body.getOrDefault("cvAnalysis", "");

        if (cvAnalysis.isBlank()) {
            Optional<CvProfile> profile = cvService.getCurrentProfile();
            if (profile.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse("CV profili bulunamadı. Önce CV yükleyin.", null));
            }
            cvAnalysis = profile.get().getAnalysisJson();
        }

        String mailBody = aiService.generateMailBody(cvAnalysis, companyName, positionHint);
        return ResponseEntity.ok(new ApiResponse("Mail generated", mailBody));
    }

    @PostMapping("/generate-mail-with-web")
    public ResponseEntity<ApiResponse> generateMailWithWeb(@RequestBody Map<String, String> body) {
        String companyName = body.getOrDefault("companyName", "");
        String websiteUrl  = body.getOrDefault("websiteUrl", "");
        String positionHint = body.getOrDefault("positionHint", "Staj/İş Başvurusu");
        String cvAnalysis  = body.getOrDefault("cvAnalysis", "");

        if (websiteUrl.isBlank()) {
            return ResponseEntity.badRequest().body(new ApiResponse("Website URL boş olamaz", null));
        }

        if (cvAnalysis.isBlank()) {
            Optional<CvProfile> profile = cvService.getCurrentProfile();
            if (profile.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse("CV profili bulunamadı. Önce CV yükleyin.", null));
            }
            cvAnalysis = profile.get().getAnalysisJson();
        }

        // Web sitesini scrape et
        String webContent;
        try {
            Document doc = Jsoup.connect(websiteUrl)
                    .userAgent("Mozilla/5.0")
                    .timeout(10_000)
                    .get();
            // Sadece anlamlı metni al, script/style/nav kaldır
            doc.select("script, style, nav, footer, header, aside, noscript").remove();
            String rawText = doc.body().text();
            // İlk 3000 karakteri al (token limiti için)
            webContent = rawText.length() > 3000 ? rawText.substring(0, 3000) + "..." : rawText;
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("Web sitesi açılamadı: " + e.getMessage(), null));
        }

        String mailBody = aiService.generateMailBodyWithWebContext(cvAnalysis, companyName, webContent, positionHint);
        return ResponseEntity.ok(new ApiResponse("Mail generated", mailBody));
    }

    @GetMapping("/ai-status")
    public ResponseEntity<ApiResponse> aiStatus() {
        boolean configured = aiService.isConfigured();
        return ResponseEntity.ok(new ApiResponse(
                configured ? "Gemini API configured" : "Gemini API key not set",
                configured));
    }
}
