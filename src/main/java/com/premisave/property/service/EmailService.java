package com.premisave.property.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Sends notice emails using the HTML template at
 * resources/templates/email/notice-email.html (see NOTICE_TEMPLATE).
 *
 * Best-effort send — failures are logged, never thrown. The Notice document
 * is already saved by the time this is called, so an email outage must
 * never roll back or block a notice that has already been recorded.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final String NOTICE_TEMPLATE = "email/notice-email";
    private static final DateTimeFormatter SENT_AT_FORMATTER = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a");

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${notifications.email.from:no-reply@premisave.com}")
    private String fromAddress;

    public boolean sendNoticeEmail(String toEmail, String tenantName, String subject,
                                    String noticeType, String content) {
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Skipping notice email — recipient has no email on file");
            return false;
        }

        try {
            String html = renderNoticeEmail(tenantName, subject, noticeType, content);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true); // true = HTML body

            mailSender.send(mimeMessage);
            return true;
        } catch (MessagingException | RuntimeException e) {
            log.error("Failed to send notice email to {}: {}", toEmail, e.getMessage());
            return false;
        }
    }

    private String renderNoticeEmail(String tenantName, String subject, String noticeType, String content) {
        Context context = new Context();
        context.setVariable("tenantName", (tenantName != null && !tenantName.isBlank()) ? tenantName : "there");
        context.setVariable("subject", subject);
        context.setVariable("noticeType", formatNoticeType(noticeType));
        context.setVariable("sentAt", LocalDateTime.now().format(SENT_AT_FORMATTER));

        // content is free text typed by a home owner and rendered via
        // th:utext (raw HTML) so line breaks can become <br/>. Escape
        // everything first, THEN reintroduce <br/> for line breaks only —
        // never pass the raw owner-supplied string straight into th:utext.
        context.setVariable("content", toSafeHtml(content));

        return templateEngine.process(NOTICE_TEMPLATE, context);
    }

    private String toSafeHtml(String content) {
        if (content == null) {
            return "";
        }
        return escapeHtml(content).replace("\n", "<br/>");
    }

    private String escapeHtml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String formatNoticeType(String noticeType) {
        if (noticeType == null || noticeType.isBlank()) {
            return "Notice";
        }
        StringBuilder result = new StringBuilder();
        for (String word : noticeType.toLowerCase().split("_")) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(' ');
            }
        }
        return result.toString().trim();
    }
}