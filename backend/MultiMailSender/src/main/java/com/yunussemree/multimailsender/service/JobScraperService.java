package com.yunussemree.multimailsender.service;

import com.yunussemree.multimailsender.model.JobListing;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Service
public class JobScraperService {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0 Safari/537.36";
    private static final int TIMEOUT_MS = 12_000;

    private final RestTemplate restTemplate = new RestTemplate();

    // Basit in-memory cache: 30 dakika gecerli
    private List<JobListing> cachedListings = new ArrayList<>();
    private Instant cacheTime = Instant.EPOCH;
    private static final long CACHE_MINUTES = 30;

    public List<JobListing> getAllJobs(String query, String category) {
        if (isCacheValid()) {
            return filterListings(cachedListings, query, category);
        }
        return scrapeAll(query, category);
    }

    public List<JobListing> forceRefresh(String query, String category) {
        cacheTime = Instant.EPOCH;
        return scrapeAll(query, category);
    }

    private boolean isCacheValid() {
        return !cachedListings.isEmpty() &&
                Instant.now().isBefore(cacheTime.plusSeconds(CACHE_MINUTES * 60));
    }

    private List<JobListing> scrapeAll(String query, String category) {
        ExecutorService executor = Executors.newFixedThreadPool(6);
        List<Future<List<JobListing>>> futures = new ArrayList<>();

        futures.add(executor.submit(this::scrapeRementist));
        futures.add(executor.submit(this::scrapeYouthAll));
        futures.add(executor.submit(this::scrapeSavunmaKariyer));
        futures.add(executor.submit(this::scrapeKariyerNet));
        futures.add(executor.submit(this::scrapeIndeedRss));
        futures.add(executor.submit(this::scrapeSecretCv));
        futures.add(executor.submit(this::scrapeYenibiris));
        futures.add(executor.submit(this::scrapeEleman));

        List<JobListing> all = new ArrayList<>();
        for (Future<List<JobListing>> f : futures) {
            try {
                all.addAll(f.get(15, TimeUnit.SECONDS));
            } catch (Exception ignored) {}
        }
        executor.shutdown();

        cachedListings = all;
        cacheTime = Instant.now();
        return filterListings(all, query, category);
    }

    private List<JobListing> filterListings(List<JobListing> listings, String query, String category) {
        if ((query == null || query.isBlank()) && (category == null || category.isBlank())) {
            return listings;
        }
        return listings.stream()
                .filter(j -> {
                    boolean matchQuery = query == null || query.isBlank() ||
                            containsIgnoreCase(j.getTitle(), query) ||
                            containsIgnoreCase(j.getCompany(), query) ||
                            containsIgnoreCase(j.getDescription(), query);
                    boolean matchCategory = category == null || category.isBlank() ||
                            containsIgnoreCase(j.getCategory(), category);
                    return matchQuery && matchCategory;
                })
                .toList();
    }

    // ─────────────────────────────────────────────
    // Rementist
    // ─────────────────────────────────────────────
    private List<JobListing> scrapeRementist() {
        List<JobListing> results = new ArrayList<>();
        try {
            Document doc = Jsoup.connect("https://rementist.com/staj-ilanlari")
                    .userAgent(USER_AGENT).timeout(TIMEOUT_MS).get();

            Elements cards = doc.select("article, .job-card, .ilan-kart, .card, [class*='job'], [class*='ilan']");
            if (cards.isEmpty()) cards = doc.select("a[href*='/ilan/'], a[href*='/staj/']");

            for (Element card : cards) {
                String title = textOf(card, "h2, h3, h4, .title, .baslik, [class*='title']");
                String company = textOf(card, ".company, .sirket, [class*='company'], [class*='sirket']");
                String location = textOf(card, ".location, .konum, [class*='location']");
                String url = absUrl(card, "a");
                if (title.isBlank()) continue;
                results.add(JobListing.builder()
                        .title(title).company(company).location(location)
                        .source("Rementist").url(url).category("Staj").build());
            }
        } catch (Exception e) {
            System.err.println("[Rementist] Scrape failed: " + e.getMessage());
        }
        return results;
    }

    // ─────────────────────────────────────────────
    // YouthAll (public API)
    // ─────────────────────────────────────────────
    private List<JobListing> scrapeYouthAll() {
        List<JobListing> results = new ArrayList<>();
        try {
            // YouthAll JSON API
            String json = restTemplate.getForObject(
                    "https://api.youthall.com/api/v2/internship-positions?page=1&per_page=30",
                    String.class);
            if (json == null) return results;

            // Manuel JSON parse (SDK kullanmadan)
            String[] items = json.split("\\{\"id\":");
            for (int i = 1; i < items.length; i++) {
                String chunk = "{\"id\":" + items[i];
                String title = extractJsonField(chunk, "title");
                String company = extractJsonField(chunk, "company_name");
                String location = extractJsonField(chunk, "city");
                String slug = extractJsonField(chunk, "slug");
                if (title.isBlank()) continue;
                String url = "https://www.youthall.com/tr/jobs/" + slug;
                results.add(JobListing.builder()
                        .title(title).company(company).location(location)
                        .source("YouthAll").url(url).category("Staj").build());
            }
        } catch (Exception e) {
            // API deneme basarisizsa web scraping dene
            try {
                Document doc = Jsoup.connect("https://www.youthall.com/tr/jobs/")
                        .userAgent(USER_AGENT).timeout(TIMEOUT_MS).get();
                Elements cards = doc.select("[class*='JobCard'], [class*='job-card'], article");
                for (Element card : cards) {
                    String title = textOf(card, "h2,h3,h4,[class*='title']");
                    String company = textOf(card, "[class*='company']");
                    String url = absUrl(card, "a");
                    if (title.isBlank()) continue;
                    results.add(JobListing.builder()
                            .title(title).company(company)
                            .source("YouthAll").url(url).category("Staj").build());
                }
            } catch (Exception ignored) {
                System.err.println("[YouthAll] Scrape failed: " + e.getMessage());
            }
        }
        return results;
    }

    // ─────────────────────────────────────────────
    // SavunmaKariyer (paginated)
    // ─────────────────────────────────────────────
    private List<JobListing> scrapeSavunmaKariyer() {
        List<JobListing> results = new ArrayList<>();
        try {
            for (int page = 1; page <= 3; page++) {
                Document doc = Jsoup.connect("https://savunmakariyer.com/ilanlar?page=" + page)
                        .userAgent(USER_AGENT).timeout(TIMEOUT_MS).get();
                Elements cards = doc.select(".job-item, .ilan-item, article, .card, [class*='job'], [class*='ilan']");
                if (cards.isEmpty()) cards = doc.select("tr:has(td)");
                for (Element card : cards) {
                    String title = textOf(card, "h2,h3,h4,.title,[class*='title'],td:first-child");
                    String company = textOf(card, ".company,[class*='company'],td:nth-child(2)");
                    String location = textOf(card, ".location,[class*='location'],td:nth-child(3)");
                    String url = absUrl(card, "a");
                    if (title.isBlank()) continue;
                    results.add(JobListing.builder()
                            .title(title).company(company).location(location)
                            .source("SavunmaKariyer").url(url).category("Savunma").build());
                }
                if (cards.isEmpty()) break;
            }
        } catch (Exception e) {
            System.err.println("[SavunmaKariyer] Scrape failed: " + e.getMessage());
        }
        return results;
    }

    // ─────────────────────────────────────────────
    // KariyerNet
    // ─────────────────────────────────────────────
    private List<JobListing> scrapeKariyerNet() {
        List<JobListing> results = new ArrayList<>();
        try {
            Document doc = Jsoup.connect("https://www.kariyer.net/is-ilanlari/yeni+baslayan?lpst=5")
                    .userAgent(USER_AGENT).timeout(TIMEOUT_MS)
                    .header("Accept-Language", "tr-TR,tr;q=0.9")
                    .get();
            Elements cards = doc.select(".list-items-wrapper .job-item, [class*='job-list'] article, .jb-item");
            if (cards.isEmpty()) cards = doc.select("[data-id], [class*='position'], [class*='ilan']");
            for (Element card : cards) {
                String title = textOf(card, ".job-title, h3, h4, [class*='title']");
                String company = textOf(card, ".company-name, [class*='company']");
                String location = textOf(card, ".location, [class*='location'], [class*='city']");
                String url = absUrl(card, "a");
                if (title.isBlank()) continue;
                results.add(JobListing.builder()
                        .title(title).company(company).location(location)
                        .source("KariyerNet").url("https://www.kariyer.net" + (url.startsWith("http") ? url.substring(url.indexOf("/", 8)) : url))
                        .category("Genel").build());
            }
        } catch (Exception e) {
            System.err.println("[KariyerNet] Scrape failed: " + e.getMessage());
        }
        return results;
    }

    // ─────────────────────────────────────────────
    // Indeed Turkey - RSS Feed
    // ─────────────────────────────────────────────
    private List<JobListing> scrapeIndeedRss() {
        List<JobListing> results = new ArrayList<>();
        String[] queries = {"staj+yazilim", "muhendis+yeni+mezun", "junior+developer"};
        for (String q : queries) {
            try {
                Document doc = Jsoup.connect("https://tr.indeed.com/rss?q=" + q + "&l=Turkey")
                        .userAgent(USER_AGENT).timeout(TIMEOUT_MS).get();
                Elements items = doc.select("item");
                for (Element item : items) {
                    String title = item.selectFirst("title") != null ? item.selectFirst("title").text() : "";
                    String company = item.selectFirst("source") != null ? item.selectFirst("source").text() : "";
                    String link = item.selectFirst("link") != null ? item.selectFirst("link").text() : "";
                    String desc = item.selectFirst("description") != null
                            ? Jsoup.parse(item.selectFirst("description").text()).text() : "";
                    if (title.isBlank()) continue;
                    results.add(JobListing.builder()
                            .title(title).company(company).description(desc.length() > 200 ? desc.substring(0, 200) : desc)
                            .source("Indeed").url(link).category("Genel").build());
                }
            } catch (Exception e) {
                System.err.println("[Indeed] RSS failed for query " + q + ": " + e.getMessage());
            }
        }
        return results;
    }

    // ─────────────────────────────────────────────
    // SecretCV
    // ─────────────────────────────────────────────
    private List<JobListing> scrapeSecretCv() {
        List<JobListing> results = new ArrayList<>();
        try {
            Document doc = Jsoup.connect("https://www.secretcv.com/is-ilanlari")
                    .userAgent(USER_AGENT).timeout(TIMEOUT_MS).get();
            Elements cards = doc.select(".job-item, article, [class*='job'], [class*='position'], .ilan");
            for (Element card : cards) {
                String title = textOf(card, "h2,h3,h4,.title,[class*='title']");
                String company = textOf(card, ".company,[class*='company'],.employer");
                String location = textOf(card, ".location,[class*='location'],.city");
                String url = absUrl(card, "a");
                if (title.isBlank()) continue;
                results.add(JobListing.builder()
                        .title(title).company(company).location(location)
                        .source("SecretCV").url(url).category("Genel").build());
            }
        } catch (Exception e) {
            System.err.println("[SecretCV] Scrape failed: " + e.getMessage());
        }
        return results;
    }

    // ─────────────────────────────────────────────
    // Yenibiris
    // ─────────────────────────────────────────────
    private List<JobListing> scrapeYenibiris() {
        List<JobListing> results = new ArrayList<>();
        try {
            Document doc = Jsoup.connect("https://www.yenibiris.com/is-ilanlari/muhendislik")
                    .userAgent(USER_AGENT).timeout(TIMEOUT_MS).get();
            Elements cards = doc.select(".ilanListItem, article, [class*='job'], [class*='ilan'], li.list-group-item");
            for (Element card : cards) {
                String title = textOf(card, "h2,h3,h4,.title,[class*='title'],[class*='pozisyon']");
                String company = textOf(card, ".company,[class*='company'],[class*='firma']");
                String location = textOf(card, ".location,[class*='location'],[class*='sehir']");
                String url = absUrl(card, "a");
                if (title.isBlank()) continue;
                results.add(JobListing.builder()
                        .title(title).company(company).location(location)
                        .source("Yenibiris").url(url).category("Mühendislik").build());
            }
        } catch (Exception e) {
            System.err.println("[Yenibiris] Scrape failed: " + e.getMessage());
        }
        return results;
    }

    // ─────────────────────────────────────────────
    // Eleman.net
    // ─────────────────────────────────────────────
    private List<JobListing> scrapeEleman() {
        List<JobListing> results = new ArrayList<>();
        try {
            Document doc = Jsoup.connect("https://www.eleman.net/is-ilanlari?arandi=e&poz[]=14")
                    .userAgent(USER_AGENT).timeout(TIMEOUT_MS).get();
            Elements cards = doc.select(".list-item, article, [class*='job'], [class*='ilan'], .advert");
            for (Element card : cards) {
                String title = textOf(card, "h2,h3,h4,.title,[class*='title'],[class*='pozisyon']");
                String company = textOf(card, ".company,[class*='company'],[class*='firma']");
                String location = textOf(card, ".location,[class*='location'],[class*='sehir'],[class*='city']");
                String url = absUrl(card, "a");
                if (title.isBlank()) continue;
                results.add(JobListing.builder()
                        .title(title).company(company).location(location)
                        .source("Eleman.net").url(url).category("Genel").build());
            }
        } catch (Exception e) {
            System.err.println("[Eleman.net] Scrape failed: " + e.getMessage());
        }
        return results;
    }

    // ─────────────────────────────────────────────
    // Yardımcı metodlar
    // ─────────────────────────────────────────────
    private String textOf(Element parent, String cssQuery) {
        Element el = parent.selectFirst(cssQuery);
        return el != null ? el.text().trim() : "";
    }

    private String absUrl(Element parent, String tag) {
        Element el = parent.selectFirst(tag);
        if (el == null) return "";
        String href = el.attr("abs:href");
        return href.isBlank() ? el.attr("href") : href;
    }

    private String extractJsonField(String json, String field) {
        String key = "\"" + field + "\":\"";
        int idx = json.indexOf(key);
        if (idx == -1) return "";
        int start = idx + key.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return "";
        return json.substring(start, end);
    }

    private boolean containsIgnoreCase(String text, String query) {
        if (text == null || query == null) return false;
        return text.toLowerCase().contains(query.toLowerCase());
    }
}
