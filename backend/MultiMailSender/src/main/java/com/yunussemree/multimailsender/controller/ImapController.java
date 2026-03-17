package com.yunussemree.multimailsender.controller;

import com.yunussemree.multimailsender.model.ApiResponse;
import com.yunussemree.multimailsender.service.ImapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/imap")
@RequiredArgsConstructor
public class ImapController {

    private final ImapService imapService;

    /**
     * Gmail INBOX'ini tarayarak geri dönen mailleri BOUNCED olarak işaretler.
     * Body: { "email": "...", "appPassword": "..." }
     */
    @PostMapping("/check-bounces")
    public ResponseEntity<ApiResponse> checkBounces(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String appPassword = body.get("appPassword");

        if (email == null || email.isBlank() || appPassword == null || appPassword.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("email ve appPassword zorunludur", null));
        }

        try {
            int updated = imapService.checkBounces(email.trim(), appPassword.trim());
            String msg = updated == 0
                    ? "Bounce bulunamadı. Tüm mailler iletilmiş görünüyor."
                    : updated + " başvuru BOUNCED olarak işaretlendi.";
            return ResponseEntity.ok(new ApiResponse(msg, updated));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(e.getMessage(), null));
        }
    }
}
