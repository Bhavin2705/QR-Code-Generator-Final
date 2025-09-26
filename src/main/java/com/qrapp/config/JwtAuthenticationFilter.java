
package com.qrapp.config;

import java.io.IOException;
import java.util.List;

import com.qrapp.util.JwtUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;
    @Autowired
    private com.qrapp.repository.UserRepository userRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        String username;
        try {
            username = jwtUtil.extractUsername(token);
        } catch (JwtException e) {
            filterChain.doFilter(request, response);
            return;
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Check if user is blocked; if so, reject the request so tokens are effectively
            // invalidated
            try {
                var optUser = userRepository.findByUsername(username);
                if (optUser.isPresent() && "blocked".equalsIgnoreCase(optUser.get().getStatus())) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write(
                            "{\"success\":false,\"message\":\"Your account has been blocked by an administrator\"}");
                    return;
                }
            } catch (Exception e) {
                // on error, proceed to normal flow (do not block request due to check failure)
            }

            UserDetails userDetails;
            try {
                userDetails = userDetailsService.loadUserByUsername(username);
            } catch (UsernameNotFoundException e) {
                filterChain.doFilter(request, response);
                return;
            }
            if (jwtUtil.validateToken(token, userDetails.getUsername())) {
                String role = null;
                try {
                    Object roleObj = jwtUtil.extractClaim(token, claims -> claims.get("role"));
                    if (roleObj != null) {
                        role = roleObj.toString();
                    }
                } catch (Exception e) {
                    System.err.println("Failed to extract role from JWT: " + e.getMessage());
                }
                List<SimpleGrantedAuthority> authorities;
                if (role != null) {
                    authorities = List.of(new SimpleGrantedAuthority(role));
                } else {
                    authorities = userDetails.getAuthorities().stream()
                            .map(a -> new SimpleGrantedAuthority(a.getAuthority()))
                            .toList();
                }
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, authorities);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}