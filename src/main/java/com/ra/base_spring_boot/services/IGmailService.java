package com.ra.base_spring_boot.services;

public interface IGmailService {
    void sendEmail(String toEmail, String subject, String bodyText) throws Exception;

    void sendEmailAfterDelayInMinutes(String toEmail, String subject, String bodyText, int delayMinutes);

}
