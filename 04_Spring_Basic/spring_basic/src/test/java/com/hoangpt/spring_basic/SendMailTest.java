package com.hoangpt.spring_basic;

import com.hoangpt.spring_basic.utils.EmailSender;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

@SpringBootTest
@RequiredArgsConstructor
public class SendMailTest {
    private final EmailSender mailSender;

    @Test
    void sendMail() {
       String to = "hoangpham130201@gmail.com";
       String subject = "Test OTP Simple";
       String content = "This is a test";

       mailSender.sendEmail(to, subject, content);
    }

}
