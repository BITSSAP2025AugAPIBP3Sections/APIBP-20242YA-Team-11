package com.openshop.user.controller;


import com.openshop.user.dto.UserDTO;

import com.openshop.user.exception.UnauthorizedException;
import com.openshop.user.exception.UserNotFoundException;
import com.openshop.user.jwt.SecurityUtils;
import com.openshop.user.mapper.UserMapper;
import com.openshop.user.model.User;
import com.openshop.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Controller for managing User-related administrative operations.
 * Only accessible to users with ADMIN role.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "APIs for managing user accounts and profiles")
@SecurityRequirement(name = "bearer-jwt")
public class UserController {

    private final UserService userService;





    /**
     * Delete a user by ID - Only ADMIN can delete users
     *
     * @param id ID of the user to delete
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete user by ID",
        description = "Delete a user account by their ID. Only accessible to users with ADMIN role."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "User successfully deleted"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token", 
                     content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", 
                     content = @Content),
        @ApiResponse(responseCode = "404", description = "User not found", 
                     content = @Content)
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        
        String role = SecurityUtils.getCurrentUserRole();
        log.warn("User with role {} attempting to delete user with ID: {}", role, id);

        User user = userService.getUserById(id);
        if (user == null) {
            throw new UserNotFoundException("Cannot delete. User with ID " + id + " not found");
        }

        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }



    /**
     * API to fetch the currently logged-in user's profile details.
     *
     * @param authentication Spring Security Authentication object (auto-injected).
     * @return User details if found, otherwise HTTP 404.
     */
    @GetMapping("/me")
    @Operation(
        summary = "Get current user profile",
        description = "Fetches the profile details of the currently authenticated user."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved user profile",
                     content = @Content(schema = @Schema(implementation = UserDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                     content = @Content),
        @ApiResponse(responseCode = "404", description = "User not found",
                     content = @Content)
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> getMyDetails(Authentication authentication) {
        String username = authentication.getName();
        log.info("Fetching profile for logged-in user: {}", username);

        return userService.findByUsername(username)
                .map(user -> ResponseEntity.ok(UserMapper.toDTO(user)))
                .orElseThrow(() -> new UserNotFoundException("User with username '" + username + "' not found"));
    }


    /**
     * Update an existing user
     *
     * @param user User object with updated data
     * @return Updated UserDTO
     */
    @PutMapping("/me")
    @Operation(
        summary = "Update current user profile",
        description = "Update the profile information of the currently authenticated user."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User profile successfully updated",
                     content = @Content(schema = @Schema(implementation = UserDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request body or validation error",
                     content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                     content = @Content),
        @ApiResponse(responseCode = "404", description = "User not found",
                     content = @Content)
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> updateUser(Authentication authentication, @Valid @RequestBody User user) {
        String username = authentication.getName(); // Get the username of the logged-in user
        log.info("Fetching profile for logged-in user: {}", username);

        Optional<User> existingUser = userService.findByUsername(username);
        if (existingUser.isEmpty()) {
            throw new UserNotFoundException("Cannot update. User not found");
        }
        else{
            User updatedUser = userService.updateUser(existingUser.get().getId(),user);
            return ResponseEntity.ok(UserMapper.toDTO(updatedUser));
        }


    }
}
