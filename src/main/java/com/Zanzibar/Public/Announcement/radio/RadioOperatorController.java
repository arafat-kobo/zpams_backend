package com.Zanzibar.Public.Announcement.radio;

import com.Zanzibar.Public.Announcement.annoucement.*;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.io.*;
import java.nio.file.*;
import java.util.List;

@RestController
@RequestMapping("/api/radio")
@RequiredArgsConstructor
public class RadioOperatorController {
    private final AnnouncementRepository announcementRepository;

    @GetMapping("/announcements")
    public ResponseEntity<List<AnnouncementResponse>> getApprovedAnnouncements() {
        return ResponseEntity.ok(announcementRepository.findByStatus(AnnouncementStatus.APPROVED)
                .stream().map(this::toResponse).toList());
    }

    @GetMapping(value="/announcements/{id}/pdf", produces=MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> getPdf(@PathVariable Long id) throws IOException {
        Announcement a=find(id);
        if(a.getStatus()!=AnnouncementStatus.APPROVED && a.getStatus()!=AnnouncementStatus.BROADCASTED)
            return ResponseEntity.notFound().build();
        Path path=resolvePdf(a);
        if(!Files.exists(path) || !Files.isRegularFile(path)) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"inline; filename=announcement-"+id+".pdf")
                .contentType(MediaType.APPLICATION_PDF).body(new UrlResource(path.toUri()));
    }

    @PatchMapping("/announcements/{id}/broadcast")
    public ResponseEntity<AnnouncementResponse> markAsBroadcasted(@PathVariable Long id) {
        Announcement a=find(id);
        if(a.getStatus()!=AnnouncementStatus.APPROVED) throw new RuntimeException("Only approved announcements can be marked as broadcasted");
        a.setStatus(AnnouncementStatus.BROADCASTED);
        if(a.getBroadcastScript()==null || a.getBroadcastScript().isBlank()) a.setBroadcastScript(a.getGeneratedText());
        return ResponseEntity.ok(toResponse(announcementRepository.save(a)));
    }

    private Path resolvePdf(Announcement a) throws IOException {
        if(a.getPdfFilePath()!=null && !a.getPdfFilePath().isBlank()) {
            Path p=Paths.get(a.getPdfFilePath());
            if(!p.isAbsolute()) p=Paths.get(System.getProperty("user.dir")).resolve(p).normalize();
            if(Files.exists(p) && Files.isRegularFile(p)) return p;
        }
        Path dir=Paths.get(System.getProperty("user.dir"),"uploads","announcements");
        Files.createDirectories(dir);
        Path pdf=dir.resolve("announcement-"+a.getId()+".pdf");
        try(ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            Document d=new Document(); PdfWriter.getInstance(d,out); d.open();
            d.add(new Paragraph("ZANZIBAR PUBLIC ANNOUNCEMENT")); d.add(new Paragraph(" "));
            d.add(new Paragraph("Announcement ID: "+a.getId())); d.add(new Paragraph("Type: "+a.getType()));
            d.add(new Paragraph("Applicant: "+a.getApplicantName())); d.add(new Paragraph("Phone: "+a.getPhoneNumber()));
            d.add(new Paragraph(" ")); d.add(new Paragraph(a.getGeneratedText()==null?"":a.getGeneratedText())); d.close();
            Files.write(pdf,out.toByteArray());
        } catch(DocumentException e) { throw new IOException("Failed to generate PDF",e); }
        a.setPdfFilePath(pdf.toString()); announcementRepository.save(a); return pdf;
    }
    private Announcement find(Long id){return announcementRepository.findById(id).orElseThrow(()->new RuntimeException("Announcement not found"));}
    private AnnouncementResponse toResponse(Announcement a){return AnnouncementResponse.builder().id(a.getId()).referenceNumber(a.getReferenceNumber()).type(a.getType())
        .applicantName(a.getApplicantName()).phoneNumber(a.getPhoneNumber()).deceasedName(a.getDeceasedName()).gender(a.getGender()).age(a.getAge())
        .dateOfDeath(a.getDateOfDeath()).placeOfDeath(a.getPlaceOfDeath()).burialPlace(a.getBurialPlace()).burialDate(a.getBurialDate()).burialTime(a.getBurialTime())
        .familyMembers(a.getFamilyMembers()).contactPerson(a.getContactPerson()).itemName(a.getItemName()).itemCategory(a.getItemCategory()).description(a.getDescription())
        .location(a.getLocation()).eventDate(a.getEventDate()).reward(a.getReward()).additionalInformation(a.getAdditionalInformation()).generatedText(a.getGeneratedText())
        .broadcastScript(a.getBroadcastScript()).imagePath(a.getImagePath()).pdfFilePath(a.getPdfFilePath()).status(a.getStatus()).createdAt(a.getCreatedAt())
        .submittedAt(a.getSubmittedAt()).reviewedAt(a.getReviewedAt()).reviewedBy(a.getReviewedBy()==null?null:a.getReviewedBy().getId()).build();}
}
