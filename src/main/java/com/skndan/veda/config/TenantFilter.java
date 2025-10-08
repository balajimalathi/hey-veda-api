package com.skndan.veda.config;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHENTICATION - 1)
public class TenantFilter implements ContainerRequestFilter {

    @Inject
    TenantContext tenantContext;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String tenantId = requestContext.getHeaderString("X-Tenant-ID"); // or from token, etc.
        if (tenantId != null && !tenantId.isEmpty()) {
            tenantContext.setTenantId(tenantId);
        }
    }
}