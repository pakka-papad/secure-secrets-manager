package com.example.secrets_manager.tasks.models;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/** Base interface for all task output payloads. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface TaskOutput {}
