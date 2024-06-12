//
//
//import android.media.MediaCodec
//import android.util.Pair
//import com.pedro.common.ConnectChecker
//import com.pedro.common.VideoCodec
//import com.pedro.rtmp.rtmp.RtmpClient
//import dji.sdk.keyvalue.value.common.ComponentIndexType
//import dji.v5.manager.datacenter.MediaDataCenter
//import dji.v5.manager.datacenter.camera.StreamInfo
//import dji.v5.manager.interfaces.ICameraStreamManager
//import java.nio.ByteBuffer
//
//class RtmpPublisher private constructor() : ConnectChecker, ICameraStreamManager.ReceiveStreamListener {
//
//    private val rtmpUrl = "rtmp://192.168.1.4:1935/live/12345"
//    private val mVideoInfo = MediaCodec.BufferInfo()
//    private val mRtmpClient by lazy {
//        RtmpClient(this).apply {
//            setOnlyVideo(true)
//            setReTries(Integer.MAX_VALUE)
//            setFps(30)
//            resizeCache(10 * 1024)
//            setLogs(false)
//            setVideoCodec(VideoCodec.H265)
//        }
//    }
//
//    fun startLiveStream() {
//        MediaDataCenter.getInstance().cameraStreamManager.addReceiveStreamListener(
//            ComponentIndexType.LEFT_OR_MAIN, this@RtmpPublisher
//        )
//    }
//
//    fun stopLiveStream() {
//        mRtmpClient.disconnect()
//        MediaDataCenter.getInstance().cameraStreamManager.removeReceiveStreamListener(this@RtmpPublisher)
//    }
//
//    override fun onReceiveStream(data: ByteArray, offset: Int, length: Int, info: StreamInfo) {
//        mVideoInfo.apply {
//            size = length
//            this.offset = offset
//            presentationTimeUs = System.nanoTime() / 1000
//            flags = if (info.isKeyFrame) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
//        }
//        if (info.isKeyFrame && !mRtmpClient.isStreaming) {
//            when (info.mimeType) {
//                ICameraStreamManager.MimeType.H264 -> decodeSpsPpsFromH264(
//                    data,
//                    length
//                )?.let { result ->
//                    mRtmpClient.setVideoCodec(VideoCodec.H264)
//                    mRtmpClient.setVideoInfo(result.first, result.second, null)
//                    mRtmpClient.connect(rtmpUrl, true)
//                }
//                ICameraStreamManager.MimeType.H265 -> decodeSpsPpsFromH265(data)?.let { result ->
//                    mRtmpClient.setVideoCodec(VideoCodec.H265)
//                    mRtmpClient.setVideoInfo(
//                        ByteBuffer.wrap(result.first),
//                        ByteBuffer.wrap(result.second),
//                        ByteBuffer.wrap(result.third)
//                    )
//                    mRtmpClient.connect(rtmpUrl, true)
//                }
//            }
//        }
//        if (mRtmpClient.isStreaming) {
//            val videoBuffer = ByteBuffer.wrap(data)
//            mRtmpClient.sendVideo(videoBuffer, mVideoInfo)
//        }
//    }
//
//    override fun onAuthError() {}
//
//    override fun onAuthSuccess() {}
//
//    override fun onConnectionFailed(reason: String) {
//        mRtmpClient.reConnect(200L)
//    }
//
//    override fun onConnectionStarted(url: String) {}
//
//    override fun onConnectionSuccess() {}
//
//    override fun onDisconnect() {}
//
//    override fun onNewBitrate(bitrate: Long) {}
//
//    private fun decodeSpsPpsFromH265(videoByteArray: ByteArray): Triple<ByteArray, ByteArray, ByteArray>? {
//        var spsIndex = -1
//        var ppsIndex = -1
//        var vpsIndex = -1
//        var ppsEndIndex = -1
//
//        var index = 0
//        while (index + 3 < videoByteArray.size) {
//            if (videoByteArray[index].toInt() == 0x00 &&
//                videoByteArray[index + 1].toInt() == 0x00 &&
//                videoByteArray[index + 2].toInt() == 0x00 &&
//                videoByteArray[index + 3].toInt() == 0x01
//            ) {
//                val nalType = (videoByteArray[index + 4].toInt() and 0x7E) ushr 1
//                println("nalType: $nalType")
//                when (nalType) {
//                    // VPS
//                    32 -> vpsIndex = index
//                    // SPS
//                    33 -> spsIndex = index
//                    // PPS
//                    34 -> ppsIndex = index
//                    // PPS
//                    20 -> {
//                        ppsEndIndex = index
//                        break
//                    }
//                }
//            }
//            index++
//        }
//        println("vpsIndex: $vpsIndex  spsIndex:$spsIndex   ppsIndex:$ppsIndex")
//
//        if (spsIndex == -1 || ppsIndex == -1 || ppsEndIndex == -1) {
//            return null
//        }
//
//        val vpsLength = spsIndex - vpsIndex
//        val spsLength = ppsIndex - spsIndex
//        val ppsLength = ppsEndIndex - ppsIndex
//
//        val vps = ByteArray(vpsLength).apply {
//            System.arraycopy(videoByteArray, vpsIndex, this, 0, vpsLength)
//        }
//        val sps = ByteArray(spsLength).apply {
//            System.arraycopy(videoByteArray, spsIndex, this, 0, spsLength)
//        }
//        val pps = ByteArray(ppsLength).apply {
//            System.arraycopy(videoByteArray, ppsIndex, this, 0, ppsLength)
//        }
//
//        println("VPS: ${vps.joinToString(", ")}")
//        println("SPS: ${sps.joinToString(", ")}")
//        println("PPS: ${pps.joinToString(", ")}")
//
//        return Triple(sps, pps, vps)
//    }
//
//
//    private fun decodeSpsPpsFromH264(
//        videoArray: ByteArray,
//        length: Int
//    ): Pair<ByteBuffer, ByteBuffer>? {
//        var spsIndex = -1
//        var ppsIndex = -1
//        val ppsEndIndex = length - 1
//        for (i in 0 until length) {
//            if (videoArray[i].toInt() == 0x00 && videoArray[i + 1].toInt() == 0x00
//                && videoArray[i + 2].toInt() == 0x00 && videoArray[i + 3].toInt() == 0x01
//            ) {
//                if (videoArray[i + 4].toInt() == 0x67) {
//                    spsIndex = i
//                } else if (videoArray[i + 4].toInt() == 0x68) {
//                    ppsIndex = i
//                }
//            }
//        }
//        val spsLength = ppsIndex - spsIndex
//        val ppsLength = ppsEndIndex - ppsIndex
//        if (spsIndex != -1 && ppsIndex != -1) {
//            val sps = ByteArray(spsLength)
//            System.arraycopy(videoArray, spsIndex, sps, 0, spsLength)
//            val pps = ByteArray(ppsLength)
//            System.arraycopy(videoArray, ppsIndex, pps, 0, ppsLength)
//            return Pair.create(ByteBuffer.wrap(sps), ByteBuffer.wrap(pps))
//        }
//        return null
//    }
//
//    companion object {
//
//        const val TAG: String = "RtmpPublisher"
//
//        @Volatile
//        private var instance: RtmpPublisher? = null
//
//        fun getInstance(): RtmpPublisher {
//            return instance ?: synchronized(this) {
//                instance ?: RtmpPublisher().also { instance = it }
//            }
//        }
//    }
//}