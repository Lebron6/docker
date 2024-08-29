package com.aros.apron.manager

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
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest
import com.amazonaws.services.s3.model.ProgressEvent
import com.amazonaws.services.s3.model.ProgressListener
import com.amazonaws.services.s3.model.PutObjectRequest
import com.aros.apron.base.BaseManager
import com.aros.apron.entity.FileUploadResult
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
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.eclipse.paho.android.service.MqttAndroidClient
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


object MediaManager : BaseManager() {

    private val mediaFileDir = "/apronPic"
    private var mState: MediaFileListState? = null
    private var mediaFiles: List<MediaFile>? = ArrayList()
    private var mqttClient : MqttAndroidClient?=null

    fun init(mqttAndroidClient: MqttAndroidClient) {
        mqttClient=mqttAndroidClient
        val isConnect =
            KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection))
        if (isConnect != null && isConnect) {
            val mediaManager = MediaDataCenter.getInstance().mediaManager
            mediaManager.addMediaFileListStateListener { mediaFileListState ->
                mState = mediaFileListState
                Log.e(TAG, "当前媒体文件状态：" + mediaFileListState.name)
            }
        }
    }

    var enablePlayBackTimes: Int = 0
    var enablePlayBackSuccess: Boolean = false

    //删除媒体文件和预览视频回放需要相机进入到回放模式，即调用enable接口,进入媒体模式
    fun enablePlayback(mqttAndroidClient: MqttAndroidClient) {
        MediaDataCenter.getInstance().mediaManager.enable(object : CompletionCallback {
            override fun onSuccess() {
                Log.e(TAG, "enablePlayback Success")
                pullMediaFileListFromCamera(mqttAndroidClient)
                enablePlayBackTimes = 0
                enablePlayBackSuccess = true
            }

            override fun onFailure(idjiError: IDJIError) {
                if (!enablePlayBackSuccess) {
                    if (enablePlayBackTimes < 5) {
                        enablePlayBackTimes++
                        LogUtil.log(TAG, "第${enablePlayBackTimes}次进入媒体模式失败:${Gson().toJson(idjiError)}")
                        Handler().postDelayed(Runnable {
                            enablePlayback(mqttAndroidClient)
                        }, 2000)
                    } else {
                        LogUtil.log(TAG, "进入媒体模式失败:${idjiError.description()}")
                        sendMissionExecuteEvents(mqttClient,"媒体模式进入失败:等待归中关机")
                        SystemManager.getInstance().isMediaFilePushOver = true
//                        if (SystemManager.getInstance().isMediaFilePushOver) {
                        if (SystemManager.getInstance().isMediaFilePushOver && SystemManager.getInstance().isItCentered) {
                            DroneShutdownManager.getInstance().sendDroneShutDownMsg2Server(mqttAndroidClient)
                        }
                    }

                }

            }
        })
    }

    fun removeAllFiles(mqttAndroidClient: MqttAndroidClient) {
        MediaDataCenter.getInstance().mediaManager.deleteMediaFiles(
            mediaFiles,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    LogUtil.log(TAG, "清除文件成功 ")
                    sendMissionExecuteEvents(mqttClient,"媒体文件已清除")
                    disablePlayback(mqttAndroidClient)
                }

                override fun onFailure(p0: IDJIError) {
                    LogUtil.log(TAG, "清除文件失败: ${p0.description()} ")
                    sendMissionExecuteEvents(mqttClient,"媒体文件清除失败")
                    SystemManager.getInstance().isMediaFilePushOver = true
//                    if (SystemManager.getInstance().isMediaFilePushOver) {
                    if (SystemManager.getInstance().isMediaFilePushOver && SystemManager.getInstance().isItCentered) {
                        DroneShutdownManager.getInstance().sendDroneShutDownMsg2Server(mqttAndroidClient)
                    }
                }
            })
    }

    //退出媒体模式
    fun disablePlayback(mqttAndroidClient: MqttAndroidClient) {
        MediaDataCenter.getInstance().mediaManager.disable(object : CompletionCallback {
            override fun onSuccess() {
                LogUtil.log(TAG, "退出媒体模式成功")
                sendMissionExecuteEvents(mqttClient,"退出媒体模式")
                SystemManager.getInstance().isMediaFilePushOver = true
//                if (SystemManager.getInstance().isMediaFilePushOver) {
                if (SystemManager.getInstance().isMediaFilePushOver && SystemManager.getInstance().isItCentered) {
                    DroneShutdownManager.getInstance().sendDroneShutDownMsg2Server(mqttAndroidClient)
                    LogUtil.log(TAG, "发送关闭无人机")

                }
            }

            override fun onFailure(idjiError: IDJIError) {
                sendMissionExecuteEvents(mqttClient,"退出媒体模式失败")
                LogUtil.log(TAG, "退出媒体模式失败:${idjiError.description()}")
                SystemManager.getInstance().isMediaFilePushOver = true
//                if (SystemManager.getInstance().isMediaFilePushOver) {
                if (SystemManager.getInstance().isMediaFilePushOver && SystemManager.getInstance().isItCentered) {
                    DroneShutdownManager.getInstance().sendDroneShutDownMsg2Server(mqttAndroidClient)
                    LogUtil.log(TAG, "发送关闭无人机")
                }
            }
        })
    }

    private var downLoadMediaFileIndex = 0

    //从相机拉取媒体文件
    fun pullMediaFileListFromCamera(mqttAndroidClient: MqttAndroidClient) {
        MediaDataCenter.getInstance().mediaManager.pullMediaFileListFromCamera(
            PullMediaFileListParam.Builder().count(-1).build(),
            object : CompletionCallback {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onSuccess() {
                    Handler().postDelayed(Runnable {
                        if (mState == MediaFileListState.UP_TO_DATE) {
                            mediaFiles =
                                MediaDataCenter.getInstance().mediaManager.mediaFileListData.data
                            if (mediaFiles != null && mediaFiles!!.isNotEmpty()) {
                                pullOriginalMediaFileFromCamera(mqttAndroidClient)
                            } else {
                                LogUtil.log(TAG, "拉取媒体文件为空")
                                sendMissionExecuteEvents(mqttClient,"拉取媒体文件为空")
                                disablePlayback(mqttAndroidClient)
                            }
                        } else {
                            sendMissionExecuteEvents(mqttClient,"拉取媒体文件失败,当前状态:$mState")
                            LogUtil.log(TAG, "拉取媒体文件失败,当前状态:$mState")
                            disablePlayback(mqttAndroidClient)
                        }
                    }, 2000)

                }

                override fun onFailure(idjiError: IDJIError) {
                    LogUtil.log(TAG, "拉取媒体文件失败:" + Gson().toJson(idjiError))
                    sendMissionExecuteEvents(mqttClient,"拉取媒体文件失败")
                    disablePlayback(mqttAndroidClient)
                }
            })
    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun pullOriginalMediaFileFromCamera(mqttAndroidClient: MqttAndroidClient) {


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
                removeAllFiles(mqttAndroidClient)
            } else {
                LogUtil.log(TAG, "跳过一段文件下载:${mediaFile.fileName}")

                pullOriginalMediaFileFromCamera(mqttAndroidClient)
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
                    minIOUpLoad(mqttAndroidClient, file, mediaFile)
                    try {
                        outputStream.close()
                        bos.close()
                    } catch (error: IOException) {
                        LogUtil.log(TAG, "文件$downLoadMediaFileIndex  error: ${error.message}")
                        SystemManager.getInstance().isMediaFilePushOver = true
//                        if (SystemManager.getInstance().isMediaFilePushOver) {
                        if (SystemManager.getInstance().isMediaFilePushOver && SystemManager.getInstance().isItCentered) {
                            DroneShutdownManager.getInstance().sendDroneShutDownMsg2Server(mqttAndroidClient)
                        }
                    }
                }

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
                    sendMissionExecuteEvents(mqttClient,"第 $downLoadMediaFileIndex 张图片下载失败")
                    downLoadMediaFileIndex=0
                    SystemManager.getInstance().isMediaFilePushOver = true
//                    if (SystemManager.getInstance().isMediaFilePushOver) {
                    if (SystemManager.getInstance().isMediaFilePushOver && SystemManager.getInstance().isItCentered) {
                        DroneShutdownManager.getInstance().sendDroneShutDownMsg2Server(mqttAndroidClient)
                    }
                }
            })
    }


    private fun minIOUpLoad(
        mqttAndroidClient: MqttAndroidClient,
        file: File,
        mediaFile: MediaFile
    ) {

        Observable.create<String> { emitter ->
            //服务器地址
            s3.endpoint = PreferenceUtils.getInstance().uploadUrl //http://ip:端口号
            val bucketExists = s3.doesBucketExist(PreferenceUtils.getInstance().bucketName)
            if (!bucketExists) {
                s3.createBucket(PreferenceUtils.getInstance().bucketName)
            }
            //上传文件到网关MINIO存储服务
            s3.putObject(
                PutObjectRequest(
                    PreferenceUtils.getInstance().bucketName,
                    "/${PreferenceUtils.getInstance().key}/${PreferenceUtils.getInstance().sortiesId}/${mediaFile.fileName}",
                    file
                ).withProgressListener { progressEvent ->
                    when (progressEvent?.eventCode) {
                        ProgressEvent.PREPARING_EVENT_CODE -> {
                            LogUtil.log(TAG, "第${downLoadMediaFileIndex}张图片上传准备")
                        }

                        ProgressEvent.STARTED_EVENT_CODE -> {
                            val bytesTransferred = progressEvent.bytesTransferred
                            val percentage = (bytesTransferred * 100 / file.length()).toInt()
                            LogUtil.log(TAG, "第${downLoadMediaFileIndex}张图片上传已开始$percentage% (${bytesTransferred} out of ${file.length()} bytes)")
                        }

                        ProgressEvent.COMPLETED_EVENT_CODE -> {
                            LogUtil.log(TAG, "第${downLoadMediaFileIndex}张图片上传完成")
                        }

                        ProgressEvent.FAILED_EVENT_CODE -> {
                            LogUtil.log(TAG, "第${downLoadMediaFileIndex}张图片上传失败")
                        }

                        ProgressEvent.RESET_EVENT_CODE -> {
                            LogUtil.log(TAG, "第${downLoadMediaFileIndex}张图片上传重置")
                        }
                    }
                },
            )
            //获取文件上传后访问地址url
            val urlRequest = GeneratePresignedUrlRequest(
                PreferenceUtils.getInstance().bucketName,
                "/${PreferenceUtils.getInstance().key}/${mediaFile.fileName}"
            )
            val url = s3.generatePresignedUrl(urlRequest).toString()
            //文件上传后访问地址url
            emitter.onNext(url)
            emitter.onComplete()
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread()).subscribe(object : Observer<String> {
                override fun onSubscribe(d: Disposable) {
                }
                override fun onNext(s: String) {
                    var fileUploadResult = FileUploadResult().apply {
                        this.fileName = mediaFile.fileName
                        this.fileSize = mediaFile.fileSize
                        this.fileNum = mediaFiles!!.size
                        this.buckName = PreferenceUtils.getInstance().bucketName
                        this.objectKey = PreferenceUtils.getInstance().key
                        this.sortiesId = PreferenceUtils.getInstance().sortiesId
                        this.url = PreferenceUtils.getInstance().uploadUrl
                        this.offIndex = downLoadMediaFileIndex
                        this.flightId = PreferenceUtils.getInstance().flightId
                    }
                    sendFileUploadCallback(mqttAndroidClient, fileUploadResult)
                }

                @RequiresApi(Build.VERSION_CODES.O)
                override fun onError(e: Throwable) {
                    //每上传失败一张就清除缓存
                    FileUtil.deleteFile(file)
                    LogUtil.log(TAG, "第$downLoadMediaFileIndex 张图片上传错误${e.message}")

                    downLoadMediaFileIndex++
                    if (downLoadMediaFileIndex == mediaFiles?.size) {
                        //这里指的是所有文件已经下载完成或失败,清空SD卡,缓存,退出媒体模式发送无人机关机
                        downLoadMediaFileIndex=0
                        removeAllFiles(mqttAndroidClient)
                    } else {
                        pullOriginalMediaFileFromCamera(mqttAndroidClient)
                    }
                }

                @RequiresApi(Build.VERSION_CODES.O)
                override fun onComplete() {
                    //每上传一张就清除缓存
                    FileUtil.deleteFile(file)
                    LogUtil.log(TAG, "第${downLoadMediaFileIndex}张图片上传成功")
                    sendMissionExecuteEvents(mqttClient,"第 $downLoadMediaFileIndex 张图片已上传")

                    downLoadMediaFileIndex++
                    if (downLoadMediaFileIndex == mediaFiles?.size) {
                        //这里指的是所有文件已经下载完成或失败,清空SD卡,缓存,退出媒体模式发送无人机关机
                        sendMissionExecuteEvents(mqttClient,"媒体文件上传完成")
                        removeAllFiles(mqttAndroidClient)
                        downLoadMediaFileIndex == 0
                    } else {
                        pullOriginalMediaFileFromCamera(mqttAndroidClient)
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

    private var s3 = AmazonS3Client(object : AWSCredentials {
        override fun getAWSAccessKeyId(): String {
            return PreferenceUtils.getInstance().accessKey //minio的key
        }

        override fun getAWSSecretKey(): String {
            return PreferenceUtils.getInstance().secretKey //minio的密钥
        }
    }, Region.getRegion(Regions.US_EAST_1), ClientConfiguration())


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

    //写入exif信息
    fun setMediaFileXMPCustomInfo(
        client: MqttAndroidClient,
        message: MQMessage
    ) {
        MediaDataCenter.getInstance().mediaManager.setMediaFileXMPCustomInfo(
            message.xmpInfo,
            object :
                CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    sendMsg2Server(client, message)
                    sendMissionExecuteEvents(client,"设置文件XMP:${message.xmpInfo}")
                }

                override fun onFailure(error: IDJIError) {
                    sendMsg2Server(client, message, "写入exif失败: ${Gson().toJson(error)}")
                }
            })
    }


    private fun checkSDCard(): Boolean {
        return TextUtils.equals(Environment.MEDIA_MOUNTED, Environment.getExternalStorageState())
    }
}




