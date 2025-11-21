package com.openshop.product.config;

import com.openshop.user.jwt.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Interceptor to extract authentication from Spring Security context and add to GraphQL context.
 * This replaces the insecure header-based authentication with JWT-based authentication.
 * 
 * Security Flow:
 * 1. JWT is validated by JwtRequestFilter (Spring Security)
 * 2. Authentication is stored in SecurityContext
 * 3. This interceptor extracts user info from SecurityContext
 * 4. User info is added to GraphQL context for use in resolvers
 */
@Component
@Slf4j
public class GraphQLWebInterceptor implements WebGraphQlInterceptor {

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        // Get authentication from Spring Security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() 
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            
            String username = authentication.getName();
            
            // Extract user ID from authentication details (set by JwtRequestFilter)
            Long userId = null;
            Object details = authentication.getDetails();
            if (details instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> detailsMap = (Map<String, Object>) details;
                Object userIdObj = detailsMap.get("userId");
                if (userIdObj instanceof Long) {
                    userId = (Long) userIdObj;
                } else if (userIdObj instanceof Integer) {
                    userId = ((Integer) userIdObj).longValue();
                }
            }
            
            // If userId not in details, try SecurityUtils
            if (userId == null) {
                userId = SecurityUtils.getCurrentUserId();
            }
            
            // Extract role from authorities or details
            String role = null;
            if (details instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> detailsMap = (Map<String, Object>) details;
                Object roleObj = detailsMap.get("role");
                if (roleObj instanceof String) {
                    role = (String) roleObj;
                }
            }
            
            // Fallback to extracting role from authorities
            if (role == null) {
                role = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(auth -> auth.startsWith("ROLE_"))
                    .map(auth -> auth.substring(5)) // Remove "ROLE_" prefix
                    .findFirst()
                    .orElse(null);
            }
            
            log.debug("GraphQL Request - Authenticated User: {}, User ID: {}, Role: {}", 
                username, userId, role);
            
            // Add authentication info to GraphQL context (using new keys without X- prefix)
            Long finalUserId = userId;
            String finalRole = role;
            request.configureExecutionInput((executionInput, builder) -> {
                return builder.graphQLContext(contextBuilder -> {
                    contextBuilder.put("username", username);
                    if (finalUserId != null) {
                        contextBuilder.put("userId", finalUserId);
                    }
                    if (finalRole != null) {
                        contextBuilder.put("role", finalRole);
                    }
                    contextBuilder.put("authenticated", true);
                }).build();
            });
        } else {
            log.debug("GraphQL Request - No authentication found");
            
            // For unauthenticated requests, still create context but mark as not authenticated
            request.configureExecutionInput((executionInput, builder) -> {
                return builder.graphQLContext(contextBuilder -> {
                    contextBuilder.put("authenticated", false);
                }).build();
            });
        }
        
        return chain.next(request);
    }
}
