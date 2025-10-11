package com.skndan.veda.config;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

@Provider
public class RequestLoggingFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(RequestLoggingFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String method = requestContext.getMethod();
        String path = requestContext.getUriInfo().getPath();

        LOG.info("➡ Incoming request: " + method + " " + path);

        if (!requestContext.hasEntity()) {
            return;
        }

        MediaType mediaType = requestContext.getMediaType();

        // Check for multipart uploads
        if (mediaType != null && mediaType.isCompatible(MediaType.MULTIPART_FORM_DATA_TYPE)) {
            LOG.info("📁 Multipart request detected");

            // Try to extract file names from Content-Disposition headers if present
            List<String> contentDispositionHeaders = requestContext.getHeaders().get("Content-Disposition");
            if (contentDispositionHeaders != null && !contentDispositionHeaders.isEmpty()) {
                for (String disposition : contentDispositionHeaders) {
                    if (disposition.contains("filename=")) {
                        String filename = disposition.replaceAll(".*filename=\"([^\"]+)\".*", "$1");
                        LOG.info("📄 Uploaded file: " + filename);
                    }
                }
            } else {
                LOG.info("📄 Multipart request (file content skipped)");
            }

            // Don’t consume the input stream for multipart
            return;
        }

        // For non-multipart, read and log body
        InputStream entityStream = requestContext.getEntityStream();
        String body;
        try (Scanner scanner = new Scanner(entityStream, StandardCharsets.UTF_8.name())) {
            body = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
        }

        LOG.info("📦 Payload: " + body);

        // Reset stream
        requestContext.setEntityStream(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
    }

}