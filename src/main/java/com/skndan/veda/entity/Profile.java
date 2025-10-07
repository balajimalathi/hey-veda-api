package com.skndan.veda.entity;

import jakarta.persistence.Entity;

@Entity
public class Profile extends BaseEntity {
    public String email;
    public String name;
    public String uid;
    public String picture;
}