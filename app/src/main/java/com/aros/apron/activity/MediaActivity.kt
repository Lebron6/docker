package com.aros.apron.activity

import android.os.Bundle
import android.util.Log
import com.aros.apron.base.BaseActivity
import com.aros.apron.databinding.ActivityGalleryBinding
import com.aros.apron.manager.MediaManager
import com.aros.apron.tools.GalleryAdapter
import com.aros.apron.tools.RecyclerViewHelper
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.KeyTools
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.KeyManager
import dji.v5.manager.datacenter.MediaDataCenter
import dji.v5.manager.datacenter.media.MediaFileListData
import dji.v5.manager.datacenter.media.MediaFileListState
import dji.v5.manager.datacenter.media.PullMediaFileListParam

class MediaActivity : BaseActivity() {

    private var adapter: GalleryAdapter? = null
    private var mediaFileListData: MediaFileListData? = null
    private lateinit var galleryBinding: ActivityGalleryBinding

    override fun useEventBus(): Boolean {
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        galleryBinding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(galleryBinding.root)
        galleryBinding.btnDisable?.setOnClickListener {
            MediaManager.disablePlayback(mqttAndroidClient)
        }
        galleryBinding.btnEnable?.setOnClickListener {
            MediaManager.enablePlayback(mqttAndroidClient)
        }
        galleryBinding.btnRemoveAll?.setOnClickListener {
            deleteMediaFiles()
        }
        galleryBinding.btnRefreshFileList?.setOnClickListener {
            MediaDataCenter.getInstance().mediaManager.pullMediaFileListFromCamera(
                PullMediaFileListParam.Builder().count(20).build(), object :
                    CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        Log.e("MediaActivity", "fetch success")
                    }

                    override fun onFailure(error: IDJIError) {
                        Log.e("MediaActivity", "fetch failed$error")
                    }
                })
        }
        adapter = GalleryAdapter()
        RecyclerViewHelper.initRecyclerViewG(this, galleryBinding.rvPic, adapter, 2)
//            addMediaFileStateListener()

    }


    private fun addMediaFileStateListener() {
        val isConnect =
            KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection))
        if (isConnect != null && isConnect) {
            val mediaManager = MediaDataCenter.getInstance().mediaManager
            mediaManager.addMediaFileListStateListener { mediaFileListState ->
                Log.e("MediaActivity", "状态" + mediaFileListState.name)
                if (mediaFileListState == MediaFileListState.UP_TO_DATE) {
                    mediaFileListData = mediaManager.mediaFileListData
                    Log.e("MediaActivity", "媒体文件数量" + mediaManager.mediaFileListData.data.size)

                    runOnUiThread {
                        adapter?.setData(mediaFileListData)
                    }
                }
            }
        } else {
            Log.e("MediaActivity", "设备未连接")
        }
    }

    //删除
    private fun deleteMediaFiles() {
        val mediaManager = MediaDataCenter.getInstance().mediaManager
        Log.e("MediaActivity", "清除文件数量" + mediaFileListData?.data?.size+"")
        mediaManager.deleteMediaFiles(mediaFileListData?.data,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    TODO("Not yet implemented")
                    adapter?.setData(mediaFileListData)
                }

                override fun onFailure(p0: IDJIError) {
                    Log.e("MediaActivity", "清除文件失败" + p0.description())
                }
            })

    }
}