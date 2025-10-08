package com.skndan.veda.config;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class TenantContext {
    private String tenantId;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}