package vn.ueh.nhom1.khobanhang.common;

import java.time.LocalDateTime;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ghi nhat ky thao tac vao audit_log. Bang nay chi duoc INSERT (D02 muc 4.2).
 */
@Component
public class AuditLogger {

    private final NamedParameterJdbcTemplate jdbc;

    public AuditLogger(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void log(Long userId, String action, String entity, Long entityId, String detail) {
        jdbc.update("""
                INSERT INTO audit_log (user_id, action, entity, entity_id, detail, created_at)
                VALUES (:userId, :action, :entity, :entityId, :detail, :createdAt)
                """,
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("action", action)
                        .addValue("entity", entity)
                        .addValue("entityId", entityId)
                        .addValue("detail", detail)
                        .addValue("createdAt", LocalDateTime.now()));
    }
}
