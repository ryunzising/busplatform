package com.westwood.busplatform.util;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.westwood.busplatform.model.KMBETAdto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class RestUtil {

    //向客戶端發起Bark推送
    public boolean barkPush(String BarkID,String message){
        String url="https://api.day.app";
        url=url+"/"+BarkID+"/";
        url=url+message;
        log.info(url);
        String body = HttpRequest.post(url).execute().body();
        log.info(body);
        return true;
    }
    //獲取對應巴士對象
    public KMBETAdto getBUSInfoByURL(String targetURL){
        String body = HttpRequest.get(targetURL).execute().body();
        log.info(body);
        KMBETAdto bean = JSONUtil.toBean(body, KMBETAdto.class);
        log.info(bean.getData().get(0).getDest_sc());
        log.info(bean.getData().get(0).getData_timestamp().toString());
        return bean;
    }
    public KMBETAdto getBUSInfoByStopAndRoute(String stop,String route){
        String originalURL="https://data.etabus.gov.hk/v1/transport/kmb/eta/";
        String newURL=originalURL+"/"+stop+"/"+route+"/1";
        log.info(newURL);
        String body = HttpRequest.get(newURL).execute().body();
        KMBETAdto bean = JSONUtil.toBean(body, KMBETAdto.class);
        log.info(bean.getData().get(0).getDest_sc());
        log.info(bean.getData().get(0).getData_timestamp().toString());
        return bean;
    }
}
