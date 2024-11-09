package com.westwood.busplatform.controller;

import com.westwood.busplatform.constants.BusCompany;
import com.westwood.busplatform.constants.PushingMethod;
import com.westwood.busplatform.job.BusTimerJob;
import com.westwood.busplatform.job.PushingJob;
import com.westwood.busplatform.util.RestUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Controller
@Slf4j
@RequestMapping("/test")
public class TestController {
    @Resource
    RestUtil restUtil;
    @Resource
    Scheduler scheduler;


    @RequestMapping("/push")
    public String pushTest() {
        restUtil.barkPush("12", "測試");
        return "test";
    }

    @RequestMapping("/bus")
    public String busTest() {
        restUtil.getBUSInfoByURL("https://data.etabus.gov.hk/v1/transport/kmb/eta/90B75A8D2983AC20/75X/1");
        return "test";
    }

    @RequestMapping("/timer")
    public void TimerTest() throws SchedulerException {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("company", BusCompany.KMB);
        jobDataMap.put("route", "75X");
        jobDataMap.put("stop", "90B75A8D2983AC20");
        jobDataMap.put("minusMinutes", 5L);
        jobDataMap.put("addMinutes", 1L);
        jobDataMap.put("userID", "admin");
        jobDataMap.put("pushingMethod", PushingMethod.Telegram);
        jobDataMap.put("pushingID", "YOUR PUSHING ID");
        JobDetail jobDetail = JobBuilder.newJob(BusTimerJob.class)
                .withIdentity("test", "testGroup")
                .usingJobData(jobDataMap)
                .build();
        SimpleTrigger simpleTrigger = (SimpleTrigger) TriggerBuilder.newTrigger()
                .withIdentity("testTrigger", "testGroup")
                .startNow().build();
        scheduler.scheduleJob(jobDetail, simpleTrigger);
    }

    @RequestMapping("/monitor")
    public void listScheduledJobs() {
        try {
            for (String groupName : scheduler.getJobGroupNames()) {
                for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
                    List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
                    for (Trigger trigger : triggers) {
                        if (trigger.getNextFireTime() != null) {
                            log.info("Scheduled Job: " + jobKey.getName() + " in group: " + groupName);
                            log.info("Next Fire Time: " + trigger.getNextFireTime());
                        }
                    }
                }
            }
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

}
