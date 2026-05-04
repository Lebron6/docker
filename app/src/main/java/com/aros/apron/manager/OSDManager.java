package com.aros.apron.manager;

import static dji.sdk.keyvalue.key.KeyTools.createKey;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import com.aros.apron.base.BaseManager;
import com.aros.apron.constant.AMSConfig;
import com.aros.apron.entity.Movement;
import com.aros.apron.entity.Osd;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.MqttManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dji.sdk.keyvalue.key.CameraKey;
import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.value.camera.CameraVideoStreamSourceType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.v5.manager.KeyManager;
import dji.v5.manager.datacenter.MediaDataCenter;
import dji.v5.manager.datacenter.livestream.StreamQuality;
import dji.v5.manager.interfaces.ILiveStreamManager;

public class OSDManager extends BaseManager {


    private static final long INTERVAL = 2000L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    Osd.Data._$5300 _5300 = new Osd.Data._$5300();
    Osd.Data data = new Osd.Data();
    Osd.Data.Battery battery = new Osd.Data.Battery();
    List<Osd.Data.Battery.Batteries> batteries = new ArrayList<>();
    Osd.Data.Battery.Batteries batterieA = new Osd.Data.Battery.Batteries();
    Osd.Data.Battery.Batteries batterieB = new Osd.Data.Battery.Batteries();
    Osd.Data.LiveStatus liveStatus = new Osd.Data.LiveStatus();
    List<Osd.Data.LiveStatus> liveStatusArrays=new ArrayList<>();


    List<Osd.Data.Cameras> cameras = new ArrayList<>();
    Osd.Data.Cameras cameraA = new Osd.Data.Cameras();
    Osd.Data.Cameras.IrMeteringPoint irMeteringPoint = new Osd.Data.Cameras.IrMeteringPoint();
    Osd.Data.DistanceLimitStatus distanceLimitStatus = new Osd.Data.DistanceLimitStatus();
    Osd.Data.ObstacleAvoidance obstacleAvoidance = new Osd.Data.ObstacleAvoidance();
    Osd.Data.PositionState positionState = new Osd.Data.PositionState();
    Osd.Data.Storage storage = new Osd.Data.Storage();
    Osd osd = Osd.getInstance();
    // 使用GsonBuilder配置Gson实例以允许序列化特殊浮点数值
    Gson gson = new GsonBuilder()
            .serializeSpecialFloatingPointValues() // 这是关键
            .create();
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
            Boolean isConnect = KeyManager.getInstance().getValue(createKey(FlightControllerKey.KeyConnection));
            if (isConnect != null && isConnect
//                    && !Movement.getInstance().isMissionFinish()
            ) {
                pushFlightAttitude();
            }
//            else{
//                LogUtil.log(TAG,"osd stop:flight controller is null");
//            }
            // 始终基于“实际执行时间”来调度
            handler.postDelayed(this, INTERVAL);
        }
    };

    private OSDManager() {
    }

    public static OSDManager getInstance() {
        return OSDHolder.INSTANCE;
    }

    public void initOsd() {
        Boolean isConnect = KeyManager.getInstance().getValue(createKey(FlightControllerKey.KeyConnection));
        if (isConnect != null && isConnect) {
            handler.postDelayed(runnable, INTERVAL);
        } else {
            LogUtil.log(TAG, "初始化飞控osd失败" + "flight controller is null");
        }
    }

    private void pushFlightAttitude() {
        try {

            if (liveStatusArrays != null && liveStatusArrays.size() > 0) {
                liveStatusArrays.clear();
            }
            if (batteries != null && batteries.size() > 0) {
                batteries.clear();
            }
            if (cameras != null && cameras.size() > 0) {
                cameras.clear();
            }
            //相机固件?
            _5300.setGimbal_yaw(Movement.getInstance().getGimbal_yaw());
            _5300.setGimbal_roll(Movement.getInstance().getGimbal_roll());
            _5300.setGimbal_pitch(Movement.getInstance().getGimbal_pitch());
            _5300.setMeasure_target_altitude(Movement.getInstance().getMeasure_target_altitude());
            _5300.setMeasure_target_distance(Movement.getInstance().getMeasure_target_distance());
            _5300.setMeasure_target_error_state(Movement.getInstance().getMeasure_target_error_state());

            _5300.setMeasure_target_latitude(Movement.getInstance().getMeasure_target_latitude());
            _5300.setMeasure_target_longitude(Movement.getInstance().getMeasure_target_longitude());
            _5300.setPayload_index("53-0-0");
            _5300.setThermal_current_palette_style(Movement.getInstance().getThermal_current_palette_style());
            _5300.setThermal_gain_mode(Movement.getInstance().getThermal_gain_mode());
            _5300.setThermal_global_temperature_max(Movement.getInstance().getThermal_global_temperature_max());
            _5300.setThermal_global_temperature_min(Movement.getInstance().getThermal_global_temperature_min());
            _5300.setThermal_isotherm_lower_limit(Movement.getInstance().getThermal_isotherm_lower_limit());
            _5300.setThermal_isotherm_state(Movement.getInstance().getThermal_isotherm_state());
            _5300.setThermal_isotherm_upper_limit(Movement.getInstance().getThermal_isotherm_upper_limit());
            _5300.setVersion(1);
            data.set_$5300(_5300);
            data.setActivation_time(1);
            data.setAttitude_head(Movement.getInstance().getAttitude_head());
            data.setAttitude_pitch(Movement.getInstance().getAttitude_pitch());
            data.setAttitude_roll(Movement.getInstance().getAttitude_roll());

            liveStatus.setError_status(-1);
            liveStatus.setStatus(Movement.getInstance().getLiveStatus());
//            liveStatus.setVideo_id("{"+Movement.getInstance().getCameraSn()+"}"+"/"+"{53-0-0}"+"/"+"{0}");
            liveStatus.setVideo_id("1581F8DBW25CG00B363D/53-0-0/normal-0");
            ILiveStreamManager liveStreamManager = MediaDataCenter.getInstance().getLiveStreamManager();
            if (liveStreamManager!=null) {
                StreamQuality liveStreamQuality = liveStreamManager.getLiveStreamQuality();
                switch (liveStreamQuality.co_a) {
                    case 1:
                        liveStatus.setVideo_quality(2);
                        break;
                    case 2:
                        liveStatus.setVideo_quality(3);
                        break;
                    case 3:
                        liveStatus.setVideo_quality(4);
                        break;
                    case 100:
                        liveStatus.setVideo_quality(0);
                        break;
                    default:
                        liveStatus.setVideo_quality(1);
                        break;
                }

            }
            CameraVideoStreamSourceType cameraVideoStreamSourceType =
                    KeyManager.getInstance().getValue(createKey(CameraKey.
                    KeyCameraVideoStreamSource, ComponentIndexType.PORT_1));
            if (cameraVideoStreamSourceType!=null){
                switch (cameraVideoStreamSourceType.value()){
                    case 3:
                        liveStatus.setVideo_type("ir");
                        break;
                    case 0:
                        liveStatus.setVideo_type("normal");
                        break;
                    case 1:
                        liveStatus.setVideo_type("wide");
                        break;
                    case 2:
                        liveStatus.setVideo_type("zoom");
                        break;
                }
            }
            liveStatusArrays.add(liveStatus);
            data.setLive_status(liveStatusArrays);

            batterieA.setCapacity_percent(Movement.getInstance().getBattery_a_capacity_percent());
            batterieA.setFirmware_version(Movement.getInstance().getBattery_a_battery_firmware_version());
            batterieA.setHigh_voltage_storage_days(Movement.getInstance().getBattery_a_high_voltage_storage_days());
            batterieA.setIndex(0);
            batterieA.setLoop_times(Movement.getInstance().getBattery_a_loop_times());
            batterieA.setSn(Movement.getInstance().getBattery_a_battery_sn());
            batterieA.setSub_type(Movement.getInstance().getBattery_a_sub_type());
            batterieA.setType(Movement.getInstance().getBattery_a_type());
            batterieA.setTemperature(Movement.getInstance().getBattery_a_temperature());
            batterieA.setSub_type(Movement.getInstance().getBattery_a_sub_type());
            batterieA.setVoltage(Movement.getInstance().getBattery_a_voltage());
            if (Movement.getInstance().getBattery_b_capacity_percent() == 0) {
                batterieB.setCapacity_percent(Movement.getInstance().getBattery_a_capacity_percent());
                batterieB.setFirmware_version(Movement.getInstance().getBattery_a_battery_firmware_version());
                batterieB.setHigh_voltage_storage_days(Movement.getInstance().getBattery_a_high_voltage_storage_days());
                batterieB.setIndex(1);
                batterieB.setLoop_times(Movement.getInstance().getBattery_a_loop_times());
                batterieB.setSn(Movement.getInstance().getBattery_a_battery_sn());
                batterieB.setSub_type(Movement.getInstance().getBattery_a_sub_type());
                batterieB.setType(Movement.getInstance().getBattery_a_type());
                batterieB.setTemperature(Movement.getInstance().getBattery_a_temperature());
                batterieB.setSub_type(Movement.getInstance().getBattery_a_sub_type());
                batterieB.setVoltage(Movement.getInstance().getBattery_a_voltage());
            } else {
                batterieB.setCapacity_percent(Movement.getInstance().getBattery_b_capacity_percent());
                batterieB.setFirmware_version(Movement.getInstance().getBattery_b_battery_firmware_version());
                batterieB.setHigh_voltage_storage_days(Movement.getInstance().getBattery_b_high_voltage_storage_days());
                batterieB.setIndex(1);
                batterieB.setLoop_times(Movement.getInstance().getBattery_b_loop_times());
                batterieB.setSn(Movement.getInstance().getBattery_b_battery_sn());
                batterieB.setSub_type(Movement.getInstance().getBattery_b_sub_type());
                batterieB.setType(Movement.getInstance().getBattery_b_type());
                batterieB.setTemperature(Movement.getInstance().getBattery_b_temperature());
                batterieB.setSub_type(Movement.getInstance().getBattery_b_sub_type());
                batterieB.setVoltage(Movement.getInstance().getBattery_b_voltage());
            }

            batteries.add(batterieA);
            batteries.add(batterieB);
            battery.setBatteries(batteries);
            battery.setCapacity_percent(Movement.getInstance().getCapacity_percent());
            battery.setLanding_power(Movement.getInstance().getLanding_power());
            battery.setRemain_flight_time(Movement.getInstance().getRemain_flight_time());
            battery.setReturn_home_power(Movement.getInstance().getReturn_home_power());
            data.setBattery(battery);

            cameraA.setCamera_mode(Movement.getInstance().getCamera_mode());
            cameraA.setIr_metering_mode(Movement.getInstance().getIr_metering_mode());
            cameraA.setCamera_mode(Movement.getInstance().getCamera_mode());
            irMeteringPoint.setTemperature(Movement.getInstance().getTemperature());
            irMeteringPoint.setX(Movement.getInstance().getX());
            irMeteringPoint.setY(Movement.getInstance().getY());
            cameraA.setIr_metering_point(irMeteringPoint);
            cameraA.setIr_zoom_factor(Movement.getInstance().getIr_zoom_factor());
            cameraA.setPayload_index("53-0-0");
            cameraA.setPhoto_state(Movement.getInstance().getPhoto_state());
            cameraA.setPhoto_storage_settings(Movement.getInstance().getPhoto_storage_settings());
            cameraA.setRecord_time(Movement.getInstance().getRecord_time());
            cameraA.setRecording_state(Movement.getInstance().getRecording_state());
            cameraA.setRemain_photo_num(Movement.getInstance().getRemain_photo_num());
            cameraA.setRemain_record_duration(Movement.getInstance().getRemain_record_duration());
            cameraA.setScreen_split_enable(Movement.getInstance().isScreen_split_enable());
            cameraA.setWide_exposure_mode(Movement.getInstance().getWide_exposure_mode());
            cameraA.setWide_exposure_value(Movement.getInstance().getWide_exposure_value());
            cameraA.setWide_iso(Movement.getInstance().getWide_iso());
            cameraA.setWide_shutter_speed(Movement.getInstance().getWide_shutter_speed());
            cameraA.setZoom_calibrate_farthest_focus_value(Movement.getInstance().getZoom_calibrate_farthest_focus_value());
            cameraA.setZoom_calibrate_nearest_focus_value(Movement.getInstance().getZoom_calibrate_nearest_focus_value());
            cameraA.setZoom_exposure_mode(Movement.getInstance().getZoom_exposure_mode());
            cameraA.setZoom_exposure_value(Movement.getInstance().getZoom_exposure_value());
            cameraA.setZoom_factor(Movement.getInstance().getZoom_factor());
            cameraA.setZoom_focus_mode(Movement.getInstance().getZoom_focus_mode());
            cameraA.setZoom_focus_state(Movement.getInstance().getZoom_focus_state());
            cameraA.setZoom_focus_value(Movement.getInstance().getZoom_focus_value());
            cameraA.setZoom_iso(Movement.getInstance().getZoom_iso());
            cameraA.setZoom_max_focus_value(Movement.getInstance().getZoom_max_focus_value());
            cameraA.setZoom_min_focus_value(Movement.getInstance().getZoom_min_focus_value());
            cameraA.setZoom_shutter_speed(Movement.getInstance().getZoom_shutter_speed());
            cameras.add(cameraA);
            data.setCameras(cameras);

            data.setCountry("CN");
            distanceLimitStatus.setDistance_limit(Movement.getInstance().getDistance_limit());
            distanceLimitStatus.setIs_near_distance_limit(Movement.getInstance().getIs_near_distance_limit());
            distanceLimitStatus.setState(Movement.getInstance().getState());
            data.setDistance_limit_status(distanceLimitStatus);

            data.setElevation(Movement.getInstance().getElevation());
            data.setFirmware_version(Movement.getInstance().getFirmware_version());
            data.setGear(Movement.getInstance().getGear());
            data.setHeight(Movement.getInstance().getHeight());
            data.setHeight_limit(Movement.getInstance().getHeight_limit());
            data.setHome_distance(Movement.getInstance().getHome_distance());
            data.setHorizontal_speed(Movement.getInstance().getHorizontal_speed());
            data.setIs_near_area_limit(Movement.getInstance().getIs_near_area_limit());
            data.setIs_near_height_limit(Movement.getInstance().getIs_near_height_limit());
            data.setLatitude(Movement.getInstance().getLatitude());
            data.setLongitude(Movement.getInstance().getLongitude());
            data.setMode_code(Movement.getInstance().getMode_code());
            data.setNight_lights_state(Movement.getInstance().getNight_lights_state());

            obstacleAvoidance.setUpside(Movement.getInstance().getUpside());
            obstacleAvoidance.setHorizon(Movement.getInstance().getHorizon());
            obstacleAvoidance.setDownside(Movement.getInstance().getDownside());
            data.setObstacle_avoidance(obstacleAvoidance);

            positionState.setGps_number(Movement.getInstance().getGps_number());
            positionState.setIs_fixed(Movement.getInstance().getIs_fixed());
            positionState.setQuality(Movement.getInstance().getQuality());
            positionState.setRtk_number(Movement.getInstance().getRtk_number());
            data.setPosition_state(positionState);

            data.setRc_lost_action(Movement.getInstance().getRc_lost_action());
            data.setRid_state(Movement.getInstance().isRid_state());
            data.setRth_altitude(Movement.getInstance().getRth_altitude());

            storage.setUsed(Movement.getInstance().getUsed());
            storage.setTotal(Movement.getInstance().getTotal());
            data.setStorage(storage);

            data.setTotal_flight_distance(Movement.getInstance().getTotal_flight_distance());
            data.setTotal_flight_sorties(Movement.getInstance().getTotal_flight_sorties());
            data.setTotal_flight_time(Movement.getInstance().getTotal_flight_time());
            data.setTrack_id(Movement.getInstance().getTrack_id());
            data.setVertical_speed(Movement.getInstance().getVertical_speed());
            data.setWind_direction(Movement.getInstance().getWind_direction());
            data.setWind_speed(Movement.getInstance().getWind_speed());
            data.setDrc_state(Movement.getInstance().getIsVirtualStickEnable()==0?0:2);
            data.setHomepoint_latitude(Movement.getInstance().getHomepoint_latitude());
            data.setHomepoint_longitude(Movement.getInstance().getHomepoint_longitude());
            data.setRtk_takeoff_altitude(Movement.getInstance().getRtk_takeoff_altitude());

            osd.setTid(UUID.randomUUID().toString());
            osd.setBid(UUID.randomUUID().toString());
            osd.setTimestamp(System.currentTimeMillis());
            osd.setGateway("gateway_sn");
            osd.setMethod("osd");

            osd.setData(data);

            MqttMessage flightMessage =
                    new MqttMessage(gson.toJson(osd).getBytes(StandardCharsets.UTF_8));
            flightMessage.setQos(0);

            publish(
                    MqttManager.getInstance().mqttAndroidClient,
                    AMSConfig.UP_UAV_EVENT,
                    flightMessage
            );
//            Log.e(TAG,"推送osd:"+gson.toJson(osd));
        } catch (Exception e) {
            LogUtil.log(TAG, "推送osd异常: " + e);
        }

    }

    private static class OSDHolder {
        private static final OSDManager INSTANCE = new OSDManager();
    }
}