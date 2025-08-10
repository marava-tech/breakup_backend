package com.breakupstories.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {

    @Bean(name = "deviceMappingExecutor")
    public Executor deviceMappingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("DeviceMapping-");
        executor.setRejectedExecutionHandler((r, executor1) -> {
            log.warn("Device mapping task rejected, will be handled synchronously");
            r.run();
        });
        executor.initialize();
        return executor;
    }
}
