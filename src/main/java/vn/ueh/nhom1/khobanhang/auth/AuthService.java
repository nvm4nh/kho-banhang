package vn.ueh.nhom1.khobanhang.auth;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.ueh.nhom1.khobanhang.common.AuditLogger;
import vn.ueh.nhom1.khobanhang.common.BusinessException;

/**
 * Dang nhap (F01): mat khau bcrypt, khoa tai khoan sau 5 lan sai lien tiep (NF03).
 */
@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditLogger audit;
    private final int maxFailedAttempts;
    private final int lockMinutes;

    public AuthService(UserRepository users,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuditLogger audit,
                       @Value("${app.login.max-failed-attempts}") int maxFailedAttempts,
                       @Value("${app.login.lock-minutes}") int lockMinutes) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.audit = audit;
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockMinutes = lockMinutes;
    }

    public record LoginResult(String token, String fullName, List<String> roles) {
    }

    // noRollbackFor: dem so lan sai van phai duoc luu du sau do nem loi dang nhap that bai.
    @Transactional(noRollbackFor = BusinessException.class)
    public LoginResult login(String username, String rawPassword) {
        LocalDateTime now = LocalDateTime.now();
        UserAccount user = users.findByUsername(username)
                .filter(UserAccount::active)
                .orElseThrow(AuthService::invalidCredentials);

        if (user.isLockedAt(now)) {
            throw new BusinessException("ACCOUNT_LOCKED",
                    "Tai khoan tam khoa do nhap sai mat khau nhieu lan. Thu lai sau " + lockMinutes + " phut.",
                    HttpStatus.LOCKED);
        }

        if (!passwordEncoder.matches(rawPassword, user.passwordHash())) {
            int failed = user.failedLoginCount() + 1;
            if (failed >= maxFailedAttempts) {
                users.updateLoginState(user.id(), 0, now.plusMinutes(lockMinutes));
                audit.log(user.id(), "ACCOUNT_LOCKED", "app_user", user.id(), "Sai mat khau " + failed + " lan");
            } else {
                users.updateLoginState(user.id(), failed, null);
            }
            throw invalidCredentials();
        }

        users.updateLoginState(user.id(), 0, null);
        audit.log(user.id(), "LOGIN", "app_user", user.id(), null);
        List<String> roles = users.findAssignments(user.id()).stream()
                .map(a -> a.warehouseId() == null ? a.role() : a.role() + "@" + a.warehouseId())
                .toList();
        return new LoginResult(jwtService.issue(user), user.fullName(), roles);
    }

    // Cung mot thong bao cho sai ten dang nhap va sai mat khau, de khong lo ten dang nhap nao ton tai.
    private static BusinessException invalidCredentials() {
        return new BusinessException("INVALID_CREDENTIALS",
                "Ten dang nhap hoac mat khau khong dung", HttpStatus.UNAUTHORIZED);
    }
}
