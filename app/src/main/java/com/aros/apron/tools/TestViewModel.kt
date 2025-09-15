package com.aros.apron.tools

import android.content.Context
import com.aros.apron.tools.LogUtil
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.value.common.Attitude
import dji.sdk.keyvalue.value.flightcontroller.FlightCoordinateSystem
import dji.sdk.keyvalue.value.flightcontroller.RollPitchControlMode
import dji.sdk.keyvalue.value.flightcontroller.VerticalControlMode
import dji.sdk.keyvalue.value.flightcontroller.VirtualStickFlightControlParam
import dji.sdk.keyvalue.value.flightcontroller.YawControlMode
import dji.v5.common.callback.CommonCallbacks.CompletionCallback
import dji.v5.common.error.IDJIError
import dji.v5.et.create
import dji.v5.manager.KeyManager
import dji.v5.manager.aircraft.virtualstick.VirtualStickManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.abs


const val TAG = "TestViewModel"

class TestViewModel : BaseViewModel() {

    // 当前偏航角（用于判断动作完成）
    private var currentYaw: Double = 0.0
    private var currentRoll: Double = 0.0
    private var currentPitch: Double = 0.0

    private val param = VirtualStickFlightControlParam(
        0.0,
        0.0,
        0.0,
        0.0,
        VerticalControlMode.VELOCITY,
        RollPitchControlMode.VELOCITY,
        YawControlMode.ANGULAR_VELOCITY,
        FlightCoordinateSystem.BODY
    )

    init {
        FlightControllerKey.KeyAircraftAttitude.create()
        KeyManager.getInstance().listen(
            FlightControllerKey.KeyAircraftAttitude.create(),
            this,
            true
        ) { _, newValue ->
            currentYaw = newValue?.yaw ?: 0.0
            currentRoll = newValue?.roll ?: 0.0
            currentPitch = newValue?.pitch ?: 0.0
        }
    }


    /**
     * 启动虚拟摇杆
     */
    suspend fun enableVirtualStick(): IDJIError? {
        return suspendCancellableCoroutine { continuation ->
            VirtualStickManager.getInstance().enableVirtualStick(object : CompletionCallback {
                override fun onSuccess() {
                    continuation.resume(null)
                }

                override fun onFailure(error: IDJIError) {
                    continuation.resume(error)
                }
            })
        }
    }


    /**
     * 停止虚拟摇杆
     */
    suspend fun disableVirtualStick(): IDJIError? {
        return suspendCancellableCoroutine { continuation ->
            VirtualStickManager.getInstance().disableVirtualStick(object : CompletionCallback {
                override fun onSuccess() {
                    continuation.resume(null)
                }

                override fun onFailure(error: IDJIError) {
                    continuation.resume(error)
                }
            })
        }
    }


    /**
     * 开启虚拟摇杆高级模式
     */
    fun setVirtualStickAdvancedModeEnabled(enabled: Boolean) {
        VirtualStickManager.getInstance().setVirtualStickAdvancedModeEnabled(enabled)
    }


    private fun sendStickCommand(param: VirtualStickFlightControlParam) {
        VirtualStickManager.getInstance().sendVirtualStickAdvancedParam(param)
    }


    fun performSwingSequence() {
        //重置虚拟数值
        param.apply {
            this.roll = 0.0
            this.yaw = 0.0
            this.pitch = 0.0
        }

        launchOnUI {
            LogUtil.log(TAG, "偏航来回3次")
            // 偏航来回3次
            repeat(3) {
                swingYaw(30f)
                swingYaw(-30f)

                delay(500)

                swingYaw(-30f)
                swingYaw(30f)
            }

            LogUtil.log(TAG, "横滚来回3次")
            // 横滚来回3次
            repeat(3) {
                swingRoll(4f)
                swingRoll(-4f)

                delay(500)

                swingRoll(-4f)
                swingRoll(4f)
            }

            LogUtil.log(TAG, "俯仰来回3次")
            // 俯仰来回3次
            repeat(3) {
                swingPitch(5f)
                swingPitch(-5f)
                delay(500)

                swingPitch(-5f)
                swingPitch(5f)
            }
        }
    }


    private suspend fun swingYaw(yaw: Float) {
        param.yaw = yaw.toDouble()
        param.apply {
            this.roll = 0.0
            this.yaw = yaw.toDouble()
            this.pitch = 0.0
            rollPitchControlMode = RollPitchControlMode.VELOCITY
            yawControlMode = YawControlMode.ANGULAR_VELOCITY
            rollPitchCoordinateSystem = FlightCoordinateSystem.BODY
        }

        repeat(20) {
            sendStickCommand(param)
            delay(50)
        }.also {
            //停止
            param.apply {
                this.roll = 0.0
                this.yaw = 0.0
                this.pitch = 0.0
                rollPitchControlMode = RollPitchControlMode.VELOCITY
                yawControlMode = YawControlMode.ANGULAR_VELOCITY
                rollPitchCoordinateSystem = FlightCoordinateSystem.BODY
            }
            sendStickCommand(param)
        }

    }


    private suspend fun swingRoll(roll: Float) {
        param.apply {
            this.roll = 0.0
            this.yaw = 0.0
            this.pitch = roll.toDouble()
            rollPitchControlMode = RollPitchControlMode.VELOCITY
            yawControlMode = YawControlMode.ANGULAR_VELOCITY
            rollPitchCoordinateSystem = FlightCoordinateSystem.BODY
        }
        repeat(20) {
            sendStickCommand(param)
            delay(50)
        }.also {
            //停止
            param.apply {
                this.roll = 0.0
                this.yaw = 0.0
                this.pitch = 0.0
                rollPitchControlMode = RollPitchControlMode.VELOCITY
                yawControlMode = YawControlMode.ANGULAR_VELOCITY
                rollPitchCoordinateSystem = FlightCoordinateSystem.BODY
            }
            sendStickCommand(param)
        }
    }


    private suspend fun swingPitch(pitch: Float) {
        param.apply {
            this.roll = pitch.toDouble()
            this.yaw = 0.0
            this.pitch = 0.0
            rollPitchControlMode = RollPitchControlMode.VELOCITY
            yawControlMode = YawControlMode.ANGULAR_VELOCITY
            rollPitchCoordinateSystem = FlightCoordinateSystem.BODY
        }
        repeat(20) {
            sendStickCommand(param)
            delay(50)
        }.also {
            //停止
            param.apply {
                this.roll = 0.0
                this.yaw = 0.0
                this.pitch = 0.0
                rollPitchControlMode = RollPitchControlMode.VELOCITY
                yawControlMode = YawControlMode.ANGULAR_VELOCITY
                rollPitchCoordinateSystem = FlightCoordinateSystem.BODY
            }
            sendStickCommand(param)
        }
    }



    /**
     * 计算两个角度之间的最短距离（处理360°环回）
     */
    private fun shortestAngleDistance(current: Double, target: Double): Double {
        var diff = target - current
        while (diff > 180f) diff -= 360f
        while (diff < -180f) diff += 360f
        return diff
    }
}