package com.example.backend_service.common.email;

import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@code sendHtml} is what registration/password-reset emails use and must stay exactly as it
 * was — HTML-only, single part. {@code sendMultipartHtml} is the separate, invite-only method:
 * a message with only an HTML part (no plain-text alternative) is one of the classic signals
 * spam filters hold against a sender with no reputation, so invites go out as a proper
 * multipart/alternative message instead. These tests lock in both behaviors so a future change
 * can't accidentally blend them back together.
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Test
    void sendHtml_staysSinglePartHtmlOnly() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        EmailService service = new EmailService(mailSender, "noreply@example.com", "UniVerse");
        service.sendHtml("student@example.com", "Verify your email", "<p>Your code: <strong>123456</strong></p>");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage sent = captor.getValue();
        sent.saveChanges();

        assertThat(sent.getContentType()).containsIgnoringCase("text/html");
        assertThat(sent.getContent()).isInstanceOf(String.class);
        assertThat((String) sent.getContent()).contains("123456");
    }

    @Test
    void sendMultipartHtml_includesBothPlainTextAndHtmlParts() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        EmailService service = new EmailService(mailSender, "noreply@example.com", "UniVerse");
        service.sendMultipartHtml(
                "friend@example.com",
                "Join us",
                "<div><p>Hello <strong>there</strong></p><p>Link: <a href=\"https://x/y\">here</a></p></div>"
        );

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage sent = captor.getValue();
        // The real JavaMailSenderImpl.send() calls this before handing off to Transport; the
        // mocked sender here never gets that far, so finalize the headers ourselves.
        sent.saveChanges();

        assertThat(sent.getContentType()).containsIgnoringCase("multipart");

        List<BodyPart> parts = collectLeafParts(sent.getContent());
        BodyPart plainPart = findPartWithType(parts, "text/plain");
        BodyPart htmlPart = findPartWithType(parts, "text/html");

        assertThat(plainPart).as("expected a text/plain alternative").isNotNull();
        assertThat(htmlPart).as("expected a text/html alternative").isNotNull();

        String plainText = (String) plainPart.getContent();
        assertThat(plainText).contains("Hello there").doesNotContain("<strong>");

        String htmlText = (String) htmlPart.getContent();
        assertThat(htmlText).contains("<strong>there</strong>");
    }

    private BodyPart findPartWithType(List<BodyPart> parts, String contentType) throws Exception {
        for (BodyPart part : parts) {
            if (part.isMimeType(contentType)) {
                return part;
            }
        }
        return null;
    }

    /** Recursively flattens a (possibly nested multipart/mixed > multipart/alternative) tree. */
    private List<BodyPart> collectLeafParts(Object content) throws Exception {
        List<BodyPart> leaves = new ArrayList<>();
        if (!(content instanceof Multipart multipart)) {
            return leaves;
        }
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            if (part.getContent() instanceof Multipart nested) {
                leaves.addAll(collectLeafParts(nested));
            } else {
                leaves.add(part);
            }
        }
        return leaves;
    }
}
