package com.aros.apron.manager;


import androidx.annotation.NonNull;

import com.aros.apron.base.BaseManager;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.PreferenceUtils;
import com.google.gson.Gson;

import dji.sdk.keyvalue.key.FlightAssistantKey;
import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.flightassistant.AuxiliaryLightMode;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.KeyManager;


public class LEDsSettingsManager extends BaseManager {


    private LEDsSettingsManager() {
    }

    private static class LEDsSettingsHolder {
        private static final LEDsSettingsManager INSTANCE = new LEDsSettingsManager();
    }

    public static LEDsSettingsManager getInstance() {
        return LEDsSettingsHolder.INSTANCE;
    }

    public void initLEDsInfo() {

        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
//            LEDsSettings leDsSettings = new LEDsSettings();
//            leDsSettings.setNavigationLEDsOn(PreferenceUtils.getInstance().getNavigationLEDsOn());
//            leDsSettings.setFrontLEDsOn(true);
//            leDsSettings.setRearLEDsOn(true);
//            leDsSettings.setStatusIndicatorLEDsOn(true);
            boolean navigationLEDsOn = PreferenceUtils.getInstance().getNavigationLEDsOn();
            KeyManager.getInstance().setValue(KeyTools.createKey(FlightAssistantKey.KeyBottomAuxiliaryLightMode), navigationLEDsOn ? AuxiliaryLightMode.ON : AuxiliaryLightMode.OFF, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    LogUtil.log(TAG, "设置夜航灯使能:" + PreferenceUtils.getInstance().getNavigationLEDsOn());
                }

                @Override
                public void onFailure(@NonNull IDJIError idjiError) {
                    LogUtil.log(TAG, "设置夜航灯使能失败:" + new Gson().toJson(idjiError));
                }
            });
//            KeyManager.getInstance().setValue(KeyTools.createKey(FlightControllerKey.KeyLEDsSettings), leDsSettings, new CommonCallbacks.CompletionCallback() {
//                @Override
//                public void onSuccess() {
//                    LogUtil.log(TAG, "设置夜航灯使能:" + PreferenceUtils.getInstance().getNavigationLEDsOn());
//                }
//
//                @Override
//                public void onFailure(@NonNull IDJIError idjiError) {
//                    LogUtil.log(TAG, "设置夜航灯使能失败:" + new Gson().toJson(idjiError));
//                }
//            });
        } else {
            LogUtil.log(TAG, "设置夜航灯失败");
        }
    }
}
