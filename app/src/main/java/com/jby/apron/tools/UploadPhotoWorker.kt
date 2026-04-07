//package com.shd.nest.gateway
//
//import android.provider.MediaStore
//import com.amazonaws.ClientConfiguration
//import com.amazonaws.auth.AWSSessionCredentials
//import com.amazonaws.regions.Region
//import com.amazonaws.regions.Regions
//import com.amazonaws.services.s3.AmazonS3Client
//import com.amazonaws.services.s3.S3ClientOptions
//import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest
//import com.drew.imaging.ImageMetadataReader
//import com.drew.metadata.Metadata
//import com.drew.metadata.exif.ExifIFD0Directory
//import com.drew.metadata.xmp.XmpDirectory
//import com.shd.nest.App
//import com.shd.nest.dji.AircraftInfoManager
//import com.shd.nest.extension.ThreadScope
//import com.shd.nest.gateway.mqtt.MqttClientManager
//import com.shd.nest.gateway.mqtt.MqttMessageListener
//import com.shd.nest.gateway.mqtt.MqttMessageManager
//import com.shd.nest.gateway.mqtt.MqttMethod
//import com.shd.nest.gateway.mqtt.model.OssConfiguration
//import com.shd.nest.util.JsonUtils
//import com.shd.nest.util.LogUtils
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.FlowPreview
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.flow.asFlow
//import kotlinx.coroutines.flow.catch
//import kotlinx.coroutines.flow.flatMapConcat
//import kotlinx.coroutines.flow.flow
//import kotlinx.coroutines.flow.flowOn
//import kotlinx.coroutines.flow.onCompletion
//import kotlinx.coroutines.flow.onEach
//import kotlinx.coroutines.flow.retryWhen
//import kotlinx.coroutines.launch
//import java.io.FilenameFilter
//import java.io.File
//import java.text.SimpleDateFormat
//import java.util.Locale
//
//private const val TAG = "UploadPhotoWorker"
//
//class UploadPhotoWorker private constructor() {
//
//    private val createTimeFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.CHINA)
//    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.CHINA)
//    private val photoNameRegex = """^DJI_\d{14}_\d{4}_(T|V|W)_([a-zA-Z0-9]+)\.JPG$""".toRegex()
//    private val newPhotoNameRegex = Regex("(_[a-zA-Z0-9]+)\\.JPG")
//
//    //所有照片list
//    private val photoFileList: MutableList<File> = mutableListOf()
//    //根据任务id区分开照片list
//    private val taskPhotoMap: MutableMap<String, MutableList<File>> = mutableMapOf()
//
//    fun queryTaskPhotos(sdCardPath: String) {
//        ThreadScope.launch {
//            photoFileList.clear()
//            taskPhotoMap.clear()
//
//            //查询SD卡DCIM目录下的所有照片
//            val djiPhotofilter = FilenameFilter { _, name -> name.endsWith(".JPG") }
//            val photoDirectory = File("$sdCardPath/DCIM")
//            photoDirectory.listFiles()?.forEach { file ->
//                if (file.isDirectory) {
//                    val photoFileArray = file.listFiles(djiPhotofilter)?.toMutableList() ?: mutableListOf()
//                    photoFileList.addAll(photoFileArray)
//                } else {
//                    photoFileList.add(file)
//                }
//            }
//            //根据任务id分组照片
//            photoFileList.forEach { file ->
//                if (photoNameRegex.matches(file.name)) {
//                    val flightId = photoNameRegex.find(file.name)?.groupValues?.getOrNull(2)
//                    if (!flightId.isNullOrEmpty()) {
//                        taskPhotoMap.getOrPut(flightId) { mutableListOf() }.add(file)
//                    }
//                }
//            }
//            //通知工单app的任务照片列表信息
//            AircraftInfoManager.notifyTaskPhotoList(taskPhotoMap)
//        }
//    }
//
//
//    /**
//     * sd卡拔出数据重置
//     */
//    fun resetTaskPhotos() {
//        photoFileList.clear()
//        taskPhotoMap.clear()
//    }
//
//
//    /**
//     * 根据任务id上传该任务的照片到网关
//     */
//    fun uploadTask(flightId: String) {
//        val photoList: MutableList<File> = taskPhotoMap[flightId] ?: mutableListOf()
//        if (photoList.isEmpty()) {
//            LogUtils.d(TAG, "该任务图片上传完了")
//            AircraftInfoManager.notifyPhotoUploadCompleted(flightId, 0, 0)
//            return
//        }
//        //上传任务前需要获取凭证
//        MqttMessageManager.requestUploadConfig()
//        MqttClientManager.getInstance()
//            .listen(MqttMethod.STORAGE_CONFIG, true, object : MqttMessageListener {
//                override fun onMessage(topic: String, method: String, data: Any) {
//                    ThreadScope.launch {
//                        uploadTaskImp(flightId, data as OssConfiguration, photoList)
//                    }
//                }
//        })
//    }
//
//
//    @OptIn(FlowPreview::class)
//    private suspend fun uploadTaskImp(flightId: String, ossConfiguration: OssConfiguration, photoList: MutableList<File>) {
//        LogUtils.d(TAG, "ossConfiguration: ${JsonUtils.toJson(ossConfiguration)}")
//        //创建亚马逊oss client
//        val amazonS3 = AmazonS3Client(
//            object : AWSSessionCredentials {
//                override fun getAWSAccessKeyId(): String {
//                    return ossConfiguration.credentials.access_key_id
//                }
//
//                override fun getAWSSecretKey(): String {
//                    return ossConfiguration.credentials.access_key_secret
//                }
//
//                override fun getSessionToken(): String {
//                    return ossConfiguration.credentials.security_token
//                }
//            },
//            Region.getRegion(Regions.fromName(ossConfiguration.region)),
//            ClientConfiguration()
//        ).apply {
//            endpoint = ossConfiguration.endpoint
//            //endpoint = "http://${GatewayConstant.GatewayIp}:${GatewayConstant.GatewayMinIOPort}"
//            setS3ClientOptions(S3ClientOptions.builder().setPathStyleAccess(true).build())
//        }
//
//        //总照片数
//        val totalPhotoCount = photoList.size
//        //上传出错图片数量
//        var uploadException = 0
//        //循环进行上传
//        photoList.toList().asFlow().onEach {
//            LogUtils.d(TAG, "开始上传 ${it.name}")
//        }.flatMapConcat { file ->
//            // 设置最大重试次数为2
//            val retries = 2
//            flow {
//                val newFileName = file.name.replace(newPhotoNameRegex, ".JPG")
//                LogUtils.d(TAG, "开始上传 $newFileName")
//                //objectKey格式：{object_key_prefix}/{任务ID}/{文件名称}
//                //val objectKey = "${ossConfiguration.object_key_prefix}/$flightId/${file.name}"
//                val objectKey = "${ossConfiguration.object_key_prefix}/$flightId/$newFileName"
//
//                //判断bucket是否存在，如果不存在，创建相应的bucket
//                val bucketExists = amazonS3.doesBucketExist(ossConfiguration.bucket)
//                if (!bucketExists) {
//                    amazonS3.createBucket(ossConfiguration.bucket)
//                }
//                //上传文件到网关MINIO存储服务
//                amazonS3.putObject(
//                    com.amazonaws.services.s3.model.PutObjectRequest(
//                        ossConfiguration.bucket,
//                        objectKey,
//                        file
//                    )
//                )
//
//                //获取文件上传后访问地址url
//                val urlRequest = GeneratePresignedUrlRequest(ossConfiguration.bucket, objectKey)
//                val url = amazonS3.generatePresignedUrl(urlRequest).toString()
//
//                //解析照片exif
//                val metadata: Metadata? = ImageMetadataReader.readMetadata(file)
//                val xmpMap = metadata?.getFirstDirectoryOfType(XmpDirectory::class.java)?.xmpProperties
//                val tagDateTime = metadata?.getFirstDirectoryOfType(ExifIFD0Directory::class.java)?.getString(ExifIFD0Directory.TAG_DATETIME)
//                xmpMap?.let {
//                    val latitude = it["drone-dji:GpsLatitude"]?.toDouble() ?: 0.0
//                    val longitude = it["drone-dji:GpsLongitude"]?.toDouble() ?: 0.0
//                    val absoluteAltitude = it["drone-dji:AbsoluteAltitude"]?.toFloat() ?: 0f
//                    val relativeAltitude = it["drone-dji:RelativeAltitude"]?.toFloat() ?: 0f
//                    val gimbalYawDegree = it["drone-dji:GimbalYawDegree"]?.toFloat() ?: 0f
//                    val droneModel = it["drone-dji:DroneModel"] ?: (it["tiff:Model"] ?: "")
//                    val createTime =  it["xmp:CreateDate"] ?: dateFormat.format(createTimeFormat.parse(tagDateTime!!)!!)
//                    //照片上传完成结果上报到网关
//                    MqttMessageManager.notifyGatewayUploadResult(
//                        flightId,
//                        objectKey,
//                        url,
//                        newFileName,
//                        gimbalYawDegree,
//                        absoluteAltitude,
//                        relativeAltitude,
//                        latitude, longitude,
//                        createTime,
//                        droneModel
//                    )
//                }
//
//                //上传成功后删除SD卡的该照片
//                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
//                    val fileUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
//                    App.context.contentResolver.delete(
//                        fileUri,
//                        "${MediaStore.Files.FileColumns.DATA}=?",
//                        arrayOf<String>(file.absolutePath)
//                    )
//                } else {
//                   file.delete()
//                }
//                emit(Result.success(UploadResult(file, null)))
//            }.retryWhen { cause, attempt ->
//                if (attempt < retries ) {
//                    //延时重试
//                    delay(1000)
//                    return@retryWhen true
//                } else {
//                    return@retryWhen false
//                }
//            }.catch { exception ->
//                uploadException += 1
//                emit(Result.success(UploadResult(file, exception)))
//            }
//        }.flowOn(Dispatchers.IO)
//            .catch { e -> LogUtils.e(TAG, "Error uploading file: ${e.message}") }
//            .onCompletion {
//                AircraftInfoManager.notifyPhotoUploadCompleted(
//                    flightId,
//                    totalPhotoCount,
//                    uploadException
//                )
//            }
//            .collect {
//                it.fold({ result ->
//                    result.exception?.run {
//                        LogUtils.e(TAG, "${result.file.name} 上传失败:${message}")
//                    } ?: run {
//                        LogUtils.d(TAG, "${result.file.name} 上传成功")
//                        photoFileList.remove(result.file)
//                        taskPhotoMap[flightId]?.apply { remove(result.file) }
//                    }
//                    AircraftInfoManager.notifyTaskPhotoUploadResult(
//                        flightId,
//                        result.file.name,
//                        result.exception?.message
//                    )
//                }, { error ->
//                    LogUtils.e(TAG, "上传失败: ${error.message}")
//                })
//            }
//    }
//
//
//    data class UploadResult(val file: File, val exception: Throwable?)
//
//    companion object {
//
//        @Volatile
//        private var instance: UploadPhotoWorker? = null
//
//        fun getInstance(): UploadPhotoWorker {
//            return instance ?: synchronized(this) {
//                instance ?: UploadPhotoWorker().also { instance = it }
//            }
//        }
//    }
//}