package com.skndan.veda.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Profile extends PanacheEntityBase {
    @Id
    public String id;
    public String email;
    public String name;
}