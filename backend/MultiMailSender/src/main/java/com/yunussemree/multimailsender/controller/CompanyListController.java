package com.yunussemree.multimailsender.controller;

import com.yunussemree.multimailsender.model.ApiResponse;
import com.yunussemree.multimailsender.model.CompanyListItem;
import com.yunussemree.multimailsender.service.CompanyListService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/companies")
@RequiredArgsConstructor
public class CompanyListController {

    private final CompanyListService service;

    @GetMapping
    public ResponseEntity<List<CompanyListItem>> getAll(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(service.getAll(city, search));
    }

    @GetMapping("/cities")
    public ResponseEntity<List<String>> getCities() {
        return ResponseEntity.ok(service.getCities());
    }

    @PostMapping
    public ResponseEntity<CompanyListItem> create(@RequestBody CompanyListItem item) {
        return ResponseEntity.ok(service.create(item));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompanyListItem> update(@PathVariable Long id, @RequestBody CompanyListItem patch) {
        return ResponseEntity.ok(service.update(id, patch));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * JSON dosyasından toplu içe aktarma.
     * Body: { "city": "ankara", "companies": [ { companyName, companyMail, ... } ] }
     *
     * Frontend, mevcut internalData JSON'larını parse ederek bu endpoint'e gönderir.
     */
    @PostMapping("/import")
    public ResponseEntity<ApiResponse> bulkImport(@RequestBody Map<String, Object> body) {
        String city = (String) body.get("city");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> companiesRaw = (List<Map<String, Object>>) body.get("companies");

        if (companiesRaw == null || companiesRaw.isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiResponse("Şirket listesi boş", null));
        }

        List<CompanyListItem> items = companiesRaw.stream().map(m -> {
            CompanyListItem item = new CompanyListItem();
            item.setCity(city);
            item.setCompanyName(str(m, "companyName"));
            item.setCompanyMail(str(m, "companyMail"));
            item.setCompanyWebsite(str(m, "companyWebsite"));
            item.setCompanyNumber(str(m, "companyNumber"));
            return item;
        }).toList();

        int count = service.bulkImport(items, city);
        return ResponseEntity.ok(new ApiResponse(count + " şirket içe aktarıldı (zaten olanlar atlandı)", count));
    }

    private String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString() : null;
    }
}
