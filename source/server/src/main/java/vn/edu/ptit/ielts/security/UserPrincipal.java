package vn.edu.ptit.ielts.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import vn.edu.ptit.ielts.dto.UserRecord;
import vn.edu.ptit.ielts.enums.Role;

public record UserPrincipal(long id, String email, String passwordHash, Role role) implements UserDetails {
    public static UserPrincipal from(UserRecord user) { return new UserPrincipal(user.id(), user.email(), user.passwordHash(), user.role()); }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(new SimpleGrantedAuthority("ROLE_" + role.name())); }
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }
}
