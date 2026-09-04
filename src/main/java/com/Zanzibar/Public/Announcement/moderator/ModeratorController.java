package com.Zanzibar.Public.Announcement.moderator;

import com.Zanzibar.Public.Announcement.annoucement.Announcement;
import com.Zanzibar.Public.Announcement.annoucement.AnnouncementRepository;
import com.Zanzibar.Public.Announcement.annoucement.AnnouncementResponse;
import com.Zanzibar.Public.Announcement.annoucement.AnnouncementStatus;
import com.Zanzibar.Public.Announcement.annoucement.AnnouncementType;
import com.Zanzibar.Public.Announcement.user.User;
import com.Zanzibar.Public.Announcement.user.UserRepository;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/moderator")
@RequiredArgsConstructor
public class ModeratorController {

    private final AnnouncementRepository announcementRepository;

    private final UserRepository userRepository;


    // ==========================================
    // GET ALL SUBMITTED ANNOUNCEMENTS
    // ==========================================

    @GetMapping("/announcements")
    public ResponseEntity<List<AnnouncementResponse>>
    getAllAnnouncements() {

        List<AnnouncementResponse> response =
                announcementRepository.findAll()
                        .stream()
                        .filter(a ->
                                a.getSubmittedAt() != null
                        )
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }


    // ==========================================
    // GET BY ID
    // ==========================================

    @GetMapping("/announcements/{id}")
    public ResponseEntity<AnnouncementResponse>
    getAnnouncement(
            @PathVariable Long id
    ) {

        Announcement announcement =
                findAnnouncement(id);

        return ResponseEntity.ok(
                toResponse(announcement)
        );
    }


    // ==========================================
    // GET BY STATUS
    // ==========================================

    @GetMapping("/announcements/status/{status}")
    public ResponseEntity<List<AnnouncementResponse>>
    getByStatus(
            @PathVariable AnnouncementStatus status
    ) {

        List<AnnouncementResponse> response =
                announcementRepository
                        .findByStatus(status)
                        .stream()
                        .filter(a ->
                                a.getSubmittedAt() != null
                        )
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }


    // ==========================================
    // GET BY TYPE
    // ==========================================

    @GetMapping("/announcements/type/{type}")
    public ResponseEntity<List<AnnouncementResponse>>
    getByType(
            @PathVariable AnnouncementType type
    ) {

        List<AnnouncementResponse> response =
                announcementRepository
                        .findByType(type)
                        .stream()
                        .filter(a ->
                                a.getSubmittedAt() != null
                        )
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }


    // ==========================================
    // SEARCH
    // ==========================================

    @GetMapping("/announcements/search")
    public ResponseEntity<List<AnnouncementResponse>>
    searchAnnouncements(
            @RequestParam String keyword
    ) {

        String search =
                keyword.toLowerCase().trim();

        List<AnnouncementResponse> response =
                announcementRepository.findAll()
                        .stream()
                        .filter(a ->
                                a.getSubmittedAt() != null
                        )
                        .filter(a ->
                                contains(
                                        a.getApplicantName(),
                                        search
                                )
                                        ||
                                        contains(
                                                a.getPhoneNumber(),
                                                search
                                        )
                                        ||
                                        contains(
                                                a.getDeceasedName(),
                                                search
                                        )
                                        ||
                                        contains(
                                                a.getItemName(),
                                                search
                                        )
                                        ||
                                        contains(
                                                a.getLocation(),
                                                search
                                        )
                        )
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }


    // ==========================================
    // EDIT MINOR ERRORS
    // ==========================================

    @PutMapping("/announcements/{id}")
    public ResponseEntity<AnnouncementResponse>
    updateAnnouncement(
            @PathVariable Long id,
            @RequestBody ModeratorUpdateRequest request
    ) {

        Announcement announcement =
                findAnnouncement(id);


        if (announcement.getSubmittedAt() == null) {

            throw new RuntimeException(
                    "Announcement has not been submitted"
            );
        }


        if (announcement.getStatus()
                != AnnouncementStatus.PENDING) {

            throw new RuntimeException(
                    "Only pending announcements can be edited"
            );
        }


        // ==========================================
        // APPLICANT
        // ==========================================

        if (request.getApplicantName() != null) {
            announcement.setApplicantName(
                    request.getApplicantName()
            );
        }

        if (request.getPhoneNumber() != null) {
            announcement.setPhoneNumber(
                    request.getPhoneNumber()
            );
        }


        // ==========================================
        // DEATH
        // ==========================================

        if (request.getDeceasedName() != null) {
            announcement.setDeceasedName(
                    request.getDeceasedName()
            );
        }

        if (request.getGender() != null) {
            announcement.setGender(
                    request.getGender()
            );
        }

        if (request.getAge() != null) {
            announcement.setAge(
                    request.getAge()
            );
        }

        if (request.getDateOfDeath() != null) {
            announcement.setDateOfDeath(
                    request.getDateOfDeath()
            );
        }

        if (request.getPlaceOfDeath() != null) {
            announcement.setPlaceOfDeath(
                    request.getPlaceOfDeath()
            );
        }

        if (request.getBurialPlace() != null) {
            announcement.setBurialPlace(
                    request.getBurialPlace()
            );
        }

        if (request.getBurialDate() != null) {
            announcement.setBurialDate(
                    request.getBurialDate()
            );
        }

        if (request.getBurialTime() != null) {
            announcement.setBurialTime(
                    request.getBurialTime()
            );
        }

        if (request.getFamilyMembers() != null) {
            announcement.setFamilyMembers(
                    request.getFamilyMembers()
            );
        }

        if (request.getContactPerson() != null) {
            announcement.setContactPerson(
                    request.getContactPerson()
            );
        }


        // ==========================================
        // LOST / FOUND
        // ==========================================

        if (request.getItemName() != null) {
            announcement.setItemName(
                    request.getItemName()
            );
        }

        if (request.getItemCategory() != null) {
            announcement.setItemCategory(
                    request.getItemCategory()
            );
        }

        if (request.getDescription() != null) {
            announcement.setDescription(
                    request.getDescription()
            );
        }

        if (request.getLocation() != null) {
            announcement.setLocation(
                    request.getLocation()
            );
        }

        if (request.getEventDate() != null) {
            announcement.setEventDate(
                    request.getEventDate()
            );
        }

        if (request.getReward() != null) {
            announcement.setReward(
                    request.getReward()
            );
        }


        // ==========================================
        // GENERAL
        // ==========================================

        if (request.getAdditionalInformation() != null) {
            announcement.setAdditionalInformation(
                    request.getAdditionalInformation()
            );
        }


        // ==========================================
        // REGENERATE ANNOUNCEMENT TEXT
        // ==========================================

        announcement.setGeneratedText(
                generateAnnouncementText(
                        announcement
                )
        );


        Announcement saved =
                announcementRepository.save(
                        announcement
                );

        return ResponseEntity.ok(
                toResponse(saved)
        );
    }


    // ==========================================
    // APPROVE
    // ==========================================

    @PatchMapping("/announcements/{id}/approve")
    public ResponseEntity<AnnouncementResponse>
    approveAnnouncement(
            @PathVariable Long id,
            Authentication authentication
    ) {

        Announcement announcement =
                findAnnouncement(id);


        if (announcement.getSubmittedAt() == null) {

            throw new RuntimeException(
                    "Announcement has not been submitted"
            );
        }


        if (announcement.getStatus()
                != AnnouncementStatus.PENDING) {

            throw new RuntimeException(
                    "Only pending announcements can be approved"
            );
        }


        User moderator =
                findAuthenticatedUser(
                        authentication
                );


        announcement.setStatus(
                AnnouncementStatus.APPROVED
        );

        announcement.setReviewedAt(
                LocalDateTime.now()
        );

        announcement.setReviewedBy(
                moderator
        );

        // Prepare the official PDF immediately after approval so the
        // Radio Operator can receive it without first generating it.
        try {
            generateAndStorePdf(announcement);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Announcement approved, but PDF generation failed",
                    e
            );
        }

        Announcement saved =
                announcementRepository.save(
                        announcement
                );


        return ResponseEntity.ok(
                toResponse(saved)
        );
    }


    // ==========================================
    // REJECT
    // ==========================================

    @PatchMapping("/announcements/{id}/reject")
    public ResponseEntity<AnnouncementResponse>
    rejectAnnouncement(
            @PathVariable Long id,
            Authentication authentication
    ) {

        Announcement announcement =
                findAnnouncement(id);


        if (announcement.getSubmittedAt() == null) {

            throw new RuntimeException(
                    "Announcement has not been submitted"
            );
        }


        if (announcement.getStatus()
                != AnnouncementStatus.PENDING) {

            throw new RuntimeException(
                    "Only pending announcements can be rejected"
            );
        }


        User moderator =
                findAuthenticatedUser(
                        authentication
                );


        announcement.setStatus(
                AnnouncementStatus.REJECTED
        );

        announcement.setReviewedAt(
                LocalDateTime.now()
        );

        announcement.setReviewedBy(
                moderator
        );

        // Prepare the official PDF immediately after approval so the
        // Radio Operator can receive it without first generating it.
        try {
            generateAndStorePdf(announcement);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Announcement approved, but PDF generation failed",
                    e
            );
        }

        Announcement saved =
                announcementRepository.save(
                        announcement
                );


        return ResponseEntity.ok(
                toResponse(saved)
        );
    }


    private void generateAndStorePdf(Announcement announcement)
            throws IOException, DocumentException {

        ByteArrayOutputStream outputStream =
                new ByteArrayOutputStream();

        Document document = new Document();
        PdfWriter.getInstance(document, outputStream);
        document.open();

        document.add(new Paragraph("ZANZIBAR PUBLIC ANNOUNCEMENT"));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Announcement ID: " + announcement.getId()));
        document.add(new Paragraph("Type: " + announcement.getType()));
        document.add(new Paragraph("Applicant: " + announcement.getApplicantName()));
        document.add(new Paragraph("Phone: " + announcement.getPhoneNumber()));
        document.add(new Paragraph(" "));
        document.add(new Paragraph(
                announcement.getGeneratedText() == null
                        ? ""
                        : announcement.getGeneratedText()
        ));
        document.close();

        Path directory = Paths.get("uploads", "announcements");
        Files.createDirectories(directory);

        Path pdfPath = directory.resolve(
                "announcement-" + announcement.getId() + ".pdf"
        );

        Files.write(pdfPath, outputStream.toByteArray());
        announcement.setPdfFilePath(pdfPath.toString());
    }


    // ==========================================
    // GENERATE PDF
    // ==========================================

    @GetMapping(
            value = "/announcements/{id}/pdf",
            produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]>
    generatePdf(
            @PathVariable Long id
    ) {

        Announcement announcement =
                findAnnouncement(id);


        if (announcement.getStatus()
                != AnnouncementStatus.APPROVED) {

            throw new RuntimeException(
                    "Only approved announcements can generate PDF"
            );
        }


        try {

            ByteArrayOutputStream outputStream =
                    new ByteArrayOutputStream();


            Document document =
                    new Document();


            PdfWriter.getInstance(
                    document,
                    outputStream
            );


            document.open();


            document.add(
                    new Paragraph(
                            "ZANZIBAR PUBLIC ANNOUNCEMENT"
                    )
            );

            document.add(
                    new Paragraph(
                            " "
                    )
            );

            document.add(
                    new Paragraph(
                            "Announcement ID: "
                                    + announcement.getId()
                    )
            );

            document.add(
                    new Paragraph(
                            "Type: "
                                    + announcement.getType()
                    )
            );

            document.add(
                    new Paragraph(
                            "Applicant: "
                                    + announcement.getApplicantName()
                    )
            );

            document.add(
                    new Paragraph(
                            "Phone: "
                                    + announcement.getPhoneNumber()
                    )
            );

            document.add(
                    new Paragraph(
                            " "
                    )
            );

            document.add(
                    new Paragraph(
                            announcement.getGeneratedText()
                    )
            );


            document.close();


            byte[] pdf =
                    outputStream.toByteArray();


            // ==========================================
            // SAVE PDF PATH
            // ==========================================

            Path directory =
                    Paths.get(
                            "uploads",
                            "announcements"
                    );

            Files.createDirectories(
                    directory
            );


            Path pdfPath =
                    directory.resolve(
                            "announcement-"
                                    + announcement.getId()
                                    + ".pdf"
                    );


            Files.write(
                    pdfPath,
                    pdf
            );


            announcement.setPdfFilePath(
                    pdfPath.toString()
            );

            announcementRepository.save(
                    announcement
            );


            return ResponseEntity.ok()
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=announcement-"
                                    + announcement.getId()
                                    + ".pdf"
                    )
                    .contentType(
                            MediaType.APPLICATION_PDF
                    )
                    .body(pdf);

        } catch (
                DocumentException
                | IOException e
        ) {

            throw new RuntimeException(
                    "Failed to generate PDF",
                    e
            );
        }
    }


    // ==========================================
    // RADIO SCRIPT
    // ==========================================

    @GetMapping(
            value = "/announcements/{id}/radio-script",
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public ResponseEntity<String>
    generateRadioScript(
            @PathVariable Long id
    ) {

        Announcement announcement =
                findAnnouncement(id);


        if (announcement.getStatus()
                != AnnouncementStatus.APPROVED) {

            throw new RuntimeException(
                    "Radio script is available only for approved announcements"
            );
        }


        String script =
                "ZANZIBAR PUBLIC ANNOUNCEMENT\n\n"
                        + "Announcement ID: "
                        + announcement.getId()
                        + "\n\n"
                        + announcement.getGeneratedText()
                        + "\n\n"
                        + "For more information, please contact "
                        + announcement.getPhoneNumber()
                        + ".";


        return ResponseEntity.ok(
                script
        );
    }


    // ==========================================
    // MARK AS BROADCASTED
    // ==========================================

    @PatchMapping("/announcements/{id}/broadcast")
    public ResponseEntity<AnnouncementResponse>
    markAsBroadcasted(
            @PathVariable Long id
    ) {

        Announcement announcement =
                findAnnouncement(id);


        if (announcement.getStatus()
                != AnnouncementStatus.APPROVED) {

            throw new RuntimeException(
                    "Only approved announcements can be marked as broadcasted"
            );
        }


        announcement.setStatus(
                AnnouncementStatus.BROADCASTED
        );


        Announcement saved =
                announcementRepository.save(
                        announcement
                );


        return ResponseEntity.ok(
                toResponse(saved)
        );
    }


    // ==========================================
    // FIND ANNOUNCEMENT
    // ==========================================

    private Announcement findAnnouncement(
            Long id
    ) {

        return announcementRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Announcement not found"
                        )
                );
    }


    // ==========================================
    // FIND AUTHENTICATED USER
    // ==========================================

    private User findAuthenticatedUser(
            Authentication authentication
    ) {

        return userRepository
                .findByEmail(
                        authentication.getName()
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "Authenticated user not found"
                        )
                );
    }


    // ==========================================
    // SEARCH HELPER
    // ==========================================

    private boolean contains(
            String value,
            String keyword
    ) {

        return value != null
                &&
                value.toLowerCase()
                        .contains(keyword);
    }


    // ==========================================
    // ENTITY → RESPONSE
    // ==========================================

    private AnnouncementResponse toResponse(
            Announcement announcement
    ) {

        return AnnouncementResponse.builder()

                .id(announcement.getId())

                .type(announcement.getType())

                .applicantName(
                        announcement.getApplicantName()
                )

                .phoneNumber(
                        announcement.getPhoneNumber()
                )

                .deceasedName(
                        announcement.getDeceasedName()
                )

                .gender(
                        announcement.getGender()
                )

                .age(
                        announcement.getAge()
                )

                .dateOfDeath(
                        announcement.getDateOfDeath()
                )

                .placeOfDeath(
                        announcement.getPlaceOfDeath()
                )

                .burialPlace(
                        announcement.getBurialPlace()
                )

                .burialDate(
                        announcement.getBurialDate()
                )

                .burialTime(
                        announcement.getBurialTime()
                )

                .familyMembers(
                        announcement.getFamilyMembers()
                )

                .contactPerson(
                        announcement.getContactPerson()
                )

                .itemName(
                        announcement.getItemName()
                )

                .itemCategory(
                        announcement.getItemCategory()
                )

                .description(
                        announcement.getDescription()
                )

                .location(
                        announcement.getLocation()
                )

                .eventDate(
                        announcement.getEventDate()
                )

                .reward(
                        announcement.getReward()
                )

                .additionalInformation(
                        announcement.getAdditionalInformation()
                )

                .generatedText(
                        announcement.getGeneratedText()
                )

                .status(
                        announcement.getStatus()
                )

                .submittedAt(
                        announcement.getSubmittedAt()
                )

                .reviewedAt(
                        announcement.getReviewedAt()
                )

                .pdfFilePath(
                        announcement.getPdfFilePath()
                )

                .reviewedBy(
                        announcement.getReviewedBy() != null
                                ? announcement
                                .getReviewedBy()
                                .getId()
                                : null
                )

                .build();
    }


    // ==========================================
    // GENERATE TEXT
    // ==========================================

    private String generateAnnouncementText(
            Announcement a
    ) {

        return switch (a.getType()) {

            case DEATH ->
                    generateDeathText(a);

            case LOST ->
                    generateLostText(a);

            case FOUND ->
                    generateFoundText(a);
        };
    }


    private String generateDeathText(
            Announcement a
    ) {

        StringBuilder text =
                new StringBuilder();

        text.append(
                "DEATH ANNOUNCEMENT\n\n"
        );

        text.append(
                "We announce the death of "
        );

        text.append(
                a.getDeceasedName()
        );

        text.append(". ");


        if (a.getDateOfDeath() != null) {

            text.append(
                    "The deceased passed away on "
            );

            text.append(
                    a.getDateOfDeath()
            );

            text.append(". ");
        }


        if (a.getPlaceOfDeath() != null) {

            text.append(
                    "The death occurred at "
            );

            text.append(
                    a.getPlaceOfDeath()
            );

            text.append(". ");
        }


        if (a.getBurialPlace() != null) {

            text.append(
                    "Burial will take place at "
            );

            text.append(
                    a.getBurialPlace()
            );

            text.append(". ");
        }


        if (a.getBurialDate() != null) {

            text.append(
                    "Burial date: "
            );

            text.append(
                    a.getBurialDate()
            );

            text.append(". ");
        }


        if (a.getBurialTime() != null) {

            text.append(
                    "Burial time: "
            );

            text.append(
                    a.getBurialTime()
            );

            text.append(". ");
        }


        if (a.getAdditionalInformation() != null
                &&
                !a.getAdditionalInformation().isBlank()) {

            text.append(
                    a.getAdditionalInformation()
            );
        }

        return text.toString();
    }


    private String generateLostText(
            Announcement a
    ) {

        StringBuilder text =
                new StringBuilder();

        text.append(
                "LOST ITEM ANNOUNCEMENT\n\n"
        );

        text.append("A ");

        text.append(
                a.getItemName()
        );

        text.append(
                " has been reported lost. "
        );


        if (a.getItemCategory() != null
                &&
                !a.getItemCategory().isBlank()) {

            text.append(
                    "Category: "
            );

            text.append(
                    a.getItemCategory()
            );

            text.append(". ");
        }


        if (a.getDescription() != null
                &&
                !a.getDescription().isBlank()) {

            text.append(
                    "Description: "
            );

            text.append(
                    a.getDescription()
            );

            text.append(". ");
        }


        if (a.getLocation() != null
                &&
                !a.getLocation().isBlank()) {

            text.append(
                    "It was lost at "
            );

            text.append(
                    a.getLocation()
            );

            text.append(". ");
        }


        if (a.getEventDate() != null) {

            text.append(
                    "Date lost: "
            );

            text.append(
                    a.getEventDate()
            );

            text.append(". ");
        }


        if (a.getReward() != null
                &&
                !a.getReward().isBlank()) {

            text.append(
                    "Reward: "
            );

            text.append(
                    a.getReward()
            );

            text.append(". ");
        }


        text.append(
                "For information, please contact "
        );

        text.append(
                a.getPhoneNumber()
        );

        text.append(".");

        return text.toString();
    }


    private String generateFoundText(
            Announcement a
    ) {

        StringBuilder text =
                new StringBuilder();

        text.append(
                "FOUND ITEM ANNOUNCEMENT\n\n"
        );

        text.append("A ");

        text.append(
                a.getItemName()
        );

        text.append(
                " has been found. "
        );


        if (a.getItemCategory() != null
                &&
                !a.getItemCategory().isBlank()) {

            text.append(
                    "Category: "
            );

            text.append(
                    a.getItemCategory()
            );

            text.append(". ");
        }


        if (a.getDescription() != null
                &&
                !a.getDescription().isBlank()) {

            text.append(
                    "Description: "
            );

            text.append(
                    a.getDescription()
            );

            text.append(". ");
        }


        if (a.getLocation() != null
                &&
                !a.getLocation().isBlank()) {

            text.append(
                    "It was found at "
            );

            text.append(
                    a.getLocation()
            );

            text.append(". ");
        }


        if (a.getEventDate() != null) {

            text.append(
                    "Date found: "
            );

            text.append(
                    a.getEventDate()
            );

            text.append(". ");
        }


        text.append(
                "For information, please contact "
        );

        text.append(
                a.getPhoneNumber()
        );

        text.append(".");

        return text.toString();
    }
}