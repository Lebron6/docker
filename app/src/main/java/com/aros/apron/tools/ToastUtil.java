package com.aros.apron.tools;

import android.widget.Toast;

import com.aros.apron.app.ApronApp;


public class ToastUtil {
    private static String oldMsg;
    protected static Toast toast = null;
    private static long oneTime = 0L;
    private static long twoTime = 0L;

    public ToastUtil() {
    }

    public static void showToast(String s) {
        if(toast == null) {
            toast = Toast.makeText(ApronApp.Companion.getApplication(), s, 0);
            toast.show();
            oneTime = System.currentTimeMillis();
        } else {
            twoTime = System.currentTimeMillis();
            if(s.equals(oldMsg)) {
                if(twoTime - oneTime > 0L) {
                    toast.show();
                }
            } else {
                oldMsg = s;
                toast.setText(s);
                toast.show();
            }
        }

        oneTime = twoTime;
    }
}
