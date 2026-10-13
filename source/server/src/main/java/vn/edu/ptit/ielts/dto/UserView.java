package vn.edu.ptit.ielts.dto;

import java.time.Instant;
import vn.edu.ptit.ielts.enums.Role;

public record UserView(long id, String email, String fullName, Role role, Instant createdAt) {}
