package com.openshop.user.validation;

import com.openshop.user.dto.LoginRequestDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator implementation for LoginIdentifierValid annotation
 * Validates that either username or email is provided for login
 */
public class LoginIdentifierValidator implements ConstraintValidator<LoginIdentifierValid, LoginRequestDTO> {
    
    @Override
    public void initialize(LoginIdentifierValid constraintAnnotation) {
        // No initialization needed
    }
    
    @Override
    public boolean isValid(LoginRequestDTO dto, ConstraintValidatorContext context) {
        if (dto == null) {
            return false;
        }
        
        boolean hasUsername = dto.getUsername() != null && !dto.getUsername().trim().isEmpty();
        boolean hasEmail = dto.getEmail() != null && !dto.getEmail().trim().isEmpty();
        
        return hasUsername || hasEmail;
    }
}
