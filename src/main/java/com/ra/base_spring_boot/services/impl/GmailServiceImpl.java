package com.ra.base_spring_boot.services.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;

import com.ra.base_spring_boot.services.IGmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@Service
public class GmailServiceImpl implements IGmailService {
    private static final String CLIENT_ID = "697567886606-je9ktolau7gb1fab9cvbalskfku6ii14.apps.googleusercontent.com";
    private static final String CLIENT_SECRET = "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";
    private static final String REFRESH_TOKEN = "1//04CljnauBZVOaCgYIARAAGAQSNwF-L9IrVzn0AHjFq6cbF8j8X239vsGWzs3nNYIkcElr3UyJI_DU4MXxc6jTOtqD4GeiW3rN2GI";
    private static final String FROM_EMAIL = "phuchgce181933@gmail.com"; // Thay bằng email xác thực với Google
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Override
    public void sendEmail(String toEmail, String subject, String bodyText) throws Exception {
        GoogleCredential credential = new GoogleCredential.Builder()
                .setClientSecrets(CLIENT_ID, CLIENT_SECRET)
                .setTransport(GoogleNetHttpTransport.newTrustedTransport())
                .setJsonFactory(JacksonFactory.getDefaultInstance())
                .build()
                .setRefreshToken(REFRESH_TOKEN);

        credential.refreshToken();

        Gmail gmail = new Gmail.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                credential)
                .setApplicationName("Ecommer App")
                .build();

        MimeMessage email = createEmail(toEmail, FROM_EMAIL, subject, bodyText);
        Message message = createMessageWithEmail(email);
        gmail.users().messages().send("me", message).execute();
    }

    @Override
    public void sendEmailAfterDelayInMinutes(String toEmail, String subject, String bodyText, int delayMinutes) {
        scheduler.schedule(() -> {
            try {
                sendEmail(toEmail, subject, bodyText);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, delayMinutes, TimeUnit.MINUTES);
    }

    private MimeMessage createEmail(String to, String from, String subject, String bodyText) throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(from));
        email.addRecipient(MimeMessage.RecipientType.TO, new InternetAddress(to));
        email.setSubject(subject);
        email.setText(bodyText);
        return email;
    }

    private Message createMessageWithEmail(MimeMessage emailContent) throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        String encodedEmail = Base64.getUrlEncoder().encodeToString(buffer.toByteArray());

        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

    }
