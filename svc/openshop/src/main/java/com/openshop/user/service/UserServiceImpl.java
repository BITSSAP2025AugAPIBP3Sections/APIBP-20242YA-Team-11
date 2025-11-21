package com.openshop.user.service;



import com.openshop.user.exception.DuplicateUsernameException;
import com.openshop.user.exception.UserNotFoundException;
import com.openshop.user.model.Role;
import com.openshop.user.model.User;
import com.openshop.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }


    /**
     * Registers a new user by encoding the password and saving the user to the repository.
     *
     * @param user The user object to be registered.
     * @return The saved user object.
     */
    public User createUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        
        try {
            logger.info("Registering user with username: {}", user.getUsername());

            if (userRepository.existsByUsername(user.getUsername())) {
                throw new DuplicateUsernameException(
                        "Username already exists: " + user.getUsername());
            }

            String encodedPassword = passwordEncoder.encode(user.getPassword());
            user.setPassword(encodedPassword);
            if(user.getRole() == null) user.setRole(Role.CUSTOMER);
            
            User savedUser = userRepository.save(user);
            logger.info("User registered successfully with username: {}", user.getUsername());
            return savedUser;
        } catch (DuplicateUsernameException e) {
            logger.error("Duplicate username: {}", user.getUsername());
            throw e;
        } catch (Exception e) {
            logger.error("Error registering user with username: {}", user.getUsername(), e);
            throw new RuntimeException("Failed to create user: " + e.getMessage(), e);
        }
    }

    /**
     * Finds a user by their username.
     *
     * @param username The username of the user to be found.
     * @return An Optional containing the user if found, or empty if not.
     */
    public Optional<User> findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        
        try {
            logger.info("Searching for user with username: {}", username);
            Optional<User> user = userRepository.findByUsername(username);
            if (user.isPresent()) {
                logger.info("User found with username: {}", username);
            } else {
                logger.warn("User not found with username: {}", username);
            }
            return user;
        } catch (Exception e) {
            logger.error("Error searching for user with username: {}", username, e);
            throw new RuntimeException("Failed to find user: " + e.getMessage(), e);
        }
    }

    /**
     * Checks if a user with the given username already exists in the repository.
     *
     * @param username The username to check for existence.
     * @return True if the user exists, false otherwise.
     */
    public boolean userExists(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        
        try {
            logger.info("Checking if user exists with username: {}", username);
            boolean exists = userRepository.existsByUsername(username);
            if (exists) {
                logger.info("User exists with username: {}", username);
            } else {
                logger.info("User does not exist with username: {}", username);
            }
            return exists;
        } catch (Exception e) {
            logger.error("Error checking if user exists with username: {}", username, e);
            throw new RuntimeException("Failed to check user existence: " + e.getMessage(), e);
        }
    }

    /**
     * Updates an existing user's information.
     *
     * @param id The ID of the user to update.
     * @param updatedUser The new user data.
     * @return The updated user object.
     */
    public User updateUser(Long id, User updatedUser) {
        if (id == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (updatedUser == null) {
            throw new IllegalArgumentException("Updated user data cannot be null");
        }
        
        try {
            logger.info("Updating user with ID: {}", id);
            return userRepository.findById(id).map(existingUser -> {
                try {
                    // Update name if provided
                    if (updatedUser.getName() != null && !updatedUser.getName().trim().isEmpty()) {
                        existingUser.setName(updatedUser.getName());
                    }
                    
                    // Update email if provided and different
                    if (updatedUser.getEmail() != null && !updatedUser.getEmail().trim().isEmpty()) {
                        existingUser.setEmail(updatedUser.getEmail());
                    }
                    
                    // Update username if provided and different from existing
                    if (updatedUser.getUsername() != null && 
                        !updatedUser.getUsername().equals(existingUser.getUsername())) {
                        // Check if new username already exists
                        if (userRepository.existsByUsername(updatedUser.getUsername())) {
                            throw new DuplicateUsernameException(
                                "Username already exists: " + updatedUser.getUsername());
                        }
                        existingUser.setUsername(updatedUser.getUsername());
                    }

                    // Only admin should be able to change roles - skip role updates for now
                    // if (updatedUser.getRole() != null) {
                    //     existingUser.setRole(updatedUser.getRole());
                    // }
                    
                    // Update password if provided
                    if (updatedUser.getPassword() != null && !updatedUser.getPassword().trim().isEmpty()) {
                        existingUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
                    }
                    
                    logger.info("User updated successfully with ID: {}", id);
                    return userRepository.save(existingUser);
                } catch (DuplicateUsernameException e) {
                    logger.error("Username already exists during update for user ID: {}", id);
                    throw e;
                } catch (Exception e) {
                    logger.error("Error saving updated user with ID: {}", id, e);
                    throw new RuntimeException("Failed to save updated user", e);
                }
            }).orElseThrow(() -> {
                logger.warn("User with ID: {} not found for update", id);
                return new UserNotFoundException("User with ID " + id + " not found");
            });
        } catch (UserNotFoundException | DuplicateUsernameException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error updating user with ID: {}", id, e);
            throw new RuntimeException("Failed to update user: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes a user by their ID.
     *
     * @param id The ID of the user to delete.
     */
    public void deleteUser(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        try {
            if (userRepository.existsById(id)) {
                userRepository.deleteById(id);
                logger.info("Deleted user with ID: {}", id);
            } else {
                logger.warn("User with ID: {} does not exist", id);
                throw new UserNotFoundException("User with ID " + id + " does not exist");
            }
        } catch (UserNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error deleting user with ID: {}", id, e);
            throw new RuntimeException("Failed to delete user: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves a list of all users in the system.
     *
     * @return List of all users.
     */
    public List<User> getAllUsers() {
        try {
            logger.info("Retrieving all users");
            return userRepository.findAll();
        } catch (Exception e) {
            logger.error("Error retrieving all users", e);
            throw e;
        }
    }

    /**
     * Finds a user by their ID.
     *
     * @param id The ID of the user to find.
     * @return The user object.
     */
    public User getUserById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        try {
            logger.info("Finding user by ID: {}", id);
            return userRepository.findById(id)
                    .orElseThrow(() -> new UserNotFoundException("User with ID " + id + " not found"));
        } catch (UserNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error finding user by ID: {}", id, e);
            throw new RuntimeException("Failed to find user: " + e.getMessage(), e);
        }
    }

    /**
     * Finds users by their role.
     *
     * @param role The role to filter users by.
     * @return List of users with the specified role.
     */
    public List<User> getUsersByRole(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }
        
        try {
            logger.info("Finding users by role: {}", role);
            return userRepository.findByRole(role);
        } catch (Exception e) {
            logger.error("Error finding users by role: {}", role, e);
            throw new RuntimeException("Failed to find users by role: " + e.getMessage(), e);
        }
    }
    
    /**
     * Checks if a user with the given email already exists.
     *
     * @param email The email to check for existence.
     * @return True if the email exists, false otherwise.
     */
    @Override
    public boolean existsByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        
        try {
            logger.debug("Checking if email exists: {}", email);
            return userRepository.existsByEmail(email);
        } catch (Exception e) {
            logger.error("Error checking if email exists: {}", email, e);
            throw new RuntimeException("Failed to check email existence: " + e.getMessage(), e);
        }
    }
}
