package vn.ueh.nhom1.khobanhang.common;

import org.springframework.http.HttpStatus;

/**
 * Loi nghiep vu co ma loi va thong bao tieng Viet de hien cho nguoi dung (D02 muc 5.3).
 */
public class BusinessException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public BusinessException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static BusinessException badRequest(String code, String message) {
        return new BusinessException(code, message, HttpStatus.BAD_REQUEST);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException("FORBIDDEN_BRANCH", message, HttpStatus.FORBIDDEN);
    }
}
