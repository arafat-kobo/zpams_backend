package com.Zanzibar.Public.Announcement.annoucement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnnouncementRepository
        extends JpaRepository<Announcement, Long> {

    List<Announcement> findByType(
            AnnouncementType type
    );

    List<Announcement> findByStatus(
            AnnouncementStatus status
    );

    List<Announcement> findByStatusInOrderBySubmittedAtDesc(
            List<AnnouncementStatus> statuses
    );

    List<Announcement> findByPhoneNumber(
            String phoneNumber
    );

    boolean existsByReferenceNumber(String referenceNumber);

    java.util.Optional<Announcement> findByReferenceNumber(String referenceNumber);




}
