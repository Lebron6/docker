package com.aros.apron.activity
import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.aros.apron.BuildConfig
import com.aros.apron.R
import com.aros.apron.base.BaseActivity
import com.aros.apron.callback.MqttCallBack
import com.aros.apron.databinding.ActivityMainBinding
import com.aros.apron.entity.CurrentWayline
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
import com.aros.apron.manager.MLTEManager
import com.aros.apron.manager.MediaManager
import com.aros.apron.manager.MissionManager
import com.aros.apron.manager.OffSiteLandingManager
import com.aros.apron.manager.PayloadWidgetManager
import com.aros.apron.manager.RTKManager
import com.aros.apron.manager.RemoteManager
import com.aros.apron.manager.StickManager
import com.aros.apron.manager.StreamManager
import com.aros.apron.manager.WayLineExecutingInterruptManager
import com.aros.apron.tools.AlternateArucoDetect
import com.aros.apron.tools.ApronArucoDetect
import com.aros.apron.tools.DroneHelper
import com.aros.apron.tools.LogUtil
import com.aros.apron.tools.MqttManager
import com.aros.apron.tools.PreferenceUtils
import com.dji.wpmzsdk.manager.WPMZManager
import com.google.gson.Gson
import dji.sdk.keyvalue.key.CameraKey
import dji.sdk.keyvalue.key.DJIKey
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.key.ProductKey
import dji.sdk.keyvalue.value.common.CameraLensType
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.sdk.keyvalue.value.common.EmptyMsg
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.callback.CommonCallbacks.CompletionCallbackWithProgress
import dji.v5.common.error.IDJIError
import dji.v5.common.utils.GeoidManager
import dji.v5.manager.KeyManager
import dji.v5.manager.aircraft.waypoint3.WaypointMissionManager
import dji.v5.manager.datacenter.MediaDataCenter
import dji.v5.manager.interfaces.ICameraStreamManager
import dji.v5.manager.interfaces.ICameraStreamManager.AvailableCameraUpdatedListener
import dji.v5.manager.interfaces.IWaypointMissionManager
import dji.v5.network.DJINetworkManager
import dji.v5.network.IDJINetworkStatusListener
import dji.v5.utils.common.JsonUtil
import dji.v5.utils.common.LogUtils
import dji.v5.ux.accessory.RTKStartServiceHelper.startRtkService
import dji.v5.ux.cameracore.widget.autoexposurelock.AutoExposureLockWidget
import dji.v5.ux.cameracore.widget.cameracontrols.CameraControlsWidget
import dji.v5.ux.cameracore.widget.cameracontrols.lenscontrol.LensControlWidget
import dji.v5.ux.cameracore.widget.focusexposureswitch.FocusExposureSwitchWidget
import dji.v5.ux.cameracore.widget.focusmode.FocusModeWidget
import dji.v5.ux.cameracore.widget.fpvinteraction.FPVInteractionWidget
import dji.v5.ux.core.base.SchedulerProvider.io
import dji.v5.ux.core.base.SchedulerProvider.ui
import dji.v5.ux.core.communication.BroadcastValues
import dji.v5.ux.core.communication.GlobalPreferenceKeys
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.communication.UXKeys
import dji.v5.ux.core.extension.hide
import dji.v5.ux.core.extension.toggleVisibility
import dji.v5.ux.core.panel.systemstatus.SystemStatusListPanelWidget
import dji.v5.ux.core.panel.topbar.TopBarPanelWidget
import dji.v5.ux.core.util.CameraUtil
import dji.v5.ux.core.util.CommonUtils
import dji.v5.ux.core.util.DataProcessor
import dji.v5.ux.core.util.ViewUtil
import dji.v5.ux.core.widget.fpv.FPVStreamSourceListener
import dji.v5.ux.core.widget.fpv.FPVWidget
import dji.v5.ux.core.widget.hsi.HorizontalSituationIndicatorWidget
import dji.v5.ux.core.widget.hsi.PrimaryFlightDisplayWidget
import dji.v5.ux.core.widget.remainingflighttime.RemainingFlightTimeWidget
import dji.v5.ux.core.widget.setting.SettingWidget
import dji.v5.ux.flight.returnhome.ReturnHomeWidget
import dji.v5.ux.flight.takeoff.TakeOffWidget
import dji.v5.ux.gimbal.GimbalFineTuneWidget
import dji.v5.ux.training.simulatorcontrol.SimulatorControlWidget
import dji.v5.ux.training.simulatorcontrol.SimulatorControlWidget.UIState.VisibilityUpdated
import dji.v5.ux.visualcamera.CameraNDVIPanelWidget
import dji.v5.ux.visualcamera.CameraVisiblePanelWidget
import dji.v5.ux.visualcamera.zoom.FocalZoomWidget
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.functions.Consumer
import org.eclipse.paho.client.mqttv3.MqttException
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.aruco.Aruco
import org.opencv.aruco.Dictionary
import java.util.concurrent.TimeUnit


open class MainActivity : BaseActivity() {


    companion object {
        // 如果不需要改变 isAppStarted 的值，可以直接这样声明
        var isAppStarted: Boolean = false
    }
    private var primaryFpvWidget: FPVWidget? = null
    private var fpvInteractionWidget: FPVInteractionWidget? = null
    private var secondaryFPVWidget: FPVWidget? = null
    private var systemStatusListPanelWidget: SystemStatusListPanelWidget? = null
    private var simulatorControlWidget: SimulatorControlWidget? = null
        private var lensControlWidget: LensControlWidget? = null
    private var autoExposureLockWidget: AutoExposureLockWidget? = null
    private var focusModeWidget: FocusModeWidget? = null
    private var focusExposureSwitchWidget: FocusExposureSwitchWidget? = null
    private var cameraControlsWidget: CameraControlsWidget? = null
        private var takeOffWidget: TakeOffWidget? = null
    private var returnHomeWidget: ReturnHomeWidget? = null
    private var horizontalSituationIndicatorWidget: HorizontalSituationIndicatorWidget? = null
    private var pfvFlightDisplayWidget: PrimaryFlightDisplayWidget? = null
    private var ndviCameraPanel: CameraNDVIPanelWidget? = null
    private var visualCameraPanel: CameraVisiblePanelWidget? = null
    private var focalZoomWidget: FocalZoomWidget? = null
    private var settingWidget: SettingWidget? = null

    //    private var mapWidget: MapWidget? = null
    private var topBarPanel: TopBarPanelWidget? = null
    private var remainingFlightTimeWidget: RemainingFlightTimeWidget? = null
    private var fpvParentView: ConstraintLayout? = null
    private var mDrawerLayout: DrawerLayout? = null
    private var gimbalAdjustDone: TextView? = null
    private var gimbalFineTuneWidget: GimbalFineTuneWidget? = null
    private var lastDevicePosition = ComponentIndexType.UNKNOWN
    private var lastLensType = CameraLensType.UNKNOWN


    private var compositeDisposable: CompositeDisposable? = null
    private val cameraSourceProcessor = DataProcessor.create(
        CameraSource(
            ComponentIndexType.UNKNOWN,
            CameraLensType.UNKNOWN
        )
    )
    private val networkStatusListener =
        IDJINetworkStatusListener { isNetworkAvailable: Boolean ->
            if (isNetworkAvailable) {
                LogUtils.d(TAG, "isNetworkAvailable=" + true)
                startRtkService(false)
            }
        }
    private val availableCameraUpdatedListener =
        AvailableCameraUpdatedListener { availableCameraList: List<ComponentIndexType> ->
            runOnUiThread { updateFPVWidgetSource(availableCameraList) }
        }


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
        compositeDisposable = CompositeDisposable()
        compositeDisposable?.add(
            systemStatusListPanelWidget!!.closeButtonPressed()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { pressed: Boolean ->
                    if (pressed) {
                        systemStatusListPanelWidget!!.hide()
                    }
                })
        compositeDisposable?.add(
            simulatorControlWidget!!.getUIStateUpdates()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { simulatorControlWidgetState: SimulatorControlWidget.UIState ->
                    if (simulatorControlWidgetState is VisibilityUpdated) {
                        if (simulatorControlWidgetState.isVisible) {
                            hideOtherPanels(simulatorControlWidget!!)
                        }
                    }
                })
        compositeDisposable?.add(
            cameraSourceProcessor.toFlowable()
                .observeOn(io())
                .throttleLast(500, TimeUnit.MILLISECONDS)
                .subscribeOn(io())
                .subscribe(Consumer { result: CameraSource ->
                    runOnUiThread { onCameraSourceUpdated(result.devicePosition, result.lensType) }
                })
        )
        compositeDisposable?.add(ObservableInMemoryKeyedStore.getInstance()
            .addObserver(UXKeys.create(GlobalPreferenceKeys.GIMBAL_ADJUST_CLICKED))
            .observeOn(ui())
            .subscribe { broadcastValues: BroadcastValues? ->
                isGimableAdjustClicked(
                    broadcastValues!!
                )
            })
        ViewUtil.setKeepScreen(this, true)
    }

    override fun onPause() {
        if (compositeDisposable != null) {
            compositeDisposable!!.dispose()
            compositeDisposable = null
        }
//        mapWidget!!.onPause()
        super.onPause()
        ViewUtil.setKeepScreen(this, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isAppStarted = true

        requestWindowFeature(Window.FEATURE_NO_TITLE)
//        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mainBinding!!.root)
//        mapWidget = findViewById<MapWidget>(R.id.widget_map)
//        mapWidget?.initMapLibreMap(applicationContext, OnMapReadyListener { map: DJIMap ->
//            val uiSetting = map.uiSettings
//            uiSetting?.setZoomControlsEnabled(false)
//        })
//        mapWidget?.onCreate(savedInstanceState)
        GeoidManager.getInstance().init(this)
        WPMZManager.getInstance().init(this)
        //连接mqtt
        MqttManager.getInstance().needConnect()

        initDJIManager()
        initCameraStream()
        initView()
    }

    @SuppressLint("SuspiciousIndentation")
    private fun initView() {
        fpvParentView = findViewById<ConstraintLayout>(R.id.fpv_holder)
        mDrawerLayout = findViewById<DrawerLayout>(R.id.root_view)
        topBarPanel = findViewById<TopBarPanelWidget>(R.id.panel_top_bar)
        remainingFlightTimeWidget =
            findViewById<RemainingFlightTimeWidget>(R.id.widget_remaining_flight_time)
        settingWidget = topBarPanel?.settingWidget
        primaryFpvWidget = findViewById<FPVWidget>(R.id.widget_primary_fpv)
        takeOffWidget = findViewById<TakeOffWidget>(R.id.widget_take_off)
        returnHomeWidget = findViewById<ReturnHomeWidget>(R.id.widget_return_to_home)
        primaryFpvWidget = findViewById<FPVWidget>(R.id.widget_primary_fpv)
        fpvInteractionWidget = findViewById<FPVInteractionWidget>(R.id.widget_fpv_interaction)
        secondaryFPVWidget = findViewById<FPVWidget>(R.id.widget_secondary_fpv)
        systemStatusListPanelWidget =
            findViewById<SystemStatusListPanelWidget>(R.id.widget_panel_system_status_list)
        simulatorControlWidget = findViewById<SimulatorControlWidget>(R.id.widget_simulator_control)
        lensControlWidget = findViewById<LensControlWidget>(R.id.widget_lens_control)
        ndviCameraPanel = findViewById<CameraNDVIPanelWidget>(R.id.panel_ndvi_camera)
        visualCameraPanel = findViewById<CameraVisiblePanelWidget>(R.id.panel_visual_camera)
        autoExposureLockWidget =
            findViewById<AutoExposureLockWidget>(R.id.widget_auto_exposure_lock)
        focusModeWidget = findViewById<FocusModeWidget>(R.id.widget_focus_mode)
        focusExposureSwitchWidget =
            findViewById<FocusExposureSwitchWidget>(R.id.widget_focus_exposure_switch)
        pfvFlightDisplayWidget =
            findViewById<PrimaryFlightDisplayWidget>(R.id.widget_fpv_flight_display_widget)
        focalZoomWidget = findViewById<FocalZoomWidget>(R.id.widget_focal_zoom)
        cameraControlsWidget = findViewById<CameraControlsWidget>(R.id.widget_camera_controls)
        horizontalSituationIndicatorWidget =
            findViewById<HorizontalSituationIndicatorWidget>(R.id.widget_horizontal_situation_indicator)
//        gimbalAdjustDone = findViewById<TextView>(R.id.fpv_gimbal_ok_btn)
      var  btn_test = findViewById<TextView>(R.id.btn_test)
        btn_test.setOnClickListener {


//            var message=MQMessage().apply {
//                msg_type=60666
//                upload_url="http://223.108.157.174:9000"
//                bucketName="test"
//                access_key="admin"
//                secret_key="admin123"
//                objectKey="log"
//            }
//            AMSLogManager.getInstance().enableLogList(mqttAndroidClient,message)

//            var message=MQMessage().apply {
//                msg_type=60125
//                zoomTargetX=0.6
//                zoomTargetY=0.6
//                zoom=4.0
//            }
//CameraManager.getInstance().tapZoomAtTarget(mqttAndroidClient,message)
//
//            val zoomTargetPointInfo = ZoomTargetPointInfo()
//            zoomTargetPointInfo.x = 0.3
//            zoomTargetPointInfo.y = 0.3


//            zoomTargetPointInfo.tapZoomModeEnable=true
//            zoomTargetPointInfo.mode=TapZoomMode.GIMBAL_FOLLOW
//            KeyManager.getInstance().performAction(KeyTools.createCameraKey(CameraKey.KeyTapZoomAtTarget,
//                ComponentIndexType.LEFT_OR_MAIN,
//                CameraLensType.CAMERA_LENS_ZOOM),
//                zoomTargetPointInfo,
//                 object : CompletionCallbackWithParam<EmptyMsg?> {
//                override fun onSuccess(p0: EmptyMsg?) {
//                }
//
//                override fun onFailure(error: IDJIError) {
//                    LogUtil.log(TAG, "指点对焦失败:" + Gson().toJson(error))
//
//                }
//            })


        }
        gimbalFineTuneWidget =
            findViewById<GimbalFineTuneWidget>(R.id.setting_menu_gimbal_fine_tune)

        initClickListener()
        MediaDataCenter.getInstance().cameraStreamManager.addAvailableCameraUpdatedListener(
            availableCameraUpdatedListener
        )
        primaryFpvWidget?.setOnFPVStreamSourceListener(object : FPVStreamSourceListener {
            override fun onStreamSourceUpdated(
                devicePosition: ComponentIndexType,
                lensType: CameraLensType
            ) {
                cameraSourceProcessor.onNext(
                    CameraSource(devicePosition, lensType)
                )
            }
        })

        //小surfaceView放置在顶部，避免被大的遮挡
        secondaryFPVWidget?.setSurfaceViewZOrderOnTop(true)
        secondaryFPVWidget?.setSurfaceViewZOrderMediaOverlay(true)
        window.setBackgroundDrawable(ColorDrawable(Color.BLACK))
        //实现RTK监测网络，并自动重连机制
        DJINetworkManager.getInstance().addNetworkStatusListener(networkStatusListener)

    }

    private val handler: Handler = Handler(Looper.getMainLooper())
    private var initTimes=0
    private fun initDJIManager() {
        val isFlightControllerConnect =
            KeyManager.getInstance().getValue(DJIKey.create(FlightControllerKey.KeyConnection))
        if (isFlightControllerConnect == null || !isFlightControllerConnect) {
            handler.postDelayed({
                initDJIManager()
            }, 1000)
        } else {
            initTimes++
            LogUtil.log(TAG, "初始化$initTimes")
            RTKManager.getInstance().initRTKInfo()
            StreamManager.getInstance().initStreamManager()
            FlightManager.getInstance().initFlightInfo()
            MissionManager.getInstance().initMissionManager()
            BatteryManager.getInstance().initBatteryInfo()
            MediaManager.getInstance().init()
            LEDsSettingsManager.getInstance().initLEDsInfo()
            AlternateLandingManager.getInstance().initAlterLandingInfo()
            WayLineExecutingInterruptManager.getInstance().initWayLineExecutingInterruptInfo()
            CameraManager.getInstance().initCameraInfo()
            StickManager.getInstance().initStickInfo()
            GimbalManager.getInstance().initGimbalInfo()
            OffSiteLandingManager.getInstance().initOffSiteLandingInfo()
            RemoteManager.getInstance().initRemoteInfo()
            ApronArucoDetect.getInstance().init()
            PayloadWidgetManager.getInstance().initPayloadInfo()

            if (PreferenceUtils.getInstance().lteEnable){
                MLTEManager.getInstance().initLTEManager()
                Handler().postDelayed(Runnable {  MLTEManager.getInstance().setLTEEnhancedTransmissionType()},3000)

            }
            //这里修改推流逻辑
            if (PreferenceUtils.getInstance().customStreamType!=3) {
                Handler().postDelayed(Runnable {
                    if (PreferenceUtils.getInstance().customStreamType==1){
                        StreamManager.getInstance()
                            .startLiveWithRTSP()
                    }else if (PreferenceUtils.getInstance().customStreamType==2){
                        StreamManager.getInstance()
                            .startLiveWithCustom()
                    }else{
                        LogUtil.log(TAG,"推流方式配置有误")
                    }

                }, 5000)
            }else if(!TextUtils.isEmpty(PreferenceUtils.getInstance().customStreamUrl)){
                Handler().postDelayed(Runnable {
                        StreamManager.getInstance()
                            .startLiveWithCustom()
                }, 5000)
            }
            val productType =
                KeyManager.getInstance().getValue(KeyTools.createKey(ProductKey.KeyProductType))
            val cameraType = KeyManager.getInstance().getValue(
                KeyTools.createKey(
                    CameraKey.KeyCameraType,
                    ComponentIndexType.LEFT_OR_MAIN
                )
            )

            if (cameraType != null && productType != null) {
                LogUtil.log(TAG, "设备类型:" + productType.name + "相机类型:" + cameraType.name)
            } else {
                LogUtil.log(TAG, "设备类型:" + (productType?.name ?: "未知") + "相机类型:" + (cameraType?.name ?: "未知"))
            }
        }
    }

//    var shouldExecute = true

    private fun initCameraStream() {
//        mainBinding?.svCameraStream?.holder?.addCallback(object : SurfaceHolder.Callback {
//            override fun surfaceCreated(holder: SurfaceHolder) {}
//            override fun surfaceChanged(
//                holder: SurfaceHolder,
//                format: Int,
//                width: Int,
//                height: Int
//            ) {
//                cameraManager.putCameraStreamSurface(
//                    ComponentIndexType.LEFT_OR_MAIN,
//                    holder.surface,
//                    width,
//                    height,
//                    ICameraStreamManager.ScaleType.FIX_XY
//                )
//            }
//
//            override fun surfaceDestroyed(holder: SurfaceHolder) {
//                cameraManager.removeCameraStreamSurface(holder.surface)
//            }
//        })

        cameraManager.addFrameListener(
            ComponentIndexType.LEFT_OR_MAIN,
            ICameraStreamManager.FrameFormat.YUV420_888
        ) { frameData, _, _, width, height, _ ->
//            if (shouldExecute) {
                if (startArucoType == 1) {

                    ApronArucoDetect.getInstance()?.detectArucoTags(
                        height,
                        width,
                        frameData,
                        dictionary,
                    )


                } else if (startArucoType == 2) {
                    AlternateArucoDetect.getInstance()?.detectArucoTags(
                        height,
                        width,
                        frameData,
                        dictionary,
                    )
                }
//            }
//            shouldExecute = !shouldExecute

        }
    }

    private val mLoaderCallback: BaseLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            if (status == SUCCESS) {
                LogUtil.log(TAG,"Version Name="+BuildConfig.VERSION_NAME)
                Movement.getInstance().version=BuildConfig.VERSION_NAME
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
                            Handler().postDelayed(Runnable {
                                if (!ApronArucoDetect.getInstance().isTriggerSuccess) {
                                    LogUtil.log(TAG, "图传异常:飞往备降点")
                                    //测试图传丢失
                                    AlternateLandingManager.getInstance().startTaskProcess(null)
                                }
                            }, 6000)
                            if (startArucoType == 1) {
                                return
                            }
                            startArucoType = 1
                            ApronArucoDetect.getInstance().setDetectedBigMarkers()
                            DroneHelper.getInstance().setGimbalPitchDegree()
                            //每次触发识别二维码时，为避免获取控制权失败,使多次获取控制权
                            DroneHelper.getInstance().isVirtualStickEnable = false
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
                            Handler().postDelayed(Runnable {
                                if (!AlternateArucoDetect.getInstance().isTriggerSuccess) {
                                    LogUtil.log(TAG, "图传异常:备降点直接降落")
                                    //测试图传丢失
                                    FlightManager.getInstance().startAutoLanding(null, null)
                                }
                            }, 4000)
                            if (startArucoType == 2) {
                                return
                            }
                            startArucoType = 2
                            DroneHelper.getInstance().setGimbalPitchDegree()
                            //每次触发识别二维码时，为避免获取控制权失败,使多次获取控制权
                            DroneHelper.getInstance().isVirtualStickEnable = false
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

            MqttCallBack.FLAG_RESET_CLEAN_MODE ->
                setViewVisibilityWithCleanMode()
        }
    }


    private fun isGimableAdjustClicked(broadcastValues: BroadcastValues) {
        if (mDrawerLayout!!.isDrawerOpen(GravityCompat.END)) {
            mDrawerLayout!!.closeDrawers()
        }
        horizontalSituationIndicatorWidget!!.visibility = View.GONE
        if (gimbalFineTuneWidget != null) {
            gimbalFineTuneWidget!!.visibility = View.VISIBLE
        }
    }

    private fun initClickListener() {
        secondaryFPVWidget!!.setOnClickListener { v: View? -> swapVideoSource() }
        if (settingWidget != null) {
            settingWidget!!.setOnClickListener { v: View? -> toggleRightDrawer() }
        }

        // Setup top bar state callbacks
        val systemStatusWidget = topBarPanel!!.systemStatusWidget
        systemStatusWidget?.setOnClickListener { v: View? -> systemStatusListPanelWidget!!.toggleVisibility() }
        val simulatorIndicatorWidget = topBarPanel!!.simulatorIndicatorWidget
        simulatorIndicatorWidget?.setOnClickListener { v: View? -> simulatorControlWidget!!.toggleVisibility() }
//        gimbalAdjustDone!!.setOnClickListener { view: View? ->
//            horizontalSituationIndicatorWidget!!.visibility = View.VISIBLE
//            if (gimbalFineTuneWidget != null) {
//                gimbalFineTuneWidget!!.visibility = View.GONE
//            }
//        }
    }

    private fun toggleRightDrawer() {
        mDrawerLayout!!.openDrawer(GravityCompat.END)
    }

    //endregion
    private fun hideOtherPanels(widget: View) {
        val panels = arrayOf(
            simulatorControlWidget
        )

        for (panel in panels) {
            if (widget !== panel) {
                panel?.visibility = View.GONE
            }
        }
    }

    private fun updateFPVWidgetSource(availableCameraList: List<ComponentIndexType>) {
        LogUtils.i(TAG, JsonUtil.toJson(availableCameraList))
        if (availableCameraList == null) {
            return
        }
        val cameraList = ArrayList(availableCameraList)

        //没有数据
        if (cameraList.isEmpty()) {
            secondaryFPVWidget!!.visibility = View.GONE
            return
        }

        //仅一路数据
        if (cameraList.size == 1) {
            primaryFpvWidget!!.updateVideoSource(availableCameraList[0])
            secondaryFPVWidget!!.visibility = View.GONE
            return
        }

        //大于两路数据
        val primarySource = getSuitableSource(cameraList, ComponentIndexType.LEFT_OR_MAIN)
        primaryFpvWidget!!.updateVideoSource(primarySource)
        cameraList.remove(primarySource)
        val secondarySource = getSuitableSource(cameraList, ComponentIndexType.FPV)
        secondaryFPVWidget!!.updateVideoSource(secondarySource)
        secondaryFPVWidget!!.visibility = View.VISIBLE
    }

    private fun getSuitableSource(
        cameraList: List<ComponentIndexType>,
        defaultSource: ComponentIndexType
    ): ComponentIndexType {
        if (cameraList.contains(ComponentIndexType.LEFT_OR_MAIN)) {
            return ComponentIndexType.LEFT_OR_MAIN
        } else if (cameraList.contains(ComponentIndexType.RIGHT)) {
            return ComponentIndexType.RIGHT
        } else if (cameraList.contains(ComponentIndexType.UP)) {
            return ComponentIndexType.UP
        }
        return defaultSource
    }

    private fun onCameraSourceUpdated(
        devicePosition: ComponentIndexType,
        lensType: CameraLensType
    ) {
        LogUtils.i(TAG, devicePosition, lensType)
        if (devicePosition == lastDevicePosition && lensType == lastLensType) {
            return
        }
        lastDevicePosition = devicePosition
        lastLensType = lensType
        updateViewVisibility(devicePosition, lensType)
        updateInteractionEnabled()
        //如果无需使能或者显示的，也就没有必要切换了。
        if (fpvInteractionWidget!!.isInteractionEnabled) {
            fpvInteractionWidget!!.updateCameraSource(devicePosition, lensType)
            fpvInteractionWidget!!.updateGimbalIndex(CommonUtils.getGimbalIndex(devicePosition))
        }
//        if (lensControlWidget!!.visibility == View.VISIBLE) {
//            lensControlWidget!!.updateCameraSource(devicePosition, lensType)
//        }
        if (ndviCameraPanel!!.visibility == View.VISIBLE) {
            ndviCameraPanel!!.updateCameraSource(devicePosition, lensType)
        }
        if (visualCameraPanel!!.visibility == View.VISIBLE) {
            visualCameraPanel!!.updateCameraSource(devicePosition, lensType)
        }
        if (autoExposureLockWidget!!.visibility == View.VISIBLE) {
            autoExposureLockWidget!!.updateCameraSource(devicePosition, lensType)
        }
        if (focusModeWidget!!.visibility == View.VISIBLE) {
            focusModeWidget!!.updateCameraSource(devicePosition, lensType)
        }
        if (focusExposureSwitchWidget!!.visibility == View.VISIBLE) {
            focusExposureSwitchWidget!!.updateCameraSource(devicePosition, lensType)
        }
        if (cameraControlsWidget!!.visibility == View.VISIBLE) {
            cameraControlsWidget!!.updateCameraSource(devicePosition, lensType)
        }
        if (focalZoomWidget!!.visibility == View.VISIBLE) {
            focalZoomWidget!!.updateCameraSource(devicePosition, lensType)
        }
        if (horizontalSituationIndicatorWidget!!.visibility == View.VISIBLE) {
            horizontalSituationIndicatorWidget!!.updateCameraSource(devicePosition, lensType)
        }
        setViewVisibilityWithCleanMode()
    }

    private fun setViewVisibilityWithCleanMode() {
        if (PreferenceUtils.getInstance().isCleanMode) {
            fpvInteractionWidget?.visibility = View.GONE
            horizontalSituationIndicatorWidget?.visibility = View.GONE
            remainingFlightTimeWidget?.visibility=View.GONE
            gimbalFineTuneWidget?.visibility = View.GONE
            ndviCameraPanel?.visibility = View.GONE
            visualCameraPanel?.visibility = View.GONE
            autoExposureLockWidget?.visibility = View.GONE
            focusModeWidget?.visibility = View.GONE
            focusExposureSwitchWidget?.visibility = View.GONE
            cameraControlsWidget?.visibility = View.GONE
            focalZoomWidget?.visibility = View.GONE
            returnHomeWidget?.visibility = View.GONE
            takeOffWidget?.visibility = View.GONE
            lensControlWidget?.visibility = View.GONE
            simulatorControlWidget?.visibility = View.GONE
            pfvFlightDisplayWidget?.visibility = View.GONE
            systemStatusListPanelWidget?.visibility = View.GONE
            topBarPanel?.visibility = View.GONE
        }
    }

    private fun updateViewVisibility(devicePosition: ComponentIndexType, lensType: CameraLensType) {
        //只在fpv下显示
        pfvFlightDisplayWidget!!.visibility =
            if (devicePosition == ComponentIndexType.FPV) View.VISIBLE else View.INVISIBLE

        //fpv下不显示
//        lensControlWidget!!.visibility =
//            if (devicePosition == ComponentIndexType.FPV) View.INVISIBLE else View.VISIBLE
        ndviCameraPanel!!.visibility =
            if (devicePosition == ComponentIndexType.FPV) View.INVISIBLE else View.VISIBLE
        visualCameraPanel!!.visibility =
            if (devicePosition == ComponentIndexType.FPV) View.INVISIBLE else View.VISIBLE
        autoExposureLockWidget!!.visibility =
            if (devicePosition == ComponentIndexType.FPV) View.INVISIBLE else View.VISIBLE
        focusModeWidget!!.visibility =
            if (devicePosition == ComponentIndexType.FPV) View.INVISIBLE else View.VISIBLE
        focusExposureSwitchWidget!!.visibility =
            if (devicePosition == ComponentIndexType.FPV) View.INVISIBLE else View.VISIBLE
        cameraControlsWidget!!.visibility =
            if (devicePosition == ComponentIndexType.FPV) View.INVISIBLE else View.VISIBLE
        focalZoomWidget!!.visibility =
            if (devicePosition == ComponentIndexType.FPV) View.INVISIBLE else View.VISIBLE
        horizontalSituationIndicatorWidget!!.setSimpleModeEnable(devicePosition != ComponentIndexType.FPV)

        //只在部分len下显示
        ndviCameraPanel!!.visibility =
            if (CameraUtil.isSupportForNDVI(lensType)) View.VISIBLE else View.INVISIBLE


    }

    /**
     * Swap the video sources of the FPV and secondary FPV widgets.
     */
    private fun swapVideoSource() {
        val primarySource = primaryFpvWidget!!.widgetModel.getCameraIndex()
        val secondarySource = secondaryFPVWidget!!.widgetModel.getCameraIndex()
        //两个source都存在的情况下才进行切换
        if (primarySource != ComponentIndexType.UNKNOWN && secondarySource != ComponentIndexType.UNKNOWN) {
            primaryFpvWidget!!.updateVideoSource(secondarySource)
            secondaryFPVWidget!!.updateVideoSource(primarySource)
        }
    }

    private fun updateInteractionEnabled() {
        fpvInteractionWidget!!.isInteractionEnabled =
            primaryFpvWidget!!.widgetModel.getCameraIndex() != ComponentIndexType.FPV
    }

    private class CameraSource(var devicePosition: ComponentIndexType, var lensType: CameraLensType)

    override fun onBackPressed() {
        if (mDrawerLayout!!.isDrawerOpen(GravityCompat.END)) {
            mDrawerLayout!!.closeDrawers()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isAppStarted = false

    }


}
