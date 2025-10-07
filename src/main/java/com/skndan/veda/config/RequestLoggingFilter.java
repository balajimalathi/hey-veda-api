package com.skndan.veda.config;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

@Provider
public class RequestLoggingFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(RequestLoggingFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String method = requestContext.getMethod();
        String path = requestContext.getUriInfo().getPath();

        LOG.info("➡ Incoming request: " + method + " " + path);

        // Read the body (careful: InputStream can be consumed only once)
        if (requestContext.hasEntity()) {
            InputStream entityStream = requestContext.getEntityStream();
            String body;
            try (Scanner scanner = new Scanner(entityStream, StandardCharsets.UTF_8.name())) {
                body = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
            }
            LOG.info("📦 Payload: " + body);

            // Reset the stream so resource can read it again
            requestContext.setEntityStream(
                    new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8))
            );
        }
    }
}