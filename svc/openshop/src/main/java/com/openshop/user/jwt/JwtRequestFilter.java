package com.openshop.user.jwt;

import com.openshop.user.model.User;
import com.openshop.user.service.CustomUserDetailsService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Request Filter to validate JWT tokens and authenticate users.
 * Enhanced with better security checks and logging.
 */
@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtRequestFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    public JwtRequestFilter(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain) throws ServletException, IOException {

        final String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
        final String requestURI = request.getRequestURI();

        // Skip JWT validation for public endpoints
        if (isPublicEndpoint(requestURI)) {
            chain.doFilter(request, response);
            return;
        }

        String username = null;
        String jwt = null;

        try {
            // Extract JWT from Authorization header
            if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
                jwt = authorizationHeader.substring(BEARER_PREFIX.length());
                
                // Validate JWT format (basic check)
                if (jwt.trim().isEmpty()) {
                    log.warn("Empty JWT token received for request: {}", requestURI);
                    sendUnauthorizedResponse(response, "Invalid token format");
                    return;
                }
                
                username = jwtUtil.extractUsername(jwt);
                log.debug("Extracted username from JWT: {} for request: {}", username, requestURI);
            } else if (requiresAuthentication(requestURI)) {
                log.warn("Missing or invalid Authorization header for protected endpoint: {}", requestURI);
                sendUnauthorizedResponse(response, "Missing authentication token");
                return;
            }

            // Authenticate user if token is valid and no authentication exists
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                authenticateUser(username, jwt, request, requestURI);
            }

        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired for user: {} on request: {}", username, requestURI);
            sendUnauthorizedResponse(response, "Token expired");
            return;
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token received for request: {}", requestURI);
            sendUnauthorizedResponse(response, "Invalid token format");
            return;
        } catch (SignatureException e) {
            log.warn("JWT signature validation failed for request: {}", requestURI);
            sendUnauthorizedResponse(response, "Invalid token signature");
            return;
        } catch (Exception e) {
            log.error("Error processing JWT token for request: {}: {}", requestURI, e.getMessage());
            sendUnauthorizedResponse(response, "Authentication failed");
            return;
        }

        // Continue with the filter chain
        chain.doFilter(request, response);
    }

    /**
     * Authenticates the user based on JWT token
     * Stores user ID and role in authentication details for @PreAuthorize access
     */
    private void authenticateUser(String username, String jwt, HttpServletRequest request, String requestURI) {
        try {
            // Load user details (which includes User entity)
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // Validate token
            if (Boolean.TRUE.equals(jwtUtil.validateToken(jwt, userDetails.getUsername()))) {
                log.debug("JWT is valid for user: {} on request: {}", username, requestURI);

                // Create authentication token with authorities/roles
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                // Set authentication details with custom attributes
                Map<String, Object> customDetails = new HashMap<>();
                customDetails.put("request", request);
                
                // Extract user ID and role from UserDetails (CustomUserDetails should have User entity)
                if (userDetails instanceof org.springframework.security.core.userdetails.User) {
                    // Extract userId and role from JWT claims if needed
                    try {
                        Long userId = jwtUtil.extractUserId(jwt);
                        String role = jwtUtil.extractRole(jwt);
                        customDetails.put("userId", userId);
                        customDetails.put("role", role);
                        log.debug("Extracted userId: {} and role: {} from JWT", userId, role);
                    } catch (Exception e) {
                        log.warn("Could not extract userId/role from JWT: {}", e.getMessage());
                    }
                }
                
                authToken.setDetails(customDetails);

                // Set authentication in SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.info("User {} authenticated successfully with roles: {} for request: {}",
                        username,
                        userDetails.getAuthorities(),
                        requestURI);
            } else {
                log.warn("JWT token validation failed for user: {} on request: {}", username, requestURI);
            }
        } catch (Exception e) {
            log.error("Error authenticating user {}: {}", username, e.getMessage());
        }
    }

    /**
     * Check if the endpoint is public and doesn't require authentication
     */
    private boolean isPublicEndpoint(String requestURI) {
        return requestURI.startsWith("/api/auth/") ||
               requestURI.startsWith("/h2-console") ||
               requestURI.startsWith("/swagger-ui") ||
               requestURI.startsWith("/v3/api-docs") ||
               requestURI.startsWith("/v2/api-docs") ||
               requestURI.startsWith("/swagger-resources") ||
               requestURI.startsWith("/webjars") ||
               requestURI.equals("/") ||
               (requestURI.startsWith("/api/products") && requestURI.contains("/api/products/")) || // GET products endpoints
               (requestURI.startsWith("/api/inventory") && !requestURI.contains("/create") && !requestURI.contains("/increase") && !requestURI.contains("/reduce"));
    }

    /**
     * Check if the endpoint requires authentication
     */
    private boolean requiresAuthentication(String requestURI) {
        return requestURI.startsWith("/api/") && !isPublicEndpoint(requestURI);
    }

    /**
     * Send unauthorized response
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(String.format("{\"error\": \"Unauthorized\", \"message\": \"%s\"}", message));
        response.getWriter().flush();
    }
}
