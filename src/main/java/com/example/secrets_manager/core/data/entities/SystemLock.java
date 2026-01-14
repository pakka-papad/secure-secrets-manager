package com.example.secrets_manager.core.data.entities;

import com.example.secrets_manager.core.data.CoreDataConstants;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = CoreDataConstants.TABLE_SYSTEM_LOCKS, schema = CoreDataConstants.SCHEMA_NAME)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemLock {

  public static final String COL_LOCK_NAME = "lock_name";
  public static final String COL_DESCRIPTION = "description";

  @Id
  @Column(name = COL_LOCK_NAME)
  private String lockName;

  @Column(name = COL_DESCRIPTION)
  private String description;
}
