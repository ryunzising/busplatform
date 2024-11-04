package com.westwood.busplatform.job;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONUtil;
import com.westwood.busplatform.model.KMBETAdto;
import com.westwood.busplatform.util.RestUtil;
import jakarta.annotation.Resource;

import lombok.extern.slf4j.Slf4j;
import org.quartz.*;

import org.springframework.stereotype.Component;


import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class TestJob implements Job {
    @Resource
    RestUtil restUtil;
    @Resource
    Scheduler scheduler;


    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        log.info("start executing");
//        KMBETAdto busInfo = restUtil.getBUSInfo("https://data.etabus.gov.hk/v1/transport/kmb/eta/90B75A8D2983AC20/75X/1");
        String testJson="{\"type\":\"ETA\",\"version\":\"1.0\",\"generated_timestamp\":\"2024-11-05T01:41:40+08:00\",\"data\":[{\"co\":\"KMB\",\"route\":\"75X\",\"dir\":\"O\",\"service_type\":1,\"seq\":8,\"dest_tc\":\"九龍城碼頭\",\"dest_sc\":\"九龙城码头\",\"dest_en\":\"KOWLOON CITY FERRY\",\"eta_seq\":1,\"eta\":\"2024-11-05T01:18:52+08:00\",\"rmk_tc\":\"最後班次\",\"rmk_sc\":\"最后班次\",\"rmk_en\":\"Final Bus\",\"data_timestamp\":\"2024-11-05T00:41:35+08:00\"}]}";
        KMBETAdto busInfo = JSONUtil.toBean(testJson, KMBETAdto.class);
        if (CollectionUtil.isNotEmpty(busInfo.getData())) {
            Date dataTimestamp = busInfo.getData().get(0).getEta();
            LocalDateTime ldt = Instant.ofEpochMilli(dataTimestamp.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            log.info("info:" + busInfo.getData().toString());
            LocalDateTime triggerTime = ldt.minusMinutes(5);
            log.warn("車輛ETA:" + ldt.toString());
            log.warn("下次推送時間:" + triggerTime.toString());
            try {
                List<JobExecutionContext> currentlyExecutingJobs = scheduler.getCurrentlyExecutingJobs();
                currentlyExecutingJobs.forEach(x -> {
                    log.info(x.getJobDetail().getKey().toString());
                });
                schedulePushing(triggerTime,"testgroup");
            } catch (SchedulerException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void schedulePushing(LocalDateTime targetTime,String groupID) {
        if (targetTime.isAfter(LocalDateTime.now())) {
            try {
                String jobId = "pushingJob" + targetTime.toString();
                String triggerId = "notificationTrigger" + targetTime.toString();

                JobDetail jobDetail = JobBuilder.newJob(PushingJob.class)
                        .withIdentity(jobId, groupID)
                        .build();

                SimpleTrigger simpleTrigger = (SimpleTrigger) TriggerBuilder.newTrigger()
                        .withIdentity(triggerId, groupID)
                        .startAt(Date.from(targetTime.atZone(ZoneId.systemDefault()).toInstant()))
                        .build();
                boolean b = scheduler.checkExists(new JobKey(jobId, groupID));
                if(!b){
                    log.info("no exist job");
                    scheduler.scheduleJob(jobDetail, simpleTrigger);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
