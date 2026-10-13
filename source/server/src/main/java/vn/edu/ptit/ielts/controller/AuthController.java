package vn.edu.ptit.ielts.controller;

import vn.edu.ptit.ielts.security.AuthValidation;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.ptit.ielts.dto.AuthRequests;
import vn.edu.ptit.ielts.dto.UserView;
import vn.edu.ptit.ielts.exception.ApiException;
import vn.edu.ptit.ielts.security.UserPrincipal;
import vn.edu.ptit.ielts.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService service;
    private final AuthenticationManager authentication;
    private final SessionAuthenticationStrategy sessions;
    private final SecurityContextRepository contexts;
    public AuthController(AuthService service, AuthenticationManager authentication, SessionAuthenticationStrategy sessions, SecurityContextRepository contexts) {
        this.service = service; this.authentication = authentication; this.sessions = sessions; this.contexts = contexts;
    }
    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) { return Map.of("token", token.getToken(), "headerName", token.getHeaderName()); }
    @PostMapping("/register")
    public ResponseEntity<UserView> register(@Valid @RequestBody AuthRequests.Register input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(input));
    }
    @PostMapping("/login")
    public UserView login(@Valid @RequestBody AuthRequests.Login input, HttpServletRequest request, HttpServletResponse response) {
        AuthValidation.checkPasswordLength(input.password());
        try {
            var result = authentication.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(AuthValidation.normalizeEmail(input.email()), input.password()));
            sessions.onAuthentication(result, request, response);
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(result);
            SecurityContextHolder.setContext(context);
            contexts.saveContext(context, request, response);
            return service.current(((UserPrincipal) result.getPrincipal()).id());
        } catch (AuthenticationException ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email hoặc mật khẩu chưa đúng.");
        }
    }
    @GetMapping("/me")
    public UserView me(@AuthenticationPrincipal UserPrincipal principal) { return service.current(principal.id()); }
}
