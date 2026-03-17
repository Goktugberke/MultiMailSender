package com.yunussemree.multimailsender.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobListing {
    private String title;
    private String company;
    private String location;
    private String source;      // "KariyerNet", "Indeed", "Rementist" ...
    private String url;
    private String postedAt;
    private String description;
    private String category;    // "Staj", "Tam Zamanlı", "Yarı Zamanlı" ...
}
