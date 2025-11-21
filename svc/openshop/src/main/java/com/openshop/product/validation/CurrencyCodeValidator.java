package com.openshop.product.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Currency;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validator for ISO 4217 currency codes
 * Validates against the list of available currencies in the Java Currency class
 */
public class CurrencyCodeValidator implements ConstraintValidator<CurrencyCode, String> {
    
    private static final Set<String> VALID_CURRENCY_CODES;
    
    static {
        // Initialize the set of valid currency codes from Java's Currency class
        VALID_CURRENCY_CODES = Currency.getAvailableCurrencies()
                .stream()
                .map(Currency::getCurrencyCode)
                .collect(Collectors.toSet());
    }
    
    @Override
    public void initialize(CurrencyCode constraintAnnotation) {
        // No initialization needed
    }
    
    @Override
    public boolean isValid(String currencyCode, ConstraintValidatorContext context) {
        // Null values are considered valid (use @NotNull for null checking)
        if (currencyCode == null) {
            return true;
        }
        
        // Check if the currency code is valid
        String upperCaseCode = currencyCode.trim().toUpperCase();
        
        if (!VALID_CURRENCY_CODES.contains(upperCaseCode)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                String.format("Invalid currency code '%s'. Must be a valid ISO 4217 code (e.g., USD, EUR, GBP, INR)", currencyCode)
            ).addConstraintViolation();
            return false;
        }
        
        return true;
    }
}
