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

    /**
     * Sends an HTML email with an auto-derived plain-text alternative alongside it — a proper
     * multipart/alternative message, the way real mail clients produce mail, instead of
     * {@link #sendHtml}'s HTML-only body. An HTML-only message is one of the classic signals
     * spam filters use against a sender with no established reputation, which describes these
     * Gmail "noreply" accounts. This is a separate method rather than a change to
     * {@link #sendHtml} so it only affects callers that opt into it (currently just group-habit
     * invites) — the registration/password-reset email paths are intentionally left alone.
     *
     * <p>Throws EmailDeliveryException (unchecked) on failure so callers can decide whether to
     * surface it to the user or just log it.
     */
    public void sendMultipartHtml(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(toPlainText(htmlBody), htmlBody);
            helper.setFrom(fromAddress, fromName);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send email to {}", to, e);
            throw new EmailDeliveryException("Unable to send email", e);
        }
    }

    /**
     * Derives a readable plain-text version from one of our own HTML templates: turns
     * paragraph/line breaks into newlines, strips remaining tags, and unescapes the handful of
     * entities those templates use. Not a general-purpose HTML-to-text converter — just enough
     * for the simple templates this service actually sends.
     */
    private String toPlainText(String html) {
        String withBreaks = html
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>", "\n\n")
                .replaceAll("(?i)</div>", "\n");
        String noTags = withBreaks.replaceAll("<[^>]+>", "");
        String unescaped = noTags
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&nbsp;", " ")
                .replace("&bull;", "*");
        return unescaped
                .replaceAll("[ \\t]+", " ")
                .replaceAll(" *\\n *", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    public static class EmailDeliveryException extends RuntimeException {
        public EmailDeliveryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
