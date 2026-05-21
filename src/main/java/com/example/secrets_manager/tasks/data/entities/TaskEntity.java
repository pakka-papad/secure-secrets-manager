package com.example.secrets_manager.tasks.data.entities;

import com.example.secrets_manager.tasks.data.TaskDataConstants;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = TaskDataConstants.TABLE_TASKS, schema = TaskDataConstants.SCHEMA_NAME)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskEntity {

  public static final String COL_ID = "id";
  public static final String COL_CORRELATION_ID = "correlation_id";
  public static final String COL_PARENT_TASK_ID = "parent_task_id";
  public static final String COL_INITIATOR_USER_ID = "initiator_user_id";
  public static final String COL_CREATED_AT = "created_at";
  public static final String COL_STARTED_AT = "started_at";
  public static final String COL_COMPLETED_AT = "completed_at";
  public static final String COL_TYPE = "type";
  public static final String COL_TASK_INPUT = "task_input";
  public static final String COL_STATE = "state";
  public static final String COL_STATE_EXTRA_INFO = "state_extra_info";
  public static final String COL_TASK_OUTPUT = "task_output";
  public static final String COL_METADATA = "metadata";

  public static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id");

  @Id
  @GeneratedValue
  @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
  @Column(name = COL_ID)
  private UUID id;

  @Column(name = COL_CORRELATION_ID, nullable = false, updatable = false)
  private UUID correlationId;

  @Column(name = COL_PARENT_TASK_ID)
  private UUID parentTaskId;

  @Column(name = COL_INITIATOR_USER_ID, nullable = false)
  private UUID initiatorUserId;

  @Column(name = COL_CREATED_AT, nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = COL_STARTED_AT)
  private Instant startedAt;

  @Column(name = COL_COMPLETED_AT)
  private Instant completedAt;

  @Column(name = COL_TYPE, nullable = false, length = 31)
  private String type;

  @Column(name = COL_TASK_INPUT, columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private String taskInput;

  @Column(name = COL_STATE, nullable = false, length = 31)
  private String state;

  @Column(name = COL_STATE_EXTRA_INFO, columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private String stateExtraInfo;

  @Column(name = COL_TASK_OUTPUT, columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private String taskOutput;

  @Column(name = COL_METADATA, columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private String metadata;

  @PrePersist
  protected void onCreate() {
    createdAt = Instant.now();
  }
}
