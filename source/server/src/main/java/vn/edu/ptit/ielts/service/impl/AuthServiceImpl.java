package vn.edu.ptit.ielts.service.impl;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.edu.ptit.ielts.dto.AuthRequests;
import vn.edu.ptit.ielts.dto.UserView;
import vn.edu.ptit.ielts.enums.Role;
import vn.edu.ptit.ielts.exception.ApiException;
import vn.edu.ptit.ielts.repository.UserRepository;
import vn.edu.ptit.ielts.security.AuthValidation;
import vn.edu.ptit.ielts.service.AuthService;

@Service
public class AuthServiceImpl implements AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    public AuthServiceImpl(UserRepository users, PasswordEncoder passwords) {
        this.users = users;
        this.passwords = passwords;
    }

    @Override
    public UserView register(AuthRequests.Register input) {
        String name = input.fullName().strip();
        if (name.length() < 2) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_NAME", "Họ tên cần ít nhất 2 ký tự.");
        AuthValidation.checkPasswordLength(input.password());
        try {
            return users.create(AuthValidation.normalizeEmail(input.email()), passwords.encode(input.password()), name, Role.STUDENT).view();
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_EXISTS", "Email đã được đăng ký. Bạn có thể đăng nhập bằng tài khoản này.");
        }
    }
    @Override
    public UserView current(long id) {
        return users.findById(id).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Vui lòng đăng nhập lại.")).view();
    }
}
