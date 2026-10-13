package vn.edu.ptit.ielts.dto;

import java.time.Instant;

public record MemberView(long id, String fullName, String email, Instant joinedAt) {}
