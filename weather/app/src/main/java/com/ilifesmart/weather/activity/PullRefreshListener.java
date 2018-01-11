package com.ilifesmart.weather.activity;

/**
 * Created by hlkhjk_ok on 2018/1/11.
 */

public interface PullRefreshListener {
    void onStateChanged(int state);
    void execute();
}
