package com.openshop.user.service;



import com.openshop.user.model.User;
import com.openshop.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Custom implementation of UserDetailsService to load user details from the database
 * and return a custom UserDetailsImpl instance for authentication.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserRepository userRepository;

    /**
     * Constructor to initialize the UserRepository for fetching user data.
     *
     * @param userRepository Repository to interact with the User entity.
     */
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads user details by username or email from the database.
     * If the user is not found, a UsernameNotFoundException is thrown.
     *
     * @param usernameOrEmail The username or email of the user to load.
     * @return UserDetails containing user information for authentication.
     * @throws UsernameNotFoundException If the user is not found.
     */
    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        logger.info("Attempting to load user details for username or email: {}", usernameOrEmail);

        // Fetch the user from the repository by username or email
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> {
                    logger.error("User not found with username or email: {}", usernameOrEmail);
                    return new UsernameNotFoundException("User not found with username or email: " + usernameOrEmail);
                });

        logger.info("User found with username: {}", user.getUsername());
        return new UserDetailsImpl(user);  // Returning the custom UserDetailsImpl
    }
}
