package com.westwood.busplatform.controller;

import com.westwood.busplatform.util.RestUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@Slf4j
@RequestMapping("/test")
public class TestController {
    @Resource
    RestUtil restUtil;

    @RequestMapping("/push")
    public String pushTest(){
        restUtil.barkPush("12","測試");
        return "test";
    }

    @RequestMapping("/bus")
    public String busTest(){
        restUtil.getBUSInfo("https://data.etabus.gov.hk/v1/transport/kmb/eta/90B75A8D2983AC20/75X/1");
        return "test";
    }
}
