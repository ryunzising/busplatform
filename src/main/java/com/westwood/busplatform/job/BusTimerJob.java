package com.westwood.busplatform.job;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.westwood.busplatform.constants.BusCompany;
import com.westwood.busplatform.constants.PushingMethod;
import com.westwood.busplatform.job.pushing.BarkPushingJob;
import com.westwood.busplatform.job.pushing.TelegramPushingJob;
import com.westwood.busplatform.model.KMBETAdto;
import com.westwood.busplatform.util.RestUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.util.StringUtil;
import org.quartz.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class BusTimerJob implements Job {
    @Resource
    Scheduler scheduler;
    @Resource
    RestUtil restUtil;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        JobDataMap jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();
        String company = jobDataMap.getString("company");//公司 KMB-九巴 GMB-小巴
        String route = jobDataMap.getString("route"); // 路綫
        String stop = jobDataMap.getString("stop"); //停車站
        String period = jobDataMap.getString("period"); //每多久刷新一次數據
        long minusMinutes = jobDataMap.getLong("minusMinutes"); //到站前多久向用戶推送
        String userID = jobDataMap.getString("userID");//
        String pushingMethod = jobDataMap.getString("pushingMethod");//
        String pushingID = jobDataMap.getString("pushingID");//

        if (company.equals(BusCompany.KMB)) {
            KMBETAdto BUS = restUtil.getBUSInfoByStopAndRoute(stop, route);
            List<KMBETAdto.data> BusData = BUS.getData();
            Date eta = null;
            if (CollectionUtil.isNotEmpty(BusData)) {
                if (!Objects.isNull(BusData.get(0).getEta())) {
                    if (DateUtil.between(BusData.get(0).getEta(), new Date(), DateUnit.MINUTE) < 0) {
                        //當最近一條巴士eta為負時，去取出下一條巴士記錄,如果下一條取不到，另作處理
                        if (BusData.size() > 1) {
                            eta = BusData.get(1).getEta();
                        } else {
                            //另作處理
                            String errMsg = "很抱歉，今天的末班車已經離開(" + route + ")";
                            try {
                                ErrPushing(userID + "|" + route + "|" + stop + "ERR", pushingMethod, pushingID, errMsg);
                            } catch (SchedulerException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    } else {
                        eta = BusData.get(0).getEta();
                    }
                    LocalDateTime localDateTime = DateUtil.toLocalDateTime(eta);
                    LocalDateTime triggerTime = localDateTime.minusMinutes(minusMinutes);
                    log.warn("車輛ETA:" + localDateTime.toString());
                    log.warn("下次推送時間（理論）:" + triggerTime.toString());
                    String msg = "您所訂閲的" + route + "路巴士將於" + minusMinutes + "内抵達，ETA:+" + DateUtil.formatTime(eta);
                    try {
                        schedulePushing(triggerTime, userID + "|" + route + "|" + stop, pushingMethod, pushingID, msg);
                    } catch (SchedulerException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    String errMsg = "很抱歉，今天的末班車已經離開(" + route + ")";
                    try {
                        ErrPushing(userID + "|" + route + "|" + stop + "ERR", pushingMethod, pushingID, errMsg);
                    } catch (SchedulerException e) {
                        throw new RuntimeException(e);
                    }
                }
            } else {
                String errMsg = "很抱歉，今天的末班車已經離開(" + route + ")";
                try {
                    ErrPushing(userID + "|" + route + "|" + stop + "ERR", pushingMethod, pushingID, errMsg);
                } catch (SchedulerException e) {
                    throw new RuntimeException(e);
                }
            }
        } else if (company.equals(BusCompany.GMB)) {

        }

    }

    private void schedulePushing(LocalDateTime targetTime, String groupID, String pushingMethod, String pushingID, String msg) throws SchedulerException {
        if (targetTime.isAfter(LocalDateTime.now())) {
            String jobID = groupID + targetTime.toString();
            String triggerID = jobID + "|" + "trigger";
            SimpleTrigger simpleTrigger = (SimpleTrigger) TriggerBuilder.newTrigger()
                    .withIdentity(triggerID, groupID)
                    .startAt(Date.from(targetTime.atZone(ZoneId.systemDefault()).toInstant()))
                    .build();
            boolean b = scheduler.checkExists(new JobKey(jobID, groupID));
            JobDataMap dataMap = new JobDataMap();
            dataMap.put("pushingID", pushingID);
            dataMap.put("message", msg);
            if (!b) {
                log.info("no exist job");
                if (pushingMethod.equals(PushingMethod.BARK)) {
                    JobDetail jobDetail = JobBuilder.newJob(BarkPushingJob.class)
                            .withIdentity(jobID, groupID)
                            .usingJobData(dataMap)
                            .build();
                    scheduler.scheduleJob(jobDetail, simpleTrigger);
                } else if (pushingMethod.equals(PushingMethod.Telegram)) {
                    JobDetail jobDetail = JobBuilder.newJob(TelegramPushingJob.class)
                            .withIdentity(jobID, groupID)
                            .usingJobData(dataMap)
                            .build();
                    scheduler.scheduleJob(jobDetail, simpleTrigger);
                } else {

                }
            }

        }
    }

    private void ErrPushing(String groupID, String pushingMethod, String pushingID, String msg) throws SchedulerException {
        String jobID = groupID + LocalDateTime.now().toString() + "ERROR";
        String triggerID = jobID + "|" + "trigger";
        SimpleTrigger simpleTrigger = (SimpleTrigger) TriggerBuilder.newTrigger()
                .withIdentity(triggerID, groupID)
                .startNow()
                .build();
        boolean b = scheduler.checkExists(new JobKey(jobID, groupID));
        JobDataMap dataMap = new JobDataMap();
        dataMap.put("pushingID", pushingID);
        dataMap.put("message", msg);
        if (!b) {
            log.info("no exist job");
            if (pushingMethod.equals(PushingMethod.BARK)) {
                JobDetail jobDetail = JobBuilder.newJob(BarkPushingJob.class)
                        .withIdentity(jobID, groupID)
                        .usingJobData(dataMap)
                        .build();
                scheduler.scheduleJob(jobDetail, simpleTrigger);
            } else if (pushingMethod.equals(PushingMethod.Telegram)) {
                JobDetail jobDetail = JobBuilder.newJob(TelegramPushingJob.class)
                        .withIdentity(jobID, groupID)
                        .usingJobData(dataMap)
                        .build();
                scheduler.scheduleJob(jobDetail, simpleTrigger);
            } else {

            }
        }

    }
}
