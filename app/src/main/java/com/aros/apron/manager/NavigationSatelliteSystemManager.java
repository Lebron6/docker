package com.aros.apron.manager;

import static com.aros.apron.tools.Utils.getIDJIErrorMsg;
import static dji.sdk.keyvalue.key.KeyTools.createKey;

import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aros.apron.base.BaseManager;
import com.aros.apron.entity.Movement;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.PreferenceUtils;

import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.flightcontroller.NavigationSatelliteSystem;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.KeyManager;


public class NavigationSatelliteSystemManager extends BaseManager {


    private NavigationSatelliteSystemManager() {
    }

    private static class NavigationSatelliteHolder {
        private static final NavigationSatelliteSystemManager INSTANCE = new NavigationSatelliteSystemManager();
    }

    public static NavigationSatelliteSystemManager getInstance() {
        return NavigationSatelliteHolder.INSTANCE;
    }

    public void initNavigationSatelliteSystem() {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.
                KeyConnection));
        if (isConnect != null && isConnect) {
            KeyManager.getInstance().listen(createKey(FlightControllerKey.KeyNavigationSatelliteSystemSource),
                    this, new CommonCallbacks.KeyListener<NavigationSatelliteSystem>() {
                        @Override
                        public void onValueChange(@Nullable NavigationSatelliteSystem navigationSatelliteSystem,
                                                  @Nullable NavigationSatelliteSystem t1) {
                            if (t1 != null) {
                                LogUtil.log(TAG, "监听卫星导航系统:" + t1.name());
                                Movement.getInstance().setNavigationSatelliteSystem(t1.value());
                            }
                        }
                    });
        }
    }

    private int setLTEEnhancedTransmissionTypeTimes;
    private boolean isLTEEnhancedTransmissionLte;

    public void setNavigationSatelliteSystem() {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.
                KeyConnection));
        if (isConnect != null && isConnect) {

            KeyManager.getInstance().setValue(KeyTools.createKey(FlightControllerKey.KeyNavigationSatelliteSystemSource),
                    PreferenceUtils.getInstance().getSatelliteSystem() == 1 ?
                            NavigationSatelliteSystem.GPS_GLONASS : NavigationSatelliteSystem.BEIDOU,
                    new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onSuccess() {
                            isLTEEnhancedTransmissionLte = true;
                            LogUtil.log(TAG, "设置卫星系统" + setLTEEnhancedTransmissionTypeTimes + "次成功");
                        }

                        @Override
                        public void onFailure(@NonNull IDJIError error) {
                            LogUtil.log(TAG, "设置卫星系统第" + setLTEEnhancedTransmissionTypeTimes + "次失败:" + getIDJIErrorMsg(error));
                            if (!isLTEEnhancedTransmissionLte) {
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (setLTEEnhancedTransmissionTypeTimes < 20) {
                                            setLTEEnhancedTransmissionTypeTimes++;
                                            setNavigationSatelliteSystem();
                                        }
                                    }
                                }, 3000);
                            }
                        }
                    });
        }

    }

}
