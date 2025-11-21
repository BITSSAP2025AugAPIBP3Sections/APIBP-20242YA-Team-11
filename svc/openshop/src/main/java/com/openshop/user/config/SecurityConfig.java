package com.openshop.user.config;


import com.openshop.filter.RateLimitFilter;
import com.openshop.user.jwt.JwtRequestFilter;
import com.openshop.user.service.CustomUserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Security configuration for OpenShop with role-based access control.
 * Roles: CUSTOMER, SELLER, ADMIN
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    private final CustomUserDetailsService userDetailsService;
    private final JwtRequestFilter jwtRequestFilter;
    private final RateLimitFilter rateLimitFilter;

    public SecurityConfig(CustomUserDetailsService userDetailsService, JwtRequestFilter jwtRequestFilter, RateLimitFilter rateLimitFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtRequestFilter = jwtRequestFilter;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        logger.info("Configuring HTTP security with role-based access control...");

        http.authorizeHttpRequests(requests -> requests
                // Public endpoints - no authentication required
                .requestMatchers(
                        "/api/v1/auth/login",
                        "/api/v1/auth/register",
                        "/",
                        "/h2-console/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/v2/api-docs/**",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/graphiql/**"
                ).permitAll()
                
                // GraphQL endpoint - requires authentication
                // Public queries are allowed but mutations require proper roles
                .requestMatchers("/graphql").authenticated()
                
                // Product endpoints - public read, seller write
                .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll() // Anyone can browse products
                .requestMatchers(HttpMethod.POST, "/api/v1/products/**").hasRole("SELLER") // Only sellers can add products
                .requestMatchers(HttpMethod.PUT, "/api/v1/products/**").hasRole("SELLER") // Only sellers can update products
                .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**").hasRole("SELLER") // Only sellers can delete products
                
                // Inventory endpoints - public read, seller write
                .requestMatchers(HttpMethod.GET, "/api/v1/inventory/**").permitAll() // Anyone can check inventory
                .requestMatchers(HttpMethod.POST, "/api/v1/inventory/create").hasRole("SELLER") // Only sellers can create inventory
                .requestMatchers(HttpMethod.POST, "/api/v1/inventory/increase").hasRole("SELLER") // Only sellers can increase stock
                .requestMatchers(HttpMethod.POST, "/api/v1/inventory/reduce").authenticated() // System internal only (will be checked in service)
                
                // Cart endpoints - authenticated users (customers)
                .requestMatchers("/api/v1/cart/**").hasRole("CUSTOMER")
                
                // Order endpoints - authenticated users
                .requestMatchers(HttpMethod.POST, "/api/v1/orders/**").hasRole("CUSTOMER") // Customers place orders
                .requestMatchers(HttpMethod.GET, "/api/v1/orders/user/**").hasRole("CUSTOMER") // Customers view their orders
                .requestMatchers(HttpMethod.GET, "/api/v1/orders/{orderId}").authenticated() // Owner or admin
                .requestMatchers(HttpMethod.PUT, "/api/v1/orders/*/status").hasAnyRole("CUSTOMER","SELLER", "ADMIN") // Sellers and admins update order status
                
                // Payment endpoints - authenticated users
                .requestMatchers("/api/v1/payments/**").authenticated()
                
                // Shipping endpoints
                .requestMatchers(HttpMethod.GET, "/api/v1/shipping/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/v1/shipping/**").hasAnyRole("SELLER", "ADMIN")
                
                // Notification endpoints - internal/admin only
                .requestMatchers("/api/v1/notifications/**").hasRole("ADMIN")
                
                // User management endpoints
                .requestMatchers(HttpMethod.GET, "/api/v1/users/me").authenticated() // Any authenticated user can get their own profile
                .requestMatchers(HttpMethod.PUT, "/api/v1/users/me").authenticated() // Any authenticated user can update their own profile
                .requestMatchers(HttpMethod.GET, "/api/v1/users/{id}").hasAnyRole("ADMIN", "CUSTOMER", "SELLER") // Will be further restricted in controller
                .requestMatchers(HttpMethod.DELETE, "/api/v1/users/**").hasRole("ADMIN") // Only admin can delete users
                .requestMatchers("/api/v1/users/**").hasRole("ADMIN") // All other user operations require admin
                
                // Default: all other requests must be authenticated
                .anyRequest().authenticated()
        );

        // Stateless session management for JWT
        http.sessionManagement(session -> 
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        // Disable CSRF for stateless JWT authentication
        http.csrf(AbstractHttpConfigurer::disable);

        // Allow H2 console frames
        http.headers(headers -> 
            headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
        );

        http.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);

        // Add JWT filter before username/password authentication
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        // Enable CORS
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        logger.info("Security configuration complete with role-based access control");
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        logger.info("Creating BCryptPasswordEncoder with strength 12...");
        return new BCryptPasswordEncoder(12); // Increased strength for better security
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        logger.info("Configuring DaoAuthenticationProvider...");
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        logger.info("Configuring AuthenticationManager...");
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        
        // Security: Only allow specific trusted origins
        corsConfiguration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",
            "http://localhost:5173",
            "http://localhost:8080"
        ));
        
        // Do not use allowCredentials with allowedOrigins wildcard
        corsConfiguration.setAllowCredentials(true);
        
        // Specify allowed headers
        corsConfiguration.setAllowedHeaders(Arrays.asList(
            "Origin",
            "Content-Type",
            "Accept",
            "Authorization",
            "X-Requested-With",
            "Idempotency-Key"
        ));
        
        // Specify exposed headers
        corsConfiguration.setExposedHeaders(Arrays.asList(
            "Content-Type",
            "Authorization"
        ));
        
        // Specify allowed HTTP methods
        corsConfiguration.setAllowedMethods(Arrays.asList(
            "GET",
            "POST",
            "PUT",
            "DELETE",
            "OPTIONS",
            "PATCH"
        ));
        
        // Cache preflight response for 1 hour
        corsConfiguration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }
}
