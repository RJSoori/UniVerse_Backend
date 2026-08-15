package com.example.backend_service.common.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import jakarta.mail.internet.MimeMessage;

/**
 * Thin wrapper around Spring's JavaMailSender. Deliberately generic (not tied to any one
 * module), because UniVerse sends from three separate Gmail identities — JobHub,
 * Marketplace, and Student — each requiring its own authenticated JavaMailSender. There is
 * one EmailService instance per sender identity; see {@link MailConfig} for how those are
 * wired up. Not a singleton {@code @Service} for this reason — always injected via the
 * qualified beans MailConfig defines (e.g. {@code @Qualifier("jobhubEmailService")}).
 */
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String fromName;

    public EmailService(JavaMailSender mailSender, String fromAddress, String fromName) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
    }

    /**
     * Sends an HTML email. Throws EmailDeliveryException (unchecked) on failure so callers
     * can decide whether to surface it to the user or just log it.
     */
    public void sendHtml(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            helper.setFrom(fromAddress, fromName);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send email to {}", to, e);
            throw new EmailDeliveryException("Unable to send email", e);
        }
    }

    public static class EmailDeliveryException extends RuntimeException {
        public EmailDeliveryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
