package com.msb.mall.seckill.schedule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务
 * 1. @EnableScheduling 开启定时任务
 * 2. @Scheduled 具体开启一个定时任务 通过cron表达式来定时
 */
@Slf4j
//@EnableScheduling
//@EnableAsync
@Component
public class SeckillSchedule {

    /**
     * 默认情况下定时任务是同步任务，需要能够异步处理
     * 1. 把需要定时执行的任务交给异步处理器来处理
     * 2. 把需要执行的方法异步执行
     * @EnableAsync 开启异步任务的功能
     * @Async 标识希望异步执行的方法
     */
    @Async
//    @Scheduled(cron = "* * * * * *")
    public void schedule(){
        log.info("定时任务....");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
