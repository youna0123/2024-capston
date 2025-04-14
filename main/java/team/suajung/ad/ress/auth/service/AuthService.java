package team.suajung.ad.ress.auth.service;

import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import team.suajung.ad.ress.auth.exception.TokenExpiredException;
import team.suajung.ad.ress.auth.exception.TokenNotFoundException;
import team.suajung.ad.ress.auth.exception.UserNotFoundException;
import team.suajung.ad.ress.auth.model.Role;
import team.suajung.ad.ress.auth.model.User;
import team.suajung.ad.ress.auth.model.VerificationToken;
import team.suajung.ad.ress.auth.repository.RoleRepository;
import team.suajung.ad.ress.auth.repository.UserRepository;
import team.suajung.ad.ress.auth.repository.VerificationTokenRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    VerificationTokenRepository tokenRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    EmailService emailService;

    public User registerUser(String username, String email, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("이미 사용 중인 사용자명입니다");
        }

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("이미 사용 중인 이메일입니다");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(encoder.encode(password));
        user.setEnabled(false);
        user.setCreatedAt(System.currentTimeMillis());
        user.setUpdatedAt(System.currentTimeMillis());

        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName(Role.ERole.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("기본 역할을 찾을 수 없습니다"));
        roles.add(userRole);
        user.setRoles(roles);

        User savedUser = userRepository.save(user);

        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUserId(savedUser.getId());
        verificationToken.setType(VerificationToken.TokenType.EMAIL_VERIFICATION);
        verificationToken.setExpiryDate(Instant.now().plus(24, ChronoUnit.HOURS));
        tokenRepository.save(verificationToken);

        try {
            emailService.sendVerificationEmail(savedUser.getEmail(), token);
        } catch (MessagingException e) {
            throw new RuntimeException("이메일 발송 중 오류가 발생했습니다", e);
        }

        return savedUser;
    }

    public boolean verifyEmail(String token) {
        Optional<VerificationToken> verificationToken = tokenRepository.findByToken(token);

        if (verificationToken.isEmpty()) {
            throw new TokenNotFoundException("인증 토큰을 찾을 수 없습니다: " + token);
        }

        if (verificationToken.get().isExpired()) {
            throw new TokenExpiredException("인증 토큰이 만료되었습니다", verificationToken.get().getUserId());
        }

        String userId = verificationToken.get().getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        user.setEnabled(true);
        userRepository.save(user);

        tokenRepository.delete(verificationToken.get());

        return true;
    }

    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("해당 이메일로 등록된 사용자가 없습니다"));

        tokenRepository.findByUserIdAndType(user.getId(), VerificationToken.TokenType.PASSWORD_RESET)
                .ifPresent(tokenRepository::delete);

        String token = UUID.randomUUID().toString();
        VerificationToken resetToken = new VerificationToken();
        resetToken.setToken(token);
        resetToken.setUserId(user.getId());
        resetToken.setType(VerificationToken.TokenType.PASSWORD_RESET);
        resetToken.setExpiryDate(Instant.now().plus(1, ChronoUnit.HOURS));
        tokenRepository.save(resetToken);

        try {
            emailService.sendPasswordResetEmail(user.getEmail(), token);
        } catch (MessagingException e) {
            throw new RuntimeException("비밀번호 재설정 이메일 발송 중 오류가 발생했습니다", e);
        }
    }

    public boolean resetPassword(String token, String newPassword) {
        Optional<VerificationToken> resetToken = tokenRepository.findByToken(token);

        if (resetToken.isPresent() && !resetToken.get().isExpired()) {
            String userId = resetToken.get().getUserId();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

            user.setPassword(encoder.encode(newPassword));
            user.setUpdatedAt(System.currentTimeMillis());
            userRepository.save(user);

            tokenRepository.delete(resetToken.get());

            return true;
        }

        return false;
    }

    public void changePassword(String userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        if (!encoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("현재 비밀번호가 일치하지 않습니다");
        }

        user.setPassword(encoder.encode(newPassword));
        user.setUpdatedAt(System.currentTimeMillis());
        userRepository.save(user);
    }

    public void deleteAccount(String userId, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        if (!encoder.matches(password, user.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다");
        }

        tokenRepository.findAll().stream()
                .filter(token -> token.getUserId().equals(userId))
                .forEach(tokenRepository::delete);

        userRepository.delete(user);
    }

    public boolean isPasswordResetTokenValid(String token) {
        Optional<VerificationToken> resetToken = tokenRepository.findByToken(token);

        return resetToken.isPresent()
                && !resetToken.get().isExpired()
                && resetToken.get().getType() == VerificationToken.TokenType.PASSWORD_RESET;
    }
}

