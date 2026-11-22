package vn.ueh.nhom1.khobanhang;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * F07 qua API: ban hang thanh cong, phan quyen theo chi nhanh, thong bao loi.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SalesApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    JdbcTemplate jdbc;

    private static final String NOI_COM = "8930000000028";
    private static final String TIVI = "8930000000011";

    private String token(String username) throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", username, "password", "Matkhau@123"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    private long warehouseId(String code) {
        return jdbc.queryForObject("SELECT id FROM warehouse WHERE code = ?", Long.class, code);
    }

    private String checkoutBody(long warehouseId, String barcode, int qty) throws Exception {
        return json.writeValueAsString(Map.of(
                "warehouseId", warehouseId,
                "paymentMethod", "CASH",
                "lines", new Object[] {Map.of("barcode", barcode, "qty", qty)}));
    }

    @Test
    void khongCoToken_tuChoi401() throws Exception {
        mvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutBody(warehouseId("CN01"), NOI_COM, 1)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void banHangThanhCong_truTonVaGhiSoBienDong() throws Exception {
        long cn01 = warehouseId("CN01");

        mvc.perform(post("/api/sales")
                        .header("Authorization", "Bearer " + token("thungan1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutBody(cn01, NOI_COM, 2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(1780000))
                .andExpect(jsonPath("$.einvoiceStatus").value("PENDING"));

        Integer qty = jdbc.queryForObject("""
                SELECT s.qty FROM stock_balance s JOIN product p ON p.id = s.product_id
                WHERE s.warehouse_id = ? AND p.barcode = ?""", Integer.class, cn01, NOI_COM);
        assertThat(qty).isEqualTo(8);
        Integer movements = jdbc.queryForObject(
                "SELECT COUNT(*) FROM stock_movement WHERE ref_type = 'SALES_INVOICE'", Integer.class);
        assertThat(movements).isEqualTo(1);
    }

    @Test
    void thuNganChiNhanh2_banTaiChiNhanh1_bi403() throws Exception {
        mvc.perform(post("/api/sales")
                        .header("Authorization", "Bearer " + token("thungan2"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutBody(warehouseId("CN01"), NOI_COM, 1)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_BRANCH"));
    }

    @Test
    void thuNganChiNhanh2_xemTonChiNhanh1_bi403() throws Exception {
        mvc.perform(get("/api/stock").param("warehouseId", String.valueOf(warehouseId("CN01")))
                        .header("Authorization", "Bearer " + token("thungan2")))
                .andExpect(status().isForbidden());
    }

    @Test
    void maVachKhongTonTai_bao400VaKhongTaoHoaDon() throws Exception {
        mvc.perform(post("/api/sales")
                        .header("Authorization", "Bearer " + token("thungan1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutBody(warehouseId("CN01"), "8930000009999", 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sales_invoice", Integer.class)).isZero();
    }

    @Test
    void muaQuaSoTon_bao409VaHuyCaHoaDon() throws Exception {
        long cn01 = warehouseId("CN01");

        mvc.perform(post("/api/sales")
                        .header("Authorization", "Bearer " + token("thungan1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutBody(cn01, TIVI, 2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"))
                .andExpect(jsonPath("$.message").value("Het hang, con 1"));

        // Transaction bi rollback: khong con hoa don nao, ton giu nguyen.
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sales_invoice", Integer.class)).isZero();
    }

    @Test
    void taiKhoanBiKhoaSauKhiDangNhap_tokenCuBiTuChoi401() throws Exception {
        String token = token("thungan1");
        jdbc.update("UPDATE app_user SET locked_until = DATEADD('MINUTE', 15, CURRENT_TIMESTAMP) WHERE username = 'thungan1'");

        mvc.perform(get("/api/stock").param("warehouseId", String.valueOf(warehouseId("CN01")))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void taiKhoanNgungHoatDong_tokenCuBiTuChoi401() throws Exception {
        String token = token("thungan1");
        jdbc.update("UPDATE app_user SET active = FALSE WHERE username = 'thungan1'");

        mvc.perform(get("/api/stock").param("warehouseId", String.valueOf(warehouseId("CN01")))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void guiLaiCungMaYeuCau_khongTaoHoaDonTrung() throws Exception {
        long cn01 = warehouseId("CN01");
        String body = json.writeValueAsString(Map.of(
                "warehouseId", cn01,
                "paymentMethod", "CASH",
                "clientRequestId", "pos01-20261122-0001",
                "lines", new Object[] {Map.of("barcode", NOI_COM, "qty", 1)}));
        String token = token("thungan1");

        String first = mvc.perform(post("/api/sales").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String second = mvc.perform(post("/api/sales").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        assertThat(json.readTree(second).get("invoiceId").asLong())
                .isEqualTo(json.readTree(first).get("invoiceId").asLong());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sales_invoice", Integer.class)).isEqualTo(1);
        Integer qty = jdbc.queryForObject("""
                SELECT s.qty FROM stock_balance s JOIN product p ON p.id = s.product_id
                WHERE s.warehouse_id = ? AND p.barcode = ?""", Integer.class, cn01, NOI_COM);
        assertThat(qty).isEqualTo(9);
    }

    @Test
    void jsonSaiDinhDang_traVeDangCodeMessage() throws Exception {
        mvc.perform(post("/api/sales")
                        .header("Authorization", "Bearer " + token("thungan1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{khong phai json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void soLuongAm_bao400() throws Exception {
        mvc.perform(post("/api/sales")
                        .header("Authorization", "Bearer " + token("thungan1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutBody(warehouseId("CN01"), NOI_COM, -1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }
}
