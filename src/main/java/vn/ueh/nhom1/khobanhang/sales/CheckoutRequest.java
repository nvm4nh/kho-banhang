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
        @NotNull PaymentMethod paymentMethod) {

    public record Line(
            @NotBlank @Pattern(regexp = "\\d{8,14}", message = "ma vach phai gom 8 den 14 chu so") String barcode,
            @Min(1) @Max(999) int qty) {
    }

    public enum PaymentMethod { CASH, TRANSFER, CARD }
}
