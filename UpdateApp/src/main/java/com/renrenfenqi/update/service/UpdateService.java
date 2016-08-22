package com.renrenfenqi.update.service;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.renrenfenqi.update.BuildConfig;
import com.renrenfenqi.update.Config;
import com.renrenfenqi.update.R;
import com.renrenfenqi.update.listener.UpdateListener;
import com.renrenfenqi.update.utils.AppInfoUtil;
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
    public static final String ACTION = "me.shenfan.UPDATE_APP";
    public static final String STATUS = "status";
    public static final String PROGRESS = "progress";

    //params参数
    private static final String URL = "downloadUrl";
    private static final String ICO_RES_ID = "icoResId";
    private static final String ICO_SMALL_RES_ID = "icoSmallResId";
    private static final String UPDATE_PROGRESS = "updateProgress";
    private static final String STORE_DIR = "storeDir";
    private static final String DOWNLOAD_NOTIFICATION_FLAG = "downloadNotificationFlag";
    private static final String DOWNLOAD_SUCCESS_NOTIFICATION_FLAG = "downloadSuccessNotificationFlag";
    private static final String DOWNLOAD_ERROR_NOTIFICATION_FLAG = "downloadErrorNotificationFlag";
    private static final String IS_SEND_BROADCAST = "isSendBroadcast";
    private static final String IS_SHOW_DIALOG = "isShowDialog";
    private static final String IS_CAN_DISMISS_DIALOG = "isCanDismissDialog";


    private static final int MSG_START_DOWN=100;
    private static final int MSG_UPDATE_PROGRESS=101;


    //下载大小通知频率
    public static final int UPDATE_NUMBER_SIZE = 1;
    public static final int DEFAULT_RES_ID = -1;
    //更新状态
    public static final int UPDATE_PROGRESS_STATUS = 0;
    public static final int UPDATE_ERROR_STATUS = -1;
    public static final int UPDATE_SUCCESS_STATUS = 1;

    private String downloadUrl;
    private int icoResId;
    private int icoSmallResId;
    private int updateProgress;
    private int downloadNotificationFlag;
    private int downloadSuccessNotificationFlag;
    private int downloadErrorNotificationFlag;
    private boolean isSendBroadcast;
    private boolean isShowDialog;
    private boolean isCanDismissDialog;

    private UpdateListener updateListener;//回调接口

    private LocalBinder localBinder = new LocalBinder();

    public String storeDir;//默认存放路径

    private String appName;//app名字

    private boolean startDownload;//开始下载
    private int lastProgressNumber;
    private NotificationCompat.Builder builder;
    private NotificationManager manager;
    private int notifyId;
    private LocalBroadcastManager localBroadcastManager;
    private Intent localIntent;
    private DownloadApk downloadApkTask;

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
        appName = AppInfoUtil.getApplicationName(this);
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
            isSendBroadcast = intent.getBooleanExtra(IS_SEND_BROADCAST, false);


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
            }

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
    private void sendLocalBroadcast(int status, int progress){
        if (!isSendBroadcast || localIntent == null){
            return;
        }
        localIntent.putExtra(STATUS, status);
        localIntent.putExtra(PROGRESS, progress);
        localBroadcastManager.sendBroadcast(localIntent);
    }

    /**
     * 下载请求
     */
    private static class DownloadApk extends AsyncTask<String, Integer, String> {

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
            int updateTotalSize = 0;
            java.net.URL url;
            try {
                url = new URL(downloadUrl);
                httpConnection = (HttpURLConnection) url.openConnection();
                httpConnection.setConnectTimeout(Config.TIME_OUT_CONNECT);
                httpConnection.setReadTimeout(Config.TIME_OUT_READ);

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
                    service.success(s);
                }else {
                    service.error();
                }
            }
        }
    }


    /**
     * 下载开始
     */
    private void start(){
        builder.setContentTitle(appName);
        builder.setContentText(getString(R.string.update_app_model_prepare, 1));
        manager.notify(notifyId, builder.build());
        sendLocalBroadcast(UPDATE_PROGRESS_STATUS, 1);
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
            builder.setProgress(100, progress, false);
            builder.setContentText(getString(R.string.update_app_model_progress, progress, "%"));
            manager.notify(notifyId, builder.build());
            sendLocalBroadcast(UPDATE_PROGRESS_STATUS, progress);
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
        builder.setProgress(0, 0, false);
        builder.setContentText(getString(R.string.update_app_model_success));
        Intent i = InstallApkUtil.installIntent(path);
        PendingIntent intent = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(intent);
        builder.setDefaults(downloadSuccessNotificationFlag);
        Notification n = builder.build();
        n.contentIntent = intent;
        manager.notify(notifyId, n);
        sendLocalBroadcast(UPDATE_SUCCESS_STATUS, 100);
        if (updateListener != null){
            updateListener.success();
        }
        startActivity(i);
        stopSelf();
    }

    /**
     * 下载失败
     */
    private void error(){
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
        sendLocalBroadcast(UPDATE_ERROR_STATUS, -1);
        if (updateListener != null){
            updateListener.error();
        }
        stopSelf();
    }


    /**
     * builder类 辅助使用更新服务
     */
    public static class Builder{

        private String downloadUrl;
        private int icoResId = DEFAULT_RES_ID;             //default app ico
        private int icoSmallResId = DEFAULT_RES_ID;
        private int updateProgress = UPDATE_NUMBER_SIZE;   //update notification progress when it add number
        private String storeDir;                           //default sdcard/Android/package/update
        private int downloadNotificationFlag;
        private int downloadSuccessNotificationFlag;
        private int downloadErrorNotificationFlag;
        private boolean isSendBroadcast;

        protected Builder(String downloadUrl){
            this.downloadUrl = downloadUrl;
        }

        public static Builder create(String downloadUrl){
            if (downloadUrl == null) {
                throw new NullPointerException("downloadUrl == null");
            }
            return new Builder(downloadUrl);
        }


        public String getDownloadUrl() {
            return downloadUrl;
        }

        public int getIcoResId() {
            return icoResId;
        }

        public Builder setIcoResId(int icoResId) {
            this.icoResId = icoResId;
            return this;
        }

        public int getIcoSmallResId() {
            return icoSmallResId;
        }

        public Builder setIcoSmallResId(int icoSmallResId) {
            this.icoSmallResId = icoSmallResId;
            return this;
        }

        public int getUpdateProgress() {
            return updateProgress;
        }

        public Builder setUpdateProgress(int updateProgress) {
            if (updateProgress < 1){
                throw new IllegalArgumentException("updateProgress < 1");
            }
            this.updateProgress = updateProgress;
            return this;
        }

        public String getStoreDir() {
            return storeDir;
        }

        public Builder setStoreDir(String storeDir) {
            this.storeDir = storeDir;
            return this;
        }

        public int getDownloadNotificationFlag() {
            return downloadNotificationFlag;
        }

        public Builder setDownloadNotificationFlag(int downloadNotificationFlag) {
            this.downloadNotificationFlag = downloadNotificationFlag;
            return this;
        }

        public int getDownloadSuccessNotificationFlag() {
            return downloadSuccessNotificationFlag;
        }

        public Builder setDownloadSuccessNotificationFlag(int downloadSuccessNotificationFlag) {
            this.downloadSuccessNotificationFlag = downloadSuccessNotificationFlag;
            return this;
        }

        public int getDownloadErrorNotificationFlag() {
            return downloadErrorNotificationFlag;
        }

        public Builder setDownloadErrorNotificationFlag(int downloadErrorNotificationFlag) {
            this.downloadErrorNotificationFlag = downloadErrorNotificationFlag;
            return this;
        }

        public boolean isSendBroadcast() {
            return isSendBroadcast;
        }

        public Builder setIsSendBroadcast(boolean isSendBroadcast) {
            this.isSendBroadcast = isSendBroadcast;
            return this;
        }

        public Builder build(Context context){
            if (context == null){
                throw new NullPointerException("context == null");
            }
            Intent intent = new Intent();
            intent.setClass(context, UpdateService.class);
            intent.putExtra(URL, downloadUrl);

            if (icoResId == DEFAULT_RES_ID){
                icoResId = getIcon(context);
            }

            if (icoSmallResId == DEFAULT_RES_ID){
                icoSmallResId = icoResId;
            }
            intent.putExtra(ICO_RES_ID, icoResId);
            intent.putExtra(STORE_DIR, storeDir);
            intent.putExtra(ICO_SMALL_RES_ID, icoSmallResId);
            intent.putExtra(UPDATE_PROGRESS, updateProgress);
            intent.putExtra(DOWNLOAD_NOTIFICATION_FLAG, downloadNotificationFlag);
            intent.putExtra(DOWNLOAD_SUCCESS_NOTIFICATION_FLAG, downloadSuccessNotificationFlag);
            intent.putExtra(DOWNLOAD_ERROR_NOTIFICATION_FLAG, downloadErrorNotificationFlag);
            intent.putExtra(IS_SEND_BROADCAST, isSendBroadcast);
            context.startService(intent);

            return this;
        }

        private int getIcon(Context context){

            final PackageManager packageManager = context.getPackageManager();
            ApplicationInfo appInfo = null;
            try {
                appInfo = packageManager.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            if (appInfo != null){
                return appInfo.icon;
            }
            return 0;
        }
    }
}
