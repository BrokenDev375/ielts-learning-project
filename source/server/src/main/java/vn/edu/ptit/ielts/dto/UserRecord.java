package vn.edu.ptit.ielts.dto;

import java.time.Instant;
import vn.edu.ptit.ielts.enums.Role;

public record UserRecord(long id, String email, String passwordHash, String fullName, Role role, Instant createdAt) {
    public UserView view() { return new UserView(id, email, fullName, role, createdAt); }
}
