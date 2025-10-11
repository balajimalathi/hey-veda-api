package com.skndan.veda.entity;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;

@Entity
public class FileInfo extends BaseEntity {

    public String name;

    public String type; // pdf, docx, txt, etc.

    @ManyToOne
    public Workspace workspace;

    public UUID uploaderId;

    public String storageUrl;

    public BigDecimal sizeInBytes;

    @Enumerated(EnumType.STRING)
    @Column(name = "ingestion_status")
    public IngestionStatus ingestionStatus = IngestionStatus.PENDING;

    @Column(name = "ingestion_error")
    public String ingestionError;

    /**
     * Ingestion status enumeration
     */
    public enum IngestionStatus {
        PENDING,    // Not yet ingested
        PROCESSING, // Currently being ingested
        COMPLETED,  // Successfully ingested
        FAILED      // Ingestion failed
    }
}
