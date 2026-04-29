package com.example.secrets_manager.tasks.models;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/** Base interface for additional runtime state info (e.g. progress stats). */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface TaskStateExtraInfo {}
