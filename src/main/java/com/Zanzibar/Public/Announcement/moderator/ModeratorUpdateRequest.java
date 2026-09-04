package com.Zanzibar.Public.Announcement.moderator;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class ModeratorUpdateRequest {

    // ==========================================
    // APPLICANT
    // ==========================================

    private String applicantName;

    private String phoneNumber;


    // ==========================================
    // DEATH
    // ==========================================

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


    // ==========================================
    // LOST / FOUND
    // ==========================================

    private String itemName;

    private String itemCategory;

    private String description;

    private String location;

    private LocalDate eventDate;

    private String reward;


    // ==========================================
    // GENERAL
    // ==========================================

    private String additionalInformation;
}