package vn.ueh.nhom1.khobanhang;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import vn.ueh.nhom1.khobanhang.auth.UserRepository;
import vn.ueh.nhom1.khobanhang.common.BusinessException;
import vn.ueh.nhom1.khobanhang.sales.CheckoutRequest;
import vn.ueh.nhom1.khobanhang.sales.SalesService;
import vn.ueh.nhom1.khobanhang.security.CurrentUser;

/**
 * NF04, D02 muc 3.2: hai thu ngan cung ban chiec tivi cuoi cung o chi nhanh 1.
 * Dung mot nguoi thanh cong, nguoi con lai nhan INSUFFICIENT_STOCK, ton khong am.
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ConcurrentSaleTest {

    @Autowired
    SalesService salesService;

    @Autowired
    UserRepository users;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void haiThuNganCungBanMonCuoiCung_chiMotNguoiThanhCong() throws Exception {
        long cn01 = jdbc.queryForObject("SELECT id FROM warehouse WHERE code = 'CN01'", Long.class);
        CurrentUser a = cashier("thungan1");
        CurrentUser b = cashier("thungan1b");
        CheckoutRequest request = new CheckoutRequest(cn01,
                List.of(new CheckoutRequest.Line("8930000000011", 1)),
                CheckoutRequest.PaymentMethod.CASH, null);

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Callable<String> sellA = () -> sell(start, request, a);
        Callable<String> sellB = () -> sell(start, request, b);
        Future<String> fa = pool.submit(sellA);
        Future<String> fb = pool.submit(sellB);
        start.countDown();
        List<String> outcomes = List.of(fa.get(), fb.get());
        pool.shutdown();

        assertThat(outcomes).containsExactlyInAnyOrder("OK", "INSUFFICIENT_STOCK");
        Integer qty = jdbc.queryForObject("""
                SELECT s.qty FROM stock_balance s JOIN product p ON p.id = s.product_id
                WHERE s.warehouse_id = ? AND p.sku = 'TV-55-A'""", Integer.class, cn01);
        assertThat(qty).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sales_invoice", Integer.class)).isEqualTo(1);
    }

    private String sell(CountDownLatch start, CheckoutRequest request, CurrentUser who) throws InterruptedException {
        start.await();
        try {
            salesService.checkout(request, who);
            return "OK";
        } catch (BusinessException e) {
            return e.getCode();
        }
    }

    private CurrentUser cashier(String username) {
        long id = users.findByUsername(username).orElseThrow().id();
        return new CurrentUser(id, username, users.findAssignments(id));
    }
}
