package com.yunussemree.multimailsender.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AiService {

    @Value("${gemini.api.key:}")
    private String apiKey;

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && !apiKey.equals("YOUR_GEMINI_API_KEY_HERE");
    }

    /**
     * CV metnini analiz eder ve JSON formatinda kullanici profili cikarir.
     */
    public String analyzeCv(String cvText) {
        if (!isConfigured()) return "{\"error\": \"Gemini API key not configured\"}";

        String prompt = """
                Asagidaki CV metnini analiz et ve asagidaki JSON formatinda cevap ver.
                Yaniti SADECE JSON olarak ver, baska hicbir sey ekleme.

                {
                  "name": "Ad Soyad",
                  "email": "email@example.com",
                  "phone": "telefon",
                  "location": "sehir",
                  "summary": "kisa ozet (2-3 cumle)",
                  "skills": ["skill1", "skill2", "..."],
                  "languages": ["Turkce", "Ingilizce", "..."],
                  "education": [
                    {"degree": "bolum", "school": "universite", "year": "2020-2024"}
                  ],
                  "experience": [
                    {"title": "unvan", "company": "sirket", "duration": "sure", "description": "aciklama"}
                  ],
                  "projects": [
                    {"name": "proje adi", "description": "aciklama", "technologies": ["tech1"]}
                  ]
                }

                CV Metni:
                """ + cvText;

        return callGemini(prompt);
    }

    /**
     * CV profili ve sirket bilgisiyle sirket ozel mail govdesi olusturur.
     */
    public String generateMailBody(String cvAnalysisJson, String companyName, String positionHint) {
        if (!isConfigured()) return "Gemini API key not configured. Please add your key to application.properties.";

        String prompt = """
                Asagidaki kullanici profilini ve sirket bilgisini kullanarak profesyonel bir is/staj basvuru maili yaz.
                Mail Turkce olmali, samimi ama profesyonel bir ton kullanmali.

                Sirket Adi: %s
                Pozisyon/Konu: %s

                Kullanici Profili (JSON):
                %s

                SADECE mail govdesini yaz (Dear ... ile baslayan, imza ile biten).
                Subject satiri ekleme. Markdown kullanma.
                """.formatted(companyName, positionHint, cvAnalysisJson);

        return callGemini(prompt);
    }

    /**
     * CV'yi sirketin web sitesiyle eslestirip ozel bir mail olusturur.
     */
    public String generateMailBodyWithWebContext(String cvAnalysisJson, String companyName,
                                                  String companyWebContent, String positionHint) {
        if (!isConfigured()) return "Gemini API key not configured.";

        String prompt = """
                Asagidaki kullanici profilini ve sirket bilgisini kullanarak profesyonel bir is/staj basvuru maili yaz.
                Sirketin web sitesinden cekilen bilgileri kullanarak maili sirket ozelinde kisisellestir.
                Mail Turkce olmali, samimi ama profesyonel bir ton kullanmali.

                Sirket Adi: %s
                Pozisyon/Konu: %s

                Sirket Hakkinda (web sitesinden):
                %s

                Kullanici Profili (JSON):
                %s

                SADECE mail govdesini yaz (Dear ... ile baslayan, imza ile biten).
                Subject satiri ekleme. Markdown kullanma.
                """.formatted(companyName, positionHint, companyWebContent, cvAnalysisJson);

        return callGemini(prompt);
    }

    private String callGemini(String prompt) {
        try {
            String url = GEMINI_URL + apiKey;
            String requestBody = """
                    {
                      "contents": [{
                        "parts": [{"text": %s}]
                      }],
                      "generationConfig": {
                        "temperature": 0.7,
                        "maxOutputTokens": 2048
                      }
                    }
                    """.formatted(toJsonString(prompt));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            return extractGeminiText(response.getBody());
        } catch (Exception e) {
            return "AI service error: " + e.getMessage();
        }
    }

    private String extractGeminiText(String responseBody) {
        if (responseBody == null) return "";
        // Simple extraction: find "text":"..." pattern
        int textIdx = responseBody.indexOf("\"text\":");
        if (textIdx == -1) return responseBody;
        int start = responseBody.indexOf("\"", textIdx + 7) + 1;
        int end = findJsonStringEnd(responseBody, start);
        if (start <= 0 || end <= start) return responseBody;
        return unescapeJson(responseBody.substring(start, end));
    }

    private int findJsonStringEnd(String s, int from) {
        for (int i = from; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') { i++; continue; }
            if (c == '"') return i;
        }
        return -1;
    }

    private String unescapeJson(String s) {
        return s.replace("\\n", "\n").replace("\\t", "\t").replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private String toJsonString(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "") + "\"";
    }
}
