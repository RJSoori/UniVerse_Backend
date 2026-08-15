package com.example.backend_service.common.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * UniVerse sends transactional email from three separate Gmail accounts, one per
 * module — a recruiter password-reset email should come from "UniVerse JobHub", not a
 * generic shared address, and likewise for Marketplace and Student. Spring Boot's
 * spring.mail.* autoconfiguration only ever builds a single JavaMailSender, so each
 * identity is wired here by hand: one JavaMailSenderImpl + one qualified EmailService
 * bean per account, all pointed at Gmail's SMTP endpoint with STARTTLS.
 */
@Configuration
public class MailConfig {

    @Bean
    public JavaMailSender jobhubMailSender(
            @Value("${app.mail.jobhub.username:}") String username,
            @Value("${app.mail.jobhub.app-password:}") String appPassword) {
        return buildSender(username, appPassword);
    }

    @Bean
    public EmailService jobhubEmailService(
            JavaMailSender jobhubMailSender,
            @Value("${app.mail.jobhub.username:}") String username,
            @Value("${app.mail.jobhub.from-name:UniVerse JobHub}") String fromName) {
        return new EmailService(jobhubMailSender, username, fromName);
    }

    @Bean
    public JavaMailSender marketplaceMailSender(
            @Value("${app.mail.marketplace.username:}") String username,
            @Value("${app.mail.marketplace.app-password:}") String appPassword) {
        return buildSender(username, appPassword);
    }

    @Bean
    public EmailService marketplaceEmailService(
            JavaMailSender marketplaceMailSender,
            @Value("${app.mail.marketplace.username:}") String username,
            @Value("${app.mail.marketplace.from-name:UniVerse Marketplace}") String fromName) {
        return new EmailService(marketplaceMailSender, username, fromName);
    }

    @Bean
    public JavaMailSender studentMailSender(
            @Value("${app.mail.student.username:}") String username,
            @Value("${app.mail.student.app-password:}") String appPassword) {
        return buildSender(username, appPassword);
    }

    @Bean
    public EmailService studentEmailService(
            JavaMailSender studentMailSender,
            @Value("${app.mail.student.username:}") String username,
            @Value("${app.mail.student.from-name:UniVerse}") String fromName) {
        return new EmailService(studentMailSender, username, fromName);
    }

    private JavaMailSender buildSender(String username, String appPassword) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost("smtp.gmail.com");
        sender.setPort(587);
        sender.setUsername(username);
        sender.setPassword(appPassword);

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        return sender;
    }
}
