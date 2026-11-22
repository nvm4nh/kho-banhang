package vn.ueh.nhom1.khobanhang.auth;

import java.time.LocalDateTime;

public record UserAccount(
        long id,
        String username,
        String passwordHash,
        String fullName,
        int failedLoginCount,
        LocalDateTime lockedUntil,
        boolean active) {

    public boolean isLockedAt(LocalDateTime now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }
}
