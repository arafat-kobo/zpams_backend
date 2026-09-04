package com.Zanzibar.Public.Announcement.annoucement;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementRepository announcementRepository;


    // =========================================================
    // PUBLIC - PREVIEW ANNOUNCEMENT
    // =========================================================

    @PostMapping("/preview")
    public ResponseEntity<AnnouncementResponse> previewAnnouncement(
            @Valid @RequestBody AnnouncementRequest request
    ) {

        validateAnnouncement(request);

        Announcement announcement = new Announcement();

        mapRequestToEntity(
                request,
                announcement
        );


        announcement.setGeneratedText(
                request.getGeneratedText() != null
                        && !request.getGeneratedText().isBlank()
                        ? request.getGeneratedText().trim()
                        : generateAnnouncementText(announcement)
        );

        return ResponseEntity.ok(
                toResponse(announcement)
        );
    }


    // =========================================================
    // PUBLIC - CREATE ANNOUNCEMENT DRAFT
    // =========================================================

    @PostMapping
    public ResponseEntity<AnnouncementResponse> createAnnouncement(
            @Valid @RequestBody AnnouncementRequest request
    ) {

        validateAnnouncement(request);

        Announcement announcement = new Announcement();

        mapRequestToEntity(
                request,
                announcement
        );

        announcement.setStatus(
                AnnouncementStatus.PENDING
        );

        // Announcements are free: creation is the submission event.
        announcement.setSubmittedAt(LocalDateTime.now());

        announcement.setGeneratedText(
                request.getGeneratedText() != null
                        && !request.getGeneratedText().isBlank()
                        ? request.getGeneratedText().trim()
                        : generateAnnouncementText(announcement)
        );

        announcement.setBroadcastScript(
                announcement.getGeneratedText()
        );

        Announcement saved =
                announcementRepository.save(announcement);

        // Generate a human-friendly reference after the database ID exists.
        // The ID is unique and therefore the reference remains unique.
        if (saved.getReferenceNumber() == null || saved.getReferenceNumber().isBlank()) {
            saved.setReferenceNumber(
                    String.format("ZPAMS-%d-%05d",
                            announcement.getSubmittedAt() != null
                                    ? announcement.getSubmittedAt().getYear()
                                    : LocalDateTime.now().getYear(),
                            saved.getId())
            );
            saved = announcementRepository.save(saved);
        }

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(saved));
    }


    // =========================================================
    // PUBLIC - SUBMIT ANNOUNCEMENT
    // =========================================================

    @PostMapping("/{id}/submit")
    public ResponseEntity<AnnouncementResponse> submitAnnouncement(
            @PathVariable Long id
    ) {

        Announcement announcement =
                announcementRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Announcement not found"
                                )
                        );

        // =====================================================
        // DOUBLE SUBMISSION CHECK
        // =====================================================

        if (announcement.getSubmittedAt() != null) {

            throw new RuntimeException(
                    "Announcement has already been submitted"
            );
        }

        // =====================================================
        // SUBMIT
        // =====================================================

        announcement.setStatus(
                AnnouncementStatus.PENDING
        );

        announcement.setSubmittedAt(
                LocalDateTime.now()
        );

        if (announcement.getGeneratedText() == null
                ||
                announcement.getGeneratedText().isBlank()) {

            announcement.setGeneratedText(
                    generateAnnouncementText(announcement)
            );
        }

        if (announcement.getBroadcastScript() == null
                ||
                announcement.getBroadcastScript().isBlank()) {

            announcement.setBroadcastScript(
                    announcement.getGeneratedText()
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


    // =========================================================
    // PUBLIC - UPLOAD SUPPORTING FILE
    // =========================================================

    @PostMapping(
            value = "/{id}/upload",
            consumes = "multipart/form-data"
    )
    public ResponseEntity<AnnouncementResponse> uploadFile(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) throws IOException {

        Announcement announcement =
                announcementRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Announcement not found"
                                )
                        );

        if (file == null || file.isEmpty()) {

            throw new IllegalArgumentException(
                    "File is required"
            );
        }

        String contentType =
                file.getContentType();

        if (contentType == null ||
                !isAllowedFileType(contentType)) {

            throw new IllegalArgumentException(
                    "Only JPG, PNG and WEBP files are allowed"
            );
        }

        Path uploadDirectory =
                Paths.get(System.getProperty("user.dir"), "uploads", "announcements")
                        .toAbsolutePath()
                        .normalize();

        Files.createDirectories(
                uploadDirectory
        );

        String originalName =
                file.getOriginalFilename();

        String extension =
                getFileExtension(originalName);

        String fileName =
                UUID.randomUUID()
                        + extension;

        Path filePath =
                uploadDirectory.resolve(fileName);

        Files.copy(
                file.getInputStream(),
                filePath
        );

        // Store the absolute path of the uploaded file so the same
        // running backend can reliably serve it later, regardless of
        // the IDE/process working directory.
        announcement.setImagePath(
                filePath.toAbsolutePath().normalize().toString()
        );

        Announcement saved =
                announcementRepository.save(
                        announcement
                );

        return ResponseEntity.ok(
                toResponse(saved)
        );
    }


    // =========================================================
    // PUBLIC - PUBLISHED ANNOUNCEMENTS
    // =========================================================

    @GetMapping("/public")
    public ResponseEntity<List<AnnouncementResponse>>
    getPublicAnnouncements() {

        List<AnnouncementStatus> visibleStatuses =
                List.of(
                        AnnouncementStatus.APPROVED,
                        AnnouncementStatus.BROADCASTED
                );

        List<AnnouncementResponse> response =
                announcementRepository
                        .findByStatusInOrderBySubmittedAtDesc(visibleStatuses)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }


    // =========================================================
    // PUBLIC - TRACK BY REFERENCE NUMBER (NO LOGIN)
    // =========================================================

    @GetMapping("/public/track/{referenceNumber}")
    public ResponseEntity<PublicTrackingResponse> trackAnnouncement(
            @PathVariable String referenceNumber
    ) {
        Announcement announcement = announcementRepository
                .findByReferenceNumber(referenceNumber.trim().toUpperCase())
                .orElseThrow(() -> new RuntimeException("Announcement not found"));

        return ResponseEntity.ok(PublicTrackingResponse.builder()
                .referenceNumber(announcement.getReferenceNumber())
                .type(announcement.getType())
                .status(announcement.getStatus())
                .submittedAt(announcement.getSubmittedAt())
                .reviewedAt(announcement.getReviewedAt())
                .build());
    }


    // =========================================================
    // PUBLIC - GET ALL
    // =========================================================

    @GetMapping
    public ResponseEntity<List<AnnouncementResponse>>
    getAllAnnouncements() {

        List<AnnouncementResponse> response =
                announcementRepository
                        .findAll()
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }


    // =========================================================
    // PUBLIC - GET BY ID
    // =========================================================

    @GetMapping("/{id}")
    public ResponseEntity<AnnouncementResponse>
    getAnnouncement(
            @PathVariable Long id
    ) {

        Announcement announcement =
                announcementRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Announcement not found"
                                )
                        );

        return ResponseEntity.ok(
                toResponse(announcement)
        );
    }


    // =========================================================
    // PUBLIC - GET BY TYPE
    // =========================================================

    @GetMapping("/type/{type}")
    public ResponseEntity<List<AnnouncementResponse>>
    getByType(
            @PathVariable AnnouncementType type
    ) {

        List<AnnouncementResponse> response =
                announcementRepository
                        .findByType(type)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }


    // =========================================================
    // PUBLIC - GET PENDING
    // =========================================================

    @GetMapping("/pending")
    public ResponseEntity<List<AnnouncementResponse>>
    getPendingAnnouncements() {

        List<AnnouncementResponse> response =
                announcementRepository
                        .findByStatus(
                                AnnouncementStatus.PENDING
                        )
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }


    // =========================================================
    // ADMIN - GET ALL ANNOUNCEMENTS
    // =========================================================

    @GetMapping("/admin")
    public ResponseEntity<List<AnnouncementResponse>>
    getAdminAnnouncements() {

        List<AnnouncementResponse> response =
                announcementRepository
                        .findAll()
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }


    // =========================================================
    // ADMIN / MODERATOR - GET ITEM IMAGE
    // =========================================================

    @GetMapping("/{id}/image")
    public ResponseEntity<Resource> getAnnouncementImage(
            @PathVariable Long id
    ) throws IOException {

        Announcement announcement = findAnnouncement(id);

        if (announcement.getImagePath() == null
                || announcement.getImagePath().isBlank()) {
            return ResponseEntity.notFound().build();
        }

        Path uploadRoot =
                Paths.get(System.getProperty("user.dir"), "uploads", "announcements")
                        .toAbsolutePath()
                        .normalize();

        Path path = Paths.get(announcement.getImagePath());

        // Older records may contain a relative path such as
        // uploads/announcements/abc.jpg. Resolve those against the
        // backend working directory.
        if (!path.isAbsolute()) {
            path = Paths.get(System.getProperty("user.dir"))
                    .resolve(path)
                    .normalize();
        }

        // If the stored path was created under a different working
        // directory, recover the file by its filename from the current
        // announcement upload directory.
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            Path fallback = uploadRoot
                    .resolve(path.getFileName().toString())
                    .normalize();

            if (Files.exists(fallback) && Files.isRegularFile(fallback)) {
                path = fallback;
            } else {
                return ResponseEntity.notFound().build();
            }
        }

        // Never serve a file outside the configured announcement upload
        // directory when resolving relative/fallback paths.
        if (!path.toAbsolutePath().normalize().startsWith(uploadRoot)
                && !Paths.get(announcement.getImagePath()).isAbsolute()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new UrlResource(path.toUri());

        String contentType = Files.probeContentType(path);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;

        if (contentType != null) {
            try {
                mediaType = MediaType.parseMediaType(contentType);
            } catch (IllegalArgumentException ignored) {
                // Keep binary fallback.
            }
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(resource);
    }


    // =========================================================
    // =========================================================
    // MODERATOR SECTION
    // =========================================================
    // =========================================================


    // =========================================================
    // MODERATOR - GET ALL ANNOUNCEMENTS
    // =========================================================

    @GetMapping("/moderator")
    public ResponseEntity<List<AnnouncementResponse>>
    getModeratorAnnouncements() {

        List<AnnouncementResponse> response =
                announcementRepository
                        .findAll()
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }


    // =========================================================
    // MODERATOR - GET ONE
    // =========================================================

    @GetMapping("/moderator/{id}")
    public ResponseEntity<AnnouncementResponse>
    getModeratorAnnouncement(
            @PathVariable Long id
    ) {

        Announcement announcement =
                announcementRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Announcement not found"
                                )
                        );

        return ResponseEntity.ok(
                toResponse(announcement)
        );
    }


    // =========================================================
    // MODERATOR - SEARCH
    // =========================================================

    @GetMapping("/moderator/search")
    public ResponseEntity<List<AnnouncementResponse>>
    searchModeratorAnnouncements(
            @RequestParam String keyword
    ) {

        String search =
                keyword == null
                        ? ""
                        : keyword.trim().toLowerCase();

        List<AnnouncementResponse> response =
                announcementRepository
                        .findAll()
                        .stream()
                        .filter(a -> matchesSearch(a, search))
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }


    // =========================================================
    // MODERATOR - APPROVE
    // =========================================================

    @PatchMapping("/moderator/{id}/approve")
    public ResponseEntity<AnnouncementResponse>
    approveAnnouncement(
            @PathVariable Long id
    ) {

        Announcement announcement =
                findAnnouncement(id);

        if (announcement.getStatus() != AnnouncementStatus.PENDING) {
            throw new RuntimeException(
                    "Only pending announcements can be approved"
            );
        }

        ensureReferenceNumber(announcement);

        if (announcement.getGeneratedText() == null
                || announcement.getGeneratedText().isBlank()) {
            announcement.setGeneratedText(
                    generateAnnouncementText(announcement)
            );
        }

        announcement.setBroadcastScript(
                announcement.getGeneratedText()
        );
        announcement.setStatus(AnnouncementStatus.APPROVED);
        announcement.setReviewedAt(LocalDateTime.now());

        // Approval is the official publication gate: generate the final PDF
        // once here so the Radio Operator only consumes the approved document.
        try {
            announcement.setPdfFilePath(
                    generateAndStorePdf(announcement)
            );
        } catch (IOException | DocumentException e) {
            throw new RuntimeException("Unable to generate official PDF", e);
        }

        Announcement saved =
                announcementRepository.save(announcement);

        return ResponseEntity.ok(toResponse(saved));
    }


    // =========================================================
    // MODERATOR - REJECT
    // =========================================================

    @PatchMapping("/moderator/{id}/reject")
    public ResponseEntity<AnnouncementResponse>
    rejectAnnouncement(
            @PathVariable Long id
    ) {

        Announcement announcement =
                findAnnouncement(id);

        if (announcement.getStatus()
                == AnnouncementStatus.BROADCASTED) {

            throw new RuntimeException(
                    "Broadcasted announcement cannot be rejected"
            );
        }

        announcement.setStatus(
                AnnouncementStatus.REJECTED
        );

        announcement.setReviewedAt(
                LocalDateTime.now()
        );


        Announcement saved =
                announcementRepository.save(
                        announcement
                );

        return ResponseEntity.ok(
                toResponse(saved)
        );
    }


    // =========================================================
    // MODERATOR - DELETE REJECTED ANNOUNCEMENT
    // =========================================================

    @DeleteMapping("/moderator/{id}")
    public ResponseEntity<Void> deleteRejectedAnnouncement(
            @PathVariable Long id
    ) throws IOException {

        Announcement announcement = findAnnouncement(id);

        // Only rejected announcements may be permanently deleted.
        if (announcement.getStatus() != AnnouncementStatus.REJECTED) {
            throw new IllegalStateException(
                    "Only rejected announcements can be deleted"
            );
        }

        // Remove uploaded supporting files before deleting the database row.
        deleteStoredAnnouncementFile(announcement.getImagePath());
        deleteStoredAnnouncementFile(announcement.getPdfFilePath());

        announcementRepository.delete(announcement);

        return ResponseEntity.noContent().build();
    }


    // =========================================================
    // DELETE STORED ANNOUNCEMENT FILE SAFELY
    // =========================================================

    private void deleteStoredAnnouncementFile(String storedPath) throws IOException {

        if (storedPath == null || storedPath.isBlank()) {
            return;
        }

        Path uploadRoot =
                Paths.get(System.getProperty("user.dir"), "uploads", "announcements")
                        .toAbsolutePath()
                        .normalize();

        Path path = Paths.get(storedPath);

        if (!path.isAbsolute()) {
            path = Paths.get(System.getProperty("user.dir"))
                    .resolve(path)
                    .normalize();
        } else {
            path = path.toAbsolutePath().normalize();
        }

        // Never allow a database value to delete a file outside the
        // announcement upload directory.
        if (!path.startsWith(uploadRoot)) {
            Path fallback = uploadRoot
                    .resolve(path.getFileName().toString())
                    .normalize();

            if (fallback.startsWith(uploadRoot)) {
                path = fallback;
            } else {
                return;
            }
        }

        Files.deleteIfExists(path);
    }


    // =========================================================
    // MODERATOR - SUBMIT TO ZBC / BROADCAST
    // =========================================================

    @PatchMapping("/moderator/{id}/broadcast")
    public ResponseEntity<AnnouncementResponse>
    broadcastAnnouncement(
            @PathVariable Long id
    ) {

        Announcement announcement =
                findAnnouncement(id);

        // Only approved announcements can be broadcasted
        if (announcement.getStatus()
                != AnnouncementStatus.APPROVED) {

            throw new RuntimeException(
                    "Only approved announcements can be submitted to ZBC"
            );
        }

        if (announcement.getBroadcastScript() == null
                ||
                announcement.getBroadcastScript().isBlank()) {

            announcement.setBroadcastScript(
                    announcement.getGeneratedText()
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


    // =========================================================
    // MODERATOR - REMOVE PUBLIC ANNOUNCEMENT
    // =========================================================

    @DeleteMapping("/moderator/public/{id}")
    public ResponseEntity<Void> deletePublicAnnouncement(
            @PathVariable Long id
    ) throws IOException {
        Announcement announcement = findAnnouncement(id);

        if (announcement.getStatus() != AnnouncementStatus.APPROVED
                && announcement.getStatus() != AnnouncementStatus.BROADCASTED) {
            throw new IllegalStateException(
                    "Only approved or broadcasted announcements can be removed from public view"
            );
        }

        deleteStoredAnnouncementFile(announcement.getImagePath());
        deleteStoredAnnouncementFile(announcement.getPdfFilePath());
        announcementRepository.delete(announcement);

        return ResponseEntity.noContent().build();
    }


    // =========================================================
    // MODERATOR - UPDATE ANNOUNCEMENT
    // =========================================================

    @PutMapping("/moderator/{id}")
    public ResponseEntity<AnnouncementResponse>
    updateAnnouncement(
            @PathVariable Long id,
            @RequestBody AnnouncementRequest request
    ) {

        Announcement announcement =
                findAnnouncement(id);

        if (announcement.getStatus() != AnnouncementStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending announcements can be edited"
            );
        }

        validateAnnouncement(request);

        mapRequestToEntity(
                request,
                announcement
        );

        /*
         * Regenerate announcement text after
         * moderator makes corrections.
         */
        announcement.setGeneratedText(
                generateAnnouncementText(announcement)
        );

        /*
         * Keep broadcast script synchronized
         * when announcement is edited.
         */
        announcement.setBroadcastScript(
                announcement.getGeneratedText()
        );

        // Any moderator correction must go through approval again.
        announcement.setStatus(AnnouncementStatus.PENDING);
        announcement.setReviewedAt(null);
        announcement.setReviewedBy(null);
        announcement.setPdfFilePath(null);

        Announcement saved =
                announcementRepository.save(
                        announcement
                );

        return ResponseEntity.ok(
                toResponse(saved)
        );
    }


    // =========================================================
    // REFERENCE NUMBER
    // =========================================================

    private void ensureReferenceNumber(Announcement announcement) {
        if (announcement.getReferenceNumber() == null || announcement.getReferenceNumber().isBlank()) {
            if (announcement.getId() == null) {
                return;
            }
            announcement.setReferenceNumber(
                    String.format("ZPAMS-%d-%05d",
                            announcement.getSubmittedAt() != null
                                    ? announcement.getSubmittedAt().getYear()
                                    : LocalDateTime.now().getYear(),
                            announcement.getId())
            );
        }
    }


    // =========================================================
    // GENERATE OFFICIAL PDF
    // =========================================================

    private String generateAndStorePdf(Announcement announcement)
            throws IOException, DocumentException {

        Path uploadDirectory = Paths.get(
                System.getProperty("user.dir"),
                "uploads",
                "announcements"
        ).toAbsolutePath().normalize();

        Files.createDirectories(uploadDirectory);

        Path pdfPath = uploadDirectory.resolve(
                "announcement-" + announcement.getId() + ".pdf"
        );

        Document document = new Document();

        try {
            PdfWriter.getInstance(document, Files.newOutputStream(pdfPath));
            document.open();

            Font title = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font heading = new Font(Font.HELVETICA, 11, Font.BOLD);
            Font normal = new Font(Font.HELVETICA, 10, Font.NORMAL);

            Paragraph brand = new Paragraph(
                    "ZANZIBAR PUBLIC ANNOUNCEMENT MOBILE SYSTEM", title
            );
            brand.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(brand);

            Paragraph official = new Paragraph("OFFICIAL ANNOUNCEMENT", heading);
            official.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(official);
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.addCell(new Phrase("Reference Number", heading));
            table.addCell(new Phrase(
                    announcement.getReferenceNumber() == null ? "-" : announcement.getReferenceNumber(), normal));
            table.addCell(new Phrase("Announcement Type", heading));
            table.addCell(new Phrase(String.valueOf(announcement.getType()), normal));
            table.addCell(new Phrase("Applicant", heading));
            table.addCell(new Phrase(announcement.getApplicantName() == null ? "-" : announcement.getApplicantName(), normal));
            table.addCell(new Phrase("Submitted", heading));
            table.addCell(new Phrase(String.valueOf(announcement.getSubmittedAt()), normal));
            document.add(table);
            document.add(new Paragraph(" "));

            Paragraph contentHeading = new Paragraph("ANNOUNCEMENT TEXT", heading);
            document.add(contentHeading);
            document.add(new Paragraph(
                    announcement.getGeneratedText() == null ? "" : announcement.getGeneratedText(), normal));

            document.add(new Paragraph(" "));
            Paragraph radioHeading = new Paragraph("RADIO BROADCAST COPY", heading);
            document.add(radioHeading);
            document.add(new Paragraph(
                    announcement.getBroadcastScript() == null ? "" : announcement.getBroadcastScript(), normal));

            document.add(new Paragraph(" "));
            Paragraph footer = new Paragraph(
                    "Generated by ZPAMS • Official document for authorized broadcasting use",
                    new Font(Font.HELVETICA, 8, Font.ITALIC));
            footer.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(footer);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }

        return pdfPath.toAbsolutePath().normalize().toString();
    }


    // =========================================================
    // FIND ANNOUNCEMENT
    // =========================================================

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


    // =========================================================
    // SEARCH MATCHING
    // =========================================================

    private boolean matchesSearch(
            Announcement a,
            String keyword
    ) {

        if (keyword.isBlank()) {
            return true;
        }

        return contains(a.getReferenceNumber(), keyword)
                || contains(a.getApplicantName(), keyword)
                || contains(a.getPhoneNumber(), keyword)
                || contains(a.getDeceasedName(), keyword)
                || contains(a.getItemName(), keyword)
                || contains(a.getDescription(), keyword)
                || contains(a.getLocation(), keyword)
                || contains(a.getStatus() != null
                ? a.getStatus().name()
                : null, keyword)
                || contains(a.getType() != null
                ? a.getType().name()
                : null, keyword);
    }


    private boolean contains(
            String value,
            String keyword
    ) {

        return value != null
                &&
                value.toLowerCase()
                        .contains(keyword);
    }


    // =========================================================
    // VALIDATE ANNOUNCEMENT
    // =========================================================

    private void validateAnnouncement(
            AnnouncementRequest request
    ) {

        if (request.getType() == null) {

            throw new IllegalArgumentException(
                    "Announcement type is required"
            );
        }


        // =====================================================
        // DEATH
        // =====================================================

        if (request.getType()
                == AnnouncementType.DEATH) {

            if (isBlank(
                    request.getDeceasedName()
            )) {

                throw new IllegalArgumentException(
                        "Deceased name is required"
                );
            }

            if (request.getDateOfDeath() == null) {

                throw new IllegalArgumentException(
                        "Date of death is required"
                );
            }

            if (request.getDateOfDeath()
                    .isAfter(LocalDate.now())) {

                throw new IllegalArgumentException(
                        "Date of death cannot be in the future"
                );
            }

            if (isBlank(
                    request.getBurialPlace()
            )) {

                throw new IllegalArgumentException(
                        "Burial place is required"
                );
            }

            if (request.getBurialDate() != null
                    &&
                    request.getDateOfDeath() != null
                    &&
                    request.getBurialDate()
                            .isBefore(
                                    request.getDateOfDeath()
                            )) {

                throw new IllegalArgumentException(
                        "Burial date cannot be before date of death"
                );
            }
        }


        // =====================================================
        // LOST / FOUND
        // =====================================================

        if (request.getType()
                == AnnouncementType.LOST
                ||
                request.getType()
                        == AnnouncementType.FOUND) {

            if (isBlank(
                    request.getItemName()
            )) {

                throw new IllegalArgumentException(
                        "Item name is required"
                );
            }

            if (isBlank(
                    request.getDescription()
            )) {

                throw new IllegalArgumentException(
                        "Item description is required"
                );
            }

            if (isBlank(
                    request.getLocation()
            )) {

                throw new IllegalArgumentException(
                        "Location is required"
                );
            }

            if (request.getEventDate() == null) {

                throw new IllegalArgumentException(
                        "Date is required"
                );
            }

            if (request.getEventDate()
                    .isAfter(LocalDate.now())) {

                throw new IllegalArgumentException(
                        "Event date cannot be in the future"
                );
            }
        }
    }


    // =========================================================
    // CHECK BLANK
    // =========================================================

    private boolean isBlank(
            String value
    ) {

        return value == null
                || value.isBlank();
    }


    // =========================================================
    // REQUEST → ENTITY
    // =========================================================

    private void mapRequestToEntity(
            AnnouncementRequest request,
            Announcement announcement
    ) {

        // =====================================================
        // GENERAL
        // =====================================================

        announcement.setType(
                request.getType()
        );

        announcement.setApplicantName(
                request.getApplicantName()
        );

        announcement.setPhoneNumber(
                request.getPhoneNumber()
        );


        // =====================================================
        // DEATH
        // =====================================================

        announcement.setDeceasedName(
                request.getDeceasedName()
        );

        announcement.setGender(
                request.getGender()
        );

        announcement.setAge(
                request.getAge()
        );

        announcement.setDateOfDeath(
                request.getDateOfDeath()
        );

        announcement.setPlaceOfDeath(
                request.getPlaceOfDeath()
        );

        announcement.setBurialPlace(
                request.getBurialPlace()
        );

        announcement.setBurialDate(
                request.getBurialDate()
        );

        announcement.setBurialTime(
                request.getBurialTime()
        );

        announcement.setFamilyMembers(
                request.getFamilyMembers()
        );

        announcement.setContactPerson(
                request.getContactPerson()
        );


        // =====================================================
        // LOST / FOUND
        // =====================================================

        announcement.setItemName(
                request.getItemName()
        );

        announcement.setItemCategory(
                request.getItemCategory()
        );

        announcement.setDescription(
                request.getDescription()
        );

        announcement.setLocation(
                request.getLocation()
        );

        announcement.setEventDate(
                request.getEventDate()
        );

        announcement.setReward(
                request.getReward()
        );


        // =====================================================
        // GENERAL
        // =====================================================

        announcement.setAdditionalInformation(
                request.getAdditionalInformation()
        );
    }


    // =========================================================
    // ENTITY → RESPONSE
    // =========================================================

    private AnnouncementResponse toResponse(
            Announcement a
    ) {

        return AnnouncementResponse.builder()

                .id(a.getId())
                .referenceNumber(a.getReferenceNumber())
                .type(a.getType())

                // Applicant
                .applicantName(a.getApplicantName())
                .phoneNumber(a.getPhoneNumber())

                // Death
                .deceasedName(a.getDeceasedName())
                .gender(a.getGender())
                .age(a.getAge())
                .dateOfDeath(a.getDateOfDeath())
                .placeOfDeath(a.getPlaceOfDeath())
                .burialPlace(a.getBurialPlace())
                .burialDate(a.getBurialDate())
                .burialTime(a.getBurialTime())
                .familyMembers(a.getFamilyMembers())
                .contactPerson(a.getContactPerson())

                // Lost / Found
                .itemName(a.getItemName())
                .itemCategory(a.getItemCategory())
                .description(a.getDescription())
                .location(a.getLocation())
                .eventDate(a.getEventDate())
                .reward(a.getReward())

                // General
                .additionalInformation(
                        a.getAdditionalInformation()
                )

                // Generated
                .generatedText(
                        a.getGeneratedText()
                )

                .broadcastScript(
                        a.getBroadcastScript()
                )

                // Files
                .imagePath(
                        a.getImagePath()
                )

                .pdfFilePath(
                        a.getPdfFilePath()
                )

                // Status
                .status(
                        a.getStatus()
                )

                // Dates
                .createdAt(
                        a.getCreatedAt()
                )

                .submittedAt(
                        a.getSubmittedAt()
                )

                .reviewedAt(
                        a.getReviewedAt()
                )

                // Review
                .reviewedBy(
                        a.getReviewedBy() != null
                                ? a.getReviewedBy().getId()
                                : null
                )

                .reviewRemarks(
                        a.getReviewRemarks()
                )

                .build();
    }


    // =========================================================
    // GENERATE ANNOUNCEMENT
    // =========================================================

    private String generateAnnouncementText(
            Announcement announcement
    ) {

        return switch (
                announcement.getType()
                ) {

            case DEATH ->
                    generateDeathText(announcement);

            case LOST ->
                    generateLostText(announcement);

            case FOUND ->
                    generateFoundText(announcement);
        };
    }


    // =========================================================
    // DEATH TEXT
    // =========================================================

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


        if (a.getGender() != null
                && !a.getGender().isBlank()) {

            text.append(
                    "The deceased was "
            );

            text.append(
                    a.getGender()
            );

            text.append(". ");
        }


        if (a.getAge() != null) {

            text.append(
                    "The deceased was "
            );

            text.append(
                    a.getAge()
            );

            text.append(
                    " years old. "
            );
        }


        if (a.getDateOfDeath() != null) {

            text.append(
                    "The deceased passed away on "
            );

            text.append(
                    a.getDateOfDeath()
            );

            text.append(". ");
        }


        if (a.getPlaceOfDeath() != null
                && !a.getPlaceOfDeath().isBlank()) {

            text.append(
                    "The death occurred at "
            );

            text.append(
                    a.getPlaceOfDeath()
            );

            text.append(". ");
        }


        if (a.getBurialPlace() != null
                && !a.getBurialPlace().isBlank()) {

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


        if (a.getFamilyMembers() != null
                && !a.getFamilyMembers().isBlank()) {

            text.append(
                    "The announcement is made by the family: "
            );

            text.append(
                    a.getFamilyMembers()
            );

            text.append(". ");
        }


        if (a.getContactPerson() != null
                && !a.getContactPerson().isBlank()) {

            text.append(
                    "Contact person: "
            );

            text.append(
                    a.getContactPerson()
            );

            text.append(". ");
        }


        if (a.getAdditionalInformation() != null
                && !a.getAdditionalInformation().isBlank()) {

            text.append(
                    a.getAdditionalInformation()
            );

            text.append(" ");
        }


        text.append(
                "May the deceased rest in peace."
        );

        return text.toString();
    }


    // =========================================================
    // LOST TEXT
    // =========================================================

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
                && !a.getItemCategory().isBlank()) {

            text.append(
                    "Category: "
            );

            text.append(
                    a.getItemCategory()
            );

            text.append(". ");
        }


        if (a.getDescription() != null
                && !a.getDescription().isBlank()) {

            text.append(
                    "Description: "
            );

            text.append(
                    a.getDescription()
            );

            text.append(". ");
        }


        if (a.getLocation() != null
                && !a.getLocation().isBlank()) {

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
                && !a.getReward().isBlank()) {

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


    // =========================================================
    // FOUND TEXT
    // =========================================================

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
                && !a.getItemCategory().isBlank()) {

            text.append(
                    "Category: "
            );

            text.append(
                    a.getItemCategory()
            );

            text.append(". ");
        }


        if (a.getDescription() != null
                && !a.getDescription().isBlank()) {

            text.append(
                    "Description: "
            );

            text.append(
                    a.getDescription()
            );

            text.append(". ");
        }


        if (a.getLocation() != null
                && !a.getLocation().isBlank()) {

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


    // =========================================================
    // FILE TYPE CHECK
    // =========================================================

    private boolean isAllowedFileType(
            String contentType
    ) {

        return contentType.equalsIgnoreCase(
                "image/jpeg"
        )
                ||
                contentType.equalsIgnoreCase(
                        "image/png"
                )
                ||
                contentType.equalsIgnoreCase(
                        "image/webp"
                );
    }


    // =========================================================
    // FILE EXTENSION
    // =========================================================

    private String getFileExtension(
            String fileName
    ) {

        if (fileName == null
                || !fileName.contains(".")) {

            return "";
        }

        return fileName.substring(
                fileName.lastIndexOf(".")
        );
    }
}