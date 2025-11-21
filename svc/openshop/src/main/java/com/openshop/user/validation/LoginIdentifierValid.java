package com.openshop.user.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Custom validation annotation to ensure either username or email is provided for login
 * Move validation logic from controller to declarative constraint
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = LoginIdentifierValidator.class)
@Documented
public @interface LoginIdentifierValid {
    String message() default "Either username or email must be provided";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
