package vn.ueh.nhom1.khobanhang.security;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.ueh.nhom1.khobanhang.auth.JwtService;
import vn.ueh.nhom1.khobanhang.auth.UserAccount;
import vn.ueh.nhom1.khobanhang.auth.UserRepository;

/**
 * Doc header "Authorization: Bearer <token>". Vai tro duoc doc lai tu CSDL o moi request,
 * nen thu hoi quyen co hieu luc ngay, khong phai cho token het han.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository users;

    public JwtAuthFilter(JwtService jwtService, UserRepository users) {
        this.jwtService = jwtService;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            Claims claims = jwtService.parse(header.substring(7));
            // Token hop le van bi tu choi neu tai khoan da ngung hoat dong hoac dang bi khoa (INS-03, FDR-03).
            boolean accountUsable = claims != null && users.findById(claims.get("uid", Long.class))
                    .filter(UserAccount::active)
                    .filter(u -> !u.isLockedAt(LocalDateTime.now()))
                    .isPresent();
            if (accountUsable) {
                long userId = claims.get("uid", Long.class);
                List<Assignment> assignments = users.findAssignments(userId);
                CurrentUser principal = new CurrentUser(userId, claims.getSubject(), assignments);
                List<SimpleGrantedAuthority> authorities = assignments.stream()
                        .map(a -> new SimpleGrantedAuthority("ROLE_" + a.role()))
                        .distinct()
                        .toList();
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(principal, null, authorities));
            }
        }
        chain.doFilter(request, response);
    }
}
