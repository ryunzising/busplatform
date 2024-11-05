package com.westwood.busplatform.util;

import lombok.Data;
import org.springframework.stereotype.Component;

@Component
@Data
public class ResultBean<T> {
    private static final long serialVersionUID = 1L;
    Boolean success;
    T data;
    String errCode;
    String errMsg;

    public ResultBean(){
        super();
    }

    public ResultBean(Boolean success, T data, String errCode, String errMsg) {
        this.success = success;
        this.data = data;
        this.errCode = errCode;
        this.errMsg = errMsg;
    }
    public ResultBean(Boolean success) {
        this.success = success;
    }
}
