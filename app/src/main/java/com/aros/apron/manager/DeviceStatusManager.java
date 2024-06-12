package com.aros.apron.manager;

import android.text.TextUtils;
import com.aros.apron.base.BaseManager;
import com.aros.apron.entity.Movement;
import org.eclipse.paho.android.service.MqttAndroidClient;
import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.v5.manager.KeyManager;
import dji.v5.manager.diagnostic.DJIDeviceStatus;
import dji.v5.manager.diagnostic.DJIDeviceStatusChangeListener;
import dji.v5.manager.interfaces.IDeviceStatusManager;

public class DeviceStatusManager extends BaseManager {

    MqttAndroidClient client;
    private IDeviceStatusManager iDeviceStatusManager;


    private DeviceStatusManager() {
    }

    private static class DeviceStatusHolder {
        private static final DeviceStatusManager INSTANCE = new DeviceStatusManager();
    }

    public static DeviceStatusManager getInstance() {
        return DeviceStatusHolder.INSTANCE;
    }

    public void initDeviceStatus(MqttAndroidClient client) {
        this.client = client;

        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            iDeviceStatusManager = dji.v5.manager.diagnostic.DeviceStatusManager.getInstance();
            iDeviceStatusManager.addDJIDeviceStatusChangeListener(djiDeviceStatusChangeListener);
        }
    }

    DJIDeviceStatusChangeListener djiDeviceStatusChangeListener=new DJIDeviceStatusChangeListener() {
        @Override
        public void onDeviceStatusUpdate(DJIDeviceStatus from, DJIDeviceStatus to) {
            if(to!=null&& !TextUtils.isEmpty(to.name())){
                Movement.getInstance().setPlaneMessage(to.name());
            }
        }
    };

//    private void publishDeviceStatus2Server() {
//        if (isFlyClickTime()) {
//            MqttMessage flightMessage = null;
//            try {
////                Log.e("推送DeviceStatus状态", new Gson().toJson(DeviceStatusEntity.getInstance()));
//                flightMessage = new MqttMessage(new Gson().toJson(DeviceStatusEntity.getInstance()).getBytes("UTF-8"));
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//            flightMessage.setQos(1);
//            publish(client, MqttConfig.MQTT_DEVICE_STATUS, flightMessage);
//        }
//    }
    public void releaseDeviceStatusKey(){
        if (iDeviceStatusManager!=null){
            iDeviceStatusManager.removeDJIDeviceStatusChangeListener(djiDeviceStatusChangeListener);
        }
    }
}
