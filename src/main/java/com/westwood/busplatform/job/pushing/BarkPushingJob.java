package com.westwood.busplatform.job.pushing;

import com.westwood.busplatform.util.RestUtil;
import jakarta.annotation.Resource;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class BarkPushingJob implements Job {
    @Resource
    RestUtil restUtil;
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        JobDataMap jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();
        String id=jobDataMap.getString("pushingID");
        String message=jobDataMap.getString("message");
        restUtil.barkPush(id, message);
    }
}
