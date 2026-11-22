package vn.ueh.nhom1.khobanhang.sales;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Du lieu man hinh POS gui len khi thu ngan bam Xac nhan (D02 muc 3.1, muc 5.2).
 */
public record CheckoutRequest(
        @NotNull Long warehouseId,
        @NotEmpty @Valid List<Line> lines,
        @NotNull PaymentMethod paymentMethod,
        // Tuy chon: ma duy nhat cho moi lan thanh toan, de gui lai khong tao hoa don trung (FDR-02, INS-05).
        @Pattern(regexp = "[A-Za-z0-9-]{8,64}", message = "mã yêu cầu gồm 8 đến 64 ký tự chữ, số hoặc dấu gạch") String clientRequestId) {

    public record Line(
            @NotBlank @Pattern(regexp = "\\d{8,14}", message = "mã vạch phải gồm 8 đến 14 chữ số") String barcode,
            @Min(1) @Max(999) int qty) {
    }

    public enum PaymentMethod { CASH, TRANSFER, CARD }
}
