package team.suajung.ad.ress.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.base-url}")
    private String baseUrl;

    public void sendVerificationEmail(String to, String token) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject("[AD*RESS] 이메일 주소 인증을 완료해주세요");

        String verificationUrl = baseUrl + "/verify-email?token=" + token;

        String emailContent =
                "<!DOCTYPE html>" +
                        "<html lang='ko'>" +
                        "<head>" +
                        "<meta charset='UTF-8'>" +
                        "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                        "<title>이메일 인증</title>" +
                        "</head>" +
                        "<body style='margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4;'>" +
                        "<table border='0' cellpadding='0' cellspacing='0' width='100%' style='max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);'>" +
                        "<tr>" +
                        "<td align='center' bgcolor='#4285F4' style='padding: 20px 0; border-top-left-radius: 8px; border-top-right-radius: 8px;'>" +
                        "<img src='https://your-service-domain.com/logo.png' alt='로고' width='150' style='display: block;'>" +
                        "</td>" +
                        "</tr>" +
                        "<tr>" +
                        "<td style='padding: 40px 30px;'>" +
                        "<h1 style='color: #333333; font-size: 24px; margin: 0 0 20px 0; text-align: center;'>이메일 주소 인증</h1>" +
                        "<p style='color: #666666; font-size: 16px; line-height: 1.6; margin-bottom: 30px;'>안녕하세요,</p>" +
                        "<p style='color: #666666; font-size: 16px; line-height: 1.6; margin-bottom: 30px;'>회원가입을 완료하려면 아래 버튼을 클릭하여 이메일 주소를 인증해주세요. 이 링크는 <strong>24시간</strong> 동안 유효합니다.</p>" +
                        "<table border='0' cellpadding='0' cellspacing='0' width='100%'>" +
                        "<tr>" +
                        "<td align='center'>" +
                        "<a href='" + verificationUrl + "' target='_blank' style='background-color: #4285F4; border-radius: 4px; color: #ffffff; display: inline-block; font-size: 16px; font-weight: bold; padding: 12px 24px; text-decoration: none; text-align: center;'>이메일 인증하기</a>" +
                        "</td>" +
                        "</tr>" +
                        "</table>" +
                        "<p style='color: #666666; font-size: 16px; line-height: 1.6; margin-top: 30px;'>버튼이 작동하지 않는 경우, 아래 링크를 브라우저 주소창에 복사하여 붙여넣기 해주세요:</p>" +
                        "<p style='color: #999999; font-size: 14px; line-height: 1.4; word-break: break-all; background-color: #f7f7f7; padding: 10px; border-radius: 4px;'>" + verificationUrl + "</p>" +
                        "</td>" +
                        "</tr>" +
                        "<tr>" +
                        "<td style='padding: 20px 30px; background-color: #f8f8f8; border-bottom-left-radius: 8px; border-bottom-right-radius: 8px; color: #999999; font-size: 14px; text-align: center;'>" +
                        "<p style='margin: 0 0 10px 0;'>이 이메일은 자동으로 발송되었습니다. 회원가입 요청을 하지 않았다면 이 이메일을 무시하셔도 됩니다.</p>" +
                        "<p style='margin: 0;'>문의사항은 s01035495574@gmail.com으로 연락해주세요.</p>" +
                        "</td>" +
                        "</tr>" +
                        "</table>" +
                        "</body>" +
                        "</html>";

        helper.setText(emailContent, true);
        mailSender.send(message);
    }


    public void sendPasswordResetEmail(String to, String token) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject("[AD*RESS] 비밀번호 재설정 안내");

        String resetUrl = baseUrl + "/reset-password?token=" + token;

        String emailContent =
                "<!DOCTYPE html>" +
                        "<html lang='ko'>" +
                        "<head>" +
                        "<meta charset='UTF-8'>" +
                        "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                        "<title>비밀번호 재설정</title>" +
                        "</head>" +
                        "<body style='margin: 0; padding: 0; font-family: 'Noto Sans KR', Arial, sans-serif; background-color: #f4f4f4;'>" +
                        "<table border='0' cellpadding='0' cellspacing='0' width='100%' style='max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);'>" +
                        "<tr>" +
                        "<td align='center' bgcolor='#3B82F6' style='padding: 24px 0; border-top-left-radius: 8px; border-top-right-radius: 8px;'>" +
                        "<img src='https://your-service-domain.com/logo.png' alt='로고' width='150' style='display: block;'>" +
                        "</td>" +
                        "</tr>" +
                        "<tr>" +
                        "<td style='padding: 40px 30px;'>" +
                        "<table border='0' cellpadding='0' cellspacing='0' width='100%'>" +
                        "<tr>" +
                        "<td style='padding-bottom: 20px;'>" +
                        "<h1 style='color: #1F2937; font-size: 24px; margin: 0; text-align: center;'>비밀번호 재설정</h1>" +
                        "</td>" +
                        "</tr>" +
                        "<tr>" +
                        "<td style='padding-bottom: 25px;'>" +
                        "<p style='color: #4B5563; font-size: 16px; line-height: 1.6; margin: 0;'>안녕하세요,</p>" +
                        "<p style='color: #4B5563; font-size: 16px; line-height: 1.6; margin: 16px 0 0;'>계정에 대한 비밀번호 재설정 요청이 접수되었습니다. 아래 버튼을 클릭하여 새 비밀번호를 설정하세요.</p>" +
                        "</td>" +
                        "</tr>" +
                        "<tr>" +
                        "<td align='center' style='padding-bottom: 25px;'>" +
                        "<a href='" + resetUrl + "' target='_blank' style='background-color: #3B82F6; border-radius: 4px; color: #ffffff; display: inline-block; font-size: 16px; font-weight: bold; padding: 12px 35px; text-decoration: none; text-align: center;'>비밀번호 재설정하기</a>" +
                        "</td>" +
                        "</tr>" +
                        "<tr>" +
                        "<td style='padding-bottom: 25px;'>" +
                        "<p style='color: #4B5563; font-size: 16px; line-height: 1.6; margin: 0;'>이 링크는 <strong>보안을 위해 1시간 동안만 유효</strong>합니다.</p>" +
                        "<p style='color: #4B5563; font-size: 16px; line-height: 1.6; margin: 16px 0 0;'>버튼이 작동하지 않는 경우, 아래 URL을 브라우저에 복사하여 붙여넣기 해주세요:</p>" +
                        "<p style='color: #6B7280; font-size: 14px; line-height: 1.4; word-break: break-all; background-color: #f7f7f7; padding: 12px; border-radius: 4px; margin: 8px 0 0;'>" + resetUrl + "</p>" +
                        "</td>" +
                        "</tr>" +
                        "<tr>" +
                        "<td style='padding: 16px; background-color: #FFFBEB; border-left: 4px solid #F59E0B; border-radius: 4px;'>" +
                        "<p style='color: #92400E; font-size: 14px; line-height: 1.6; margin: 0;'>" +
                        "<strong>⚠️ 보안 안내:</strong> 비밀번호 재설정을 요청하지 않았다면, 계정 보안을 위해 s01035495574gmail.com으로 즉시 알려주시기 바랍니다." +
                        "</p>" +
                        "</td>" +
                        "</tr>" +
                        "</table>" +
                        "</td>" +
                        "</tr>" +
                        "<tr>" +
                        "<td style='padding: 20px 30px; background-color: #F9FAFB; border-bottom-left-radius: 8px; border-bottom-right-radius: 8px; color: #6B7280; font-size: 14px; text-align: center;'>" +
                        "<p style='margin: 0;'>도움이 필요하시면 s01035495574@gmail.com로 문의해주세요.</p>" +
                        "</td>" +
                        "</tr>" +
                        "</table>" +
                        "</body>" +
                        "</html>";

        helper.setText(emailContent, true);
        mailSender.send(message);
    }

}

