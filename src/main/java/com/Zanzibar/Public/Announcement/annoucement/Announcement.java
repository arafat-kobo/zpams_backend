package com.Zanzibar.Public.Announcement.annoucement;

import com.Zanzibar.Public.Announcement.user.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(
        name = "announcements",
        indexes = {
                @Index(name = "idx_announcement_type", columnList = "type"),
                @Index(name = "idx_announcement_status", columnList = "status"),
                @Index(name = "idx_announcement_phone", columnList = "phone_number"),
                @Index(name = "idx_announcement_submitted_at", columnList = "submitted_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_number", unique = true, length = 30)
    private String referenceNumber;


    // =========================================================
    // ANNOUNCEMENT TYPE
    // =========================================================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnnouncementType type;


    // =========================================================
    // APPLICANT INFORMATION
    // =========================================================

    @Column(nullable = false, length = 100)
    private String applicantName;

    @Column(nullable = false, length = 20)
    private String phoneNumber;




    // =========================================================
    // DEATH ANNOUNCEMENT INFORMATION
    // =========================================================

    @Column(length = 150)
    private String deceasedName;

    @Column(length = 20)
    private String gender;

    private Integer age;

    private LocalDate dateOfDeath;

    @Column(length = 150)
    private String placeOfDeath;

    @Column(length = 150)
    private String burialPlace;

    private LocalDate burialDate;

    private LocalTime burialTime;

    /**
     * Can contain names of family members / relatives.
     */
    @Column(columnDefinition = "TEXT")
    private String familyMembers;

    @Column(length = 100)
    private String contactPerson;


    // =========================================================
    // LOST / FOUND ITEM INFORMATION
    // =========================================================

    @Column(length = 150)
    private String itemName;

    @Column(length = 100)
    private String itemCategory;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 200)
    private String location;

    /**
     * Date item was lost/found.
     */
    private LocalDate eventDate;

    @Column(length = 100)
    private String reward;


    // =========================================================
    // GENERAL INFORMATION
    // =========================================================

    @Column(columnDefinition = "TEXT")
    private String additionalInformation;


    // =========================================================
    // GENERATED ANNOUNCEMENT
    // =========================================================

    /**
     * Automatically generated announcement shown
     * during preview and used as the base for final output.
     */
    @Column(columnDefinition = "TEXT")
    private String generatedText;


    // =========================================================
    // BROADCAST SCRIPT
    // =========================================================

    /**
     * Final script prepared for radio broadcasting.
     *
     * This may initially contain generatedText and later
     * be edited by the authorized radio/broadcast operator.
     */
    @Column(columnDefinition = "TEXT")
    private String broadcastScript;


    // =========================================================
    // SUPPORTING FILE
    // =========================================================

    /**
     * Path/URL of uploaded supporting image/document.
     *
     * Example:
     * /uploads/announcements/abc123.jpg
     */
    @Column(length = 500)
    private String imagePath;


    // =========================================================
    // GENERATED PDF
    // =========================================================

    @Column(length = 500)
    private String pdfFilePath;


    // =========================================================
    // ANNOUNCEMENT STATUS
    // =========================================================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AnnouncementStatus status;


    // =========================================================
    // REVIEW / MODERATION
    // =========================================================

    private LocalDateTime reviewedAt;

    /**
     * Approval / rejection / correction remarks.
     */
    @Column(columnDefinition = "TEXT")
    private String reviewRemarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    @JsonIgnoreProperties({
            "reviewedAnnouncements",
            "password"
    })
    private User reviewedBy;


    // =========================================================
    // CREATION / SUBMISSION TIMESTAMPS
    // =========================================================

    /**
     * When draft was created.
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * Time when the citizen submitted the free announcement.
     */
    private LocalDateTime submittedAt;


    // =========================================================
    // PRE-PERSIST
    // =========================================================

    @PrePersist
    protected void onCreate() {

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (status == null) {
            status = AnnouncementStatus.PENDING;
        }

        // Announcements are free and are submitted immediately.
        if (submittedAt == null) {
            submittedAt = createdAt;
        }
    }
}