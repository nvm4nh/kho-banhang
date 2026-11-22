package vn.ueh.nhom1.khobanhang;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import vn.ueh.nhom1.khobanhang.auth.AuthService;
import vn.ueh.nhom1.khobanhang.auth.UserRepository;
import vn.ueh.nhom1.khobanhang.common.BusinessException;

/**
 * F01, NF03: dang nhap bang bcrypt, khoa tai khoan sau 5 lan sai.
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuthServiceTest {

    @Autowired
    AuthService authService;

    @Autowired
    UserRepository users;

    @Test
    void dangNhapDungMatKhau_traVeTokenVaVaiTro() {
        AuthService.LoginResult result = authService.login("thungan1", "Matkhau@123");

        assertThat(result.token()).isNotBlank();
        assertThat(result.roles()).hasSize(1);
        assertThat(result.roles().get(0)).startsWith("CASHIER@");
    }

    @Test
    void saiMatKhau_baoLoiChungKhongLoTenDangNhap() {
        assertThatThrownBy(() -> authService.login("thungan1", "sai"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("INVALID_CREDENTIALS");
        assertThatThrownBy(() -> authService.login("khongtontai", "sai"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    void sai5Lan_khoaTaiKhoan_keCaKhiNhapDungMatKhau() {
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> authService.login("thungan2", "sai"))
                    .extracting("code").isEqualTo("INVALID_CREDENTIALS");
        }

        assertThatThrownBy(() -> authService.login("thungan2", "Matkhau@123"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("ACCOUNT_LOCKED");
        assertThat(users.findByUsername("thungan2").orElseThrow().lockedUntil()).isNotNull();
    }

    @Test
    void sai4LanRoiDung_xoaBoDem() {
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> authService.login("thungan1b", "sai"));
        }
        authService.login("thungan1b", "Matkhau@123");

        assertThat(users.findByUsername("thungan1b").orElseThrow().failedLoginCount()).isZero();
    }
}
