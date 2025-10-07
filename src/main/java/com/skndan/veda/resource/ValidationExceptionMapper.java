package com.skndan.veda.resource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider; 

@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        // Create a detailed error response with all validation errors
        ValidationErrorResponse errorResponse = new ValidationErrorResponse();
        
        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            errorResponse.addError(
                violation.getPropertyPath().toString(), 
                violation.getMessage()
            );
        }

        return Response
            .status(Response.Status.BAD_REQUEST)
            .entity(errorResponse)
            .build();
    }

    // Inner class to structure validation error response
    public static class ValidationErrorResponse {
        private java.util.List<ValidationError> errors = new java.util.ArrayList<>();

        public void addError(String field, String message) {
            errors.add(new ValidationError(field, message));
        }

        public java.util.List<ValidationError> getErrors() {
            return errors;
        }
    }

    // Inner class representing a single validation error
    public static class ValidationError {
        private String field;
        private String message;

        public ValidationError(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() {
            return field;
        }

        public String getMessage() {
            return message;
        }
    }
}