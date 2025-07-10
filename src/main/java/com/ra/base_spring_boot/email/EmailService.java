package com.ra.base_spring_boot.email;

import com.ra.base_spring_boot.dto.req.ContactFormRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
//import lombok.Value;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendResetPasswordEmail(String to, String resetUrl) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject("Reset your password");
            helper.setText("<p>Click the link below to reset your password:</p>" +
                    "<p><a href=\"" + resetUrl + "\">Reset Password</a></p>", true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error sending email: " + e.getMessage());
        }
    }
    public void sendCartReminder(String toEmail, String username) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Giỏ hàng của bạn vẫn đang chờ!");
        message.setText("Chào " + username + ",\n\nBạn vẫn còn sản phẩm trong giỏ hàng. Hãy hoàn tất đơn hàng của mình nhé!\n\nCảm ơn bạn.");
        mailSender.send(message);
    }

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendContactEmail(ContactFormRequest request) {
        if (!isValidEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email người gửi không hợp lệ");
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail); // Email người gửi (được cấu hình)
            message.setTo("yasuaphongcao@gmail.com"); // Email người nhận (admin)
               message.setReplyTo(request.getEmail()); // Email của khách hàng
               message.setSubject("Liên hệ từ: " + request.getName());
               message.setText("Tên: " + request.getName() + "\n"
                    + "Email: " + request.getEmail() + "\n"
                    + "Nội dung: " + request.getMessage());

            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Gửi email thất bại: " + e.getMessage());
        }
    }
    public boolean isValidEmail(String email) {
        return EmailValidator.getInstance().isValid(email);
    }

}
