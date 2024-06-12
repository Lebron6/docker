//package com.aros.apron.manager;
//
//import com.aros.apron.base.BaseManager;
//import com.aros.apron.constant.MqttConfig;
//import com.aros.apron.entity.CameraStateEntity;
//import com.aros.apron.entity.DeviceHealthEntity;
//import com.aros.apron.entity.DeviceStatusEntity;
//import com.google.gson.Gson;
//
//import org.eclipse.paho.android.service.MqttAndroidClient;
//import org.eclipse.paho.client.mqttv3.MqttMessage;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import dji.sdk.keyvalue.key.FlightControllerKey;
//import dji.sdk.keyvalue.key.KeyTools;
//import dji.v5.manager.KeyManager;
//import dji.v5.manager.diagnostic.DJIDeviceHealthInfo;
//import dji.v5.manager.diagnostic.DJIDeviceHealthInfoChangeListener;
//import dji.v5.manager.diagnostic.DJIDeviceStatus;
//import dji.v5.manager.diagnostic.DJIDeviceStatusChangeListener;
//import dji.v5.manager.interfaces.IDeviceHealthManager;
//import dji.v5.manager.interfaces.IDeviceStatusManager;
//
//public class DeviceHealthManager extends BaseManager {
//
//    MqttAndroidClient client;
//    private IDeviceHealthManager iDeviceHealthManager;
//
//
//    private DeviceHealthManager() {
//    }
//
//    private static class DeviceHealthHolder {
//        private static final DeviceHealthManager INSTANCE = new DeviceHealthManager();
//    }
//
//    public static DeviceHealthManager getInstance() {
//        return DeviceHealthHolder.INSTANCE;
//    }
//
//    public void initDeviceHealth(MqttAndroidClient client) {
//        this.client = client;
//        Boolean isConnect = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection));
//        if (isConnect != null && isConnect) {
//            iDeviceHealthManager = dji.v5.manager.diagnostic.DeviceHealthManager.getInstance();
//            iDeviceHealthManager.addDJIDeviceHealthInfoChangeListener(djiDeviceHealthInfoChangeListener);
//        }
//
//    }
//
//    DJIDeviceHealthInfoChangeListener djiDeviceHealthInfoChangeListener=new DJIDeviceHealthInfoChangeListener() {
//        @Override
//        public void onDeviceHealthInfoUpdate(List<DJIDeviceHealthInfo> infos) {
//            if (infos!=null&&infos.size()>0){
//                List<DeviceHealthEntity.DeviceHealthInfo> deviceHealthInfos=new ArrayList<>();
//                for (int i = 0; i < infos.size(); i++) {
//                    DeviceHealthEntity.DeviceHealthInfo healthInfo=new DeviceHealthEntity.DeviceHealthInfo();
//                    healthInfo.setDescription(infos.get(i).description());
//                    healthInfo.setWarningLevel(infos.get(i).warningLevel().ordinal());
//                    healthInfo.setTitle(infos.get(i).title());
//                    healthInfo.setInformationCode(infos.get(i).informationCode());
//                    deviceHealthInfos.add(healthInfo);
//                }
//                DeviceHealthEntity.getInstance().setDjiDeviceHealthInfos(deviceHealthInfos);
//                publishHealthInfo2Server();
//            }
//        }
//    };
//
//    private void publishHealthInfo2Server() {
//        if (isFlyClickTime()) {
//            MqttMessage flightMessage = null;
//            try {
//                DeviceHealthEntity.getInstance().setTimeStamp(String.valueOf(System.currentTimeMillis()));
//                flightMessage = new MqttMessage(new Gson().toJson(DeviceHealthEntity.getInstance()).getBytes("UTF-8"));
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//            flightMessage.setQos(1);
//            publish(client, MqttConfig.MQTT_DEVICE_HEALTH, flightMessage);
//        }
//    }
//    public void releaseDeviceHealthKey(){
//        if (iDeviceHealthManager!=null){
//            iDeviceHealthManager.removeDJIDeviceHealthInfoChangeListener(djiDeviceHealthInfoChangeListener);
//        }
//    }
//}
