package com.skndan.veda.config;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
public class TenantFilter implements ContainerRequestFilter {

    @Inject
    TenantContext tenantContext;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String tenantId = requestContext.getHeaderString("X-Tenant-ID"); // or from token, etc.
        tenantContext.setTenantId(tenantId);
    }
}