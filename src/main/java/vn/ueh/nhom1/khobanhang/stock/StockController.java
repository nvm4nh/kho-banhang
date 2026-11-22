package vn.ueh.nhom1.khobanhang.stock;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.ueh.nhom1.khobanhang.security.CurrentUser;

@RestController
@RequestMapping("/api/stock")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    // Thu ngan tra cuu ton tai chi nhanh cua minh (F07); quan tri xem duoc moi kho.
    @GetMapping
    @PreAuthorize("hasAnyRole('CASHIER', 'ADMIN')")
    public List<StockService.StockLine> list(@RequestParam long warehouseId,
                                             @AuthenticationPrincipal CurrentUser user) {
        if (!user.hasRoleAt("ADMIN", warehouseId)) {
            user.requireRoleAt("CASHIER", warehouseId);
        }
        return stockService.listByWarehouse(warehouseId);
    }
}
