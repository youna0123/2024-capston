package team.suajung.ad.ress.auth.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import team.suajung.ad.ress.auth.exception.TokenExpiredException;
import team.suajung.ad.ress.auth.exception.TokenNotFoundException;
import team.suajung.ad.ress.auth.exception.UserNotFoundException;
import team.suajung.ad.ress.auth.service.AuthService;

@Controller
public class VerifyEmailAndPasswordResetController {

    @Autowired
    private AuthService authService;

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam String token, Model model) {
        try {
            boolean isTokenValid = authService.isPasswordResetTokenValid(token);

            if (!isTokenValid) {
                model.addAttribute("tokenExpired", true);
                return "reset-password";
            }

            model.addAttribute("token", token);
            model.addAttribute("tokenExpired", false);
            model.addAttribute("resetSuccess", false);
            return "reset-password";

        } catch (Exception e) {
            model.addAttribute("tokenExpired", true);
            model.addAttribute("errorMessage", "토큰 검증 중 오류가 발생했습니다: " + e.getMessage());
            return "reset-password";
        }
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String token, @RequestParam String password, Model model) {
        try {
            boolean resetSuccessful = authService.resetPassword(token, password);

            if (resetSuccessful) {
                model.addAttribute("resetSuccess", true);
                model.addAttribute("tokenExpired", false);
            } else {
                model.addAttribute("tokenExpired", true);
                model.addAttribute("resetSuccess", false);
            }
        } catch (Exception e) {
            model.addAttribute("tokenExpired", false);
            model.addAttribute("resetSuccess", false);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("token", token);
        }

        return "reset-password";
    }

    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam String token, Model model) {
        try {
            boolean verified = authService.verifyEmail(token);

            if (verified) {
                model.addAttribute("success", true);
                model.addAttribute("message", "이메일 인증이 성공적으로 완료되었습니다. 이제 로그인하실 수 있습니다.");
            } else {
                model.addAttribute("success", false);
                model.addAttribute("message", "인증 처리 중 문제가 발생했습니다. 다시 시도해주세요.");
            }
        } catch (TokenNotFoundException e) {
            model.addAttribute("success", false);
            model.addAttribute("message", "유효하지 않은 인증 토큰입니다. 인증 링크를 다시 확인해주세요.");
            model.addAttribute("error", "invalid_token");
        } catch (TokenExpiredException e) {
            model.addAttribute("success", false);
            model.addAttribute("message", "인증 토큰이 만료되었습니다. 새로운 인증 메일을 요청해주세요.");
            model.addAttribute("error", "expired_token");
            model.addAttribute("userId", e.getUserId()); // 재발송을 위한 사용자 ID
        } catch (UserNotFoundException e) {
            model.addAttribute("success", false);
            model.addAttribute("message", "사용자 정보를 찾을 수 없습니다. 관리자에게 문의해주세요.");
            model.addAttribute("error", "user_not_found");
        } catch (Exception e) {
            model.addAttribute("success", false);
            model.addAttribute("message", "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
            model.addAttribute("error", "server_error");
        }

        return "email-verification";
    }
}

