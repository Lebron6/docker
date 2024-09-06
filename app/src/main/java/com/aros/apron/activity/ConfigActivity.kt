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
    }

    private fun initView() {
        configBinding.cbHaveRtk.isChecked = PreferenceUtils.getInstance().haveRTK
        configBinding.cbCloseObstacle.isChecked = PreferenceUtils.getInstance().closeObsEnable
        configBinding.cbDebuggingMode.isChecked = PreferenceUtils.getInstance().isDebugMode
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
        configBinding.cbCustomStream.isChecked = PreferenceUtils.getInstance().customStreamEnable
        configBinding.layoutStream.visibility =
            if (PreferenceUtils.getInstance().customStreamEnable) VISIBLE else {
                GONE
            }
        configBinding.cbCustomStream.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                configBinding.layoutStream.visibility = VISIBLE
            } else {
                configBinding.layoutStream.visibility = GONE
            }
        }
        configBinding.etStreamUrl.setText(PreferenceUtils.getInstance().customStreamUrl)

        configBinding.etNtrip.setText(PreferenceUtils.getInstance().ntrip)
        configBinding.etNtrPort.setText(PreferenceUtils.getInstance().ntrPort)
        configBinding.etNtrAccount.setText(PreferenceUtils.getInstance().ntrAccount)
        configBinding.etNtrPassword.setText(PreferenceUtils.getInstance().ntrPassword)
        configBinding.etNtrMountpoint.setText(PreferenceUtils.getInstance().ntrMountPoint)
        configBinding.etMqttServerUri.setText(PreferenceUtils.getInstance().mqttServerUri)
        configBinding.etMqttUsername.setText(PreferenceUtils.getInstance().mqttUserName)
        configBinding.etMqttPassword.setText(PreferenceUtils.getInstance().mqttPassword)
        configBinding.etMqttSn.setText(PreferenceUtils.getInstance().mqttSn)

//        configBinding.etDockerLat.setText(PreferenceUtils.getInstance().dockerLat)
//        configBinding.etDockerLon.setText(PreferenceUtils.getInstance().dockerLon)
//        configBinding.etAircraftHeading.setText(PreferenceUtils.getInstance().aircraftHeading)

        configBinding.etAlternateLat.setText(PreferenceUtils.getInstance().alternatePointLat)
        configBinding.etAlternateLon.setText(PreferenceUtils.getInstance().alternatePointLon)
        configBinding.etSetAlternateSecurityHeight.setText(PreferenceUtils.getInstance().alternatePointSecurityHeight)
        configBinding.etSetAlternateHeight.setText(PreferenceUtils.getInstance().alternatePointHeight)
        configBinding.etSetAlternateTimes.setText(PreferenceUtils.getInstance().alternatePointTimes)

        configBinding.cbNeedUploadVideo.isChecked = PreferenceUtils.getInstance().needUpLoadVideo
        when (PreferenceUtils.getInstance().airPortType) {
            1 -> configBinding.rbAd2.isChecked = true
            2 -> configBinding.rbAd3.isChecked = true
            3 -> configBinding.rbArs350.isChecked = true
        }
        when (PreferenceUtils.getInstance().missionInterruptAction) {
            1 -> configBinding.rbHover.isChecked = true
            2 -> configBinding.rbResume.isChecked = true
            3 -> configBinding.rbGohome.isChecked = true
        }
        configBinding.rbRtkFirst.isChecked = PreferenceUtils.getInstance().landType == 1
        configBinding.rbVisionFirst.isChecked = PreferenceUtils.getInstance().landType == 2
        configBinding.btnConfig.setOnClickListener { config() }
        configBinding.tvSetAlternate.setOnClickListener {
            val isConnect = KeyManager.getInstance()
                .getValue(KeyTools.createKey(FlightControllerKey.KeyConnection))
            if (isConnect != null && isConnect) {
                var locationCoordinate3D = KeyManager.getInstance()
                    .getValue(KeyTools.createKey(FlightControllerKey.KeyAircraftLocation3D))
                if (locationCoordinate3D != null) {
                    LogUtil.log(TAG,"标定备降点经纬度:${locationCoordinate3D?.latitude.toString()}---${locationCoordinate3D?.longitude.toString()}")
                    configBinding.etAlternateLat.setText(locationCoordinate3D?.latitude.toString())
                    configBinding.etAlternateLon.setText(locationCoordinate3D?.longitude.toString())
                } else {
                    configBinding.etAlternateLat.setText("")
                    configBinding.etAlternateLon.setText("")
                    ToastUtil.showToast("获取备降点经纬度失败")
                }
            } else {
                ToastUtil.showToast("设备未连接")
            }
        }

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
        if (!configBinding.rbAd2.isChecked && !configBinding.rbAd3.isChecked && !configBinding.rbArs350.isChecked) {
            ToastUtil.showToast("未配置机库类型")
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
        if (configBinding.cbCustomStream.isChecked) {
            if (TextUtils.isEmpty(configBinding.etStreamUrl.text)) {
                ToastUtil.showToast("未配置推流地址")
                return
            }
        }
//        if (TextUtils.isEmpty(configBinding.etDockerLat.text) || TextUtils.isEmpty(configBinding.etDockerLon.text)
//            || TextUtils.isEmpty(configBinding.etAircraftHeading.text)) {
//            ToastUtil.showToast("未标定起飞朝向")
//            return
//        }
        if (TextUtils.isEmpty(configBinding.etAlternateLat.text) || TextUtils.isEmpty(configBinding.etAlternateLon.text)) {
            ToastUtil.showToast("未配置备降点经纬度")
            return
        }

        if (TextUtils.isEmpty(configBinding.etSetAlternateSecurityHeight.text) ) {
            ToastUtil.showToast("未配置备降点安全起飞高度")
            return
        }
        if (TextUtils.isEmpty(configBinding.etSetAlternateHeight.text) ) {
            ToastUtil.showToast("未配置飞往备降点高度")
            return
        }

        PreferenceUtils.getInstance().alternatePointLat =
            configBinding.etAlternateLat.text.toString()
        PreferenceUtils.getInstance().alternatePointLon =
            configBinding.etAlternateLon.text.toString()
        PreferenceUtils.getInstance().alternatePointHeight =
            configBinding.etSetAlternateHeight.text.toString()
        PreferenceUtils.getInstance().alternatePointSecurityHeight =
            configBinding.etSetAlternateSecurityHeight.text.toString()
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
        PreferenceUtils.getInstance().navigationLEDsOn = configBinding.cbLEDsSettings.isChecked
        PreferenceUtils.getInstance().customStreamEnable = configBinding.cbCustomStream.isChecked
        if (configBinding.cbCustomStream.isChecked) {
            PreferenceUtils.getInstance().customStreamUrl =
                configBinding.etStreamUrl.text.toString().replace("", "")
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
        PreferenceUtils.getInstance().needUpLoadVideo = configBinding.cbNeedUploadVideo.isChecked

        if (configBinding.rbAd2.isChecked) {
            PreferenceUtils.getInstance().airPortType = 1
        } else if (configBinding.rbAd3.isChecked) {
            PreferenceUtils.getInstance().airPortType = 2
        } else {
            PreferenceUtils.getInstance().airPortType = 3
        }

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
        ToastUtil.showToast("配置已保存")
        Handler().postDelayed(Runnable {
            restartApp()
        }, 1000)

    }


}