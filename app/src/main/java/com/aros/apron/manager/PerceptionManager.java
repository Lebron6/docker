package com.aros.apron.manager;//package com.aros.apron.manager;

import static dji.sdk.keyvalue.key.KeyTools.createKey;

import android.os.Handler;

import androidx.annotation.NonNull;

import com.aros.apron.base.BaseManager;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.PreferenceUtils;
import com.google.gson.Gson;

import org.eclipse.paho.android.service.MqttAndroidClient;

import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.KeyManager;
import dji.v5.manager.aircraft.perception.data.ObstacleAvoidanceType;
import dji.v5.manager.interfaces.IPerceptionManager;

public class PerceptionManager extends BaseManager {

    MqttAndroidClient client;

    private PerceptionManager() {
    }

    private static class PerceptionManagerHolder {
        private static final PerceptionManager INSTANCE = new PerceptionManager();
    }

    public static PerceptionManager getInstance() {
        return PerceptionManagerHolder.INSTANCE;
    }

    private int closePerceptionTimes;
    private boolean closePerceptionSuccess;

    public void setPerceptionEnable(boolean perceptionEnable) {
        if (PreferenceUtils.getInstance().getCloseObsEnable() && perceptionEnable) {
            LogUtil.log(TAG, "全局避障关闭,不开启避障");
            return;
        }
        Boolean isConnect = KeyManager.getInstance().getValue(createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            IPerceptionManager perceptionManager = dji.v5.manager.aircraft.perception.PerceptionManager.getInstance();
            perceptionManager.setObstacleAvoidanceType(perceptionEnable ? ObstacleAvoidanceType.BRAKE : ObstacleAvoidanceType.CLOSE, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    if (perceptionEnable) {
                        LogUtil.log(TAG, "避障已开启");
                    } else {
                        closePerceptionSuccess = true;
                        LogUtil.log(TAG, "避障已关闭");
                    }
                }

                @Override
                public void onFailure(@NonNull IDJIError idjiError) {
                    if (perceptionEnable) {
                        LogUtil.log(TAG, "避障开启失败:" + new Gson().toJson(idjiError));
                    } else {
                        LogUtil.log(TAG, "第" + closePerceptionTimes + "次关闭避障失败:" + new Gson().toJson(idjiError));
                        if (!closePerceptionSuccess) {
                            if (closePerceptionTimes <= 5) {
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        closePerceptionTimes++;
                                        setPerceptionEnable(false);
                                    }
                                }, 2000);
                            } else {
                                LogUtil.log(TAG, "避障关闭" + closePerceptionTimes + "次失败");
                            }
                        }
                    }
                }
            });
        }

    }


}
