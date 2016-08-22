package com.renrenfenqi.updatedemo;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.renrenfenqi.update.service.UpdateService;

import static android.icu.lang.UCharacter.GraphemeClusterBreak.L;

public class MainActivity extends AppCompatActivity {
    private static final String URL = "http://27.221.81.15/dd.myapp.com/16891/63C4DA61823B87026BBC8C22BBBE212F.apk?mkey=575e443c53406290&f=8b5d&c=0&fsname=com.daimajia.gold_3.2.0_80.apk&p=.apk";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    public void doClick(View view){
        Toast.makeText(MainActivity.this,"doClick",Toast.LENGTH_LONG).show();
        UpdateService.Builder.create(URL)
                .setDownloadSuccessNotificationFlag(Notification.DEFAULT_ALL)
                .setDownloadErrorNotificationFlag(Notification.DEFAULT_ALL)
                .build(this);



        UpdateDialog dialog=new UpdateDialog(this);
        dialog.show();
    }
}