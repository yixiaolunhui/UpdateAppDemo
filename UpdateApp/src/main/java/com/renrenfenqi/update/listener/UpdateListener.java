package com.renrenfenqi.update.listener;

/**
 * 下载回调接口
 * Created by zhouweilong on 16/8/22.
 */

public interface UpdateListener {

    /**
     * 开始下载
     */
    public void start();

    /**
     * 下载中
     * @param progress  进度
     */
    public void update(int progress);

    /**
     * 下载成功
     */
    public void success();

    /**
     * 下载错误
     */
    public void error();
}
