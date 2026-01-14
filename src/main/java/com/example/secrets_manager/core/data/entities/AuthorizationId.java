package com.example.secrets_manager.core.data.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationId implements Serializable {

  public static final String COL_USER_ID = "user_id";
  public static final String COL_GROUP_ID = "group_id";

  @Column(name = COL_USER_ID)
  private UUID userId;

  @Column(name = COL_GROUP_ID)
  private UUID groupId;
}
