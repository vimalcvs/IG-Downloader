package com.abc.instadownpro.base;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import androidx.multidex.MultiDexApplication;

import com.abc.instadownpro.R;
import com.liulishuo.filedownloader.FileDownloader;
import com.onesignal.OneSignal;

public class MainApplication extends MultiDexApplication {


    private static Context sContext;
    private static MainApplication sApplication;

    @Override
    public void onCreate() {
        super.onCreate();
        sApplication = this;
        FileDownloader.setupOnApplicationOnCreate(this);
        OneSignal.startInit(this)
                .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
                .autoPromptLocation(true)

                .init();


        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(new NotificationChannel("download-notification", getString(R.string.app_name), NotificationManager.IMPORTANCE_HIGH));
        }
    }



    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        sContext = base;
    }

    public static MainApplication getInstance() {
        return sApplication;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }


}
