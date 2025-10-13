package com.skndan.veda.entity;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
public class Users extends BaseEntity {

  // name: string;
  // email: string;
  // group: IGroup;
  // status: 'ACTIVE' | 'INACTIVE';

  @NotBlank(message = "User name cannot be blank")
  @Size(min = 2, max = 50, message = "Group name must be between 2 and 50 characters")
  public String name;

  @NotBlank(message = "Group description cannot be blank")
  @Size(min = 2, max = 100, message = "Group description must be between 2 and 100 characters")
  public String email;

  @Enumerated(EnumType.STRING)
  public UserStatus status;

  @ManyToOne
  public Groups group;

  public UUID ownerId;
}
