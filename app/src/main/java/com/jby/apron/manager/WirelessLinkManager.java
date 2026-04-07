package com.jby.apron.manager;

import static dji.sdk.keyvalue.key.KeyTools.createKey;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import com.jby.apron.base.BaseManager;
import com.jby.apron.entity.Movement;
import com.jby.apron.tools.LogUtil;

import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.v5.manager.KeyManager;

public class WirelessLinkManager extends BaseManager {


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

            sendWireless2Server();

            // 始终基于“实际执行时间”来调度
            handler.postDelayed(this, INTERVAL);
        }
    };

    private WirelessLinkManager() {
    }

    public static WirelessLinkManager getInstance() {
        return WirelessLinkHolder.INSTANCE;
    }

    public void initWirelessLink() {
        Boolean isConnect = KeyManager.getInstance().getValue(createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
        handler.postDelayed(runnable, INTERVAL);
        } else {
            LogUtil.log(TAG, "初始化sdr/4g失败" + "flight controller is null");
        }
    }

    private static class WirelessLinkHolder {
        private static final WirelessLinkManager INSTANCE = new WirelessLinkManager();
    }
}