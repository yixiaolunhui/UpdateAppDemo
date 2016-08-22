package com.renrenfenqi.update.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.renrenfenqi.update.Config;
import com.renrenfenqi.update.service.UpdateService;

import java.io.File;

/**
 * App信息工具类
 * Created by zhouweilong on 16/8/22.
 */

public class AppInfoUtil {

    /**
     * 获取app名字
     * @param context
     * @return
     */
    public static String getAppName(Context context) {
        String appName = "";
        try {
            PackageInfo pi = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            appName = pi.applicationInfo.loadLabel(context.getPackageManager()).toString();
            if (appName == null || appName.length() <= 0) {
                return "";
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return appName;
    }

    /**
     * 获取app  版本号name
     * @param context
     * @return
     */
    public static String getAppVersionName(Context context) {
        String versionName = "";
        try {
            PackageInfo pi = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            versionName = pi.versionName;
            if (versionName == null || versionName.length() <= 0) {
                return "";
            }
        } catch (Exception e) {
            Log.e("VersionInfo", "Exception", e);
        }
        return versionName;
    }

    /**
     * 获取包名
     * @param context
     * @return
     */
    public static String getAppPackageName(Context context) {
        return context.getPackageName();
    }


    /**
     * 获取app的icon
     * @param context
     * @return
     */
    public static Drawable getAppIcon(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo info = pm.getApplicationInfo(context.getPackageName(), 0);
            return info.loadIcon(pm);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();

        }
        return null;
    }

    /**
     * 获取下载的文件名
     * @param downloadUrl
     * @return
     */
    public static String getSaveFileName(String downloadUrl) {
        if (downloadUrl == null || TextUtils.isEmpty(downloadUrl)) {
            return Config.DEFAULT_APP_SAVENAME;
        }
        return downloadUrl.substring(downloadUrl.lastIndexOf("/"));
    }


    /**
     * 获取下载路径
     * @param service
     * @return
     */
    public static File getDownloadDir(UpdateService service){
        File downloadDir = null;
        if (android.os.Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED)) {
            if (service.storeDir != null){
                downloadDir = new File(Environment.getExternalStorageDirectory(), service.storeDir);
            }else {
                downloadDir = new File(service.getExternalCacheDir(), "update");
            }
        } else {
            downloadDir = new File(service.getCacheDir(), "update");
        }
        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }
        return downloadDir;
    }

    public static String getApplicationName(Context context) {
        PackageManager packageManager = null;
        ApplicationInfo applicationInfo = null;
        try {
            packageManager = context.getApplicationContext().getPackageManager();
            applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            applicationInfo = null;
        }
        String applicationName =
                (String) packageManager.getApplicationLabel(applicationInfo);
        return applicationName;
    }
}
