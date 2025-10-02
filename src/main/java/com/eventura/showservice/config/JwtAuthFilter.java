package com.eventura.showservice.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Value("${jwt.secret:mySuperSecretKey12345678901234567890}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws IOException, ServletException {
        String authHeader = request.getHeader("Authorization");
        System.out.println("Authorization header: " + authHeader);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Claims claims = Jwts.parser()
                        .setSigningKey(jwtSecret.getBytes(StandardCharsets.UTF_8))
                        .parseClaimsJws(token)
                        .getBody();
                System.out.println("Claims: " + claims);

                String username = claims.getSubject();
                System.out.println("Username: " + username);

                Object rolesClaim = claims.get("roles"); // try plural first
                if (rolesClaim == null) {
                    rolesClaim = claims.get("role"); // fallback to singular
                }

                List<SimpleGrantedAuthority> authorities;

                if (rolesClaim instanceof String roleStr) {
                    authorities = List.of(new SimpleGrantedAuthority("ROLE_" + roleStr));
                } else if (rolesClaim instanceof List<?> roleList) {
                    authorities = roleList.stream()
                            .map(Object::toString)
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                            .toList();
                } else {
                    authorities = List.of();
                }

                System.out.println("Authorities: " + authorities);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (Exception e) {
                e.printStackTrace();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        } else {
            System.out.println("No Authorization header or not Bearer");
        }

        filterChain.doFilter(request, response);
    }
}

