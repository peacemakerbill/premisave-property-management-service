package com.premisave.property.service;

import com.premisave.property.email.EmailContent;
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

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Renders and sends Premisave emails.
 *
 * - {@link #send} sends a transactional email (payments, deposits, bills) from an
 *   {@link EmailContent}, rendered by templates/email/transaction.html.
 * - {@link #sendNoticeEmail} sends an owner-written notice, rendered by
 *   templates/email/notice-email.html.
 *
 * Every message is multipart (HTML + plain text) and marked auto-generated.
 * Best-effort: failures are logged, never thrown — the business record is always
 * saved before an email is attempted, and an email outage must never undo it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final String TRANSACTION_TEMPLATE = "email/transaction";
    private static final String NOTICE_TEMPLATE = "email/notice-email";
    private static final DateTimeFormatter SENT_AT_FORMATTER =
            DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH);

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${notifications.email.from:no-reply@premisave.com}")
    private String fromAddress;

    @Value("${notifications.email.from-name:Premisave}")
    private String brandName;

    @Value("${notifications.email.support-address:support@premisave.com}")
    private String supportAddress;

    @Value("${frontend.url:http://localhost:3000}")
    private String appUrl;

    public boolean send(String toEmail, String subject, EmailContent content) {
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Skipping email '{}' — recipient has no email on file", subject);
            return false;
        }
        try {
            Context context = baseContext();
            context.setVariable("c", content);
            String html = templateEngine.process(TRANSACTION_TEMPLATE, context);
            return deliver(toEmail, subject, html, content.toPlainText(brandName, supportAddress));
        } catch (RuntimeException e) {
            log.error("Failed to render email '{}' for {}: {}", subject, toEmail, e.getMessage());
            return false;
        }
    }

    public boolean sendNoticeEmail(String toEmail, String tenantName, String subject,
                                    String noticeType, String content) {
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Skipping notice email — recipient has no email on file");
            return false;
        }
        try {
            String name = (tenantName != null && !tenantName.isBlank()) ? tenantName : "there";

            Context context = baseContext();
            context.setVariable("tenantName", name);
            context.setVariable("subject", subject);
            context.setVariable("noticeType", formatNoticeType(noticeType));
            // content is free text typed by a home owner and rendered via th:utext (raw HTML) so
            // line breaks can become <br/>. Escape everything first, THEN reintroduce <br/> for
            // line breaks only — never pass the raw owner-supplied string straight into th:utext.
            context.setVariable("content", toSafeHtml(content));

            String html = templateEngine.process(NOTICE_TEMPLATE, context);
            String text = "Hi " + name + ",\n\n" + subject + "\n\n" + (content != null ? content : "")
                    + "\n\n--\n" + brandName + " | " + supportAddress
                    + "\nThis is an automated notice. Please do not reply directly to this email.\n";
            return deliver(toEmail, subject, html, text);
        } catch (RuntimeException e) {
            log.error("Failed to render notice email to {}: {}", toEmail, e.getMessage());
            return false;
        }
    }

    private boolean deliver(String toEmail, String subject, String html, String plainText) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "UTF-8");
            helper.setFrom(fromAddress, brandName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(plainText, html);            // plain-text part first, HTML alternative second
            mimeMessage.addHeader("Auto-Submitted", "auto-generated");

            mailSender.send(mimeMessage);
            return true;
        } catch (MessagingException | UnsupportedEncodingException | RuntimeException e) {
            log.error("Failed to send email '{}' to {}: {}", subject, toEmail, e.getMessage());
            return false;
        }
    }

    private Context baseContext() {
        Context context = new Context();
        context.setVariable("brandName", brandName);
        context.setVariable("supportEmail", supportAddress);
        context.setVariable("appUrl", appUrl);
        context.setVariable("year", Year.now().getValue());
        context.setVariable("sentAt", LocalDateTime.now().format(SENT_AT_FORMATTER));
        return context;
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