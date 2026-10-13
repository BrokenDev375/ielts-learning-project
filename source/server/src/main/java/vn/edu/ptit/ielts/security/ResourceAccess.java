package vn.edu.ptit.ielts.security;

import org.springframework.http.HttpStatus;
import vn.edu.ptit.ielts.enums.Role;
import vn.edu.ptit.ielts.exception.ApiException;

public final class ResourceAccess {
    private ResourceAccess() {}
    public static ApiException forbidden() { return new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Bạn không có quyền truy cập bài này."); }
    public static ApiException missing() { return new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Không tìm thấy bài."); }
    public static void requireTeacher(UserPrincipal user) { if (user.role() != Role.TEACHER) throw forbidden(); }
}
