package com.renrenfenqi.update;

import android.app.AlertDialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.renrenfenqi.update.utils.AppInfoUtil;

/**
 * 强制升级弹出框
 * Created by zhouweilong on 16/8/22.
 */

public class UpdateDialog extends AlertDialog {

    private ProgressBar mProgress;
    private TextView mProgressText;
    private TextView mTitleName;

    public UpdateDialog(Context context) {
        super(context);
        init(context);
    }


    /**
     * 显示下载对话框
     */
    private void init(Context context){
        final LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(com.renrenfenqi.update.R.layout.update_down_view, null);
        mProgress = (ProgressBar)v.findViewById(com.renrenfenqi.update.R.id.update_progress);
        mProgressText = (TextView) v.findViewById(com.renrenfenqi.update.R.id.update_progress_text);
        mTitleName = (TextView) v.findViewById(com.renrenfenqi.update.R.id.update_title);
        mProgressText.setText("正在下载"+0+"%");
        setTitleName("新版本下载:"+ AppInfoUtil.getAppName(context));
        setView(v);
        setCanceledOnTouchOutside(false);
        setCancelable(false);

    }

    /**
     * 更新dialog下载进度
     * @param progress
     */
    public void updateProgressText(int progress){
        if(mProgressText!=null&&mProgress!=null){
            mProgressText.setText("正在下载"+progress+"%");
            mProgress.setProgress(progress);
        }
    }

    public void setTitleName(String titleName){
        if(mTitleName!=null&& !TextUtils.isEmpty(titleName)){
            mTitleName.setText(titleName);
        }
    }
}
