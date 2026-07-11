package com.premisave.property.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${notifications.email.from:no-reply@premisave.com}")
    private String fromAddress;

    /**
     * Best-effort send — failures are logged, never thrown. The Notice
     * document is already saved by the time this is called, so an email
     * outage must never roll back or block a notice that has been recorded.
     */
    public boolean sendNoticeEmail(String toEmail, String subject, String body) {
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Skipping notice email — recipient has no email on file");
            return false;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            log.error("Failed to send notice email to {}: {}", toEmail, e.getMessage());
            return false;
        }
    }
}