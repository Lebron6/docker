//package com.shd.nest.viewmodel
//
//import android.annotation.SuppressLint
//import android.graphics.Bitmap
//import android.graphics.BitmapFactory
//import android.graphics.ImageFormat
//import android.graphics.Rect
//import android.graphics.YuvImage
//import androidx.lifecycle.viewModelScope
//import com.shd.nest.base.BaseViewModel
//import com.shd.nest.dji.AircraftInfoManager
//import com.shd.nest.extension.toDegree
//import com.shd.nest.util.LogFileManager
//import dji.sdk.keyvalue.key.FlightControllerKey
//import dji.sdk.keyvalue.value.flightcontroller.FlightCoordinateSystem
//import dji.sdk.keyvalue.value.flightcontroller.RollPitchControlMode
//import dji.sdk.keyvalue.value.flightcontroller.VerticalControlMode
//import dji.sdk.keyvalue.value.flightcontroller.VirtualStickFlightControlParam
//import dji.sdk.keyvalue.value.flightcontroller.YawControlMode
//import dji.v5.common.callback.CommonCallbacks
//import dji.v5.common.error.IDJIError
//import dji.v5.et.create
//import dji.v5.manager.KeyManager
//import dji.v5.manager.aircraft.virtualstick.VirtualStickManager
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import org.opencv.android.OpenCVLoader
//import org.opencv.android.Utils
//import org.opencv.aruco.Aruco
//import org.opencv.aruco.Dictionary
//import org.opencv.calib3d.Calib3d
//import org.opencv.core.Core
//import org.opencv.core.CvType
//import org.opencv.core.Mat
//import org.opencv.core.MatOfInt
//import org.opencv.core.Scalar
//import org.opencv.imgproc.Imgproc
//import java.io.ByteArrayOutputStream
//import kotlin.math.abs
//import kotlin.math.atan2
//import kotlin.math.sqrt
//
//
//class ArucoViewModel : BaseViewModel() {
//
//    override val TAG: String = "ArucoViewModel"
//
//    //Aruco字典
//    private lateinit var mDictionary: Dictionary
//
//    //是否正在识别Aruco
//    private var isDetectingAruco: Boolean = false
//
//    //识别得到的所有Aruco码
//    private val mArucoCornerList: MutableList<Mat> = mutableListOf()
//
//    //要查找相应Aruco码list
//    private val mFindArucoList: MutableList<ArucoMarker> = mutableListOf()
//
//
//    //加载opencv
//    fun initOpenCV() {
//        if (OpenCVLoader.initDebug()) {
//            mDictionary = Aruco.getPredefinedDictionary(Aruco.DICT_6X6_50)
//        }
//    }
//
//
//    /**
//     * aruco 二维码检测
//     */
//    fun detectArucoTag(
//        yuvData: ByteArray,
//        width: Int,
//        height: Int,
//    )  {
//        if (isDetectingAruco) {
//            return
//        }
//        if (yuvData.size < width * height) {
//            return
//        }
//        isDetectingAruco = true
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                val length = width * height
//                val u = ByteArray(width * height / 4)
//                val v = ByteArray(width * height / 4)
//                for (i in u.indices) {
//                    u[i] = yuvData[length + i]
//                    v[i] = yuvData[length + u.size + i]
//                }
//                for (i in u.indices) {
//                    yuvData[length + 2 * i] = v[i]
//                    yuvData[length + 2 * i + 1] = u[i]
//                }
//
//                val yuvImage = YuvImage(yuvData, ImageFormat.NV21, width, height, null)
//                val outputStream = ByteArrayOutputStream()
//                yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, outputStream)
//                val bitmap = BitmapFactory.decodeByteArray(outputStream.toByteArray(), 0, outputStream.size())
//
//                //bitmap转mat
//                val bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true)
//                val source = Mat()
//                Utils.bitmapToMat(bmp32, source)
//
//                //灰度化处理
//                val grayImgMat = Mat()
//                Imgproc.cvtColor(source, grayImgMat, Imgproc.COLOR_RGBA2GRAY)
//
//                val ids = MatOfInt()
//                mFindArucoList.clear()
//                mArucoCornerList.clear()
//
//                //aruco码检测
//                Aruco.detectMarkers(grayImgMat, mDictionary, mArucoCornerList, ids)
//
//                //没有检测到aruco二维码，飞机高度升高扩大相机视野范围
//                if (ids.empty() || mArucoCornerList.isEmpty()) {
//                    val verticalThrottle = if (AircraftInfoManager.aircraftAltitude < 12.0) { 64 } else { 0 }
//                    VirtualStickManager.getInstance().leftStick.apply {
//                        horizontalPosition = 0
//                        verticalPosition = verticalThrottle
//                    }
//                    VirtualStickManager.getInstance().rightStick.apply {
//                        horizontalPosition = 0
//                        verticalPosition = 0
//                    }
//
//                    isDetectingAruco = false
//                    grayImgMat.release()
//                    bitmap.recycle()
//                    bmp32.recycle()
//                    source.release()
//                    ids.release()
//                    return@launch
//                }
//
//                val idArray = ids.toArray()
//                //查找id等于19的Aruco码
//                if (mFindArucoList.isEmpty()) {
//                    for (markerIndex in idArray.indices) {
//                        val markerId = idArray[markerIndex]
//                        if (markerId == 19) {
//                            mFindArucoList.add(
//                                ArucoMarker(
//                                    markerId,
//                                    ArucoMarker.MARKER_19_SIZE_CM,
//                                    mArucoCornerList[markerIndex]
//                                )
//                            )
//                            break
//                        }
//                    }
//                }
//                //查找id等于43的Aruco码
//                if (mFindArucoList.isEmpty()) {
//                    for (markerIndex in idArray.indices) {
//                        val markerId = idArray[markerIndex]
//                        if (markerId == 43) {
//                            mFindArucoList.add(
//                                ArucoMarker(
//                                    markerId,
//                                    ArucoMarker.MARKER_43_SIZE_CM,
//                                    mArucoCornerList[markerIndex]
//                                )
//                            )
//                            break
//                        }
//                    }
//                }
//                //查找id等于1的Aruco码
//                if (mFindArucoList.isEmpty()) {
//                    for (markerIndex in idArray.indices) {
//                        val markerId = idArray[markerIndex]
//                        if (markerId == 1) {
//                            mFindArucoList.add(
//                                ArucoMarker(
//                                    markerId,
//                                    ArucoMarker.MARKER_1_SIZE_CM,
//                                    mArucoCornerList[markerIndex]
//                                )
//                            )
//                            break
//                        }
//                    }
//                }
//                //查找id等于2的Aruco码
//                if (mFindArucoList.isEmpty()) {
//                    for (markerIndex in idArray.indices) {
//                        val markerId = idArray[markerIndex]
//                        if (markerId == 2) {
//                            mFindArucoList.add(
//                                ArucoMarker(
//                                    markerId,
//                                    ArucoMarker.MARKER_2_SIZE_CM,
//                                    mArucoCornerList[markerIndex]
//                                )
//                            )
//                            break
//                        }
//                    }
//                }
//                //查找id等于3的Aruco码
//                if (mFindArucoList.isEmpty()) {
//                    for (markerIndex in idArray.indices) {
//                        val markerId = idArray[markerIndex]
//                        if (markerId == 3) {
//                            mFindArucoList.add(
//                                ArucoMarker(
//                                    markerId,
//                                    ArucoMarker.MARKER_3_SIZE_CM,
//                                    mArucoCornerList[markerIndex]
//                                )
//                            )
//                            break
//                        }
//                    }
//                }
//                //查找id等于4的Aruco码
//                if (mFindArucoList.isEmpty()) {
//                    for (markerIndex in idArray.indices) {
//                        val markerId = idArray[markerIndex]
//                        if (markerId == 4) {
//                            mFindArucoList.add(
//                                ArucoMarker(
//                                    markerId,
//                                    ArucoMarker.MARKER_4_SIZE_CM,
//                                    mArucoCornerList[markerIndex]
//                                )
//                            )
//                            break
//                        }
//                    }
//                }
//
//                val arucoMarker = mFindArucoList[0]
//                //aruco二维码尺寸(单位m)
//                val markerSize = arucoMarker.arucoMarkerSize / 100
//
//                //旋转矩阵
//                val rvecs = Mat()
//                //位移矩阵
//                val tvecs = Mat()
//
//                //相机内参
//                val cameraMatrix = Mat.zeros(3, 3, CvType.CV_64F).apply {
//                    put(0, 0, 1035.501071149292) //fx
//                    put(1, 1, 1035.4725889980984) //fy
//                    put(0, 2, 713.2867513159875) //cx
//                    put(1, 2, 542.4491896129153) //cy
//                    put(2, 2, 1.0)
//                }
//                //畸变
//                val distCoeffs = Mat.zeros(5, 1, CvType.CV_64FC1).apply {
//                    put(0, 0, 0.3519238102526651)     //k1
//                    put(1, 0, -1.4538841685400365)    //k2
//                    put(2, 0, 0.00022919790876455443)   //p1
//                    put(3, 0, 0.0012223205821680879)   //p2
//                    put(4, 0, 2.0070528327672754)  //k3
//                }
//
//                //姿态预估
//                Aruco.estimatePoseSingleMarkers(
//                    mutableListOf(arucoMarker.corner),
//                    markerSize,
//                    cameraMatrix,
//                    distCoeffs,
//                    rvecs,
//                    tvecs
//                )
//
//                //单个aruco码姿态预估得到的旋转向量、平移向量
//                val rvec = rvecs.row(0)
//                val tvec = tvecs.row(0)
//
//                //罗德里变换
//                val R = Mat(3, 3, CvType.CV_32FC1)
//                Calib3d.Rodrigues(rvec, R)
//                val camR = R.t()
//
//                //左乘
//                val _camR = Mat()
//                val _1 = Scalar(-1.0)
//                Core.multiply(camR, _1, _camR)
//
//                //无人机偏航旋转角度
//                val eulerAngles = rotationMatrixToEulerAngles(camR)
//                val yawCamera: Double = eulerAngles[2].toDegree()
//                var resultYaw: Double = if (abs(yawCamera).toInt() in 5..175 && (arucoMarker.markerId == 19 || arucoMarker.markerId == 43)) {
//                    val scaleFactor = when (abs(yawCamera).toInt()) {
//                        in 0..90 -> 5.0
//                        in 90..135 -> 15.0
//                        else -> 40.0
//                    }
//                    if (yawCamera < 0) (abs(yawCamera) / scaleFactor) * 1.5 else -(abs(yawCamera) / scaleFactor) * 1.5
//                } else {
//                    0.0
//                }
//
//                var x = tvec[0, 0][0]
//                var y = tvec[0, 0][1]
//
//                /*LogFileManager.getInstance().log(
//                    TAG,
//                    "降落 ID:${arucoMarker.markerId}   x: $x   y: $y   yawCamera:$yawCamera  altitude: ${AircraftInfoManager.aircraftAltitude} "
//                )
//                isDetectAruco = false
//                return@launch*/
//
//                if ((arucoMarker.markerId == 19 &&  x < 0 && abs(x) in 0.035..0.075 && y in 0.0685..0.0865 && AircraftInfoManager.aircraftAltitude <= 1.8f)
//                    || (arucoMarker.markerId == 43 &&  x < 0 && abs(x) in 0.035..0.075 && y in 0.0685..0.0865 && AircraftInfoManager.aircraftAltitude <= 1.8f)
//                ) {
//                    exitVirtualStick()
//                    isDetectingAruco = false
//                    grayImgMat.release()
//                    bitmap.recycle()
//                    bmp32.recycle()
//                    source.release()
//                    ids.release()
//                    rvecs.release()
//                    tvecs.release()
//                    rvec.release()
//                    tvec.release()
//                    camR.release()
//                    _camR.release()
//                    R.release()
//                    mFindArucoList.clear()
//                    mArucoCornerList.clear()
//                    LogFileManager.getInstance().log(
//                        TAG,
//                        "降落 ID:${arucoMarker.markerId}   x: $x   y: $y   altitude: ${AircraftInfoManager.aircraftAltitude} "
//                    )
//                    return@launch
//                }
//                //x 正：飞机偏左     负：飞机偏右
//                //y 正：飞机偏前     负：飞机偏后
//                var downwardSpeed = if (abs(x) <= 0.45 && abs(y) <= 0.45 && AircraftInfoManager.aircraftAltitude > 4.0f) {
//                    //-3.5f
//                    -5.65f
//                } else if ((abs(x) <= 0.12 || abs(y) <= 0.12) && (AircraftInfoManager.aircraftAltitude in 2.5f..4.0f)) {
//                    -2.85f
//                } else if (AircraftInfoManager.aircraftAltitude in 1.5f..2.5f) {
//                    -1.75f
//                } else if (AircraftInfoManager.aircraftAltitude <= 1.5f) {
//                    0f
//                } else {
//                    0f
//                }
//
//                //如果识别到周围4个角的Aruco二维码，尽量控制飞机向降落板中心移动
//                if ((arucoMarker.markerId == 1 || arucoMarker.markerId == 4)) {
//                    if (y < 0 && abs(y) < 0.22) {
//                        y = -1 * (0.22 - abs(y))
//                    } else {
//                        y -= 0.22
//                    }
//                }
//                if ((arucoMarker.markerId == 2 || arucoMarker.markerId == 3)) {
//                    if (y > 0 && y < 0.22) {
//                        y = 0.22 - y
//                    } else {
//                        y += 0.22
//                    }
//                }
//                if ((arucoMarker.markerId == 1 || arucoMarker.markerId == 2)) {
//                    if (x > 0 && x < 0.355) {
//                        x = -1 * (0.355 - x)
//                    } else {
//                        x -= 0.355
//                    }
//                }
//                if ((arucoMarker.markerId == 3 || arucoMarker.markerId == 4)) {
//                    if (x > 0 && x < 0.355) {
//                        x = 0.355 - x
//                    } else {
//                        x += 0.355
//                    }
//                }
//                //调准机体中心位置相对于aruco码偏前，以达到y轴方向上降落居中
//                if ((arucoMarker.markerId == 19 || arucoMarker.markerId == 43) && (y <= 0 || y in 0.0..0.0965)) {
//                    y -= (0.0965f * 4 / 5)
//                    //y -= 0.0965f
//                }
//                //调准机体中心位置相对于aruco码偏前，以达到x轴方向上降落居中 0.065..0.07685 -0.08360405179000563
//                if ((arucoMarker.markerId == 19 || arucoMarker.markerId == 43) && (x >= 0 || y in 0.0..0.07685)) {
//                    //x = (0.07685 * 4 / 5) + abs(x)
//                    x += (0.0685 * 4 / 5)
//                }
//                if ((arucoMarker.markerId == 19 || arucoMarker.markerId == 43) && x <= 0 && abs(x) <= 0.065) {
//                    x = 0.0685 - abs(x)
//                }
//
//
//                /*if (y > 0 && y < 0.0965) {
//                    y -= 0.0965f
//                }*/
//                var outputX = updateOutSpeed(abs(x))
//                var outputY = updateOutSpeed(abs(y))
//                /*if (arucoMarker.markerId == 43) {
//                    if (outputX > 0.075f) {
//                        outputX = 0.075f
//                    }
//                    if (outputY > 0.075f) {
//                        outputY = 0.075f
//                    }
//                }*/
//                outputX = if (x < 0) -outputX else outputX
//                outputY = if (y > 0) -outputY else outputY
//
//                //如果识别到四个角aruco码，升高扩大相机视野
//                if (arucoMarker.markerId in 1..4 && AircraftInfoManager.aircraftAltitude <= 2.0f) {
//                    downwardSpeed = 1.95f
//                }
//                //如果角度偏差大于10度，不移动飞机，先旋转好角度
//                if (abs(yawCamera).toInt() in 15..165 && AircraftInfoManager.aircraftAltitude >= 3.0f) {
//                    outputX = 0f
//                    outputY = 0f
//                    downwardSpeed = 0f
//                }
//                if (AircraftInfoManager.aircraftAltitude <= 3.0f) {
//                    resultYaw = 0.0
//                }
//                //如果高度小于1.8米，飞机不再下降高度
//                if (AircraftInfoManager.aircraftAltitude <= 1.8f) {
//                    downwardSpeed = 0f
//                }
//                if ( x < 0 && x in -0.035..-0.07685) {
//                    outputX = 0f
//                }
//                if (y > 0.0685 && y <= 0.0865) {
//                    outputY = 0f
//                }
//                //使用虚拟摇杆控制飞机移动
//                val leftHorizontal = calculateActualOffset(resultYaw, 100.0).toInt()
//                val leftVertical = calculateActualOffset(downwardSpeed.toDouble(), 100.0).toInt()
//                VirtualStickManager.getInstance().leftStick.apply {
//                    horizontalPosition = leftHorizontal
//                    verticalPosition = leftVertical
//                }
//
//                val rightHorizontal = calculateActualOffset(outputX.toDouble(), 23.0).toInt()
//                val rightVertical = calculateActualOffset(outputY.toDouble(), 23.0).toInt()
//                VirtualStickManager.getInstance().rightStick.apply {
//                    verticalPosition = rightVertical
//                    horizontalPosition = rightHorizontal
//                }
//                /*LogFileManager.getInstance().log(
//                    TAG,
//                    "ID:${arucoMarker.markerId}  X:$x   Y:$y  outputX:$outputX  outputY:$outputY  rightHorizontal:$rightHorizontal  rightVertical:$rightVertical  Altitude:${AircraftInfoManager.aircraftAltitude}"
//                )*/
//                grayImgMat.release()
//                bitmap.recycle()
//                bmp32.recycle()
//                source.release()
//                ids.release()
//                rvecs.release()
//                tvecs.release()
//                rvec.release()
//                tvec.release()
//                camR.release()
//                _camR.release()
//                R.release()
//                isDetectingAruco = false
//                mFindArucoList.clear()
//                mArucoCornerList.clear()
//            } catch (e: Exception) {
//                e.printStackTrace()
//                isDetectingAruco = false
//                mFindArucoList.clear()
//                mArucoCornerList.clear()
//            }
//        }
//    }
//
//
//    /**
//     * 计算实际偏移量
//     */
//    private fun calculateActualOffset(
//        speed: Double,
//        maxSpeed: Double,
//    ): Double {
//        return speed * 660.0 / (0.25 * maxSpeed)
//    }
//
//
//    /**
//     * 根据预估距离获取移动速度
//     */
//    private fun updateOutSpeed(estimatedDistance: Double): Float {
//        val targetSpeed = if (estimatedDistance >= 4.0) {
//            0.6f
//        } else if (estimatedDistance in 1.5..4.0) {
//            0.3f
//        } else if (estimatedDistance > 0.9 && estimatedDistance < 1.5) {
//            0.25f
//        } else if (estimatedDistance in 0.15..0.9) {
//            0.125f
//        } else if (estimatedDistance >= 0.1 && estimatedDistance < 0.15) {
//            //0.125f
//            //0.0925f
//            0.0825f
//        } else if (estimatedDistance >= 0.07 && estimatedDistance < 0.1) {
//            //0.0375f
//            //0.085f
//            //0.0725f
//            0.0685f
//        } else {
//            //0.0f
//            0.0375f
//        }
//        return targetSpeed
//    }
//
//
//    /**
//     * 旋转矩阵 --> 欧拉角
//     */
//    private fun rotationMatrixToEulerAngles(R: Mat): MutableList<Double> {
//        val sy = sqrt(R[0, 0][0] * R[0, 0][0] + R[1, 0][0] * R[1, 0][0])
//        val singular = sy < 1e-6
//
//        return if (!singular) {
//            val x = atan2(R[2, 1][0], R[2, 2][0])
//            val y = atan2(-R[2, 0][0], sy)
//            val z = atan2(R[1, 0][0], R[0, 0][0])
//            mutableListOf(x, y, z)
//        } else {
//            val x = atan2(-R[1, 2][0], R[1, 1][0])
//            val y = atan2(-R[2, 0][0], sy)
//            val z = 0.0
//            mutableListOf(x, y, z)
//        }
//    }
//
//
//    /**
//     * 退出虚拟摇杆控制，并开始降落
//     */
//    @SuppressLint("CheckResult")
//    private fun exitVirtualStick() {
//        VirtualStickManager.getInstance()
//            .disableVirtualStick(object : CommonCallbacks.CompletionCallback {
//                override fun onSuccess() {
//                    //开始降落
//                    KeyManager.getInstance().performAction(
//                        FlightControllerKey.KeyStartAutoLanding.create(),
//                        null,
//                        null
//                    )
//                }
//
//                override fun onFailure(error: IDJIError) {
//                }
//            })
//    }
//
//
//    /**
//     * 发送虚拟摇杆控制数九
//     */
//    private fun sendVirtualStickCommands(pX: Float, pY: Float, pZ: Float, pYaw: Float) {
//        val verticalJoyControlMaxSpeed = 0.15f
//        val yawJoyControlMaxSpeed = 1f
//        val pitchJoyControlMaxSpeed = 1f
//        val rollJoyControlMaxSpeed = 1f
//
//        val yawResult = (yawJoyControlMaxSpeed * pYaw).toDouble()
//        val throttleResult = (verticalJoyControlMaxSpeed * pZ).toDouble()
//        val pitchResult = (pitchJoyControlMaxSpeed * pX).toDouble()
//        val rollResult = (rollJoyControlMaxSpeed * pY).toDouble()
//
//        val param = VirtualStickFlightControlParam().apply {
//            rollPitchCoordinateSystem = FlightCoordinateSystem.BODY
//            verticalControlMode = VerticalControlMode.VELOCITY
//            yawControlMode = YawControlMode.ANGULAR_VELOCITY
//            rollPitchControlMode = RollPitchControlMode.VELOCITY
//            pitch = pitchResult
//            roll = rollResult
//            yaw = yawResult
//            verticalThrottle = throttleResult
//        }
//        VirtualStickManager.getInstance().sendVirtualStickAdvancedParam(param)
//    }
//
//}