package com.westwood.busplatform.util;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
public class TelegramUtil {
    @Value("${bot.token}")
    String botToken;

    public ResultBean<String> pushingByUserID(String chat_id,String msg){
        String url=String.format("https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s", botToken, chat_id,msg);
        log.info(url);
        String result = HttpRequest.post(url).execute().body();
        log.info(result);
        JSONObject entries = JSONUtil.parseObj(result);
        if(Objects.equals(entries.getStr("ok"),"true")){
            return new ResultBean<>(true,"success","","");
        }else{
            return new ResultBean<>(false,"error","","");
        }
    }
}
