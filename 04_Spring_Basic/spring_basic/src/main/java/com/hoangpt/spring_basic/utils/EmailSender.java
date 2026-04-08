package com.hoangpt.spring_basic.utils;

import jakarta.mail.internet.MimeMessage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmailSender {
    private static final String EMAIL_HOST = "hoangpham130201@gmail.com";

    JavaMailSender javaMailSender;

    public void sendEmail( String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        message.setFrom(EMAIL_HOST);
        try {
            javaMailSender.send(message);
            System.out.println("Email sent");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void SendHtmlEmail( String to, String subject, String content) {
       try {
           MimeMessage mimeMessage = javaMailSender.createMimeMessage();
           MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);


           mimeMessageHelper.setFrom(EMAIL_HOST);
           mimeMessageHelper.setTo(to);
           mimeMessageHelper.setSubject(subject);
           mimeMessageHelper.setText(content);
           System.out.println("Email sent");
       } catch ( Exception e) {
           throw new RuntimeException(e);
       }
    }

}
