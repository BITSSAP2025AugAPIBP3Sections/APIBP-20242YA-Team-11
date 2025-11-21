package com.openshop.product.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validation annotation for ISO 4217 currency codes
 * Validates that the currency code is a valid 3-letter ISO code
 */
@Documented
@Constraint(validatedBy = CurrencyCodeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrencyCode {
    
    String message() default "Invalid currency code. Must be a valid ISO 4217 3-letter code (e.g., USD, EUR, GBP)";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}
