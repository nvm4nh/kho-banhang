package vn.ueh.nhom1.khobanhang.auth;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import vn.ueh.nhom1.khobanhang.security.Assignment;

/**
 * Truy cap bang app_user va user_assignment. Moi truy van deu dung tham so,
 * khong noi chuoi SQL (D02 muc 4.2, checklist MN-06).
 */
@Repository
public class UserRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public UserRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<UserAccount> findByUsername(String username) {
        List<UserAccount> rows = jdbc.query("""
                SELECT id, username, password_hash, full_name, failed_login_count, locked_until, active
                FROM app_user WHERE username = :username
                """,
                Map.of("username", username),
                (rs, i) -> {
                    Timestamp locked = rs.getTimestamp("locked_until");
                    return new UserAccount(
                            rs.getLong("id"),
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            rs.getString("full_name"),
                            rs.getInt("failed_login_count"),
                            locked == null ? null : locked.toLocalDateTime(),
                            rs.getBoolean("active"));
                });
        return rows.stream().findFirst();
    }

    public List<Assignment> findAssignments(long userId) {
        return jdbc.query("""
                SELECT role, warehouse_id FROM user_assignment WHERE user_id = :userId
                """,
                Map.of("userId", userId),
                (rs, i) -> new Assignment(rs.getString("role"), (Long) rs.getObject("warehouse_id", Long.class)));
    }

    public void updateLoginState(long userId, int failedCount, LocalDateTime lockedUntil) {
        jdbc.update("""
                UPDATE app_user SET failed_login_count = :failed, locked_until = :lockedUntil
                WHERE id = :id
                """,
                new MapSqlParameterSource()
                        .addValue("failed", failedCount)
                        .addValue("lockedUntil", lockedUntil)
                        .addValue("id", userId));
    }
}
