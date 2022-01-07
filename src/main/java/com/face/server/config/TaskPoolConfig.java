package com.face.server.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Configuration
public class TaskPoolConfig {
    /**
     * cpu 核心数量
     */
    public static final int cpuNum = Runtime.getRuntime().availableProcessors();

    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        // 配置核心线程池数量
        taskExecutor.setCorePoolSize(cpuNum);
        // 配置最大线程池数量
        taskExecutor.setMaxPoolSize(cpuNum * 2);
        /// 线程池所使用的缓冲队列
        taskExecutor.setQueueCapacity(2);
        // 等待时间 （默认为0，此时立即停止），并没等待xx秒后强制停止
        taskExecutor.setAwaitTerminationSeconds(60);
        // 空闲线程存活时间
        taskExecutor.setKeepAliveSeconds(60);
        // 等待任务在关机时完成--表明等待所有线程执行完
        taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        // 线程池名称前缀
        taskExecutor.setThreadNamePrefix("Sync-");
        // 线程池拒绝策略
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
        // 线程池初始化
        taskExecutor.initialize();
        log.info("核心数量：+" + cpuNum + "，线程池初始化......");
        return taskExecutor;
    }
}
