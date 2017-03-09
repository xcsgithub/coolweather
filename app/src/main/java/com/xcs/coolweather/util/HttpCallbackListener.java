package com.xcs.coolweather.util;

/**
 * Created by xcs on 2017/3/9 0009.
 */
public interface HttpCallbackListener {
    void onFinish(String response);
    void onError(Exception e);
}
