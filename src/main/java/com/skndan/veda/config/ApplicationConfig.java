package com.skndan.veda.config;

import jakarta.ws.rs.ApplicationPath;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.info.Info;

import jakarta.ws.rs.core.Application;


/**
 * Configuration class for the application's REST API and OpenAPI documentation.
 * Sets up global API information, security requirements, and base application path.
 *
 * Configures Keycloak JWT bearer token authentication for the entire application.
 */
@OpenAPIDefinition(
        info = @Info(title = "My API", version = "1.0"),
        security = @SecurityRequirement(name = "Keycloak") // Apply globally
)
@SecurityScheme(
        securitySchemeName = "Keycloak",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
@ApplicationPath("/api")
public class ApplicationConfig extends Application {
}