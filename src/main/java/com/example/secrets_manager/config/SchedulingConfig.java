package com.example.secrets_manager.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's scheduled task execution capability.
 *
 * <p>Note: Spring's scheduling infrastructure automatically looks for a bean named
 * <b>'taskScheduler'</b> to power all methods annotated with {@code @Scheduled}. Our custom
 * multi-threaded scheduler is defined in {@link TaskExecutorConfig}.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {}
