package vn.ueh.nhom1.khobanhang.sales;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.ueh.nhom1.khobanhang.common.AuditLogger;
import vn.ueh.nhom1.khobanhang.common.BusinessException;
import vn.ueh.nhom1.khobanhang.security.CurrentUser;
import vn.ueh.nhom1.khobanhang.stock.StockService;

/**
 * Ban hang tai quay (F07). Toan bo hoa don chay trong MOT transaction:
 * luu hoa don, tru ton tung dong, ghi thanh toan, ghi nhat ky (D02 muc 3, NF04).
 */
@Service
public class SalesService {

    private final NamedParameterJdbcTemplate jdbc;
    private final StockService stockService;
    private final AuditLogger audit;

    public SalesService(NamedParameterJdbcTemplate jdbc, StockService stockService, AuditLogger audit) {
        this.jdbc = jdbc;
        this.stockService = stockService;
        this.audit = audit;
    }

    public record CheckoutResult(long invoiceId, BigDecimal total, String einvoiceStatus) {
    }

    private record Product(long id, String name, BigDecimal price) {
    }

    @Transactional
    public CheckoutResult checkout(CheckoutRequest request, CurrentUser cashier) {
        long warehouseId = request.warehouseId();
        // Lop kiem tra thu hai: thu ngan chi ban duoc tai chi nhanh duoc gan (D02 muc 4.1).
        cashier.requireRoleAt("CASHIER", warehouseId);

        // Gop cac dong trung ma vach: quet cung mot mon hai lan = so luong 2.
        Map<String, Integer> qtyByBarcode = new LinkedHashMap<>();
        for (CheckoutRequest.Line line : request.lines()) {
            qtyByBarcode.merge(line.barcode(), line.qty(), Integer::sum);
        }

        Map<String, Product> products = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<String, Integer> e : qtyByBarcode.entrySet()) {
            Product p = findActiveProduct(e.getKey());
            products.put(e.getKey(), p);
            total = total.add(p.price().multiply(BigDecimal.valueOf(e.getValue())));
        }

        long invoiceId = insertInvoice(warehouseId, cashier.id(), total);
        for (Map.Entry<String, Integer> e : qtyByBarcode.entrySet()) {
            Product p = products.get(e.getKey());
            int qty = e.getValue();
            insertLine(invoiceId, p, qty);
            stockService.decrease(warehouseId, p.id(), qty, "SALES_INVOICE", invoiceId, cashier.id());
        }
        if (total.signum() > 0) {
            insertPayment(invoiceId, request.paymentMethod(), total);
        }
        audit.log(cashier.id(), "SALE", "sales_invoice", invoiceId, "Tong " + total);

        // Hoa don dien tu duoc gui sau qua hang cho (D02 muc 6.2); ban nay chi danh dau PENDING.
        return new CheckoutResult(invoiceId, total, "PENDING");
    }

    private Product findActiveProduct(String barcode) {
        List<Product> rows = jdbc.query("""
                SELECT id, name, price FROM product WHERE barcode = :barcode AND status = 'ACTIVE'
                """, Map.of("barcode", barcode),
                (rs, i) -> new Product(rs.getLong("id"), rs.getString("name"), rs.getBigDecimal("price")));
        if (rows.isEmpty()) {
            throw BusinessException.badRequest("PRODUCT_NOT_FOUND",
                    "Khong tim thay ma " + barcode + ". Quet lai hoac go ten san pham.");
        }
        return rows.get(0);
    }

    private long insertInvoice(long warehouseId, long cashierId, BigDecimal total) {
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update("""
                INSERT INTO sales_invoice (warehouse_id, cashier_id, total, status, einvoice_status, created_at)
                VALUES (:wh, :cashier, :total, 'COMPLETED', 'PENDING', :createdAt)
                """,
                new MapSqlParameterSource()
                        .addValue("wh", warehouseId)
                        .addValue("cashier", cashierId)
                        .addValue("total", total)
                        .addValue("createdAt", LocalDateTime.now()),
                key, new String[] {"id"});
        return key.getKey().longValue();
    }

    private void insertLine(long invoiceId, Product p, int qty) {
        jdbc.update("""
                INSERT INTO sales_invoice_line (invoice_id, product_id, qty, unit_price)
                VALUES (:invoiceId, :pid, :qty, :price)
                """,
                new MapSqlParameterSource()
                        .addValue("invoiceId", invoiceId)
                        .addValue("pid", p.id())
                        .addValue("qty", qty)
                        .addValue("price", p.price()));
    }

    private void insertPayment(long invoiceId, CheckoutRequest.PaymentMethod method, BigDecimal amount) {
        jdbc.update("""
                INSERT INTO payment (invoice_id, method, amount) VALUES (:invoiceId, :method, :amount)
                """,
                new MapSqlParameterSource()
                        .addValue("invoiceId", invoiceId)
                        .addValue("method", method.name())
                        .addValue("amount", amount));
    }
}
