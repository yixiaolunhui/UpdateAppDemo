package com.renrenfenqi.update.utils;

import android.content.Intent;
import android.net.Uri;

import java.io.File;

/**
 * 安装apk工具类
 * Created by zhouweilong on 16/8/22.
 */

public class InstallApkUtil {

    /**
     * 安装intent
     * @param path
     * @return
     */
    public static Intent installIntent(String path){
        Uri uri = Uri.fromFile(new File(path));
        Intent installIntent = new Intent(Intent.ACTION_VIEW);
        installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        installIntent.setDataAndType(uri, "application/vnd.android.package-archive");
        return installIntent;
    }

    /**
     * 跳转浏览器下载intent
     * @param downloadUrl
     * @return
     */
    public static Intent webLauncher(String downloadUrl){
        Uri download = Uri.parse(downloadUrl);
        Intent intent = new Intent(Intent.ACTION_VIEW, download);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }
}
