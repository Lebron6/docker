package com.aros.apron.manager;

import android.os.Handler;

import androidx.annotation.NonNull;

import com.aros.apron.base.BaseManager;
import com.aros.apron.entity.Movement;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.PreferenceUtils;
import com.google.gson.Gson;

import dji.rtk.CoordinateSystem;
import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.key.RtkMobileStationKey;
import dji.sdk.keyvalue.value.rtkbasestation.RTKCustomNetworkSetting;
import dji.sdk.keyvalue.value.rtkbasestation.RTKReferenceStationSource;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.KeyManager;
import dji.v5.manager.aircraft.rtk.RTKCenter;
import dji.v5.manager.aircraft.rtk.RTKSystemState;
import dji.v5.manager.aircraft.rtk.RTKSystemStateListener;
import dji.v5.manager.interfaces.IRTKCenter;

public class RTKManager extends BaseManager {

    private RTKManager() {
    }

    private static class RTKHolder {
        private static final RTKManager INSTANCE = new RTKManager();
    }

    public static RTKManager getInstance() {
        return RTKHolder.INSTANCE;
    }

    public void initRTKInfo() {
        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            if (PreferenceUtils.getInstance().getHaveRTK()){
                IRTKCenter irtkCenter = RTKCenter.getInstance();
                if (irtkCenter != null) {
                    irtkCenter.addRTKSystemStateListener(new RTKSystemStateListener() {
                        @Override
                        public void onUpdate(RTKSystemState newValue) {
                            if (newValue!=null){
                                if (newValue.getRTKHealthy()!=Movement.getInstance().isRtkSign()){
                                    Movement.getInstance().setRtkSign(newValue.getRTKHealthy());
                                    LogUtil.log(TAG, "机身RTK状态:" + newValue.getRTKHealthy());
                                }
                            }
                        }
                    });

                    //是否开启RTK模块
//                Boolean isRTKEnable = KeyManager.getInstance().getValue(KeyTools.createKey(RtkMobileStationKey.KeyRTKEnable));
//                if (isRTKEnable!=null&&!isRTKEnable) {
                    enableRtk(true);
//                }
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //RTK精度维持
                            Boolean isRTKMaintainAccuracyEnabled = KeyManager.getInstance().getValue(KeyTools.createKey(RtkMobileStationKey.KeyRTKKeepStatusEnable));
                            if (isRTKMaintainAccuracyEnabled != null && !isRTKMaintainAccuracyEnabled) {
                                setRTKMaintainAccuracyEnabled();
                            }
                        }
                    }, 1000);


                    //设置RTK类型
                  if (PreferenceUtils.getInstance().getRtkType()==1){
                      setRTKReferenceStationSource(RTKReferenceStationSource.CUSTOM_NETWORK_SERVICE);
                  }else if (PreferenceUtils.getInstance().getRtkType()==2){
                      setRTKReferenceStationSource(RTKReferenceStationSource.NTRIP_NETWORK_SERVICE);
                  }else{
                      LogUtil.log(TAG,"RTK类型设置有误");
                  }

                    //判断是否开启RTK服务（待测试）
//                Boolean isRTKCustomNetworkServiceEnable = KeyManager.getInstance().getValue(KeyTools.createKey(RtkBaseStationKey.KeyRTKCustomNetworkServiceEnable));
//                if (isRTKCustomNetworkServiceEnable != null && !isRTKCustomNetworkServiceEnable) {

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            startNetworkRTKService();
                        }
                    }, 2000);
//                }

                }
            }else{
                IRTKCenter irtkCenter = RTKCenter.getInstance();
                if (irtkCenter != null) {
                    irtkCenter.setAircraftRTKModuleEnabled(false, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onSuccess() {
                            LogUtil.log(TAG,"禁用RTK模块success");
                        }

                        @Override
                        public void onFailure(@NonNull IDJIError idjiError) {
                            LogUtil.log(TAG,"禁用RTK模块失败:"+new Gson().toJson(idjiError));
                        }
                    });
                }
            }
        }
    }

    private int startNetWorkRtkTimes;
    private boolean isStartNetworkRTKService;

    //开启自定义网络RTK服务
    private void startNetworkRTKService() {
        if (PreferenceUtils.getInstance().getRtkType()==1) {
            if (Movement.getInstance().isRtkSign()) {
                LogUtil.log(TAG, "RTK服务已开启");
                return;
            }
            RTKCustomNetworkSetting rtkCustomNetworkSetting = new RTKCustomNetworkSetting();
            rtkCustomNetworkSetting.setServerAddress(PreferenceUtils.getInstance().getNTRIP());
            rtkCustomNetworkSetting.setPort(Integer.valueOf(PreferenceUtils.getInstance().getNTRPort()));
            rtkCustomNetworkSetting.setUserName(PreferenceUtils.getInstance().getNTRAccount());
            rtkCustomNetworkSetting.setPassword(PreferenceUtils.getInstance().getNTRPassword());
            rtkCustomNetworkSetting.setMountPoint(PreferenceUtils.getInstance().getNTRMountPoint());
            RTKCenter.getInstance().getCustomRTKManager().setCustomNetworkRTKSettings(rtkCustomNetworkSetting);
            RTKCenter.getInstance().getCustomRTKManager().startNetworkRTKService(CoordinateSystem.WGS84, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    LogUtil.log(TAG, "自定义RTK服务"+"第"+startNetWorkRtkTimes+"次开启成功");
                    isStartNetworkRTKService=true;
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    LogUtil.log(TAG, "自定义RTK服务第" + startNetWorkRtkTimes + "次开启失败:" + new Gson().toJson(error));
                    if (!isStartNetworkRTKService){
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (startNetWorkRtkTimes < 20) {
                                    startNetWorkRtkTimes++;
                                    startNetworkRTKService();
                                }
                            }
                        }, 2000);
                    }
                }
            });
        }else if (PreferenceUtils.getInstance().getRtkType()==2){

            RTKCenter.getInstance().getCMCCRTKManager().startNetworkRTKService(CoordinateSystem.WGS84, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    LogUtil.log(TAG, "CMCCRTK服务"+"第"+startNetWorkRtkTimes+"次开启成功----");
                    isStartNetworkRTKService=true;
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    LogUtil.log(TAG, "CMCCRTK服务第" + startNetWorkRtkTimes + "次开启失败:" + new Gson().toJson(error));

                    if (!isStartNetworkRTKService){
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (startNetWorkRtkTimes < 20) {
                                    startNetWorkRtkTimes++;
                                    startNetworkRTKService();
                                }
                            }
                        }, 2000);
                    }
                }
            });
        }else{
            LogUtil.log(TAG,"RTK类型设置有误");
        }
    }


    private void setRTKReferenceStationSource(RTKReferenceStationSource rtkReferenceStationSource) {
        RTKCenter.getInstance().setRTKReferenceStationSource(rtkReferenceStationSource, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onSuccess() {
                LogUtil.log(TAG, "RTK类型设置成功");
            }

            @Override
            public void onFailure(@NonNull IDJIError error) {
                LogUtil.log(TAG, "RTK类型设置失败:" +new Gson().toJson(error) );
            }
        });
    }

    public void enableRtk(boolean isEnabled) {
        RTKCenter.getInstance().setAircraftRTKModuleEnabled(isEnabled, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onSuccess() {
                LogUtil.log(TAG, "RTK使能"+isEnabled+"成功");
            }

            @Override
            public void onFailure(@NonNull IDJIError error) {
                LogUtil.log(TAG, "RTK使能"+isEnabled+"失败:"+new Gson().toJson(error));
            }
        });
    }

    private void setRTKMaintainAccuracyEnabled() {

        RTKCenter.getInstance().setRTKMaintainAccuracyEnabled(true, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onSuccess() {
                LogUtil.log(TAG, "RTK精度保持启用成功");

            }

            @Override
            public void onFailure(@NonNull IDJIError error) {
                LogUtil.log(TAG, "RTK精度保持启用失败" + error.description());
            }
        });
    }

}
