package com.skndan.veda.entity;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
public class Workspace extends BaseEntity {

  @NotBlank(message = "Workspace name cannot be blank")
  @Size(min = 2, max = 100, message = "Workspace name must be between 2 and 100 characters")
  public String name; 

  public String emoji; 

  public UUID ownerId;
  
}
