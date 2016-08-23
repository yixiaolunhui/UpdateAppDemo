package com.renrenfenqi.update;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Created by zhouweilong on 16/8/22.
 */

public class UpdateDialog extends AlertDialog {

    private ProgressBar mProgress;
    private TextView mProgressText;

    public UpdateDialog(Context context) {
        super(context);
        init(context);
    }


    /**
     * 显示下载对话框
     */
    private void init(Context context){
        setTitle("正在下载新版本");
        final LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(com.renrenfenqi.update.R.layout.update_down_view, null);
        mProgress = (ProgressBar)v.findViewById(com.renrenfenqi.update.R.id.update_progress);
        mProgressText = (TextView) v.findViewById(com.renrenfenqi.update.R.id.update_progress_text);
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
}
