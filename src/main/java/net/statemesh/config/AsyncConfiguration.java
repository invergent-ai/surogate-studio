package net.statemesh.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import tech.jhipster.async.ExceptionHandlingAsyncTaskExecutor;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Configuration
@EnableAsync
@EnableScheduling
@Slf4j
public class AsyncConfiguration implements AsyncConfigurer {

    @Override
    @Bean(name = "smTaskExecutor")
    @Primary
    public AsyncTaskExecutor getAsyncExecutor() {
        log.debug("Creating Async Task Executor");
        ThreadFactory factory = Thread.ofVirtual().name("sm-task-", 0).factory();
        var executorService = Executors.newThreadPerTaskExecutor(factory);
        return new ExceptionHandlingAsyncTaskExecutor(
            new TaskExecutorAdapter(
                executorService
            )
        );
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }

    @Bean(name = "flowScheduler")
    public ThreadPoolTaskScheduler geFlowScheduler() {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setVirtualThreads(true);
        scheduler.setPoolSize(Runtime.getRuntime().availableProcessors() * 10);
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.setThreadNamePrefix("sm-flow-task-");
        return scheduler;
    }

    @Bean(name = "statusScheduler")
    public ThreadPoolTaskScheduler getStatusScheduler() {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setVirtualThreads(true);
        scheduler.setPoolSize(10);
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.setThreadNamePrefix("sm-poll-task-");
        return scheduler;
    }
}
