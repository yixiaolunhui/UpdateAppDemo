# A library of upgrade services

#preview

###mandatory upgrade

![image](https://github.com/dalong982242260/UpdateAppDemo/blob/master/img/update.gif?raw=true)

###general upgrade

![image](https://github.com/dalong982242260/UpdateAppDemo/blob/master/img/update2.gif?raw=true)

#use

        UpdateManager.create(this)
                .setDownloadUrl(URL)//设置下载apk网络连接
                .setStoreDir("dalong")//设置保存路径
                .setDownloadSuccessNotificationFlag(Notification.DEFAULT_ALL)
                .setDownloadErrorNotificationFlag(Notification.DEFAULT_ALL)
                .setForceUpdate(true)//是否是强制
                .setAutoInstall(false)//是否自动安装 设置false会弹出安装对话框 但是如果是强制的 设置为true也会弹出对话框
                .build();



