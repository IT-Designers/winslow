package de.itdesigners.winslow.web;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.context.request.async.TimeoutCallableProcessingInterceptor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Nonnull;
import java.util.concurrent.Callable;

/**
 * Questions, I have questions:
 * https://dzone.com/articles/streaming-data-with-spring-boot-restful-web-servic
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncTaskConfiguration implements AsyncConfigurer {

    @Autowired
    private WebMvcProperties properties;


    @Override
    @Bean(name = "taskExecutor")
    public AsyncTaskExecutor getAsyncExecutor() {
        var cpuCount = Runtime.getRuntime().availableProcessors();
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(cpuCount);
        // executor.setMaxPoolSize(cpuCount * 2);
        // executor.setQueueCapacity(cpuCount * 10);
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }

    @Bean
    public WebMvcConfigurer webMvcConfigurer(
            AsyncTaskExecutor taskExecutor,
            CallableProcessingInterceptor taskInterceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void configureAsyncSupport(@Nonnull AsyncSupportConfigurer configurer) {
                WebMvcConfigurer.super.configureAsyncSupport(
                        configurer
                                .setDefaultTimeout(properties.getAsync().getRequestTimeout().toMillis())
                                .setTaskExecutor(taskExecutor)
                                .registerCallableInterceptors(taskInterceptor)
                );
            }
        };
    }

    @Bean
    public CallableProcessingInterceptor taskInterceptor() {
        return new TimeoutCallableProcessingInterceptor() {
            @Override
            public <T> Object handleTimeout(
                    NativeWebRequest request,
                    Callable<T> task) throws Exception {
                return super.handleTimeout(request, task);
            }
        };
    }
}
