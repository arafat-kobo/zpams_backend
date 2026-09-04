package com.Zanzibar.Public.Announcement.report;

import com.Zanzibar.Public.Announcement.annoucement.Announcement;
import com.Zanzibar.Public.Announcement.annoucement.AnnouncementRepository;
import com.Zanzibar.Public.Announcement.annoucement.AnnouncementStatus;
import com.Zanzibar.Public.Announcement.annoucement.AnnouncementType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final AnnouncementRepository announcementRepository;



    // ==========================================
    // GENERAL REPORT
    // ==========================================

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>>
    getSummary() {

        List<Announcement> announcements =
                announcementRepository.findAll();

        long total =
                announcements.stream()
                        .filter(a ->
                                a.getSubmittedAt() != null
                        )
                        .count();


        long pending =
                countByStatus(
                        AnnouncementStatus.PENDING
                );


        long approved =
                countByStatus(
                        AnnouncementStatus.APPROVED
                );


        long rejected =
                countByStatus(
                        AnnouncementStatus.REJECTED
                );


        long broadcasted =
                countByStatus(
                        AnnouncementStatus.BROADCASTED
                );


        long death =
                countByType(
                        AnnouncementType.DEATH
                );


        long lost =
                countByType(
                        AnnouncementType.LOST
                );


        long found =
                countByType(
                        AnnouncementType.FOUND
                );


        Map<String, Object> report =
                new HashMap<>();

        report.put(
                "totalAnnouncements",
                total
        );

        report.put(
                "pendingAnnouncements",
                pending
        );

        report.put(
                "approvedAnnouncements",
                approved
        );

        report.put(
                "rejectedAnnouncements",
                rejected
        );

        report.put(
                "broadcastedAnnouncements",
                broadcasted
        );

        report.put(
                "deathAnnouncements",
                death
        );

        report.put(
                "lostAnnouncements",
                lost
        );

        report.put(
                "foundAnnouncements",
                found
        );


        return ResponseEntity.ok(
                report
        );
    }


    // ==========================================
    // ANNOUNCEMENT STATISTICS
    // ==========================================

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>>
    getStatistics() {

        Map<String, Object> statistics =
                new HashMap<>();


        statistics.put(
                "death",
                countByType(
                        AnnouncementType.DEATH
                )
        );

        statistics.put(
                "lost",
                countByType(
                        AnnouncementType.LOST
                )
        );

        statistics.put(
                "found",
                countByType(
                        AnnouncementType.FOUND
                )
        );


        statistics.put(
                "pending",
                countByStatus(
                        AnnouncementStatus.PENDING
                )
        );

        statistics.put(
                "approved",
                countByStatus(
                        AnnouncementStatus.APPROVED
                )
        );

        statistics.put(
                "rejected",
                countByStatus(
                        AnnouncementStatus.REJECTED
                )
        );

        statistics.put(
                "broadcasted",
                countByStatus(
                        AnnouncementStatus.BROADCASTED
                )
        );


        return ResponseEntity.ok(
                statistics
        );
    }


    // ==========================================
    // COUNT BY STATUS
    // ==========================================

    private long countByStatus(
            AnnouncementStatus status
    ) {

        return announcementRepository
                .findByStatus(status)
                .stream()
                .filter(a ->
                        a.getSubmittedAt() != null
                )
                .count();
    }


    // ==========================================
    // COUNT BY TYPE
    // ==========================================

    private long countByType(
            AnnouncementType type
    ) {

        return announcementRepository
                .findByType(type)
                .stream()
                .filter(a ->
                        a.getSubmittedAt() != null
                )
                .count();
    }
}