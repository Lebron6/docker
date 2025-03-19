package com.aros.apron.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.aros.apron.R
import com.aros.apron.app.ApronApp
import com.aros.apron.constant.AMSConfig
import com.aros.apron.databinding.ActivityConnectionBinding
import com.aros.apron.models.MSDKInfoVm
import com.aros.apron.models.MSDKManagerVM
import com.aros.apron.models.globalViewModels
import com.aros.apron.tools.LogUtil
import com.aros.apron.tools.PreferenceUtils
import com.aros.apron.tools.RestartAPPTool.restartApp
import com.aros.apron.tools.ToastUtil
import com.tencent.bugly.crashreport.CrashReport
import com.yanzhenjie.permission.AndPermission
import dji.v5.utils.common.StringUtils


class ConnectionActivity : AppCompatActivity() {

    private val REQUIRED_PERMISSION_LIST = arrayOf(
        Manifest.permission.VIBRATE,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.WAKE_LOCK,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.CHANGE_WIFI_STATE,
        Manifest.permission.CHANGE_NETWORK_STATE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        //            Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
        //            Manifest.permission.WRITE_SETTINGS,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    private val msdkInfoVm: MSDKInfoVm by viewModels()
    private val TAG="ConnectionActivity"
    private val msdkManagerVM: MSDKManagerVM by globalViewModels()
    private lateinit var connectionBinding: ActivityConnectionBinding
    private val handler: Handler = Handler(Looper.getMainLooper())


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        connectionBinding = ActivityConnectionBinding.inflate(layoutInflater)
        setContentView(connectionBinding.root)
        window.decorView.apply {
            systemUiVisibility =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
        initMSDKInfoView()
        checkAndRequestPermissions()
        connectionBinding.config?.setOnClickListener {
            startActivity(Intent(this, ConfigActivity::class.java))
        }
//        Utils.sHA1(this)
        //默认自动启动辅助服务
//        Settings.Secure.putString(
//            contentResolver,
//            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
//            "${packageName}/${BluetoothAccessibilityService::class.java.canonicalName}"
//        )
//        Settings.Secure.putInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1);
//        //开启蓝牙辅助服务
//        Intent(this@ConnectionActivity, BluetoothAccessibilityService::class.java).also {
//            LogUtil.log("ConnectionActivity", "start BluetoothAccessibilityService")
//            startService(it)
//        }
        initBugly()

    }

    private fun initBugly() {
        //bugly
        CrashReport.UserStrategy(this).apply {
            appPackageName = packageName
            isEnableANRCrashMonitor = true
            isEnableCatchAnrTrace = true
            setCrashHandleCallback(object : CrashReport.CrashHandleCallback() {
                override fun onCrashHandleStart(
                    crashType: Int, errorType: String,
                    errorMessage: String, errorStack: String
                ): Map<String, String> {
                    return mapOf()
                }

                override fun onCrashHandleStart2GetExtraDatas(
                    crashType: Int, errorType: String,
                    errorMessage: String, errorStack: String
                ): ByteArray {
                    LogUtil.log("---crash----", "\n" + errorMessage + "\n" + errorStack)
                    //如果处理了，让主程序继续运行3秒再退出，保证异步的写操作能及时完成
                    try {
                        Thread.sleep((1000).toLong())
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                    restartApp()
                    return "autoRestart".toByteArray(charset("UTF-8"))
                }
            })
            CrashReport.setAllThreadStackEnable(ApronApp.context, true, true)
//            CrashReport.initCrashReport(context, "5894201d87", true, this)
            CrashReport.initCrashReport(ApronApp.context, "67f68269b0", true, this)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initMSDKInfoView() {
        msdkInfoVm.msdkInfo.observe(this) {
            connectionBinding.textViewVersion.text =
                StringUtils.getResStr(R.string.sdk_version, it.SDKVersion + " " + it.buildVer)

            connectionBinding.textViewProductName.text =
                StringUtils.getResStr(R.string.product_name, it.productType.name)
            connectionBinding.textViewPackageProductCategory.text =
                StringUtils.getResStr(R.string.package_product_category, it.packageProductCategory)
            connectionBinding.textViewIsDebug.text =
                StringUtils.getResStr(R.string.is_sdk_debug, it.isDebug)
        }
        // 获取当前应用的 PackageManager 实例
        val packageManager = packageManager
        // 获取当前应用的 PackageInfo 对象
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        // 获取版本名
        val versionName = packageInfo.versionName
        connectionBinding.textViewAmsVersion?.text=versionName
    }

    private fun observeSDKManagerStatus() {
        msdkManagerVM.lvRegisterState.observe(this) { resultPair ->
            val statusText: String?
            if (resultPair.first) {
                statusText = StringUtils.getResStr(this, R.string.registered)
                msdkInfoVm.initListener()
//                connectionBinding.defaultLayoutButton.isEnabled = true
//                enableShowCaseButton(
//                    connectionBinding.defaultLayoutButton,
//                    MainActivity::class.java
//                )
                if (TextUtils.isEmpty(PreferenceUtils.getInstance().mqttServerUri)
                    || TextUtils.isEmpty(PreferenceUtils.getInstance().mqttUserName)
                    || TextUtils.isEmpty(PreferenceUtils.getInstance().mqttPassword)
                    || TextUtils.isEmpty(PreferenceUtils.getInstance().mqttSn)
                ) {
                    ToastUtil.showToast("未配置MQTT参数")
                    LogUtil.log(TAG, "未配置MQTT参数")
                } else if (PreferenceUtils.getInstance().haveRTK &&PreferenceUtils.getInstance().rtkType!=1&&PreferenceUtils.getInstance().rtkType!=2 ){
                    LogUtil.log(TAG, "未配置RTK类型")
                    ToastUtil.showToast("未配置RTK类型")
                }else if (PreferenceUtils.getInstance().haveRTK &&PreferenceUtils.getInstance().rtkType==1&& (TextUtils.isEmpty(
                        PreferenceUtils.getInstance().ntrip
                    ) ||
                            TextUtils.isEmpty(PreferenceUtils.getInstance().ntrAccount) ||
                            TextUtils.isEmpty(PreferenceUtils.getInstance().ntrPassword) ||
                            TextUtils.isEmpty(PreferenceUtils.getInstance().ntrMountPoint))
                ) {
                    LogUtil.log(TAG, "未配置网络RTK参数")
                    ToastUtil.showToast("未配置网络RTK参数")
                } else if (PreferenceUtils.getInstance().landType != 1 && PreferenceUtils.getInstance().landType != 2) {
                    ToastUtil.showToast("未配置降落方式")
                    LogUtil.log(TAG, "未配置降落方式")
                } else if (PreferenceUtils.getInstance().landType == 1 && !PreferenceUtils.getInstance().haveRTK) {
                    ToastUtil.showToast("RTK降落未配置网络RTK参数")
                    LogUtil.log(TAG, "RTK降落未配置网络RTK参数")
                } else if (PreferenceUtils.getInstance().airPortType != 1 && PreferenceUtils.getInstance().airPortType != 2 && PreferenceUtils.getInstance().airPortType != 3) {
                    ToastUtil.showToast("未配置机库类型")
                    LogUtil.log(TAG, "未配置机库类型")
                }else if (PreferenceUtils.getInstance().customStreamEnable&&TextUtils.isEmpty(PreferenceUtils.getInstance().customStreamUrl)) {
                    ToastUtil.showToast("未配置自定义推流地址")
                    LogUtil.log(TAG, "未配置自定义推流地址")
                }else if (PreferenceUtils.getInstance().cameraLocationType == 0){
                    ToastUtil.showToast("未配置主相机位置")
                    LogUtil.log(TAG, "未配置主相机位置")
                }else if (TextUtils.isEmpty(PreferenceUtils.getInstance().minumumBattery)||
                    TextUtils.isEmpty(PreferenceUtils.getInstance().forcedBattery)) {
                    ToastUtil.showToast("未配置允许起飞或强制返航电量阈值")
                    LogUtil.log(TAG, "未配置允许起飞或强制返航电量阈值")
                }
//                else if (TextUtils.isEmpty(PreferenceUtils.getInstance().alternatePointLon) ||TextUtils.isEmpty(PreferenceUtils.getInstance().alternatePointLat)) {
//                    ToastUtil.showToast("未设置备降点")
//                    LogUtil.log(TAG, "未设置备降点")
//                }
//                else if (TextUtils.isEmpty(PreferenceUtils.getInstance().alternatePointSecurityHeight) ) {
//                    ToastUtil.showToast("未设置备降点安全起飞高度")
//                    LogUtil.log(TAG, "未设置备降点安全起飞高度")
//                }
//                else if (TextUtils.isEmpty(PreferenceUtils.getInstance().alternatePointHeight) ) {
//                    ToastUtil.showToast("未设置飞往备降点高度")
//                    LogUtil.log(TAG, "未设置飞往备降点高度")
//                } else if (TextUtils.isEmpty(PreferenceUtils.getInstance().alternatePointTimes) ) {
//                    ToastUtil.showToast("未设置最大允许复降次数")
//                    LogUtil.log(TAG, "未设置最大允许复降次数")
//                }
                else {
                    LogUtil.log(TAG, "已加载AMS配置文件")
                    AMSConfig.getInstance().mqttServerUri =
                        PreferenceUtils.getInstance().mqttServerUri
                    AMSConfig.getInstance().userName = PreferenceUtils.getInstance().mqttUserName
                    AMSConfig.getInstance().password = PreferenceUtils.getInstance().mqttPassword
                    AMSConfig.getInstance().serialNumber = PreferenceUtils.getInstance().mqttSn
                    AMSConfig.getInstance().alternateLandingTimes = PreferenceUtils.getInstance().alternatePointTimes

                    AMSConfig.getInstance().mqttServer2MsdkTopic =
                        "nest/${AMSConfig.getInstance().serialNumber}/uav_services"
                    AMSConfig.getInstance().mqttMsdkReplyMessage2ServerTopic =
                        "nest/${AMSConfig.getInstance().serialNumber}/uav_services_reply"
                    AMSConfig.getInstance().mqttMsdkPushMessage2ServerTopic =
                        "nest/${AMSConfig.getInstance().serialNumber}/uav_status_message"
                    AMSConfig.getInstance().mqttMsdkPushEvent2ServerTopic =
                        "nest/${AMSConfig.getInstance().serialNumber}/events"
                    if (PreferenceUtils.getInstance().airPortType == 1) {
                        AMSConfig.getInstance().descentUltrasonicAltitude = 3
                        AMSConfig.getInstance().descentAltitude = 0.5
                    } else if (PreferenceUtils.getInstance().airPortType == 2) {
                        AMSConfig.getInstance().descentUltrasonicAltitude = 5
                        AMSConfig.getInstance().descentAltitude = 0.5
                    } else if (PreferenceUtils.getInstance().airPortType == 3) {
                        AMSConfig.getInstance().descentUltrasonicAltitude = 5
                        AMSConfig.getInstance().descentAltitude = 0.5
                    }
                    if (!MainActivity.isAppStarted) {
                        Handler().postDelayed(Runnable {
                            startActivity(Intent(this, MainActivity::class.java))
                        },500)

                    }

                }
            } else {
                LogUtil.log(TAG, "SDK Register Failure: ${resultPair.second}")

                ToastUtil.showToast("Register Failure: ${resultPair.second}")
                statusText = StringUtils.getResStr(this, R.string.unregistered)
            }
            connectionBinding.textViewRegistered.text =
                StringUtils.getResStr(R.string.registration_status, statusText)
        }
        msdkManagerVM.lvProductConnectionState.observe(this) { isConnect ->

        }

        msdkManagerVM.lvProductChanges.observe(this) { productId ->
            ToastUtil.showToast("Product: $productId Changed")

        }

        msdkManagerVM.lvInitProcess.observe(this) { processPair ->
            ToastUtil.showToast("Init Process event: ${processPair.first.name}")
        }

        msdkManagerVM.lvDBDownloadProgress.observe(this) { resultPair ->
            ToastUtil.showToast("Database Download Progress current: ${resultPair.first}, total: ${resultPair.second}")
        }
    }


    /**
     * Checks if there is any missing permissions, and requests runtime permission if needed.
     */
    private fun checkAndRequestPermissions() {
        AndPermission.with(this)
            .runtime()
            .permission(REQUIRED_PERMISSION_LIST)
            .onGranted {
                observeSDKManagerStatus()
            }
            .onDenied {
                // Storage permission are not allowed.
                ToastUtil.showToast("请给予app运行所需权限！！！")
                finish()
            }
            .start()
    }


    private fun <T> enableShowCaseButton(view: View, cl: Class<T>) {
        view.isEnabled = true
        view.setOnClickListener {
            Intent(this, cl).also {
                startActivity(it)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        LogUtil.log(TAG,"进入首页连接")
    }
}