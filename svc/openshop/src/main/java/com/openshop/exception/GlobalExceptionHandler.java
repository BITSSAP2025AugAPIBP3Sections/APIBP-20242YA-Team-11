package com.openshop.exception;

import com.openshop.cart.exception.CartNotFoundException;
import com.openshop.cart.exception.InsufficientStockException;
import com.openshop.common.dto.ErrorResponse;
import com.openshop.inventory.exception.InventoryNotFoundException;
import com.openshop.order.exception.OrderNotFoundException;
import com.openshop.order.exception.OrderPlacementException;
import com.openshop.product.exception.ProductNotFoundException;
import com.openshop.user.exception.DuplicateUsernameException;
import com.openshop.user.exception.UserNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // User-related exceptions
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex, WebRequest request) {
        log.warn("User not found: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .message(ex.getMessage())
                .status(HttpStatus.NOT_FOUND.value())
                .error("User Not Found")
                .errorCode("USER_NOT_FOUND")
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(DuplicateUsernameException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateUsername(DuplicateUsernameException ex, WebRequest request) {
        log.warn("Duplicate username attempted");
        ErrorResponse error = ErrorResponse.builder()
                .message(ex.getMessage())
                .status(HttpStatus.CONFLICT.value())
                .error("Conflict")
                .errorCode("DUPLICATE_USERNAME")
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // Product-related exceptions
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProductNotFound(ProductNotFoundException ex, WebRequest request) {
        log.warn("Product not found");
        ErrorResponse error = ErrorResponse.builder()
                .message(ex.getMessage())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Product Not Found")
                .errorCode("PRODUCT_NOT_FOUND")
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException ex, WebRequest request) {
        log.warn("Entity not found: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .message("The requested resource was not found")
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .errorCode("ENTITY_NOT_FOUND")
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // Cart-related exceptions
    @ExceptionHandler(CartNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCartNotFound(CartNotFoundException ex, WebRequest request) {
        log.warn("Cart not found: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .message(ex.getMessage())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Cart Not Found")
                .errorCode("CART_NOT_FOUND")
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStock(InsufficientStockException ex, WebRequest request) {
        log.warn("Insufficient stock: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .message(ex.getMessage())
                .status(HttpStatus.CONFLICT.value())
                .error("Insufficient Stock")
                .errorCode("INSUFFICIENT_STOCK")
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // Order-related exceptions
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException ex, WebRequest request) {
        log.warn("Order not found: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .message(ex.getMessage())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Order Not Found")
                .errorCode("ORDER_NOT_FOUND")
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(OrderPlacementException.class)
    public ResponseEntity<ErrorResponse> handleOrderPlacement(OrderPlacementException ex, WebRequest request) {
        log.error("Order placement failed: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .message(ex.getMessage())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Order Placement Failed")
                .errorCode("ORDER_PLACEMENT_FAILED")
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    // Inventory-related exceptions
    @ExceptionHandler(InventoryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleInventoryNotFound(InventoryNotFoundException ex, WebRequest request) {
        log.warn("Inventory not found: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .message(ex.getMessage())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Inventory Not Found")
                .errorCode("INVENTORY_NOT_FOUND")
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // Authorization exceptions - handle multiple UnauthorizedException classes
    @ExceptionHandler({
        com.openshop.cart.exception.UnauthorizedException.class,
        com.openshop.inventory.exception.UnauthorizedException.class,
        com.openshop.order.exception.UnauthorizedException.class,
        com.openshop.product.exception.UnauthorizedException.class,
        com.openshop.user.exception.UnauthorizedException.class
    })
    public ResponseEntity<ErrorResponse> handleUnauthorized(RuntimeException ex, WebRequest request) {
        log.warn("Unauthorized access attempt");
        ErrorResponse error = ErrorResponse.builder()
                .message(ex.getMessage())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Forbidden")
                .errorCode("UNAUTHORIZED_ACCESS")
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    // Generic exceptions
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex, WebRequest request) {
        log.warn("Invalid request");
        ErrorResponse error = ErrorResponse.builder()
                .message(ex.getMessage())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .errorCode("INVALID_ARGUMENT")
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // Handle validation errors from @Valid
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        log.warn("Validation failed for request");
        
        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });
        
        ErrorResponse error = ErrorResponse.builder()
                .message("Validation failed")
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .errorCode("VALIDATION_FAILED")
                .validationErrors(validationErrors)
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(java.time.LocalDateTime.now())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    // Handle malformed JSON
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, WebRequest request) {
        log.warn("Malformed JSON request");
        ErrorResponse error = ErrorResponse.builder()
                .message("Malformed JSON request. Please check your request body.")
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .errorCode("MALFORMED_JSON")
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    // Handle database constraint violations
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {
        log.error("Database constraint violation occurred: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .message("A database constraint was violated. This may be due to duplicate data or invalid references.")
                .status(HttpStatus.CONFLICT.value())
                .error("Conflict")
                .errorCode("DATA_INTEGRITY_VIOLATION")
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // Handle all database access exceptions
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessException(DataAccessException ex, WebRequest request) {
        log.error("Database access error occurred: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .message("A database error occurred. Please try again later.")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Database Error")
                .errorCode("DATABASE_ERROR")
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    
    // Handle Spring Security access denied
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        log.warn("Access denied: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .message("You do not have permission to access this resource")
                .status(HttpStatus.FORBIDDEN.value())
                .error("Forbidden")
                .errorCode("ACCESS_DENIED")
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    // Handle authentication exceptions
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, WebRequest request) {
        log.warn("Bad credentials: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .message("Invalid username or password")
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized")
                .errorCode("BAD_CREDENTIALS")
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex, WebRequest request) {
        log.warn("Authentication failed: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .message("Authentication failed. Please check your credentials.")
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized")
                .errorCode("AUTHENTICATION_FAILED")
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    // Handle missing request parameters
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParams(MissingServletRequestParameterException ex, WebRequest request) {
        log.warn("Missing request parameter: {}", ex.getParameterName());
        ErrorResponse error = ErrorResponse.builder()
                .message("Required parameter '" + ex.getParameterName() + "' is missing")
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .errorCode("MISSING_PARAMETER")
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // Handle type mismatch exceptions
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        log.warn("Type mismatch for parameter: {}", ex.getName());
        String message = String.format("Parameter '%s' should be of type %s", 
            ex.getName(), 
            ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        ErrorResponse error = ErrorResponse.builder()
                .message(message)
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .errorCode("TYPE_MISMATCH")
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // Handle null pointer exceptions with proper logging
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ErrorResponse> handleNullPointer(NullPointerException ex, WebRequest request) {
        log.error("Null pointer exception occurred", ex);
        ErrorResponse error = ErrorResponse.builder()
                .message("An internal error occurred. Please contact support if this persists.")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .errorCode("NULL_POINTER")
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    // Handle illegal state exceptions
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex, WebRequest request) {
        log.error("Illegal state exception: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .message(ex.getMessage() != null ? ex.getMessage() : "The system is in an invalid state")
                .status(HttpStatus.CONFLICT.value())
                .error("Conflict")
                .errorCode("ILLEGAL_STATE")
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    
    // Prevent stack trace exposure - catch RuntimeException before Exception
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex, WebRequest request) {
        log.error("Runtime exception occurred: {}", ex.getClass().getSimpleName());
        log.debug("Exception details", ex); // Only log full stack trace in DEBUG level
        
        ErrorResponse error = ErrorResponse.builder()
                .message("An error occurred while processing your request")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .errorCode("RUNTIME_ERROR")
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    // Prevent stack trace exposure - generic exception handler
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        log.error("Unexpected error occurred: {}", ex.getClass().getSimpleName());
        log.debug("Exception details", ex); // Only log full stack trace in DEBUG level
        
        ErrorResponse error = ErrorResponse.builder()
                .message("An unexpected error occurred. Please try again later.")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .errorCode("INTERNAL_ERROR")
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
