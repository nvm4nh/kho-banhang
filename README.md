# Hệ thống quản lý kho và bán hàng – lát cắt F01 + F07

Mã nguồn của Nhóm 1 (môn Đảm bảo chất lượng phần mềm) cho chuỗi điện máy ABC. Đây là lát cắt đầu tiên của hệ thống, làm đúng theo tài liệu thiết kế **D02**. Lát cắt gồm đăng nhập và phân quyền (F01), bán hàng tại quầy qua `StockService` (F07), và bộ test tự động.

Tài liệu liên quan:
- D02 – Thiết kế hệ thống: https://claude.ai/artifact/MujUcod391yWAhkkk5wMK2
- SQAP 1.1 & 1.3 (yêu cầu F01–F09, NF01–NF09): https://docs.google.com/document/d/1Yd3UeXlhOAYA_Aj2cr-xRDo9G57v-dZ2YCDSrpwjLUk/edit

## Chạy thử

Cần Java 17 trở lên và Maven 3.9.

```bash
mvn test              # chạy 12 test
mvn spring-boot:run   # chạy server ở http://localhost:8080 với CSDL H2 trong bộ nhớ
```

Chạy với PostgreSQL thật: tạo CSDL `khobanhang`, đặt các biến `DB_URL`, `DB_USER`, `DB_PASSWORD`, `APP_JWT_SECRET` (ít nhất 32 ký tự), rồi chạy `mvn spring-boot:run -Dspring-boot.run.profiles=postgres`.

Tài khoản mẫu, mật khẩu đều là `Matkhau@123`:

| Tài khoản | Vai trò | Chi nhánh |
| --- | --- | --- |
| `admin` | ADMIN | Toàn chuỗi |
| `thungan1`, `thungan1b` | CASHIER | CN01 (còn đúng 1 tivi) |
| `thungan2` | CASHIER | CN02 |

Ví dụ gọi API:

```bash
# Đăng nhập, lấy token
curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"thungan1","password":"Matkhau@123"}'

# Bán 2 nồi cơm ở CN01 (warehouseId của CN01 là 2 khi chạy H2 mặc định)
curl -s -X POST localhost:8080/api/sales -H "Authorization: Bearer <token>" -H 'Content-Type: application/json' \
  -d '{"warehouseId":2,"paymentMethod":"CASH","lines":[{"barcode":"8930000000028","qty":2}]}'
```

## Cấu trúc và đối chiếu với D02

| Thư mục / file | Nội dung | D02 |
| --- | --- | --- |
| `src/main/resources/schema.sql` | 10 bảng của lát cắt, ràng buộc `CHECK (qty >= 0)` ở `stock_balance` | Mục 2 |
| `auth/AuthService.java` | Đăng nhập bcrypt, khóa 15 phút sau 5 lần sai, ghi `audit_log` | Mục 4.2 |
| `auth/JwtService.java` | Token JWT hết hạn sau 8 giờ | Mục 4.2 |
| `security/SecurityConfig.java`, `JwtAuthFilter.java` | Lớp phân quyền 1: token và vai trò (`@PreAuthorize`) | Mục 4.1 |
| `security/CurrentUser.java` | Lớp phân quyền 2: đúng chi nhánh được gán | Mục 4.1 |
| `stock/StockService.java` | Nơi duy nhất trừ tồn, bằng câu UPDATE có điều kiện | Mục 1, 3.2 |
| `sales/SalesService.java` | Bán hàng trong một transaction, hóa đơn điện tử để PENDING | Mục 3.1, 6.2 |
| `common/GlobalExceptionHandler.java` | Lỗi trả về `{code, message}` | Mục 5.3 |
| `src/test/...` | 12 test: đăng nhập, khóa tài khoản, 403 khác chi nhánh, hết hàng, hai thu ngân cùng bán | NF03, NF04 |

## Chưa làm trong lát cắt này

- Các module F02–F06, F08, F09; phiếu nhập, xuất, chuyển kho, kiểm kê.
- Serial và lô hàng (`product_serial`, `stock_lot`), khuyến mãi, đổi trả.
- Gửi hóa đơn điện tử thật qua hàng chờ (hiện chỉ đánh dấu `PENDING`).
- Giao diện ReactJS; hiện chỉ có API.
- Pipeline GitHub Actions và SonarQube.

## Gợi ý cho buổi Inspection

Khoảng 840 dòng Java trong `src/main` và 300 dòng test. Nên kiểm tra theo checklist MN và KT trong tab Quality Checklist, tập trung vào `StockService`, `SalesService` và `AuthService`. Lỗi tìm được ghi vào Issue Log với mã INS-01, INS-02…
