package com.westwood.busplatform.job;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateTime;
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
import java.util.Locale;
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
        long addMinutes = jobDataMap.getLong("addMinutes");//下次抓取數據的時間
        String userID = jobDataMap.getString("userID");//
        String pushingMethod = jobDataMap.getString("pushingMethod");//推送方式 BARK TG
        String pushingID = jobDataMap.getString("pushingID");//推送ID

        if (company.equals(BusCompany.KMB)) {
            KMBETAdto BUS = restUtil.getBUSInfoByStopAndRoute(stop, route);
            List<KMBETAdto.data> BusData = BUS.getData();
            Date eta = null;
            if (CollectionUtil.isNotEmpty(BusData)) {
                if (!Objects.isNull(BusData.get(0).getEta())) {
                    LocalDateTime tempTime=DateUtil.toLocalDateTime(BusData.get(0).getEta()).minusMinutes(minusMinutes);
                    if (DateUtil.between(BusData.get(0).getEta(), new Date(), DateUnit.MINUTE) < 0||tempTime.isBefore(LocalDateTime.now())) {
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
                        if (BusData.size() > 1) {
                            eta = BusData.get(1).getEta();
                        }else{
                            String errMsg = "很抱歉，今天的末班車已經離開(" + route + ")";
                            try {
                                ErrPushing(userID + "|" + route + "|" + stop + "ERR", pushingMethod, pushingID, errMsg);
                            } catch (SchedulerException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                    LocalDateTime localDateTime = DateUtil.toLocalDateTime(eta);
                    LocalDateTime triggerTime = localDateTime.minusMinutes(minusMinutes);
                    LocalDateTime nextTriggerTime = localDateTime.plusMinutes(addMinutes);
                    log.warn("車輛ETA:" + localDateTime.toString());
                    log.warn("下次推送時間（理論）:" + triggerTime.toString());
                    String msg = "您所訂閲的" + route + "路巴士將於" + minusMinutes + "分鐘内抵達，ETA:+" + DateUtil.formatTime(eta);
                    try {
                        //安排下次推送之後
                        schedulePushing(triggerTime, userID + "|" + route + "|" + stop, pushingMethod, pushingID, msg);
                        scheduleNextBusTimer(company,route,stop,localDateTime,minusMinutes,addMinutes,userID,pushingMethod,pushingID);
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

    private void scheduleNextBusTimer(String company,String route,String stop,LocalDateTime eta,long minusMinutes,long addMinutes,String userID,String pushingMethod,String pushingID) throws SchedulerException {
        log.info("try to schedule next timer on "+new Date().toString());
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("company", company);
        jobDataMap.put("route", route);
        jobDataMap.put("stop", stop);
        jobDataMap.put("minusMinutes", minusMinutes);
        jobDataMap.put("addMinutes", addMinutes);
        jobDataMap.put("userID", userID);
        jobDataMap.put("pushingMethod", pushingMethod);
        jobDataMap.put("pushingID", pushingID);
        LocalDateTime triggerTime=eta.plusMinutes(addMinutes);
        DateTime dateTime = new DateTime(triggerTime);
        JobDetail jobDetail = JobBuilder.newJob(BusTimerJob.class)
                .withIdentity("nextScanTime"+"|"+route+"|"+stop+"|"+dateTime.toString(), userID+"|"+route+"|"+stop+"|nextSchedule")
                .usingJobData(jobDataMap)
                .build();
        SimpleTrigger simpleTrigger = (SimpleTrigger) TriggerBuilder.newTrigger()
                .withIdentity("nextScanTime"+"|"+route+"|"+stop+"|"+dateTime.toString(), userID+"|"+route+"|"+stop+"|nextSchedule")
                .startAt(dateTime).build();
        scheduler.scheduleJob(jobDetail, simpleTrigger);

    }
}
