package com.aros.apron.manager;

import android.os.Handler;

import androidx.annotation.NonNull;

import com.aros.apron.base.BaseManager;
import com.aros.apron.entity.Movement;
import com.aros.apron.tools.LogUtil;
import com.google.gson.Gson;

import java.util.List;

import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.airlink.WlmDongleInfo;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.KeyManager;
import dji.v5.manager.aircraft.lte.LTEAuthenticationInfo;
import dji.v5.manager.aircraft.lte.LTEAuthenticationInfoListener;
import dji.v5.manager.aircraft.lte.LTEDongleInfoListener;
import dji.v5.manager.aircraft.lte.LTELinkInfo;
import dji.v5.manager.aircraft.lte.LTELinkInfoListener;
import dji.v5.manager.aircraft.lte.LTELinkType;
import dji.v5.manager.aircraft.lte.LTEManager;


public class MLTEManager extends BaseManager {


    private MLTEManager() {
    }

    private static class LTEHolder {
        private static final MLTEManager INSTANCE = new MLTEManager();
    }

    public static MLTEManager getInstance() {
        return LTEHolder.INSTANCE;
    }

    public void initLTEManager() {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.
                KeyConnection));
        if (isConnect != null && isConnect) {

            LTEManager.getInstance().addLTELinkInfoListener(new LTELinkInfoListener() {
                @Override
                public void onLTELinkInfoUpdate(LTELinkInfo info) {
                    if (info != null&&info.getLTELinkType()!=null) {
                        Movement.getInstance().setLTELinkType(info.getLTELinkType().name());
                        LogUtil.log(TAG, "当前图传类型:" + info.getLTELinkType().name());
                    }
                }
            });

//            LTEManager.getInstance().addLTEDongleInfoListener(new LTEDongleInfoListener() {
//                @Override
//                public void onLTEAircraftDongleInfoUpdate(@NonNull List<WlmDongleInfo> aircraftDongleInfos) {
//                    if (!aircraftDongleInfos.isEmpty()) {
//                        LogUtil.log(TAG, "aircraft DongleInfo:" + new Gson().toJson(aircraftDongleInfos.get(0)));
//                    }
//
//                }
//
//                @Override
//                public void onLTERemoteControllerDongleInfoUpdate(@NonNull List<WlmDongleInfo> remoteControllerDongleInfos) {
//                    LogUtil.log(TAG, "remote DongleInfo:" + new Gson().toJson(remoteControllerDongleInfos.get(0)));
//
//                }
//            });
//            LTEManager.getInstance().addLTEAuthenticationInfoListener(new LTEAuthenticationInfoListener() {
//                @Override
//                public void onLTEAuthenticationInfoUpdate(LTEAuthenticationInfo info) {
//                    if (info != null) {
//                        LogUtil.log(TAG, "LTEAuthenticationInfo:" + new Gson().toJson(info));
//                    }
//                }
//            });

        }
    }

    private int setLTEEnhancedTransmissionTypeTimes;
    private boolean isLTEEnhancedTransmissionLte;

    public void setLTEEnhancedTransmissionType() {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.
                KeyConnection));
        if (isConnect != null && isConnect) {
            LTEManager.getInstance().setLTEEnhancedTransmissionType(LTELinkType.OCU_SYNC_LTE, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    isLTEEnhancedTransmissionLte=true;
                    LogUtil.log(TAG, "设置增强图传成功");
                }

                @Override
                public void onFailure(@NonNull IDJIError idjiError) {
                    LogUtil.log(TAG, "设置增强图传第" + setLTEEnhancedTransmissionTypeTimes + "次开启失败:" + new Gson().toJson(idjiError));
                    if (!isLTEEnhancedTransmissionLte){
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (setLTEEnhancedTransmissionTypeTimes < 20) {
                                    setLTEEnhancedTransmissionTypeTimes++;
                                    setLTEEnhancedTransmissionType();
                                }
                            }
                        }, 3000);
                    }
                }
            });
        }

    }

}
