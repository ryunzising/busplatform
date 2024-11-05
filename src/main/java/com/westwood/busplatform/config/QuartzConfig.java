package com.westwood.busplatform.config;

import com.westwood.busplatform.job.PushingJob;
import com.westwood.busplatform.job.TestJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {
//    @Bean
//    public JobDetail fetchTimeJobDetail() {
//        return JobBuilder.newJob(TestJob.class).withIdentity("testJob", "group1").storeDurably().build();
//    }
//
//    @Bean
//    public Trigger fetchTimeJobTrigger() {
//        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule().withIntervalInMinutes(1).repeatForever(); // 每1分鐘執行一次 .
//        return TriggerBuilder.newTrigger().forJob(fetchTimeJobDetail()).withIdentity("testTrigger", "group1").withSchedule(scheduleBuilder).build();
//    }
}
