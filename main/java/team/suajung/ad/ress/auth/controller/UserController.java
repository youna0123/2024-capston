package team.suajung.ad.ress.auth.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import team.suajung.ad.ress.auth.model.ChangePasswordRequest;
import team.suajung.ad.ress.auth.model.DeleteAccountRequest;
import team.suajung.ad.ress.auth.model.MessageResponse;
import team.suajung.ad.ress.auth.service.AuthService;
import team.suajung.ad.ress.auth.service.UserDetailsImpl;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    AuthService authService;

    @PutMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        try {
            String userId = getCurrentUserId();
            authService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword());
            return ResponseEntity.ok(new MessageResponse("비밀번호가 성공적으로 변경되었습니다."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/delete-account")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteAccount(@Valid @RequestBody DeleteAccountRequest request) {
        try {
            String userId = getCurrentUserId();
            authService.deleteAccount(userId, request.getPassword());
            SecurityContextHolder.clearContext();
            return ResponseEntity.ok(new MessageResponse("계정이 성공적으로 삭제되었습니다."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return userDetails.getId();
    }
}

