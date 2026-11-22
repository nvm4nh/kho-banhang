package vn.ueh.nhom1.khobanhang.security;

/**
 * Mot vai tro cua nguoi dung tai mot kho/chi nhanh (bang user_assignment).
 * warehouseId = null nghia la vai tro ap dung cho toan chuoi.
 */
public record Assignment(String role, Long warehouseId) {

    public boolean covers(String requiredRole, long targetWarehouseId) {
        return role.equals(requiredRole)
                && (warehouseId == null || warehouseId == targetWarehouseId);
    }
}
