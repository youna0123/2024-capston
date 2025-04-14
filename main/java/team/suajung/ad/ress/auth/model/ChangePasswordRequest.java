package team.suajung.ad.ress.auth.model;

import lombok.Data;

@Data
public class ChangePasswordRequest {
    private String currentPassword;

    private String newPassword;
}
