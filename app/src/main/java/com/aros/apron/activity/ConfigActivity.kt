package com.aros.apron.activity

import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.widget.CompoundButton.GONE
import android.widget.CompoundButton.VISIBLE
import com.aros.apron.base.BaseActivity
import com.aros.apron.databinding.ActivityConfigBinding
import com.aros.apron.tools.LogUtil
import com.aros.apron.tools.PreferenceUtils
import com.aros.apron.tools.RestartAPPTool.restartApp
import com.aros.apron.tools.ToastUtil
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.KeyTools
import dji.v5.manager.KeyManager

class ConfigActivity : BaseActivity() {

    private lateinit var configBinding: ActivityConfigBinding

    override fun useEventBus(): Boolean {
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configBinding = ActivityConfigBinding.inflate(layoutInflater)
        setContentView(configBinding.root)
        initView()
        LogUtil.log(TAG,"进入AMS配置界面")
    }

    private fun initView() {
        configBinding.cbHaveRtk.isChecked = PreferenceUtils.getInstance().haveRTK
        configBinding.cbLteEnable.isChecked = PreferenceUtils.getInstance().lteEnable
        configBinding.cbCloseObstacle.isChecked = PreferenceUtils.getInstance().closeObsEnable
        configBinding.cbDebuggingMode.isChecked = PreferenceUtils.getInstance().isDebugMode
        configBinding.cbCleanMode.isChecked = PreferenceUtils.getInstance().isCleanMode
        configBinding.cbLEDsSettings.isChecked = PreferenceUtils.getInstance().navigationLEDsOn
        configBinding.rbRtkCustom.isChecked = PreferenceUtils.getInstance().rtkType == 1
        configBinding.rbRtkDji.isChecked = PreferenceUtils.getInstance().rtkType == 2
        configBinding.layoutRtkCustom.visibility =
            if (PreferenceUtils.getInstance().rtkType == 1) VISIBLE else {
                GONE
            }
        configBinding.rbRtkCustom.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                configBinding.layoutRtkCustom.visibility = VISIBLE
            } else {
                configBinding.layoutRtkCustom.visibility = GONE
            }
        }
        configBinding.rbRtsp.isChecked = PreferenceUtils.getInstance().customStreamType==1
        configBinding.rbRtmp.isChecked = PreferenceUtils.getInstance().customStreamType==2
        configBinding.rbNo.isChecked = PreferenceUtils.getInstance().customStreamType==3
        configBinding.layoutStreamRtsp.visibility =
            if (PreferenceUtils.getInstance().customStreamType==1) VISIBLE else {
                GONE
            }
        configBinding.layoutStream.visibility =
            if (PreferenceUtils.getInstance().customStreamType==2) VISIBLE else {
                GONE
            }
        configBinding.rbRtsp.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                configBinding.layoutStreamRtsp.visibility = VISIBLE
            } else {
                configBinding.layoutStreamRtsp.visibility = GONE
            }
        }
        configBinding.rbRtmp.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                configBinding.layoutStream.visibility = VISIBLE
            } else {
                configBinding.layoutStream.visibility = GONE
            }
        }
        configBinding.rbNo.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                configBinding.layoutStream.visibility = GONE
                configBinding.layoutStreamRtsp.visibility = GONE
            }
        }
        configBinding.etStreamUrl.setText(PreferenceUtils.getInstance().customStreamUrl)
        configBinding.etRtspUserName.setText(PreferenceUtils.getInstance().rtspUserName)
        configBinding.etRtspPassword.setText(PreferenceUtils.getInstance().rtspPassWord)
        configBinding.etRtspPort.setText(PreferenceUtils.getInstance().rtspPort)

        configBinding.etNtrip.setText(PreferenceUtils.getInstance().ntrip)
        configBinding.etNtrPort.setText(PreferenceUtils.getInstance().ntrPort)
        configBinding.etNtrAccount.setText(PreferenceUtils.getInstance().ntrAccount)
        configBinding.etNtrPassword.setText(PreferenceUtils.getInstance().ntrPassword)
        configBinding.etNtrMountpoint.setText(PreferenceUtils.getInstance().ntrMountPoint)
        configBinding.etMqttServerUri.setText(PreferenceUtils.getInstance().mqttServerUri)
        configBinding.etMqttUsername.setText(PreferenceUtils.getInstance().mqttUserName)
        configBinding.etMqttPassword.setText(PreferenceUtils.getInstance().mqttPassword)
        configBinding.etMqttSn.setText(PreferenceUtils.getInstance().mqttSn)
        configBinding.etMinioUploadUrl.setText(PreferenceUtils.getInstance().uploadUrl)
        configBinding.etMinioBucketName.setText(PreferenceUtils.getInstance().bucketName)
        configBinding.etMinioObjectKey.setText(PreferenceUtils.getInstance().objectKey)
        configBinding.etMinioAccessKey.setText(PreferenceUtils.getInstance().accessKey)
        configBinding.etMinioSecretKey.setText(PreferenceUtils.getInstance().secretKey)

//        configBinding.etDockerLat.setText(PreferenceUtils.getInstance().dockerLat)
//        configBinding.etDockerLon.setText(PreferenceUtils.getInstance().dockerLon)
//        configBinding.etAircraftHeading.setText(PreferenceUtils.getInstance().aircraftHeading)
        configBinding.etMinimumBattery.setText(PreferenceUtils.getInstance().minumumBattery)
        configBinding.etSetAlternateTimes.setText(PreferenceUtils.getInstance().alternatePointTimes)

        configBinding.cbNeedUploadVideo.isChecked = PreferenceUtils.getInstance().needUpLoadVideo

        when (PreferenceUtils.getInstance().missionInterruptAction) {
            1 -> configBinding.rbHover.isChecked = true
            2 -> configBinding.rbResume.isChecked = true
            3 -> configBinding.rbGohome.isChecked = true
        }
        configBinding.rbRtkFirst.isChecked = PreferenceUtils.getInstance().landType == 1
        configBinding.rbVisionFirst.isChecked = PreferenceUtils.getInstance().landType == 2

        configBinding.rbCameraCenter.isChecked = PreferenceUtils.getInstance().cameraLocationType ==1//中间
        configBinding.rbCameraLeft.isChecked = PreferenceUtils.getInstance().cameraLocationType ==2//左边
        configBinding.btnConfig.setOnClickListener { config() }


        configBinding.tvSetAircraftLoc.setOnClickListener {
            val isConnect = KeyManager.getInstance()
                .getValue(KeyTools.createKey(FlightControllerKey.KeyConnection))
            if (isConnect != null && isConnect) {
                var locationCoordinate3D = KeyManager.getInstance()
                    .getValue(KeyTools.createKey(FlightControllerKey.KeyAircraftLocation3D))
                if (locationCoordinate3D != null) {
                    configBinding.etDockerLat.setText(locationCoordinate3D?.latitude.toString())
                    configBinding.etDockerLon.setText(locationCoordinate3D?.longitude.toString())
                    configBinding.etAircraftHeading.setText("假数据")
                } else {
                    configBinding.etDockerLat.setText("")
                    configBinding.etDockerLon.setText("")
                    configBinding.etAircraftHeading.setText("")
                    ToastUtil.showToast("获取机库位置失败")
                }
            } else {
                ToastUtil.showToast("设备未连接")
            }
        }
    }

    private fun config() {
        if (configBinding.rbRtkCustom.isChecked) {
            if (TextUtils.isEmpty(configBinding.etNtrip.text)) {
                ToastUtil.showToast("未配置网络RTK地址")
                return
            }
            if (TextUtils.isEmpty(configBinding.etNtrPort.text)) {
                ToastUtil.showToast("未配置网络RTK端口")
                return
            }
            if (TextUtils.isEmpty(configBinding.etNtrAccount.text)) {
                ToastUtil.showToast("未配置网络RTK账户")
                return
            }
            if (TextUtils.isEmpty(configBinding.etNtrPassword.text)) {
                ToastUtil.showToast("未配置网络RTK密码")
                return
            }
            if (TextUtils.isEmpty(configBinding.etNtrMountpoint.text)) {
                ToastUtil.showToast("未配置网络RTK挂载点")
                return
            }
        }
        if (configBinding.cbHaveRtk.isChecked) {
            if (!configBinding.rbRtkCustom.isChecked && !configBinding.rbRtkDji.isChecked) {
                ToastUtil.showToast("未配置RTK类型")
                return
            }
        }
        if (TextUtils.isEmpty(configBinding.etMqttServerUri.text)) {
            ToastUtil.showToast("未配置MQTT服务器地址")
            return
        }
        if (TextUtils.isEmpty(configBinding.etMqttUsername.text)) {
            ToastUtil.showToast("未配置MQTT用户名")
            return
        }
        if (TextUtils.isEmpty(configBinding.etMqttPassword.text)) {
            ToastUtil.showToast("未配置MQTT密码")
            return
        }
        if (TextUtils.isEmpty(configBinding.etMqttSn.text)) {
            ToastUtil.showToast("未配置MQTT设备编号")
            return
        }
        if (TextUtils.isEmpty(configBinding.etMinioUploadUrl.text)) {
            ToastUtil.showToast("未配置minio文件上传地址")
            return
        }
        if (TextUtils.isEmpty(configBinding.etMinioBucketName.text)) {
            ToastUtil.showToast("未配置minio桶名")
            return
        }
        if (TextUtils.isEmpty(configBinding.etMinioObjectKey.text)) {
            ToastUtil.showToast("未配置minio ObjectKey")
            return
        }
        if (TextUtils.isEmpty(configBinding.etMinioAccessKey.text)) {
            ToastUtil.showToast("未配置minio AccessKey")
            return
        }
        if (TextUtils.isEmpty(configBinding.etMinioSecretKey.text)) {
            ToastUtil.showToast("未配置minio SecretKey")
            return
        }

        if (!configBinding.rbHover.isChecked && !configBinding.rbResume.isChecked && !configBinding.rbGohome.isChecked) {
            ToastUtil.showToast("未配置航线中断动作")
            return
        }
        if (!configBinding.rbRtkFirst.isChecked && !configBinding.rbVisionFirst.isChecked) {
            ToastUtil.showToast("至少配置一种降落方式")
            return
        }
        if (configBinding.rbRtkFirst.isChecked) {
            if (!configBinding.cbHaveRtk.isChecked) {
                ToastUtil.showToast("RTK优先需配置RTK模块")
                return
            }
        }

        if (!configBinding.rbCameraLeft.isChecked && !configBinding.rbCameraCenter.isChecked) {
            ToastUtil.showToast("未配置主相机位置")
            return
        }
        if (configBinding.rbRtsp.isChecked) {
            if (TextUtils.isEmpty(configBinding.etRtspUserName.text)) {
                ToastUtil.showToast("未配置Rtsp用户名")
                return
            }
            if (TextUtils.isEmpty(configBinding.etRtspPassword.text)) {
                ToastUtil.showToast("未配置Rtsp密码")
                return
            }
            if (TextUtils.isEmpty(configBinding.etRtspPort.text)) {
                ToastUtil.showToast("未配置Rtsp端口")
                return
            }
        }
//        if (TextUtils.isEmpty(configBinding.etDockerLat.text) || TextUtils.isEmpty(configBinding.etDockerLon.text)
//            || TextUtils.isEmpty(configBinding.etAircraftHeading.text)) {
//            ToastUtil.showToast("未标定起飞朝向")
//            return
//        }
        if (TextUtils.isEmpty(configBinding.etMinimumBattery.text) ) {
            ToastUtil.showToast("未配置电池阈值")
            return
        }
        var minimumBattery=configBinding.etMinimumBattery.text.toString()
        if (minimumBattery.toInt()<35){
            ToastUtil.showToast("允许起飞电量不得低于35%")
            return
        }


        PreferenceUtils.getInstance().minumumBattery =
            configBinding.etMinimumBattery.text.toString()


        PreferenceUtils.getInstance().alternatePointTimes =
            configBinding.etSetAlternateTimes.text.toString()

        PreferenceUtils.getInstance().setHaveRtk(configBinding.cbHaveRtk.isChecked)
        PreferenceUtils.getInstance().closeObsEnable = configBinding.cbCloseObstacle.isChecked
        if (configBinding.rbRtkCustom.isChecked) {
            PreferenceUtils.getInstance().rtkType = 1
        } else if (configBinding.rbRtkDji.isChecked) {
            PreferenceUtils.getInstance().rtkType = 2
        } else {
            PreferenceUtils.getInstance().rtkType = -1
        }
        PreferenceUtils.getInstance().isDebugMode = configBinding.cbDebuggingMode.isChecked
        PreferenceUtils.getInstance().isCleanMode = configBinding.cbCleanMode.isChecked
        PreferenceUtils.getInstance().navigationLEDsOn = configBinding.cbLEDsSettings.isChecked
        PreferenceUtils.getInstance().lteEnable = configBinding.cbLteEnable.isChecked
        if (configBinding.rbRtsp.isChecked) {
            PreferenceUtils.getInstance().customStreamType = 1
        } else if (configBinding.rbRtmp.isChecked) {
            PreferenceUtils.getInstance().customStreamType = 2
        } else if (configBinding.rbNo.isChecked) {
            PreferenceUtils.getInstance().customStreamType = 3
        } else {
            PreferenceUtils.getInstance().rtkType = -1
        }

        if (configBinding.rbRtmp.isChecked) {
            PreferenceUtils.getInstance().customStreamUrl =
                configBinding.etStreamUrl.text.toString().replace("", "")
        }
        if (configBinding.rbRtsp.isChecked) {
            PreferenceUtils.getInstance().rtspUserName =
                configBinding.etRtspUserName.text.toString().replace("", "")
            PreferenceUtils.getInstance().rtspPassWord =
                configBinding.etRtspPassword.text.toString().replace("", "")
            PreferenceUtils.getInstance().rtspPort =
                configBinding.etRtspPort.text.toString().replace("", "")
        }

        PreferenceUtils.getInstance().ntrip = configBinding.etNtrip.text.toString().replace(" ", "")
        PreferenceUtils.getInstance().ntrPort =
            configBinding.etNtrPort.text.toString().replace(" ", "")
        PreferenceUtils.getInstance().ntrAccount =
            configBinding.etNtrAccount.text.toString().replace(" ", "")
        PreferenceUtils.getInstance().ntrPassword =
            configBinding.etNtrPassword.text.toString().replace(" ", "")
        PreferenceUtils.getInstance().ntrMountPoint =
            configBinding.etNtrMountpoint.text.toString().replace(" ", "")

        PreferenceUtils.getInstance().mqttServerUri =
            configBinding.etMqttServerUri.text.toString().replace(" ", "")
        PreferenceUtils.getInstance().mqttUserName =
            configBinding.etMqttUsername.text.toString().replace(" ", "")
        PreferenceUtils.getInstance().mqttPassword =
            configBinding.etMqttPassword.text.toString().replace(" ", "")
        PreferenceUtils.getInstance().mqttSn =
            configBinding.etMqttSn.text.toString().replace(" ", "")
        PreferenceUtils.getInstance().uploadUrl =
            configBinding.etMinioUploadUrl.text.toString().replace(" ", "")
        PreferenceUtils.getInstance().bucketName =
            configBinding.etMinioBucketName.text.toString().replace(" ", "")
        PreferenceUtils.getInstance().objectKey =
            configBinding.etMinioObjectKey.text.toString().replace(" ", "")
        PreferenceUtils.getInstance().accessKey =
            configBinding.etMinioAccessKey.text.toString().replace(" ", "")
        PreferenceUtils.getInstance().secretKey =
            configBinding.etMinioSecretKey.text.toString().replace(" ", "")
        PreferenceUtils.getInstance().needUpLoadVideo = configBinding.cbNeedUploadVideo.isChecked


        if (configBinding.rbHover.isChecked) {
            PreferenceUtils.getInstance().missionInterruptAction = 1
        } else if (configBinding.rbResume.isChecked) {
            PreferenceUtils.getInstance().missionInterruptAction = 2
        } else {
            PreferenceUtils.getInstance().missionInterruptAction = 3
        }

        if (configBinding.rbVisionFirst.isChecked) {
            PreferenceUtils.getInstance().landType = 2
        } else {
            PreferenceUtils.getInstance().landType = 1
        }
        if (configBinding.rbCameraCenter.isChecked) {
            PreferenceUtils.getInstance().cameraLocationType = 1
        } else {
            PreferenceUtils.getInstance().cameraLocationType = 2
        }
        ToastUtil.showToast("配置已保存")
        Handler().postDelayed(Runnable {
            restartApp()
        }, 1000)

    }


}