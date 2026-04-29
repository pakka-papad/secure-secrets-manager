package com.example.secrets_manager.tasks.data.entities;

import com.example.secrets_manager.tasks.data.TaskDataConstants;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = TaskDataConstants.TABLE_WORKER_REGISTRY, schema = TaskDataConstants.SCHEMA_NAME)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkerRegistryEntity {

  public static final String COL_ID = "id";
  public static final String COL_LAST_HEARTBEAT = "last_heartbeat";

  @Id
  @Column(name = COL_ID)
  private UUID id;

  @Column(name = COL_LAST_HEARTBEAT, nullable = false)
  private Instant lastHeartbeat;
}
