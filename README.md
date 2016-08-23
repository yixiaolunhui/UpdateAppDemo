# A library of upgrade services

#preview

###mandatory upgrade

![image](https://github.com/dalong982242260/UpdateAppDemo/blob/master/img/update.gif?raw=true)

###general upgrade

![image](https://github.com/dalong982242260/UpdateAppDemo/blob/master/img/update2.gif?raw=true)

#use

         UpdateManager.create(this)
                        .setDownloadUrl(URL)
                        .setDownloadSuccessNotificationFlag(Notification.DEFAULT_ALL)
                        .setDownloadErrorNotificationFlag(Notification.DEFAULT_ALL)
                        .setForceUpdate(true)
                        .setIsSendBroadcast(true)
                        .build();



