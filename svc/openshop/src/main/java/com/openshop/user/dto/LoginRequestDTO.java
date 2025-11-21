package com.openshop.user.dto;

import com.openshop.user.validation.LoginIdentifierValid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for login requests
 * Proper validation with custom constraint
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@LoginIdentifierValid
public class LoginRequestDTO {

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;  // Optional: provide username for login
    
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;     // Optional: provide email for login
    
    @NotBlank(message = "Password is required")
    @Size(min = 1, max = 100, message = "Password must not exceed 100 characters")
    private String password;  // Required: user password
}
