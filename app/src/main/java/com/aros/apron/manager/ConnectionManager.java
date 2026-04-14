package com.aros.apron.manager;


import static dji.sdk.keyvalue.key.KeyTools.createKey;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.aros.apron.base.BaseManager;
import com.aros.apron.entity.ApronExecutionStatus;
import com.aros.apron.entity.MessageDown;
import com.aros.apron.entity.Movement;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.PreferenceUtils;

import org.eclipse.paho.android.service.MqttAndroidClient;

import dji.sdk.keyvalue.key.CameraKey;
import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.common.CameraLensType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.manager.KeyManager;

public class ConnectionManager extends BaseManager {


    private ConnectionManager() {
    }

    private static class ConnectionHolder {
        private static final ConnectionManager INSTANCE = new ConnectionManager();
    }

    public static ConnectionManager getInstance() {
        return ConnectionHolder.INSTANCE;
    }


    public void initConnection() {
        KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyConnection),
                this, new CommonCallbacks.KeyListener<Boolean>() {
            @Override
            public void onValueChange(@Nullable Boolean oldValue, @Nullable Boolean newValue) {
                if (newValue != null&&newValue) {
                    //飞机SN号
                    KeyManager.getInstance().listen(createKey(FlightControllerKey.KeySerialNumber), this, new CommonCallbacks.KeyListener<String>() {
                        @Override
                        public void onValueChange(@Nullable String s, @Nullable String t1) {
                            if (t1 != null) {
                                PowerOnManager.getInstance().sendPowerOnMsg2Server(t1);
                            }
                        }
                    });
                }
            }
        });
    }
}
