package com.Zanzibar.Public.Announcement.annoucement;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Builder
public class AnnouncementResponse {

    // =========================================================
    // BASIC
    // =========================================================

    private Long id;

    private String referenceNumber;

    private AnnouncementType type;


    // =========================================================
    // APPLICANT
    // =========================================================

    private String applicantName;

    private String phoneNumber;


    // =========================================================
    // DEATH
    // =========================================================

    private String deceasedName;

    private String gender;

    private Integer age;

    private LocalDate dateOfDeath;

    private String placeOfDeath;

    private String burialPlace;

    private LocalDate burialDate;

    private LocalTime burialTime;

    private String familyMembers;

    private String contactPerson;


    // =========================================================
    // LOST / FOUND
    // =========================================================

    private String itemName;

    private String itemCategory;

    private String description;

    private String location;

    private LocalDate eventDate;

    private String reward;


    // =========================================================
    // GENERAL
    // =========================================================

    private String additionalInformation;


    // =========================================================
    // GENERATED CONTENT
    // =========================================================

    private String generatedText;

    private String broadcastScript;


    // =========================================================
    // FILES
    // =========================================================

    private String imagePath;

    private String pdfFilePath;



    // =========================================================
    // STATUS
    // =========================================================

    private AnnouncementStatus status;


    // =========================================================
    // TIMESTAMPS
    // =========================================================

    private LocalDateTime createdAt;

    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;


    // =========================================================
    // REVIEW
    // =========================================================

    private Long reviewedBy;

    private String reviewRemarks;
}