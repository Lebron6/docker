package com.aros.apron.manager

import android.annotation.SuppressLint
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.text.TextUtils
import android.util.Log
import androidx.annotation.RequiresApi
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.aros.apron.base.BaseManager
import com.aros.apron.entity.MQMessage
import com.aros.apron.tools.LogUtil
import com.aros.apron.tools.PreferenceUtils
import com.autonavi.base.amap.mapcore.FileUtil
import com.google.gson.Gson
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.value.camera.MediaFileType
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.callback.CommonCallbacks.CompletionCallback
import dji.v5.common.error.IDJIError
import dji.v5.manager.KeyManager
import dji.v5.manager.datacenter.MediaDataCenter
import dji.v5.manager.datacenter.media.MediaFile
import dji.v5.manager.datacenter.media.MediaFileDownloadListener
import dji.v5.manager.datacenter.media.MediaFileListState
import dji.v5.manager.datacenter.media.PullMediaFileListParam
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


object MediaPushFormManager : BaseManager() {

    private val mediaFileDir = "/apronPic"
    private var mediaFiles: List<MediaFile>? = ArrayList()
    private var mqttClient : MqttAndroidClient?=null

    fun init(mqttAndroidClient: MqttAndroidClient) {
        mqttClient=mqttAndroidClient
    }

    //删除媒体文件和预览视频回放需要相机进入到回放模式，即调用enable接口,进入媒体模式
    fun enablePlayback() {
        MediaDataCenter.getInstance().mediaManager.enable(object : CompletionCallback {
            override fun onSuccess() {
                Log.e(TAG, "enablePlayback Success")
                pullMediaFileListFromCamera()

            }

            override fun onFailure(idjiError: IDJIError) {
                DroneShutdownManager.getInstance().sendDroneShutDownMsg2Server(mqttClient)
                LogUtil.log(TAG, "进入媒体模式失败:${idjiError.description()}")
                sendMissionExecuteEvents(mqttClient, "媒体模式进入失败:关机")

            }
        })
    }

    fun removeAllFiles() {
        MediaDataCenter.getInstance().mediaManager.deleteMediaFiles(
            mediaFiles,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    LogUtil.log(TAG, "清除文件成功 ")
                    DroneShutdownManager.getInstance().sendDroneShutDownMsg2Server(mqttClient)

                    sendMissionExecuteEvents(mqttClient,"媒体文件已清除")
                    disablePlayback()
                    LogUtil.log(TAG, "发送关闭无人机")
                }

                override fun onFailure(p0: IDJIError) {
                    DroneShutdownManager.getInstance().sendDroneShutDownMsg2Server(mqttClient)

                    LogUtil.log(TAG, "清除文件失败: ${p0.description()} ")
                    sendMissionExecuteEvents(mqttClient, "媒体文件清除失败")
                    LogUtil.log(TAG, "发送关闭无人机")
                }
            })
    }

    //退出媒体模式
    fun disablePlayback() {
        MediaDataCenter.getInstance().mediaManager.disable(object : CompletionCallback {
            override fun onSuccess() {
                LogUtil.log(TAG, "退出媒体模式成功")
                sendMissionExecuteEvents(mqttClient,"退出媒体模式")

            }

            override fun onFailure(idjiError: IDJIError) {
                sendMissionExecuteEvents(mqttClient,"退出媒体模式失败")
                LogUtil.log(TAG, "退出媒体模式失败:${idjiError.description()}")
            }
        })
    }

    private var downLoadMediaFileIndex = 0

    //从相机拉取媒体文件
    fun pullMediaFileListFromCamera() {
        MediaDataCenter.getInstance().mediaManager.pullMediaFileListFromCamera(
            PullMediaFileListParam.Builder().count(-1).build(),
            object : CompletionCallback {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onSuccess() {
                    Handler().postDelayed(Runnable {
                        if (MediaDataCenter.getInstance().mediaManager.mediaFileListState == MediaFileListState.UP_TO_DATE) {
                            mediaFiles =
                                MediaDataCenter.getInstance().mediaManager.mediaFileListData.data
                            if (mediaFiles != null && mediaFiles!!.isNotEmpty()) {
                                pullOriginalMediaFileFromCamera()
                            } else {
                                LogUtil.log(TAG, "拉取媒体文件为空")
                                DroneShutdownManager.getInstance().sendDroneShutDownMsg2Server(mqttClient)

                                sendMissionExecuteEvents(mqttClient,"拉取媒体文件为空")
                                disablePlayback()
                                LogUtil.log(TAG, "发送关闭无人机")
                            }
                        } else {
                            DroneShutdownManager.getInstance().sendDroneShutDownMsg2Server(mqttClient)

                            sendMissionExecuteEvents(mqttClient,"拉取媒体文件失败,当前状态:"+MediaDataCenter.getInstance().mediaManager.mediaFileListState)
                            LogUtil.log(TAG, "拉取媒体文件失败,当前状态:"+MediaDataCenter.getInstance().mediaManager.mediaFileListState)
                            disablePlayback()
                            LogUtil.log(TAG, "发送关闭无人机")
                        }
                    }, 2000)

                }

                override fun onFailure(idjiError: IDJIError) {
                    DroneShutdownManager.getInstance().sendDroneShutDownMsg2Server(mqttClient)

                    LogUtil.log(TAG, "拉取媒体文件失败:" + Gson().toJson(idjiError))
                    sendMissionExecuteEvents(mqttClient,"拉取媒体文件失败")
                    disablePlayback()
                    LogUtil.log(TAG, "发送关闭无人机")
                }
            })
    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun pullOriginalMediaFileFromCamera() {


        val mediaFile = mediaFiles!![downLoadMediaFileIndex]
        if ((!PreferenceUtils.getInstance().needUpLoadVideo && mediaFile.fileType == MediaFileType.MP4)
            || !mediaFile.fileName.contains(
                LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyyMMdd")
                )
            )
        ) {
            downLoadMediaFileIndex++
            if (downLoadMediaFileIndex == mediaFiles?.size) {
                //这里指的是所有文件已经下载完成或失败,清空SD卡,缓存,退出媒体模式发送无人机关机
                downLoadMediaFileIndex=0
                removeAllFiles()
            } else {
                LogUtil.log(TAG, "跳过一段文件下载:${mediaFile.fileName}")

                pullOriginalMediaFileFromCamera()
            }
            return
        }
        LogUtil.log(TAG, "文件大小:" + mediaFile.fileSize)
        val dirs = File(
            getSDCardPath() + mediaFileDir
        )
        if (!dirs.exists()) {
            dirs.mkdir()
        }
        val filePath = getSDCardPath() + mediaFileDir + "/" + mediaFile.fileName
        val file = File(filePath)
        var offset = 0L
        if (file.exists()) {
            offset = file.length()
        }
        val outputStream = FileOutputStream(file, true)
        var beginTime = System.currentTimeMillis()
        val bos = BufferedOutputStream(outputStream)

        mediaFile.pullOriginalMediaFileFromCamera(
            0L,
            object : MediaFileDownloadListener {
                override fun onStart() {}
                override fun onProgress(total: Long, current: Long) {
                    val tmpProgress = (1.0 * current / total * 100).toInt()
                    Log.e(
                        TAG,
                        "第" + downLoadMediaFileIndex + "张文件:" + mediaFile.fileName + "下载进度:" + tmpProgress
                    )
                }

                override fun onRealtimeDataUpdate(
                    data: ByteArray,
                    position: Long
                ) {
                    try {
                        bos.write(data)
                        bos.flush()
                    } catch (e: IOException) {
                        //这里处理保存文件失败的问题
                        Log.e(TAG, "write error" + e.message)
                    }
                }

                override fun onFinish() {
                    LogUtil.log(TAG, "第${downLoadMediaFileIndex}张图片下载成功")
                    okHttpUpLoad(file, mediaFile)
                    try {
                        outputStream.close()
                        bos.close()
                    } catch (error: IOException) {
                        LogUtil.log(TAG, "文件$downLoadMediaFileIndex  error: ${error.message}")

                            DroneShutdownManager.getInstance().sendDroneShutDownMsg2Server(
                                mqttClient)
                    }
                }

                @SuppressLint("SuspiciousIndentation")
                override fun onFailure(error: IDJIError) {
                    //决定下载某张照片失败后是否关机
                    LogUtil.log(
                        TAG,
                        "第 $downLoadMediaFileIndex 张图片${mediaFile.fileName} 下载失败: ${
                            Gson().toJson(
                                error
                            )
                        } "
                    )
                    DroneShutdownManager.getInstance().sendDroneShutDownMsg2Server(mqttClient)
                    sendMissionExecuteEvents(mqttClient,"第 $downLoadMediaFileIndex 张图片下载失败")
                    downLoadMediaFileIndex=0
                }
            })
    }

    private fun okHttpUpLoad(file: File, mediaFile: MediaFile) {

        val client = OkHttpClient().newBuilder()
            .build()
        val body: RequestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart(
                "file", file.name,
                RequestBody.create(
                    "application/octet-stream".toMediaTypeOrNull(),
                    file
                )
            )
            .addFormDataPart("taskId", 12.toString())
            .build()
        val request: Request = Request.Builder()
            .url(PreferenceUtils.getInstance().wtUrl + "/api/windfarm/file/upload")
            .method("POST", body)
            .build()
        client.newCall(request).enqueue(object : Callback {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onFailure(call: Call, e: IOException) {
                //每上传失败一张就清除缓存
                FileUtil.deleteFile(file)
                LogUtil.log(TAG, "第${downLoadMediaFileIndex} 张图片上传错误${e.message}")

                downLoadMediaFileIndex++
                if (downLoadMediaFileIndex == mediaFiles?.size) {
                    //这里指的是所有文件已经下载完成或失败,清空SD卡,缓存,退出媒体模式发送无人机关机
                    downLoadMediaFileIndex =0
                    removeAllFiles()
                } else {
                    pullOriginalMediaFileFromCamera()
                }
            }

            @RequiresApi(Build.VERSION_CODES.O)
            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                FileUtil.deleteFile(file)
                LogUtil.log(TAG, "第${downLoadMediaFileIndex}张图片上传成功")
                sendMissionExecuteEvents(mqttClient,"第 $downLoadMediaFileIndex 张图片已上传")

               downLoadMediaFileIndex++
                if (downLoadMediaFileIndex == mediaFiles?.size) {
                    //这里指的是所有文件已经下载完成或失败,清空SD卡,缓存,退出媒体模式发送无人机关机
                    sendMissionExecuteEvents(mqttClient,"媒体文件上传完成")
                    removeAllFiles()
                    downLoadMediaFileIndex == 0
                } else {
                   pullOriginalMediaFileFromCamera()
                }
            }
        })
    }


    //清空
    fun remove() {
        MediaDataCenter.getInstance().mediaManager.enable(object : CompletionCallback {
            override fun onSuccess() {}
            override fun onFailure(idjiError: IDJIError) {}
        })
    }



    private fun getSDCardPath(): String? {
        var sdCardPathString: String? = ""
        sdCardPathString = if (checkSDCard()) {
            Environment.getExternalStorageDirectory()
                .path
        } else {
            Environment.getExternalStorageDirectory()
                .parentFile.path
        }
        return sdCardPathString
    }


    private fun checkSDCard(): Boolean {
        return TextUtils.equals(Environment.MEDIA_MOUNTED, Environment.getExternalStorageState())
    }
}




