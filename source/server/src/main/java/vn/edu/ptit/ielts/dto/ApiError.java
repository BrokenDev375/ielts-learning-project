package vn.edu.ptit.ielts.dto;

import java.util.Map;

public record ApiError(String code, String message, Map<String, String> fieldErrors) {
    public ApiError(String code, String message) { this(code, message, Map.of()); }
}
