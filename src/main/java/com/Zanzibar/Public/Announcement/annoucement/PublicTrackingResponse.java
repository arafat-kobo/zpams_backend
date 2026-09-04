package com.Zanzibar.Public.Announcement.annoucement;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PublicTrackingResponse {
    private String referenceNumber;
    private AnnouncementType type;
    private AnnouncementStatus status;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
}
