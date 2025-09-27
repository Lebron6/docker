package com.aros.apron.activity
import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.aros.apron.BuildConfig
import com.aros.apron.R
import com.aros.apron.base.BaseActivity
import com.aros.apron.callback.MqttCallBack
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
import dji.v5.common.error.IDJIError
import dji.v5.common.utils.GeoidManager
import dji.v5.manager.KeyManager
import dji.v5.manager.datacenter.MediaDataCenter
import dji.v5.manager.interfaces.ICameraStreamManager
import dji.v5.manager.interfaces.ICameraStreamManager.AvailableCameraUpdatedListener
import dji.v5.network.DJINetworkManager
import dji.v5.network.IDJINetworkStatusListener
import dji.v5.utils.common.JsonUtil
import dji.v5.utils.common.LogPath
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
import dji.v5.ux.core.util.DataProcessor
import dji.v5.ux.core.util.ViewUtil
import dji.v5.ux.core.widget.fpv.FPVStreamSourceListener
import dji.v5.ux.core.widget.fpv.FPVWidget
import dji.v5.ux.core.widget.hsi.HorizontalSituationIndicatorWidget
import dji.v5.ux.core.widget.hsi.PrimaryFlightDisplayWidget
import dji.v5.ux.core.widget.setting.SettingWidget
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
        var streamReceive: Boolean = false

    }

    protected var primaryFpvWidget: FPVWidget? = null
    protected var fpvInteractionWidget: FPVInteractionWidget? = null
    protected var secondaryFPVWidget: FPVWidget? = null
    protected var systemStatusListPanelWidget: SystemStatusListPanelWidget? = null
    protected var simulatorControlWidget: SimulatorControlWidget? = null
    protected var lensControlWidget: LensControlWidget? = null
    protected var autoExposureLockWidget: AutoExposureLockWidget? = null
    protected var focusModeWidget: FocusModeWidget? = null
    protected var focusExposureSwitchWidget: FocusExposureSwitchWidget? = null
    protected var cameraControlsWidget: CameraControlsWidget? = null
    protected var horizontalSituationIndicatorWidget: HorizontalSituationIndicatorWidget? = null
    protected var pfvFlightDisplayWidget: PrimaryFlightDisplayWidget? = null
    protected var ndviCameraPanel: CameraNDVIPanelWidget? = null
    protected var visualCameraPanel: CameraVisiblePanelWidget? = null
    protected var focalZoomWidget: FocalZoomWidget? = null
    protected var settingWidget: SettingWidget? = null
    protected var topBarPanel: TopBarPanelWidget? = null
    protected var fpvParentView: ConstraintLayout? = null
    private var mDrawerLayout: DrawerLayout? = null
    private var gimbalAdjustDone: TextView? = null
    private var btn_test: Button? = null
    private var btn_test1: Button? = null

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

    private val availableCameraUpdatedListener: AvailableCameraUpdatedListener =
        object : AvailableCameraUpdatedListener {
            override fun onAvailableCameraUpdated(availableCameraList: List<ComponentIndexType>) {
                runOnUiThread { updateFPVWidgetSource(availableCameraList) }
            }

            override fun onCameraStreamEnableUpdate(cameraStreamEnableMap: Map<ComponentIndexType, Boolean>) {
                //
            }
        }


    private var cameraManager: ICameraStreamManager = MediaDataCenter.getInstance().cameraStreamManager

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
        compositeDisposable!!.add(
            systemStatusListPanelWidget!!.closeButtonPressed()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { pressed: Boolean ->
                    if (pressed) {
                        systemStatusListPanelWidget!!.hide()
                    }
                })
        compositeDisposable!!.add(
            simulatorControlWidget!!.getUIStateUpdates()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { simulatorControlWidgetState: SimulatorControlWidget.UIState? ->
                    if (simulatorControlWidgetState is VisibilityUpdated) {
                        if ((simulatorControlWidgetState as VisibilityUpdated).isVisible) {
                            hideOtherPanels(simulatorControlWidget!!)
                        }
                    }
                })
        compositeDisposable!!.add(
            cameraSourceProcessor.toFlowable()
                .observeOn(io())
                .throttleLast(500, TimeUnit.MILLISECONDS)
                .subscribeOn(io())
                .subscribe(Consumer { result: CameraSource ->
                    runOnUiThread { onCameraSourceUpdated(result.devicePosition, result.lensType) }
                })
        )
        compositeDisposable!!.add(ObservableInMemoryKeyedStore.getInstance()
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
        super.onPause()
        ViewUtil.setKeepScreen(this, false)
    }

    //endregion
    private  fun hideOtherPanels(widget: View?) {
        val panels = arrayOf<View>(
            simulatorControlWidget!!
        )
        for (panel in panels) {
            if (widget !== panel) {
                panel.visibility = View.GONE
            }
        }
    }

    private fun updateFPVWidgetSource(availableCameraList: List<ComponentIndexType>?) {
        LogUtils.i(TAG, JsonUtil.toJson(availableCameraList))
        if (availableCameraList == null) {
            return
        }
        val cameraList = java.util.ArrayList(availableCameraList)

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
        primaryFpvWidget!!.updateVideoSource(primarySource!!)
        cameraList.remove(primarySource)

        val secondarySource = getSuitableSource(cameraList, ComponentIndexType.FPV)
        secondaryFPVWidget!!.updateVideoSource(secondarySource!!)

        secondaryFPVWidget!!.visibility = View.VISIBLE


    }

    private fun getSuitableSource(
        cameraList: List<ComponentIndexType>,
        defaultSource: ComponentIndexType
    ): ComponentIndexType? {
        if (cameraList.contains(ComponentIndexType.LEFT_OR_MAIN)) {
            return ComponentIndexType.LEFT_OR_MAIN
        } else if (cameraList.contains(ComponentIndexType.RIGHT)) {
            return ComponentIndexType.RIGHT
        } else if (cameraList.contains(ComponentIndexType.UP)) {
            return ComponentIndexType.UP
        } else if (cameraList.contains(ComponentIndexType.PORT_1)) {
            return ComponentIndexType.PORT_1
        } else if (cameraList.contains(ComponentIndexType.PORT_2)) {
            return ComponentIndexType.PORT_2
        } else if (cameraList.contains(ComponentIndexType.PORT_3)) {
            return ComponentIndexType.PORT_3
        } else if (cameraList.contains(ComponentIndexType.PORT_4)) {
            return ComponentIndexType.PORT_4
        } else if (cameraList.contains(ComponentIndexType.VISION_ASSIST)) {
            return ComponentIndexType.VISION_ASSIST
        }
        return defaultSource
    }

    private fun onCameraSourceUpdated(
        devicePosition: ComponentIndexType,
        lensType: CameraLensType
    ) {
        LogUtils.i(LogPath.SAMPLE, "onCameraSourceUpdated", devicePosition, lensType)
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
        }
        if (lensControlWidget!!.visibility == View.VISIBLE) {
            lensControlWidget!!.updateCameraSource(devicePosition, lensType)
        }
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
    }

    private  fun updateViewVisibility(
        devicePosition: ComponentIndexType,
        lensType: CameraLensType
    ) {
        //只在fpv下显示
        pfvFlightDisplayWidget!!.visibility =
            if (CameraUtil.isFPVTypeView(devicePosition)) View.VISIBLE else View.INVISIBLE

        //fpv下不显示
        lensControlWidget!!.visibility =
            if (CameraUtil.isFPVTypeView(devicePosition)) View.INVISIBLE else View.VISIBLE
        ndviCameraPanel!!.visibility =
            if (CameraUtil.isFPVTypeView(devicePosition)) View.INVISIBLE else View.VISIBLE
        visualCameraPanel!!.visibility =
            if (CameraUtil.isFPVTypeView(devicePosition)) View.INVISIBLE else View.VISIBLE
        autoExposureLockWidget!!.visibility =
            if (CameraUtil.isFPVTypeView(devicePosition)) View.INVISIBLE else View.VISIBLE
        focusModeWidget!!.visibility =
            if (CameraUtil.isFPVTypeView(devicePosition)) View.INVISIBLE else View.VISIBLE
        focusExposureSwitchWidget!!.visibility =
            if (CameraUtil.isFPVTypeView(devicePosition)) View.INVISIBLE else View.VISIBLE
        cameraControlsWidget!!.visibility =
            if (CameraUtil.isFPVTypeView(devicePosition)) View.INVISIBLE else View.VISIBLE
        focalZoomWidget!!.visibility =
            if (CameraUtil.isFPVTypeView(devicePosition)) View.INVISIBLE else View.VISIBLE
        horizontalSituationIndicatorWidget!!.setSimpleModeEnable(
            CameraUtil.isFPVTypeView(
                devicePosition
            )
        )

        //只在部分len下显示
        ndviCameraPanel!!.visibility =
            if (CameraUtil.isSupportForNDVI(lensType)) View.VISIBLE else View.INVISIBLE
    }

    /**
     * Swap the video sources of the FPV and secondary FPV widgets.
     */
    private  fun swapVideoSource() {
        val primarySource = primaryFpvWidget!!.widgetModel.getCameraIndex()
        val secondarySource = secondaryFPVWidget!!.widgetModel.getCameraIndex()
        //两个source都存在的情况下才进行切换
        if (primarySource != ComponentIndexType.UNKNOWN && secondarySource != ComponentIndexType.UNKNOWN) {
            primaryFpvWidget!!.updateVideoSource(secondarySource)
            secondaryFPVWidget!!.updateVideoSource(primarySource)
        }
    }

    private fun updateInteractionEnabled() {
        fpvInteractionWidget!!.isInteractionEnabled = !CameraUtil.isFPVTypeView(
            primaryFpvWidget!!.widgetModel.getCameraIndex()
        )
    }

    private data class CameraSource(
        val devicePosition: ComponentIndexType,
        val lensType: CameraLensType
    )
    override fun onBackPressed() {
        if (mDrawerLayout!!.isDrawerOpen(GravityCompat.END)) {
            mDrawerLayout!!.closeDrawers()
        } else {
            super.onBackPressed()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
//        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_main)
        isAppStarted = true


//        mapWidget = findViewById<MapWidget>(R.id.widget_map)
//        mapWidget?.initMapLibreMap(applicationContext, OnMapReadyListener { map: DJIMap ->
//            val uiSetting = map.uiSettings
//            uiSetting?.setZoomControlsEnabled(false)
//        })
//        mapWidget?.onCreate(savedInstanceState)
        GeoidManager.getInstance().init(this)
        WPMZManager.getInstance().init(this)
        MqttManager.getInstance().needConnect()

        initDJIManager()
        initCameraManager()
        initCameraStream()
        initView()
    }

    private fun initView() {
        fpvParentView = findViewById( R.id.fpv_holder)
        mDrawerLayout = findViewById( R.id.root_view)
        topBarPanel = findViewById( R.id.panel_top_bar)
        settingWidget = topBarPanel?.settingWidget
        primaryFpvWidget = findViewById( R.id.widget_primary_fpv)
        fpvInteractionWidget = findViewById( R.id.widget_fpv_interaction)
        secondaryFPVWidget = findViewById( R.id.widget_secondary_fpv)
        systemStatusListPanelWidget = findViewById( R.id.widget_panel_system_status_list)
        simulatorControlWidget = findViewById( R.id.widget_simulator_control)
        lensControlWidget = findViewById<LensControlWidget>( R.id.widget_lens_control)
        ndviCameraPanel = findViewById( R.id.panel_ndvi_camera)
        visualCameraPanel = findViewById( R.id.panel_visual_camera)
        autoExposureLockWidget = findViewById( R.id.widget_auto_exposure_lock)
        focusModeWidget = findViewById( R.id.widget_focus_mode)
        focusExposureSwitchWidget = findViewById( R.id.widget_focus_exposure_switch)
        pfvFlightDisplayWidget = findViewById( R.id.widget_fpv_flight_display_widget)
        focalZoomWidget = findViewById( R.id.widget_focal_zoom)
        cameraControlsWidget = findViewById( R.id.widget_camera_controls)
        horizontalSituationIndicatorWidget =
            findViewById(R.id.widget_horizontal_situation_indicator)
//        gimbalAdjustDone = findViewById<TextView>( R.id.fpv_gimbal_ok_btn)
        gimbalFineTuneWidget = findViewById( R.id.setting_menu_gimbal_fine_tune)
        btn_test = findViewById( R.id.btn_test)
        btn_test1 = findViewById( R.id.btn_test1)
        btn_test?.setOnClickListener {
//            MissionManager.getInstance().test()
            FlightManager.getInstance().startPropellerRotation(null)
        }
        btn_test1?.setOnClickListener {
            FlightManager.getInstance().stopPropellerRotation(null)
        }

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

    private  fun isGimableAdjustClicked(broadcastValues: BroadcastValues) {
        if (mDrawerLayout!!.isDrawerOpen(GravityCompat.END)) {
            mDrawerLayout!!.closeDrawers()
        }
        horizontalSituationIndicatorWidget!!.visibility = View.GONE
        if (gimbalFineTuneWidget != null) {
            gimbalFineTuneWidget!!.visibility = View.VISIBLE
        }
    }

    private  fun initClickListener() {
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

    private  fun toggleRightDrawer() {
        mDrawerLayout!!.openDrawer(GravityCompat.END)
    }


    override fun onDestroy() {
        super.onDestroy()
        isAppStarted = false
        try {
            if (MqttManager.getInstance().mqttAndroidClient != null && MqttManager.getInstance().mqttAndroidClient.isConnected) {
                MqttManager.getInstance().mqttAndroidClient.unregisterResources()
                MqttManager.getInstance().mqttAndroidClient.disconnect() //断开连接
            }
        } catch (e: MqttException) {
            e.printStackTrace()
        }
        MediaDataCenter.getInstance().cameraStreamManager.removeAvailableCameraUpdatedListener(
            availableCameraUpdatedListener
        )
        DJINetworkManager.getInstance().removeNetworkStatusListener(networkStatusListener)
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
                    ComponentIndexType.PORT_1
                )
            )

            if (cameraType != null && productType != null) {
                LogUtil.log(TAG, "设备类型:" + productType.name + "相机类型:" + cameraType.name)
            } else {
                LogUtil.log(TAG, "设备类型:" + (productType?.name ?: "未知") + "相机类型:" + (cameraType?.name ?: "未知"))
            }
        }
    }
    private val cameraHandler: Handler = Handler(Looper.getMainLooper())
    private var initCameraTimes=0
    private fun initCameraManager() {
        val isConnect = KeyManager.getInstance()
            .getValue(KeyTools.createKey(CameraKey.KeyConnection, ComponentIndexType.PORT_1))

        if (isConnect == null || !isConnect) {
            cameraHandler.postDelayed({
                initDJIManager()
            }, 1000)
        } else {
            initCameraTimes++
            LogUtil.log(TAG, "初始化相机$initCameraTimes")

            CameraManager.getInstance().initCameraInfo()
        }
    }

//    var shouldExecute = true

    @SuppressLint("SuspiciousIndentation")
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
//                    ComponentIndexType.PORT_1,
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
            ComponentIndexType.PORT_1,
            ICameraStreamManager.FrameFormat.YUV420_888
        ) { frameData, _, _, width, height, _ ->
            Movement.getInstance().isVtx=true
            //检测到图传
            streamReceive=true
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
                ApronArucoDetect.getInstance().init()

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
                                    FlightManager.getInstance().startAutoLanding(null)
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




    private fun setViewVisibilityWithCleanMode() {
        if (PreferenceUtils.getInstance().isCleanMode) {
            fpvInteractionWidget?.visibility = View.GONE
            horizontalSituationIndicatorWidget?.visibility = View.GONE
            gimbalFineTuneWidget?.visibility = View.GONE
            ndviCameraPanel?.visibility = View.GONE
            visualCameraPanel?.visibility = View.GONE
            autoExposureLockWidget?.visibility = View.GONE
            focusModeWidget?.visibility = View.GONE
            focusExposureSwitchWidget?.visibility = View.GONE
            cameraControlsWidget?.visibility = View.GONE
            focalZoomWidget?.visibility = View.GONE
//            returnHomeWidget?.visibility = View.GONE
//            takeOffWidget?.visibility = View.GONE
//            lensControlWidget?.visibility = View.GONE
            simulatorControlWidget?.visibility = View.GONE
            pfvFlightDisplayWidget?.visibility = View.GONE
            systemStatusListPanelWidget?.visibility = View.GONE
            topBarPanel?.visibility = View.GONE
        }
    }

}
