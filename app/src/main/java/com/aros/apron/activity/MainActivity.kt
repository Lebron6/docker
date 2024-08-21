package com.aros.apron.activity

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.SurfaceHolder
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import com.aros.apron.BuildConfig
import com.aros.apron.base.BaseActivity
import com.aros.apron.databinding.ActivityMainBinding
import com.aros.apron.entity.MQMessage
import com.aros.apron.entity.Movement
import com.aros.apron.manager.AlternateLandingManager
import com.aros.apron.manager.BatteryManager
import com.aros.apron.manager.CameraManager
import com.aros.apron.manager.FlightManager
import com.aros.apron.manager.FlightManager.FLAG_DOWN_LAND
import com.aros.apron.manager.FlightManager.FLAG_START_DETECT_ARUCO_ALTERNATE
import com.aros.apron.manager.FlightManager.FLAG_START_DETECT_ARUCO_APRON
import com.aros.apron.manager.FlightManager.FLAG_STOP_ARUCO
import com.aros.apron.manager.GimbalManager
import com.aros.apron.manager.LEDsSettingsManager
import com.aros.apron.manager.MediaManager
import com.aros.apron.manager.MissionManager
import com.aros.apron.manager.PayloadWidgetManager
import com.aros.apron.manager.PerceptionManager
import com.aros.apron.manager.RTKManager
import com.aros.apron.manager.StickManager
import com.aros.apron.manager.StreamManager
import com.aros.apron.manager.WayLineExecutingInterruptManager
import com.aros.apron.tools.AlternateArucoDetect
import com.aros.apron.tools.ApronArucoDetect
import com.aros.apron.tools.DroneHelper
import com.aros.apron.tools.LogUtil
import com.aros.apron.tools.PreferenceUtils
import com.aros.apron.tools.ToastUtil
import com.google.gson.Gson
import dji.sdk.keyvalue.key.CameraKey
import dji.sdk.keyvalue.key.DJIKey
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.key.ProductKey
import dji.sdk.keyvalue.value.camera.CameraMode
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.sdk.keyvalue.value.common.EmptyMsg
import dji.sdk.keyvalue.value.payload.WidgetType
import dji.sdk.keyvalue.value.payload.WidgetValue
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.callback.CommonCallbacks.CompletionCallback
import dji.v5.common.error.IDJIError
import dji.v5.manager.KeyManager
import dji.v5.manager.aircraft.payload.PayloadCenter
import dji.v5.manager.aircraft.payload.PayloadIndexType
import dji.v5.manager.datacenter.MediaDataCenter
import dji.v5.manager.interfaces.ICameraStreamManager
import okio.Utf8
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.aruco.Aruco
import org.opencv.aruco.Dictionary
import java.nio.charset.StandardCharsets


class MainActivity : BaseActivity() {

    var cameraManager = MediaDataCenter.getInstance().cameraStreamManager
    private var mainBinding: ActivityMainBinding? = null
    private var startArucoType = 0  //1执行机库二维码识别  2执行备降点二维码识别
    private var dictionary: Dictionary? = null
    private var mqMessage: MQMessage? = null

    override fun useEventBus(): Boolean {
        return true
    }

    override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback)
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
        dictionary = Aruco.getPredefinedDictionary(Aruco.DICT_6X6_250)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mainBinding!!.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        needConnect()
        initDJIManager()
        initCameraStream()
        initView()
    }

    private fun initView() {
        if (PreferenceUtils.getInstance().isDebugMode) {
            mainBinding?.layoutDebugMode?.visibility = View.VISIBLE
        }else{
            mainBinding?.layoutDebugMode?.visibility = View.GONE

        }

        mainBinding?.startAlter?.setOnClickListener {
            AlternateLandingManager.getInstance().startTaskProcess(null)
//            MediaDataCenter.getInstance().mediaManager.setMediaFileXMPCustomInfo(
//                "wangmingge",
//                object :
//                    CommonCallbacks.CompletionCallback {
//                    override fun onSuccess() {
//                        Log.e("写入成功","----")
//                    }
//
//                    override fun onFailure(error: IDJIError) {
//                        Log.e("写入失败", Gson().toJson(error))
//                    }
//                })

        }

        mainBinding?.startMission?.setOnClickListener {
//            KeyManager.getInstance().setValue<CameraMode>(
//                DJIKey.create<CameraMode>(CameraKey.KeyCameraMode),
//                CameraMode.PHOTO_NORMAL,
//                object : CompletionCallback {
//                    override fun onSuccess() {
//                        Log.e("切换模式", "success")
//                        KeyManager.getInstance().performAction<EmptyMsg>(
//                            DJIKey.create<EmptyMsg, EmptyMsg>(CameraKey.KeyStartShootPhoto),
//                            object : CommonCallbacks.CompletionCallbackWithParam<EmptyMsg?> {
//                                override fun onSuccess(emptyMsg: EmptyMsg?) {
//                                    Log.e("拍照成功",  "拍照成功:") }
//                                override fun onFailure(error: IDJIError) {
//                                    Log.e("拍照失败",  "拍照失败:" + Gson().toJson(error)) }
//
//                            })
//                    }
//
//
//                    override fun onFailure(error: IDJIError) {
//                        Log.e("切换模式失败", Gson().toJson(error))
//                    }
//
//
//                })

            val message=MQMessage ()
           message.secret_key="admin123"
           message.kmz_url="http://162.14.115.91:9000/test/kmz/测试1·.kmz"
           message.access_key="admin"
           message.flight_name="测试1·2024_04_02_09_18_45"
           message.msg_type=60003
           message.upload_url="http://162.14.115.91:9000/test/88AEDD00D02A/54403fbf-f964-4585-9dff-f8dfd7b4b61d"
           message.flightId="flightId"
//            //楼上350
           message.rtmp_push_url="rtmp://47.97.39.183/live/1581F5FJB229Q00A003W"
//            //楼下小机库2
           message.rtmp_push_url="rtmp://47.97.39.183/live/1581F5FJB229Q00A003A"
           message.isGuidingFlight=0
            if (Movement.getInstance().goHomeState != 1 && Movement.getInstance().goHomeState != 2) {
                    // 1.缓存推流地址,minIO配置
                    PreferenceUtils.getInstance().setStreamAndMinIOConfig(message)
                    // 2.收到60003直接回复
                    StreamManager.getInstance().sendReply2Server(mqttAndroidClient, message)
                    // 3.开启推流
                    StreamManager.getInstance().startLive(mqttAndroidClient, message)
                    // 4.关闭避障
                    PerceptionManager.getInstance().setPerceptionEnable(false)
                    MissionManager.getInstance().startTaskProcess(mqttAndroidClient, message)
            } else {
                LogUtil.log(TAG, "返航模式,无法上传航线")
            }

        }

        mainBinding?.btnLock?.setOnClickListener {
            val widgetValue = WidgetValue()
            widgetValue.value = 1
            widgetValue.index = 0
            widgetValue.type = WidgetType.SWITCH
            val payloadManagerMap = PayloadCenter.getInstance().payloadManager
            payloadManagerMap[PayloadIndexType.RIGHT]!!.setWidgetValue(
                widgetValue,
                object : CompletionCallback {
                    override fun onSuccess() {
                        ToastUtil.showToast("setWidgetValue success")
                    }

                    override fun onFailure(idjiError: IDJIError) {
                        Log.e(TAG, "错误:${Gson().toJson(idjiError)}")
                    }
                })
        }
        mainBinding?.btnUnlock?.setOnClickListener {
            val widgetValue = WidgetValue()
            widgetValue.value = 0
            widgetValue.index = 0
            widgetValue.type = WidgetType.SWITCH
            val payloadManagerMap = PayloadCenter.getInstance().payloadManager
            payloadManagerMap[PayloadIndexType.RIGHT]!!.setWidgetValue(
                widgetValue,
                object : CompletionCallback {
                    override fun onSuccess() {
                        ToastUtil.showToast("setWidgetValue success")
                    }

                    override fun onFailure(idjiError: IDJIError) {
                        Log.e(TAG, "错误:${Gson().toJson(idjiError)}")
                    }
                })
        }
        mainBinding?.btnRoll?.setOnClickListener {
            val widgetValue = WidgetValue()
            widgetValue.value = 1
            widgetValue.index = 1
            widgetValue.type = WidgetType.BUTTON
            val payloadManagerMap = PayloadCenter.getInstance().payloadManager
            payloadManagerMap[PayloadIndexType.RIGHT]!!.setWidgetValue(
                widgetValue,
                object : CompletionCallback {
                    override fun onSuccess() {
                        ToastUtil.showToast("setWidgetValue success")
                    }

                    override fun onFailure(idjiError: IDJIError) {
                        Log.e(TAG, "错误:${Gson().toJson(idjiError)}")
                    }
                })

        }
        mainBinding?.btnRollall?.setOnClickListener {
            val widgetValue = WidgetValue()
            widgetValue.value = 1
            widgetValue.index = 2
            widgetValue.type = WidgetType.BUTTON
            val payloadManager=PayloadCenter.getInstance().payloadManager[PayloadIndexType.RIGHT]
            payloadManager?.setWidgetValue(
                widgetValue,
                object : CompletionCallback {
                    override fun onSuccess() {
                        ToastUtil.showToast("setWidgetValue success")
                    }

                    override fun onFailure(idjiError: IDJIError) {
                        Log.e(TAG, "错误:${Gson().toJson(idjiError)}")
                    }
                })


        }
    }

    private val handler: Handler = Handler(Looper.getMainLooper())

    private fun initDJIManager() {
        val isFlightControllerConnect =
            KeyManager.getInstance().getValue(DJIKey.create(FlightControllerKey.KeyConnection))
        if (isFlightControllerConnect == null || !isFlightControllerConnect) {
            handler.postDelayed({
                initDJIManager()
            }, 1000)
        } else {
            RTKManager.getInstance().initRTKInfo()
            StreamManager.getInstance().initStreamManager(mqttAndroidClient)
            FlightManager.getInstance().initFlightInfo(mqttAndroidClient)
            MissionManager.getInstance().initMissionManager(mqttAndroidClient)
            BatteryManager.getInstance().initBatteryInfo(mqttAndroidClient)
            MediaManager.init(mqttAndroidClient)
            LEDsSettingsManager.getInstance().initLEDsInfo()
            AlternateLandingManager.getInstance().initAlterLandingInfo(mqttAndroidClient)
            WayLineExecutingInterruptManager.getInstance().initWayLineExecutingInterruptInfo(mqttAndroidClient)
            CameraManager.getInstance().initCameraInfo(mqttAndroidClient)
            StickManager.getInstance().initStickInfo(mqttAndroidClient)
            PayloadWidgetManager.getInstance().initPayloadInfo(mqttAndroidClient)
            PerceptionManager.getInstance().initPerceptionInfo()
            GimbalManager.getInstance().initGimbalInfo()
            //这里修改推流逻辑
            Handler().postDelayed(Runnable {
                StreamManager.getInstance()
                    .startLiveWithCustom()
            }, 5000)
            val productType =
                KeyManager.getInstance().getValue(KeyTools.createKey(ProductKey.KeyProductType))
            LogUtil.log(TAG, "设备类型:" + productType!!.name)
            ApronArucoDetect.getInstance().productType = productType!!.name
        }
    }


    private fun initCameraStream() {
        mainBinding?.svCameraStream?.holder?.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {}
            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                cameraManager.putCameraStreamSurface(
                    ComponentIndexType.LEFT_OR_MAIN,
                    holder.surface,
                    width,
                    height,
                    ICameraStreamManager.ScaleType.FIX_XY
                )
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                cameraManager.removeCameraStreamSurface(holder.surface)
            }
        })

        cameraManager.addFrameListener(
            ComponentIndexType.LEFT_OR_MAIN,
            ICameraStreamManager.FrameFormat.YUV420_888
        ) { frameData, _, _, width, height, _ ->
            when (startArucoType) {
                1 ->
                    ApronArucoDetect.getInstance()?.detectArucoTags(
                        height,
                        width,
                        frameData,
                        dictionary,
                    )

                2 ->
                    AlternateArucoDetect.getInstance()?.detectArucoTags(
                        height,
                        width,
                        frameData,
                        dictionary,
                    )
            }
        }
    }

    private val mLoaderCallback: BaseLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            if (status == SUCCESS) {
                LogUtil.log(
                    TAG,
                    "OpenCV loaded successfully"
                )
                LogUtil.log(TAG,"Version Name="+BuildConfig.VERSION_NAME)
            } else {
                super.onManagerConnected(status)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(message: String?) {
        when (message) {
            FLAG_START_DETECT_ARUCO_APRON ->
                KeyManager.getInstance().performAction<EmptyMsg>(
                    KeyTools.createKey<EmptyMsg, EmptyMsg>(FlightControllerKey.KeyStopAutoLanding),
                    object : CommonCallbacks.CompletionCallbackWithParam<EmptyMsg?> {
                        override fun onSuccess(emptyMsg: EmptyMsg?) {
                            LogUtil.log(TAG, "取消降落,识别机库二维码")
                            if (startArucoType == 1) {
                                return
                            }
                            startArucoType = 1
                            ApronArucoDetect.getInstance().setDetectedBigMarkers()
                            DroneHelper.getInstance().setGimbalPitchDegree()
                            //每次触发识别二维码时，为避免获取控制权失败,使多次获取控制权
                            DroneHelper.getInstance().isVirtualStickEnable=false
                            DroneHelper.getInstance().setVerticalModeToVelocity()
                        }

                        override fun onFailure(error: IDJIError) {
                            if (startArucoType == 1) {
                                return
                            }
                            startArucoType = 1
                            LogUtil.log(TAG, "取消降落,识别机库二维码失败:" + Gson().toJson(error))
                            ApronArucoDetect.getInstance().setDetectedBigMarkers()
                            DroneHelper.getInstance().setGimbalPitchDegree()
                            //每次触发识别二维码时，为避免获取控制权失败,使多次获取控制权
                            DroneHelper.getInstance().isVirtualStickEnable=false
                            DroneHelper.getInstance().setVerticalModeToVelocity()
                        }
                    })

            FLAG_START_DETECT_ARUCO_ALTERNATE ->
                KeyManager.getInstance().performAction<EmptyMsg>(
                    KeyTools.createKey<EmptyMsg, EmptyMsg>(FlightControllerKey.KeyStopAutoLanding),
                    object : CommonCallbacks.CompletionCallbackWithParam<EmptyMsg?> {
                        override fun onSuccess(emptyMsg: EmptyMsg?) {
                            LogUtil.log(TAG, "取消降落,识别备降点二维码")
                            if (startArucoType == 2) {
                                return
                            }
                            startArucoType = 2
                            DroneHelper.getInstance().setGimbalPitchDegree()
                            //每次触发识别二维码时，为避免获取控制权失败,使多次获取控制权
                            DroneHelper.getInstance().isVirtualStickEnable=false
                            DroneHelper.getInstance().setVerticalModeToVelocity()
                        }

                        override fun onFailure(error: IDJIError) {
                            if (startArucoType == 2) {
                                return
                            }
                            startArucoType = 2
                            LogUtil.log(
                                TAG,
                                "取消降落,识别备降点二维码失败:" + Gson().toJson(error)
                            )
                            DroneHelper.getInstance().setGimbalPitchDegree()
                            //每次触发识别二维码时，为避免获取控制权失败,使多次获取控制权
                            DroneHelper.getInstance().isVirtualStickEnable=false
                            DroneHelper.getInstance().setVerticalModeToVelocity()
                        }
                    })

            FLAG_DOWN_LAND ->
                KeyManager.getInstance().performAction<EmptyMsg>(
                    KeyTools.createKey<EmptyMsg, EmptyMsg>(FlightControllerKey.KeyStartAutoLanding),
                    object : CommonCallbacks.CompletionCallbackWithParam<EmptyMsg?> {
                        override fun onSuccess(emptyMsg: EmptyMsg?) {
                            startArucoType = 0
                        }
                        override fun onFailure(error: IDJIError) {
                            LogUtil.log(TAG, "自动降落调用失败${error.description()}")
                        }
                    })

            FLAG_STOP_ARUCO ->
                startArucoType = 0

        }
    }
}