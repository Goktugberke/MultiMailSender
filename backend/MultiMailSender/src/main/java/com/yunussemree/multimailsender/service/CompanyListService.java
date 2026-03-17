package com.yunussemree.multimailsender.service;

import com.yunussemree.multimailsender.model.CompanyListItem;
import com.yunussemree.multimailsender.repository.CompanyListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanyListService {

    private final CompanyListRepository repository;

    public List<CompanyListItem> getAll(String city, String search) {
        if (city != null && !city.isBlank()) {
            return repository.findByCityIgnoreCase(city);
        }
        if (search != null && !search.isBlank()) {
            return repository.findByCompanyNameContainingIgnoreCase(search);
        }
        return repository.findAllByOrderByCityAscCompanyNameAsc();
    }

    public CompanyListItem create(CompanyListItem item) {
        return repository.save(item);
    }

    public CompanyListItem update(Long id, CompanyListItem patch) {
        CompanyListItem existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Şirket bulunamadı: " + id));
        if (patch.getCompanyName() != null) existing.setCompanyName(patch.getCompanyName());
        if (patch.getCompanyMail() != null) existing.setCompanyMail(patch.getCompanyMail());
        if (patch.getCompanyWebsite() != null) existing.setCompanyWebsite(patch.getCompanyWebsite());
        if (patch.getCompanyNumber() != null) existing.setCompanyNumber(patch.getCompanyNumber());
        if (patch.getCity() != null) existing.setCity(patch.getCity());
        if (patch.getNotes() != null) existing.setNotes(patch.getNotes());
        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    /**
     * JSON içe aktarma — frontend tarafından parse edilen JSON gönderilir.
     * Zaten var olan mailler (duplicate) atlanır.
     *
     * @param items  import edilecek şirketler
     * @param city   şehir adı (dosya adından veya kullanıcı girişinden)
     * @return kaç kayıt eklendiği
     */
    public int bulkImport(List<CompanyListItem> items, String city) {
        int count = 0;
        for (CompanyListItem item : items) {
            if (item.getCompanyMail() != null && !item.getCompanyMail().isBlank()) {
                if (!repository.existsByCompanyMailIgnoreCase(item.getCompanyMail().trim())) {
                    item.setId(null);
                    if (item.getCity() == null || item.getCity().isBlank()) {
                        item.setCity(city != null ? city : "Bilinmiyor");
                    }
                    repository.save(item);
                    count++;
                }
            }
        }
        return count;
    }

    /** Distinct city listesi */
    public List<String> getCities() {
        return repository.findAllByOrderByCityAscCompanyNameAsc()
                .stream()
                .map(CompanyListItem::getCity)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
}
