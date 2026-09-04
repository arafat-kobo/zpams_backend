package com.Zanzibar.Public.Announcement.email;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendStaffCredentials(String fullName, String email, String password, String role) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("ZPAMS Staff Account Created");
        message.setText(
                "Dear " + fullName + ",\n\n" +
                "Your staff account for the Zanzibar Public Announcement Mobile System (ZPAMS) has been created by the Administrator.\n\n" +
                "Account details:\n" +
                "Full Name: " + fullName + "\n" +
                "Email: " + email + "\n" +
                "Role: " + role + "\n" +
                "Password: " + password + "\n\n" +
                "You can use these credentials to log in to the ZPAMS system.\n\n" +
                "Please keep your login credentials secure.\n\n" +
                "Regards,\n" +
                "ZPAMS Administration"
        );

        mailSender.send(message);
    }
}
