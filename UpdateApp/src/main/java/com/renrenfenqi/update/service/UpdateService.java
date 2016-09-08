package com.renrenfenqi.update.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.renrenfenqi.update.BuildConfig;
import com.renrenfenqi.update.UpdateConfig;
import com.renrenfenqi.update.R;
import com.renrenfenqi.update.listener.UpdateListener;
import com.renrenfenqi.update.utils.AppInfoUtil;
import com.renrenfenqi.update.utils.UpdateFileUtil;
import com.renrenfenqi.update.utils.InstallApkUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by zhouweilong on 16/8/22.
 */

public class UpdateService extends Service {

    public static final String TAG =  "UpdateService";

    //广播action
    public static final String ACTION = "update_action";
    public static final String STATUS = "status";
    public static final String PROGRESS = "progress";
    public static final String FILEPATH = "filepath";

    //params参数
    public static final String URL = "downloadUrl";
    public static final String ICO_RES_ID = "icoResId";
    public static final String ICO_SMALL_RES_ID = "icoSmallResId";
    public static final String UPDATE_PROGRESS = "updateProgress";
    public static final String STORE_DIR = "storeDir";
    public static final String APP_NAME = "appName";
    public static final String DOWNLOAD_NOTIFICATION_FLAG = "downloadNotificationFlag";
    public static final String DOWNLOAD_SUCCESS_NOTIFICATION_FLAG = "downloadSuccessNotificationFlag";
    public static final String DOWNLOAD_ERROR_NOTIFICATION_FLAG = "downloadErrorNotificationFlag";
    public static final String IS_SEND_BROADCAST = "isSendBroadcast";
    public static final String IS_FORCE_UPDATE = "isForceUpdate";
    public static final String IS_AUTO_INSTALL = "isAutoInstall";


    //下载大小通知频率
    public static final int UPDATE_NUMBER_SIZE = 1;
    public static final int DEFAULT_RES_ID = -1;
    //更新状态
    public static final int UPDATE_START_STATUS =0;
    public static final int UPDATE_PROGRESS_STATUS = 1;
    public static final int UPDATE_SUCCESS_STATUS = 2;
    public static final int UPDATE_ERROR_STATUS = -1;


    private String downloadUrl;
    private String appName;
    private int icoResId;
    private int icoSmallResId;
    private int updateProgress;
    private int downloadNotificationFlag;
    private int downloadSuccessNotificationFlag;
    private int downloadErrorNotificationFlag;
    private boolean isSendBroadcast=true;
    private boolean isForceUpdate;
    private boolean isAutoInstall;

    private UpdateListener updateListener;//回调接口

    public LocalBinder localBinder = new LocalBinder();

    public String storeDir;//默认存放路径

    private boolean startDownload;//开始下载
    private int lastProgressNumber;
    private NotificationCompat.Builder builder;
    private NotificationManager manager;
    private int notifyId;
    private LocalBroadcastManager localBroadcastManager;
    private Intent localIntent;
    private DownloadApk downloadApkTask;
    public int updateTotalSize=0;

    public class LocalBinder extends Binder {
        /**
         * 设置下载回调
         * @param listener
         */
        public void setUpdateListener(UpdateListener listener){
            UpdateService.this.setUpdateProgressListener(listener);
        }
    }

    public void setUpdateProgressListener(UpdateListener updateListener) {
        this.updateListener = updateListener;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return localBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        if (downloadApkTask != null){
            downloadApkTask.cancel(true);
        }
        if (updateListener != null){
            updateListener = null;
        }
        localIntent = null;
        builder = null;

        super.onDestroy();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (!startDownload && intent != null){
            startDownload = true;
            downloadUrl = intent.getStringExtra(URL);
            icoResId = intent.getIntExtra(ICO_RES_ID, DEFAULT_RES_ID);
            icoSmallResId = intent.getIntExtra(ICO_SMALL_RES_ID, DEFAULT_RES_ID);
            storeDir = intent.getStringExtra(STORE_DIR);
            updateProgress = intent.getIntExtra(UPDATE_PROGRESS, UPDATE_NUMBER_SIZE);
            downloadNotificationFlag = intent.getIntExtra(DOWNLOAD_NOTIFICATION_FLAG, 0);
            downloadErrorNotificationFlag = intent.getIntExtra(DOWNLOAD_ERROR_NOTIFICATION_FLAG, 0);
            downloadSuccessNotificationFlag = intent.getIntExtra(DOWNLOAD_SUCCESS_NOTIFICATION_FLAG, 0);
            isSendBroadcast = intent.getBooleanExtra(IS_SEND_BROADCAST, true);
            isForceUpdate = intent.getBooleanExtra(IS_FORCE_UPDATE, false);
            isAutoInstall = intent.getBooleanExtra(IS_AUTO_INSTALL, true);
            appName=intent.getStringExtra(APP_NAME);

            if(TextUtils.isEmpty(appName)){
                appName=AppInfoUtil.getAppName(this);
            }

            if (BuildConfig.DEBUG){
                Log.d(TAG, "downloadUrl: " + downloadUrl);
                Log.d(TAG, "icoResId: " + icoResId);
                Log.d(TAG, "icoSmallResId: " + icoSmallResId);
                Log.d(TAG, "storeDir: " + storeDir);
                Log.d(TAG, "updateProgress: " + updateProgress);
                Log.d(TAG, "downloadNotificationFlag: " + downloadNotificationFlag);
                Log.d(TAG, "downloadErrorNotificationFlag: " + downloadErrorNotificationFlag);
                Log.d(TAG, "downloadSuccessNotificationFlag: " + downloadSuccessNotificationFlag);
                Log.d(TAG, "isSendBroadcast: " + isSendBroadcast);
                Log.d(TAG, "isForceUpdate: " + isForceUpdate);
                Log.d(TAG, "isAutoInstall: " + isAutoInstall);
            }

            if(isForceUpdate)isSendBroadcast=true;

            Log.v(TAG,"isSendBroadcast:"+isSendBroadcast);
            notifyId = startId;
            buildNotification();
            buildBroadcast();
            downloadApkTask = new DownloadApk(this);
            downloadApkTask.execute(downloadUrl);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 初始化通知
     */
    private void buildNotification(){
        manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        builder = new NotificationCompat.Builder(this);
        builder.setContentTitle(getString(R.string.update_app_model_prepare, appName))
                .setWhen(System.currentTimeMillis())
                .setProgress(100, 1, false)
                .setSmallIcon(icoSmallResId)
                .setLargeIcon(BitmapFactory.decodeResource(
                        getResources(), icoResId))
                .setDefaults(downloadNotificationFlag);

        manager.notify(notifyId, builder.build());
    }

    /**
     * 初始化广播
     */
    private void buildBroadcast(){
        if (!isSendBroadcast){
            return;
        }
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localIntent = new Intent(ACTION);
    }

    /**
     * 发广播
     * @param status
     * @param progress
     */
    private void sendLocalBroadcast(int status, int progress,String filePath){
        if (!isSendBroadcast || localIntent == null){
            return;
        }
        localIntent.putExtra(STATUS, status);
        localIntent.putExtra(PROGRESS, progress);
        localIntent.putExtra(FILEPATH, filePath);
        localBroadcastManager.sendBroadcast(localIntent);
    }

    /**
     * 下载请求
     */
    private  class DownloadApk extends AsyncTask<String, Integer, String> {

        private WeakReference<UpdateService> updateServiceWeakReference;

        public DownloadApk(UpdateService service){
            updateServiceWeakReference = new WeakReference<>(service);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            UpdateService service = updateServiceWeakReference.get();
            if (service != null){
                service.start();
            }
        }

        @Override
        protected String doInBackground(String... params) {

            final String downloadUrl = params[0];

            final File file = new File(AppInfoUtil.getDownloadDir(updateServiceWeakReference.get()),
                    AppInfoUtil.getSaveFileName(downloadUrl));
            if (BuildConfig.DEBUG){
                Log.d(TAG, "download url is " + downloadUrl);
                Log.d(TAG, "download apk cache at " + file.getAbsolutePath());
                Log.d(TAG, "download url length " + params.length);
            }
            File dir = file.getParentFile();
            if (!dir.exists()){
                dir.mkdirs();
            }

            HttpURLConnection httpConnection = null;
            InputStream is = null;
            FileOutputStream fos = null;
            java.net.URL url;
            try {
                url = new URL(downloadUrl);
                httpConnection = (HttpURLConnection) url.openConnection();
                httpConnection.setConnectTimeout(UpdateConfig.TIME_OUT_CONNECT);
                httpConnection.setReadTimeout(UpdateConfig.TIME_OUT_READ);

                if (BuildConfig.DEBUG){
                    Log.d(TAG, "download status code: " + httpConnection.getResponseCode());
                }

                if (httpConnection.getResponseCode() != 200) {
                    return null;
                }

                updateTotalSize = httpConnection.getContentLength();

                if (file.exists()) {
                    if (updateTotalSize == file.length()) {
                        // 下载完成
                        return file.getAbsolutePath();
                    } else {
                        file.delete();
                    }
                }
                //判别内存是否足够
                if (UpdateFileUtil.getFreeDiskSpace() * 1024 < updateTotalSize) {
                    mHandler.sendEmptyMessage(-1);
                    return null;
                }
                file.createNewFile();
                is = httpConnection.getInputStream();
                fos = new FileOutputStream(file, false);
                byte buffer[] = new byte[4096];

                int readSize = 0;
                int currentSize = 0;

                while ((readSize = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, readSize);
                    currentSize += readSize;
                    publishProgress((currentSize * 100 / updateTotalSize));
                }
                // download success
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            } finally {
                if (httpConnection != null) {
                    httpConnection.disconnect();
                }
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return file.getAbsolutePath();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            if (BuildConfig.DEBUG){
                Log.d(TAG, "current progress is " + values[0]);
            }
            UpdateService service = updateServiceWeakReference.get();
            if (service != null){
                service.update(values[0]);
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            UpdateService service = updateServiceWeakReference.get();
            if (service != null){
                if (s != null){
                    File file=new File(s);
                    if (file.exists()&&updateTotalSize == file.length()) {
                        service.success(s);
                    } else {
                        service.error();
                    }

                }else {
                    service.error();
                }
            }
        }
    }

    Handler mHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case -1:
                    Toast.makeText(UpdateService.this,"sd卡空间不足",Toast.LENGTH_LONG).show();
                    break;
            }
        }
    };

    /**
     * 下载开始
     */
    private void start(){
        if(builder!=null){
            builder.setContentTitle(appName);
            builder.setContentText(getString(R.string.update_app_model_prepare, 1));
            manager.notify(notifyId, builder.build());
        }
        sendLocalBroadcast(UPDATE_START_STATUS, 1,"");
        if (updateListener != null){
            updateListener.start();
        }
    }

    /**
     *
     * 下载进度
     * @param progress
     */
    private void update(int progress){
        if (progress - lastProgressNumber > updateProgress){
            lastProgressNumber = progress;
            if(builder!=null){
                builder.setProgress(100, progress, false);
                builder.setContentText(getString(R.string.update_app_model_progress, progress, "%"));
                manager.notify(notifyId, builder.build());
            }
            sendLocalBroadcast(UPDATE_PROGRESS_STATUS, progress,"");
            if (updateListener != null){
                updateListener.update(progress);
            }
        }
    }

    /**
     * 下载成功之后
     * @param path
     */
    private void success(String path) {
        Intent i = InstallApkUtil.installIntent(path);
        if(builder!=null){
            builder.setProgress(0, 0, false);
            builder.setContentText(getString(R.string.update_app_model_success));
            PendingIntent intent = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(intent);
            builder.setDefaults(downloadSuccessNotificationFlag);
            Notification n = builder.build();
            n.contentIntent = intent;
            manager.notify(notifyId, n);
        }
        sendLocalBroadcast(UPDATE_SUCCESS_STATUS, 100,path);
        if (updateListener != null){
            updateListener.success();
        }
        //自动安装
        Log.d(TAG, "isAutoInstall: " + isAutoInstall);
        if(isAutoInstall){
            startActivity(i);
        }
        stopSelf();
    }

    /**
     * 下载失败
     */
    private void error(){
        if(builder!=null){
            Intent i = InstallApkUtil.webLauncher(downloadUrl);
            PendingIntent intent = PendingIntent.getActivity(this, 0, i,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentText(getString(R.string.update_app_model_error));
            builder.setContentIntent(intent);
            builder.setProgress(0, 0, false);
            builder.setDefaults(downloadErrorNotificationFlag);
            Notification n = builder.build();
            n.contentIntent = intent;
            manager.notify(notifyId, n);
        }
        sendLocalBroadcast(UPDATE_ERROR_STATUS, -1,"");
        if (updateListener != null){
            updateListener.error();
        }
        stopSelf();
    }

}
