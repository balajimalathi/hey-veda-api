package com.skndan.veda.entity;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
public class Groups extends BaseEntity {

  @NotBlank(message = "Group name cannot be blank")
  @Size(min = 2, max = 50, message = "Group name must be between 2 and 50 characters")
  public String name;

  @NotBlank(message = "Group description cannot be blank")
  @Size(min = 2, max = 100, message = "Group description must be between 2 and 100 characters")
  public String description;

  public UUID ownerId;
}
