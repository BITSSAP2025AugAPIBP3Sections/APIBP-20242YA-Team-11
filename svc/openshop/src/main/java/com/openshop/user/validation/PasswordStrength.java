package com.openshop.user.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validation annotation for password strength
 * Ensures passwords meet security requirements:
 * - Minimum 8 characters
 * - At least one uppercase letter
 * - At least one lowercase letter
 * - At least one digit
 * - At least one special character
 */
@Documented
@Constraint(validatedBy = PasswordStrengthValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordStrength {
    
    String message() default "Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one digit, and one special character";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Minimum length of the password
     */
    int minLength() default 8;
    
    /**
     * Require at least one uppercase letter
     */
    boolean requireUppercase() default true;
    
    /**
     * Require at least one lowercase letter
     */
    boolean requireLowercase() default true;
    
    /**
     * Require at least one digit
     */
    boolean requireDigit() default true;
    
    /**
     * Require at least one special character
     */
    boolean requireSpecial() default true;
}
