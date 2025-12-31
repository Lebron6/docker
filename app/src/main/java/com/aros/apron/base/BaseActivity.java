package com.aros.apron.base;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.aros.apron.tools.AppManager;
import org.greenrobot.eventbus.EventBus;


public abstract class BaseActivity extends AppCompatActivity {
    /**
     * activity堆栈管理
     */
    protected AppManager appManager = AppManager.getAppManager();


    protected String TAG;
    protected boolean useEventBus = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loggerSimpleName();
        if (useEventBus()) {
            EventBus.getDefault().register(this);
        }
        appManager.addActivity(this);
    }



    public abstract boolean useEventBus();

    public void loggerSimpleName() {
        TAG = getClass().getSimpleName();
//        Log.e("Aros","当前界面 ："+ TAG);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 从栈中移除activity
        appManager.finishActivity(this);
        if (useEventBus == true) {
            EventBus.getDefault().unregister(this);
        }

    }

}
