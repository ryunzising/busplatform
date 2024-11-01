package com.westwood.busplatform.util;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.westwood.busplatform.model.KMBETAdto;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class RestUtil {

    //向客戶端發起Bark推送
    public boolean barkPush(String BarkID,String message){
        String url="https://api.day.app/";
        url=url+"/"+BarkID+"/";
        url=url+message;
        String body = HttpRequest.post(url).execute().body();
        log.info(body);
        return true;
    }
    //獲取對應巴士對象
    public KMBETAdto getBUSInfo(String targetURL){
        String body = HttpRequest.get(targetURL).execute().body();
        KMBETAdto bean = JSONUtil.toBean(body, KMBETAdto.class);
        log.info(bean.getData().get(0).getDest_sc());
        return bean;
    }
}
