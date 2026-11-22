package vn.ueh.nhom1.khobanhang.security;

import java.util.List;

import vn.ueh.nhom1.khobanhang.common.BusinessException;

/**
 * Nguoi dung dang goi API, lay tu JWT va bang user_assignment.
 */
public record CurrentUser(long id, String username, List<Assignment> assignments) {

    public boolean hasRoleAt(String role, long warehouseId) {
        return assignments.stream().anyMatch(a -> a.covers(role, warehouseId));
    }

    /**
     * Lop kiem tra thu hai cua phan quyen: dung vai tro nhung phai dung chi nhanh (D02 muc 4.1).
     */
    public void requireRoleAt(String role, long warehouseId) {
        if (!hasRoleAt(role, warehouseId)) {
            throw BusinessException.forbidden("Bạn không được thao tác trên dữ liệu của kho/chi nhánh này");
        }
    }
}
