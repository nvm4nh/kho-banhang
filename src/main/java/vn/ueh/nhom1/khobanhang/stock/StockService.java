package vn.ueh.nhom1.khobanhang.stock;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import vn.ueh.nhom1.khobanhang.common.AuditLogger;
import vn.ueh.nhom1.khobanhang.common.BusinessException;

/**
 * Noi duy nhat duoc thay doi bang stock_balance (D02 muc 1 va 3).
 * Module kho va module ban hang deu goi qua day, khong tu UPDATE ton kho.
 */
@Service
public class StockService {

    private final NamedParameterJdbcTemplate jdbc;
    private final AuditLogger audit;

    public StockService(NamedParameterJdbcTemplate jdbc, AuditLogger audit) {
        this.jdbc = jdbc;
        this.audit = audit;
    }

    public record StockLine(long productId, String sku, String name, int qty, int minStock) {
    }

    /**
     * Tru ton bang mot cau UPDATE co dieu kien (D02 muc 3.2). Phai goi ben trong transaction
     * cua phieu, de neu mot dong loi thi ca phieu bi huy.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void decrease(long warehouseId, long productId, int qty,
                         String refType, long refId, long userId) {
        if (qty <= 0) {
            throw BusinessException.badRequest("INVALID_QTY", "So luong phai lon hon 0");
        }
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("wh", warehouseId)
                .addValue("pid", productId)
                .addValue("qty", qty);
        // Dieu kien qty >= :qty la lop chan thu nhat. Rang buoc CHECK (qty >= 0) cua CSDL la lop thu hai:
        // neu no bi vi pham, loi duoc de nguyen cho transaction rollback va GlobalExceptionHandler
        // tra 409, khong bat lai o day (INS-04: tren PostgreSQL transaction da hong sau loi do).
        int updated = jdbc.update("""
                UPDATE stock_balance
                SET qty = qty - :qty, version = version + 1
                WHERE warehouse_id = :wh AND product_id = :pid AND qty >= :qty
                """, params);
        if (updated == 0) {
            int remaining = currentQty(warehouseId, productId);
            throw new BusinessException("INSUFFICIENT_STOCK",
                    "Het hang, con " + remaining, HttpStatus.CONFLICT);
        }
        recordMovement(warehouseId, productId, -qty, refType, refId, userId);
        warnIfBelowMinimum(warehouseId, productId, userId);
    }

    public int currentQty(long warehouseId, long productId) {
        List<Integer> rows = jdbc.queryForList("""
                SELECT qty FROM stock_balance WHERE warehouse_id = :wh AND product_id = :pid
                """, Map.of("wh", warehouseId, "pid", productId), Integer.class);
        return rows.isEmpty() ? 0 : rows.get(0);
    }

    public List<StockLine> listByWarehouse(long warehouseId) {
        return jdbc.query("""
                SELECT p.id, p.sku, p.name, s.qty, p.min_stock
                FROM stock_balance s JOIN product p ON p.id = s.product_id
                WHERE s.warehouse_id = :wh
                ORDER BY p.sku
                """, Map.of("wh", warehouseId),
                (rs, i) -> new StockLine(rs.getLong("id"), rs.getString("sku"), rs.getString("name"),
                        rs.getInt("qty"), rs.getInt("min_stock")));
    }

    private void recordMovement(long warehouseId, long productId, int qtyChange,
                                String refType, long refId, long userId) {
        jdbc.update("""
                INSERT INTO stock_movement (warehouse_id, product_id, qty_change, ref_type, ref_id, created_by, created_at)
                VALUES (:wh, :pid, :change, :refType, :refId, :userId, :createdAt)
                """,
                new MapSqlParameterSource()
                        .addValue("wh", warehouseId)
                        .addValue("pid", productId)
                        .addValue("change", qtyChange)
                        .addValue("refType", refType)
                        .addValue("refId", refId)
                        .addValue("userId", userId)
                        .addValue("createdAt", LocalDateTime.now()));
    }

    // Canh bao ton toi thieu (F06). Ban nay ghi vao audit_log; man hinh canh bao chua lam.
    private void warnIfBelowMinimum(long warehouseId, long productId, long userId) {
        Integer below = jdbc.queryForObject("""
                SELECT COUNT(*) FROM stock_balance s JOIN product p ON p.id = s.product_id
                WHERE s.warehouse_id = :wh AND s.product_id = :pid AND s.qty < p.min_stock
                """, Map.of("wh", warehouseId, "pid", productId), Integer.class);
        if (below != null && below > 0) {
            audit.log(userId, "LOW_STOCK", "stock_balance", productId, "Kho " + warehouseId);
        }
    }
}
