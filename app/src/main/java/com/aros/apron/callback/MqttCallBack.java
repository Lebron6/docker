package com.aros.apron.callback;


import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.aros.apron.constant.AMSConfig;
import com.aros.apron.entity.MQMessage;
import com.aros.apron.entity.Movement;
import com.aros.apron.manager.AlternateLandingManager;
import com.aros.apron.manager.CameraManager;
import com.aros.apron.manager.FlightManager;
import com.aros.apron.manager.GimbalManager;
import com.aros.apron.manager.MediaManager;
import com.aros.apron.manager.MegaphoneManager;
import com.aros.apron.manager.MissionManager;
import com.aros.apron.manager.OffSiteLandingManager;
import com.aros.apron.manager.PayloadWidgetManager;
import com.aros.apron.manager.PerceptionManager;
import com.aros.apron.manager.ResetHomePointManager;
import com.aros.apron.manager.StickManager;
import com.aros.apron.manager.StreamManager;
import com.aros.apron.manager.SystemManager;
import com.aros.apron.tools.LogUtil;
import com.aros.apron.tools.PreferenceUtils;
import com.google.gson.Gson;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;

public class MqttCallBack implements MqttCallbackExtended {

    private String TAG = "MqttCallBack";
    private MqttAndroidClient mqttClient;
    private MqttConnectOptions mMqttConnectOptions;

    public MqttCallBack(MqttAndroidClient mqttClient, MqttConnectOptions mMqttConnectOptions) {
        this.mqttClient = mqttClient;
        this.mMqttConnectOptions = mMqttConnectOptions;
    }

    @Override
    public void connectionLost(Throwable cause) {
        LogUtil.log(TAG, "MQtt connectionLost-----");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    reConnect();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }, 1000);

    }

    //断线重连
    public void reConnect() throws Exception {
        if (null != mqttClient) {
            LogUtil.log(TAG, "MQtt reConnect-----");
            mqttClient.connect(mMqttConnectOptions);
        }
    }


    private boolean isReceiverMission = false;

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) {

        String jsonString = null;
        try {
            jsonString = new String(mqttMessage.getPayload(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "解析异常");
            throw new RuntimeException(e);
        }
        MQMessage message = new Gson().fromJson(jsonString, MQMessage.class);
        switch (message.getMsg_type()) {
            //检测遥控器与开发板是否连接
            case 60001:
                LogUtil.log(TAG, "收到命令：遥控器是否连接" + jsonString);
                SystemManager.getInstance().checkRemoteControlPowerStatus(mqttClient, message);
                break;
            //检测飞机是否开机
            case 60002:
                LogUtil.log(TAG, "收到命令：飞机是否开机" + jsonString);
                SystemManager.getInstance().checkAircraftPowerStatus(mqttClient, message);
                break;
            //航线和推流地址指令，收到后立即回复1，自行处理航线和推流逻辑
            case 60003:
                //默认规定不在返航时才可以上传航线
                if (Movement.getInstance().getGoHomeState() != 1 && Movement.getInstance().getGoHomeState() != 2) {
                    if (message.getIsGuidingFlight() == 0) {
                        LogUtil.log(TAG, "收到命令：航线" + jsonString);
                        if (isReceiverMission == false) {
                            isReceiverMission = true;
                            // 1.缓存推流地址,minIO配置
                            PreferenceUtils.getInstance().setStreamAndMinIOConfig(message);
                            // 2.收到60003直接回复
                            StreamManager.getInstance().sendReply2Server(mqttClient, message);
                            // 3.开启推流
                            StreamManager.getInstance().startLive(mqttClient, message);
                            // 4.关闭避障
                            PerceptionManager.getInstance().setPerceptionEnable(false);
                            // 5.清空sd卡
                            CameraManager.getInstance().formatStorage(null,null);
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    MissionManager.getInstance().startTaskProcess(mqttClient, message);
                                }
                            }, 5000);
                        }
                    } else {
                        LogUtil.log(TAG, "收到命令：指点飞行" + jsonString);
                        // 1.收到60003直接回复
                        StreamManager.getInstance().sendReply2Server(mqttClient, message);
                        // 2.下载航线
                        MissionManager.getInstance().startTaskProcess(mqttClient, message);
                    }
                } else {
                    LogUtil.log(TAG, "返航模式,无法上传航线");
                }
                break;
            //航线暂停
            case 60004:
                LogUtil.log(TAG, "收到命令：航线暂停" + jsonString);
                MissionManager.getInstance().pauseMission(mqttClient, message);
                break;
            //航线继续
            case 60005:
                LogUtil.log(TAG, "收到命令：航线继续" + jsonString);
                MissionManager.getInstance().resumeMission(mqttClient, message);
                break;
            //返航
            case 60006:
                LogUtil.log(TAG, "收到命令：返航" + jsonString);
                FlightManager.getInstance().startGoHome(mqttClient, message);
                break;
            //获取控制权
            case 60007:
                LogUtil.log(TAG, "收到命令：获取控制权" + jsonString);
                StickManager.getInstance().setVirtualStickModeEnabled(mqttClient, message);
                break;
            //虚拟摇杆数据
            case 60008:
                LogUtil.log(TAG, "收到命令：虚拟摇杆数据" + jsonString);
                StickManager.getInstance().sendVirtualStickAdvancedParam(mqttClient, message);
                break;
            //云台角度控制
            case 60009:
                LogUtil.log(TAG, "收到命令：云台角度控制" + jsonString);
                GimbalManager.getInstance().gimbalRotateByRelativeAngle(mqttClient, message);
                break;
            //收到此指令表示舱门已关闭，等待归中60012后可调用60011关机
            case 60010:
//                LogUtil.log(TAG, "收到命令：舱门已关闭" + jsonString);
//                SystemManager.getInstance().droneShutdown(mqttClient, message);
                break;
            //收到60012表示已归中，立即回复60012，且上传相册后可以发送60011关闭飞机和遥控器
            case 60012:
                LogUtil.log(TAG, "收到命令：已归中" + jsonString);
                SystemManager.getInstance().aircraftStoredReply(mqttClient, message);
                break;
            //收到60014表示Server已收到架次开始
            case 60014:
                LogUtil.log(TAG, "收到Server架次开始");
                break;
            //收到60015表示Server已收到架次结束
            case 60015:
                LogUtil.log(TAG, "收到Server架次结束");
                break;
            //取消控制权
            case 60016:
                LogUtil.log(TAG, "收到命令：取消控制权" + jsonString);
                StickManager.getInstance().setVirtualStickModeDisable(mqttClient, message);
                break;
            //切换视频源 1广角 2变焦 3红外
            case 60017:
                LogUtil.log(TAG, "收到命令：切换视频源" + jsonString);
                CameraManager.getInstance().setCameraVideoStreamSource(mqttClient, message);
                break;
            //设置相机模式 0拍照 1录像
            case 60018:
                LogUtil.log(TAG, "收到命令：设置相机模式" + jsonString);
                CameraManager.getInstance().setCameraMode(mqttClient, message);
                break;
            //开始拍照
            case 60019:
                LogUtil.log(TAG, "收到命令：开始拍照" + jsonString);
                CameraManager.getInstance().startShootPhoto(mqttClient, message);
                break;
            //开始录像
            case 60020:
                LogUtil.log(TAG, "收到命令：开始录像" + jsonString);
                CameraManager.getInstance().startRecordVideo(mqttClient, message);
                break;
            //停止录像
            case 60021:
                LogUtil.log(TAG, "收到命令：停止录像" + jsonString);
                CameraManager.getInstance().stopRecordVideo(mqttClient, message);
                break;
            //设置变焦倍率
            case 60022:
                LogUtil.log(TAG, "收到命令：设置变焦倍率" + jsonString);
                CameraManager.getInstance().setCameraZoomRatios(mqttClient, message);
                break;
            //设置红外变焦倍率
            case 60023:
                LogUtil.log(TAG, "收到命令：设置红外变焦倍率" + jsonString);
                CameraManager.getInstance().setThermalZoomRatios(mqttClient, message);
                break;
            //开始推流，可配置推流视角
            case 60100:
                LogUtil.log(TAG, "收到命令：开始推流" + jsonString);
                StreamManager.getInstance().startLive(mqttClient, message);
                break;
            //设置推流分辨率
            case 60101:
                LogUtil.log(TAG, "收到命令：设置推流分辨率" + jsonString);
                StreamManager.getInstance().setLiveStreamQuality(mqttClient, message);
                break;
            //设置限高
            case 60102:
                LogUtil.log(TAG, "收到命令：设置限高" + jsonString);
                FlightManager.getInstance().setHeightLimit(mqttClient, message);
                break;
            //设置限远
            case 60103:
                LogUtil.log(TAG, "收到命令：设置限远" + jsonString);
                FlightManager.getInstance().setDistanceLimit(mqttClient, message);
                break;
            //设置限远是否启用
            case 60104:
                LogUtil.log(TAG, "收到命令：设置限远是否启用" + jsonString);
                FlightManager.getInstance().setDistanceLimitEnabled(mqttClient, message);
                break;
            //设置红外分屏
            case 60105:
                LogUtil.log(TAG, "收到命令：设置红外分屏" + jsonString);
                CameraManager.getInstance().setThermalDisplayMode(mqttClient, message);
                break;
            //取消返航
            case 60106:
                LogUtil.log(TAG, "收到命令：取消返航" + jsonString);
                FlightManager.getInstance().stopGoHome(mqttClient, message);
                break;
            //终止航线
            case 60109:
                LogUtil.log(TAG, "收到命令：终止航线" + jsonString);
                MissionManager.getInstance().stopMission(mqttClient, message);
                break;
            //开始喊话
            case 60110:
                LogUtil.log(TAG, "收到命令：开始喊话" + jsonString);
                MegaphoneManager.getInstance().startMegaphonePlay(mqttClient, message);
                break;
            //结束喊话
            case 60111:
                LogUtil.log(TAG, "收到命令：结束喊话" + jsonString);
                MegaphoneManager.getInstance().stopPlay(mqttClient, message);
                break;
            //一键飞往紧急备降点
            case 60112:
                LogUtil.log(TAG, "收到命令：备降点降落" + jsonString);
                AlternateLandingManager.getInstance().startTaskProcess(message);
                break;
            //降落
            case 60113:
                LogUtil.log(TAG, "收到命令：降落" + jsonString);
                FlightManager.getInstance().startAutoLanding(mqttClient,message);
                break;
            //取消降落
            case 60114:
                LogUtil.log(TAG, "收到命令：取消降落" + jsonString);
                FlightManager.getInstance().stopAutoLanding(mqttClient,message);
                break;
            //解锁抛投器
            case 60115:
                LogUtil.log(TAG, "收到命令：解锁" + jsonString);
                PayloadWidgetManager.getInstance().unlock(mqttClient,message); break;
            //锁定抛投器
            case 60116:
                LogUtil.log(TAG, "收到命令：锁定" + jsonString);
                PayloadWidgetManager.getInstance().lock(mqttClient,message); break;
            //抛投
            case 60117:
                LogUtil.log(TAG, "收到命令：抛投" + jsonString);
                PayloadWidgetManager.getInstance().throwOne(mqttClient,message); break;
            //一键全抛
            case 60118:
                LogUtil.log(TAG, "收到命令：一键全投" + jsonString);
                PayloadWidgetManager.getInstance().throwAll(mqttClient,message); break;
            //设置备降点
            case 60119:
                LogUtil.log(TAG, "收到命令：设置备降点" + jsonString);
                AlternateLandingManager.getInstance().setAlternatePoint(mqttClient,message); break;
            //设置定时拍照参数
            case 60120:
                LogUtil.log(TAG, "收到命令：设置定时拍照参数" + jsonString);
                CameraManager.getInstance().setPhotoIntervalShootSettings(mqttClient,message); break;
            //结束拍照
            case 60121:
                LogUtil.log(TAG, "收到命令：结束拍照" + jsonString);
                CameraManager.getInstance().stopShootPhoto(mqttClient, message);
                break;
            //重置相机
            case 60122:
                LogUtil.log(TAG, "收到命令：重置相机设置" + jsonString);
                CameraManager.getInstance().resetCameraSetting(mqttClient, message);
                break;
            //重置云台
            case 60123:
                LogUtil.log(TAG, "收到命令：重置云台" + jsonString);
                GimbalManager.getInstance().gimbalReset(mqttClient, message);
                break;
            //设置对焦模式
            case 60124:
                LogUtil.log(TAG, "收到命令：设置对焦模式" + jsonString);
                CameraManager.getInstance().setCameraFocusMode(mqttClient, message);
                break;
            //指点对焦
            case 60125:
                LogUtil.log(TAG, "收到命令：指点对焦" + jsonString);
                CameraManager.getInstance().tapZoomAtTarget(mqttClient, message);
                break;
            //设置照片xmp写入
            case 60126:
                LogUtil.log(TAG, "收到命令：写入exif" + jsonString);
                MediaManager.INSTANCE.setMediaFileXMPCustomInfo(mqttClient, message);
                break;
            //设置曝光模式
            case 60127:
                LogUtil.log(TAG, "收到命令：设置曝光模式" + jsonString);
                CameraManager.getInstance().setExposureMode(mqttClient, message);
                break;
            //设置ev
            case 60128:
                LogUtil.log(TAG, "收到命令：设置曝光补偿" + jsonString);
                CameraManager.getInstance().setExposureCompensation(mqttClient, message);
                break;
            //发送数据到psdk
            case 60129:
                LogUtil.log(TAG, "收到命令：发送数据到psdk" + jsonString);
                PayloadWidgetManager.getInstance().sendMsgToPayload(mqttClient, message);
                break;
            //停止推流
            case 60130:
                LogUtil.log(TAG, "收到命令：停止推流" + jsonString);
                StreamManager.getInstance().stopLive(mqttClient, message);
                break;
            //设置对焦值
            case 60131:
                LogUtil.log(TAG, "收到命令：设置对焦值" + jsonString);
                CameraManager.getInstance().setCameraFocusRingValue(mqttClient, message);
                break;
            //获取里程
            case 60132:
                LogUtil.log(TAG, "收到命令：获取里程" + jsonString);
                FlightManager.getInstance().getAircraftTotalFlightDistance(mqttClient, message);
                break;
            //上传媒体文件
            case 60134:
//                LogUtil.log(TAG, "收到命令：获取里程" + jsonString);
//                FlightManager.getInstance().getAircraftTotalFlightDistance(mqttClient, message);
                break;
            //异地降落
            case 60135:
                LogUtil.log(TAG, "收到命令：异地降落" + jsonString);
                OffSiteLandingManager.getInstance().startTaskProcess(message);
                break;
            //异地降落
            case 60136:
                LogUtil.log(TAG, "收到命令：刷新返航点" + jsonString);
                ResetHomePointManager.getInstance().startTaskProcess(mqttClient,message);
                break;
            //监听机库收到AMS命令后的回执
            case 60999:
                if (!TextUtils.isEmpty(message.getStatus())) {
                    switch (message.getStatus()) {
                        case "0":
                            LogUtil.log(TAG, "收到命令：服务端响应关舱门" + jsonString);
                            break;
                        case "1":
                            LogUtil.log(TAG, "收到命令：服务端响应开舱门" + jsonString);
                            break;
                        case "2":
                            LogUtil.log(TAG, "收到命令：服务端响应入库" + jsonString);
                            break;
                        case "3":
                            LogUtil.log(TAG, "收到命令：服务端响应关机" + jsonString);
                            break;
                    }
                } else {
                    LogUtil.log(TAG, "收到命令：机库动作参数有误" + jsonString);
                }
                break;

        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        try {
            if (reconnect) {//重新订阅
                Log.e(TAG, "MQtt ConnectComplete:" + serverURI);
                mqttClient.subscribe(AMSConfig.getInstance().getMqttServer2MsdkTopic(), 1);//订阅主题:注册
                // publish(topic,"注册",0);
            }
        } catch (Exception e) {
            Log.e(TAG, "MQtt ConnectException:" + e.toString());
        }
    }
}
