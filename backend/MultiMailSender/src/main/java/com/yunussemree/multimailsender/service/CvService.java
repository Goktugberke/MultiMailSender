package com.yunussemree.multimailsender.service;

import com.yunussemree.multimailsender.model.CvProfile;
import com.yunussemree.multimailsender.repository.CvProfileRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CvService {

    private final CvProfileRepository repository;
    private final AiService aiService;

    public Optional<CvProfile> getCurrentProfile() {
        return repository.findTopByOrderByUploadedAtDesc();
    }

    /**
     * CV PDF'ini yukler, metni cikarir ve Gemini ile analiz eder.
     */
    public CvProfile uploadAndAnalyze(MultipartFile file) throws IOException {
        String rawText = extractTextFromPdf(file);

        CvProfile profile = new CvProfile();
        profile.setFileName(file.getOriginalFilename());
        profile.setRawText(rawText);
        profile.setUploadedAt(LocalDateTime.now());

        // Gemini ile analiz et
        String analysis = aiService.analyzeCv(rawText);
        profile.setAnalysisJson(analysis);
        profile.setAnalysisUpdatedAt(LocalDateTime.now());

        return repository.save(profile);
    }

    /**
     * Mevcut CV profilini Gemini ile yeniden analiz eder.
     */
    public CvProfile reanalyze(Long id) {
        CvProfile profile = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("CV profile not found"));
        String analysis = aiService.analyzeCv(profile.getRawText());
        profile.setAnalysisJson(analysis);
        profile.setAnalysisUpdatedAt(LocalDateTime.now());
        return repository.save(profile);
    }

    private String extractTextFromPdf(MultipartFile file) throws IOException {
        try (PDDocument doc = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }
}
