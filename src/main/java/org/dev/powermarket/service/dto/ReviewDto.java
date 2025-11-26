package org.dev.powermarket.service.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Setter
@Getter
public class ReviewDto {

    private UUID id;
    private UUID rentalId;
    private UUID reviewerId;
    private String reviewerName;
    private UUID reviewedUserId;
    private String reviewedUserName;
    private UUID serviceId;
    private String serviceTitle;
    private Integer rating;
    private String comment;
    private Instant createdAt;

}
