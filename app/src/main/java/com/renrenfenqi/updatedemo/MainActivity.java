package com.renrenfenqi.updatedemo;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.renrenfenqi.update.UpdateDialog;
import com.renrenfenqi.update.UpdateManager;
import com.renrenfenqi.update.listener.UpdateListener;
import com.renrenfenqi.update.service.UpdateService;

import static android.icu.lang.UCharacter.GraphemeClusterBreak.L;

public class MainActivity extends AppCompatActivity {
    private static final String URL = "http://27.221.81.15/dd.myapp.com/16891/63C4DA61823B87026BBC8C22BBBE212F.apk";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    public void doClick(View view){
        UpdateManager.create(this)
                .setDownloadUrl(URL)
                .setDownloadSuccessNotificationFlag(Notification.DEFAULT_ALL)
                .setDownloadErrorNotificationFlag(Notification.DEFAULT_ALL)
                .setForceUpdate(true)
                .setIsSendBroadcast(true)
                .build();
    }

    protected void onDestroy() {
        super.onDestroy();
        UpdateManager.create(this).unregisterReceiver();
    }
}
