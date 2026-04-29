package com.example.secrets_manager.tasks.data.entities;

import com.example.secrets_manager.tasks.data.TaskDataConstants;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = TaskDataConstants.TABLE_TASK_ASSIGNMENTS, schema = TaskDataConstants.SCHEMA_NAME)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskAssignmentEntity {

  public static final String COL_TASK_ID = "task_id";
  public static final String COL_WORKER_ID = "worker_id";
  public static final String COL_ASSIGNED_AT = "assigned_at";

  @Id
  @Column(name = COL_TASK_ID)
  private UUID taskId;

  @Column(name = COL_WORKER_ID, nullable = false)
  private UUID workerId;

  @Column(name = COL_ASSIGNED_AT, nullable = false)
  private Instant assignedAt;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = COL_TASK_ID, insertable = false, updatable = false)
  private TaskEntity task;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = COL_WORKER_ID, insertable = false, updatable = false)
  private WorkerRegistryEntity worker;
}
