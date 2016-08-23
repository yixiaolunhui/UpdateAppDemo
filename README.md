# A library of upgrade services

#preview

![image](https://github.com/dalong982242260/AndroidRuler/blob/master/img/update.gif)

#use

         UpdateManager.create(this)
                        .setDownloadUrl(URL)
                        .setDownloadSuccessNotificationFlag(Notification.DEFAULT_ALL)
                        .setDownloadErrorNotificationFlag(Notification.DEFAULT_ALL)
                        .setForceUpdate(true)
                        .setIsSendBroadcast(true)
                        .build();



