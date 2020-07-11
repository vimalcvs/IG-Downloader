package com.abc.instadownpro.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Build;
import android.view.Display;
import android.view.WindowManager;
import com.abc.instadownpro.base.MainApplication;

import java.io.File;
import java.io.FileFilter;

public final class DeviceUtil {

    private static Point screenSize;
    private static int sScreenHeight;
    private static int sScreenWidth;


    public static final int DEVICEINFO_UNKNOWN = 5;

    public static int getNumberOfCPUCores() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) {
            // Gingerbread doesn't support giving a single application access to both cores, but a
            // handful of devices (Atrix 4G and Droid X2 for example) were released with a dual-core
            // chipset and Gingerbread; that can let an app in the background run without impacting
            // the foreground application. But for our purposes, it makes them single core.
            return 1;  //上面的意思就是2.3以前不支持多核,有些特殊的设备有双核...不考虑,就当单核!!
        }
        int cores;
        try {
            cores = new File("/sys/devices/system/cpu/").listFiles(CPU_FILTER).length;
        } catch (SecurityException e) {
            cores = DEVICEINFO_UNKNOWN;   //这个常量得自己约定
        } catch (NullPointerException e) {
            cores = DEVICEINFO_UNKNOWN;
        }
        return cores;
    }

    private static final FileFilter CPU_FILTER = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            String path = pathname.getName();
            //regex is slow, so checking char by char.
            if (path.startsWith("cpu")) {
                for (int i = 3; i < path.length(); i++) {
                    if (path.charAt(i) < '0' || path.charAt(i) > '9') {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
    };

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    public static Point getScreenSize() {
        if (null == screenSize) {
            Context context = MainApplication.getInstance()
                    .getApplicationContext();
            WindowManager wm = (WindowManager) context
                    .getSystemService(Context.WINDOW_SERVICE);
            screenSize = new Point();
            if (wm != null) {
                Display display = wm.getDefaultDisplay();
                if (display != null) {
                    if (Build.VERSION.SDK_INT > 12) {
                        display.getSize(screenSize);
                    } else {
                        screenSize.x = display.getWidth();
                        screenSize.y = display.getHeight();
                    }
                }
            }

        }

        return screenSize;
    }

    public static int getScreenWidth() {
        if (sScreenWidth == 0) {
            sScreenWidth = getScreenSize().x;
        }
        return sScreenWidth;
    }

    public static int getScreenHeight() {
        if (sScreenHeight == 0) {
            sScreenHeight = getScreenSize().y;
        }
        return sScreenHeight;
    }



    public static long getInstalledTime() {
        try {
            final Context context = MainApplication.getInstance().getApplicationContext();
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            long firstInstallTime = packageInfo.firstInstallTime;
            return firstInstallTime;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return System.currentTimeMillis();
    }

    public static boolean isBeyondTime(long timeDelayed) {
        return (System.currentTimeMillis() - getInstalledTime() >= timeDelayed);
    }
}
