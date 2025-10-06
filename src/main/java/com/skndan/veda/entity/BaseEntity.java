package com.skndan.veda.entity;

import java.util.Date;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

/**
 * Base entity class providing common fields and behaviors for all database entities.
 * Extends PanacheEntityBase for simplified JPA operations and includes standard
 * tracking fields like creation and update timestamps.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity extends PanacheEntityBase {

    @Id @GeneratedValue private Long id;

    @Column(name = "created_at")
    public Date createdAt;

    @Column(name = "updated_at")
    public Date updatedAt;

    @Column(name = "created_by")
    public String createdBy;

    @Column(name = "updated_by")
    public String updatedBy;

    @Column(columnDefinition = "boolean default true")
    public Boolean active = true;

    /**
     * Automatically sets creation timestamp when a new entity is first persisted.
     * Initializes both createdAt and updatedAt with the current date.
     */
    @PrePersist
    protected void onCreate() {
        updatedAt = createdAt = new Date();
    }

    /**
     * Automatically updates the updatedAt timestamp whenever the entity is modified.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }
}