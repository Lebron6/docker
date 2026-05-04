package com.aros.apron.manager;

import static dji.sdk.keyvalue.key.KeyTools.createKey;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import com.aros.apron.base.BaseManager;
import com.aros.apron.entity.Movement;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.PreferenceUtils;
import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.v5.manager.KeyManager;

public class FlightTaskProgressManager extends BaseManager {


    private static final long INTERVAL = 5000L;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private long lastExecuteTime = 0L;
    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            long now = SystemClock.elapsedRealtime();
            if (now - lastExecuteTime < INTERVAL) {
                handler.postDelayed(this, INTERVAL - (now - lastExecuteTime));
                return;
            }
            lastExecuteTime = now;
            //如果是一键起飞航线或之后的指点飞行，这个就不用发，除此之外航线以及指点飞行都要发
            if (PreferenceUtils.getInstance().getMissionType()==0&&(Movement.getInstance().isPlaneWing()
                    || Movement.getInstance().isMotorsOn()
                    || Movement.getInstance().isMissionFinish())) {
                sendFlightTaskProgress2Server();
            }
            if (Movement.getInstance().isMissionFinish()) {
                handler.removeCallbacks(this);
                return;
            }
            // 始终基于“实际执行时间”来调度
            handler.postDelayed(this, INTERVAL);
        }
    };

    private FlightTaskProgressManager() {
    }

    public static FlightTaskProgressManager getInstance() {
        return FlightTaskHolder.INSTANCE;
    }

    public void initFlightTaskProgress() {
        Boolean isConnect = KeyManager.getInstance().getValue(createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
        handler.postDelayed(runnable, INTERVAL);
        } else {
            LogUtil.log(TAG, "初始化航线任务进度失败" + "flight controller is null");
        }
    }

    private static class FlightTaskHolder {
        private static final FlightTaskProgressManager INSTANCE = new FlightTaskProgressManager();
    }
}