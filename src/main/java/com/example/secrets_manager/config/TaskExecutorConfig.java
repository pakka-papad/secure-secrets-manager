package com.example.secrets_manager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class TaskExecutorConfig {

  /** Dedicated executor for background task logic (e.g. Master Key Migration). */
  @Bean(name = "taskExecutor")
  public ThreadPoolTaskExecutor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5);
    executor.setMaxPoolSize(10);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("TaskWorker-");
    executor.initialize();
    return executor;
  }

  /**
   * Dedicated scheduler for heartbeats and polling cycles.
   *
   * <p>Naming this bean <b>'taskScheduler'</b> ensures that Spring's {@code @EnableScheduling}
   * infrastructure automatically discovers and uses this multi-threaded pool instead of the default
   * single-threaded one.
   */
  @Bean(name = "taskScheduler")
  public ThreadPoolTaskScheduler taskScheduler() {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(3);
    scheduler.setThreadNamePrefix("TaskSched-");
    scheduler.initialize();
    return scheduler;
  }
}
