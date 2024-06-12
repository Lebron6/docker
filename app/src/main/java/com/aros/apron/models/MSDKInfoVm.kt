package com.aros.apron.models

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.aros.apron.entity.MSDKInfo
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.ProductKey
import dji.v5.et.create
import dji.v5.et.get
import dji.v5.et.listen
import dji.v5.manager.KeyManager
import dji.v5.manager.SDKManager
import dji.v5.manager.ldm.LDMManager
import dji.v5.utils.common.LogUtils
import java.util.concurrent.atomic.AtomicBoolean


/**
 * 提供SDKInfo View相关接口，操作MSDKInfoModel相关功能
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
 class MSDKInfoVm : ViewModel() {

    val tag: String = LogUtils.getTag(this)

    val msdkInfo = MutableLiveData<MSDKInfo>()
    val mainTitle = MutableLiveData<String>()
    private val isInited = AtomicBoolean(false)
    private val msdkInfoModel: MSDKInfoModel = MSDKInfoModel()

    init {
        msdkInfo.value = MSDKInfo(msdkInfoModel.getSDKVersion())
        msdkInfo.value?.buildVer = msdkInfoModel.getBuildVersion()
        msdkInfo.value?.isDebug = msdkInfoModel.isDebug()
        msdkInfo.value?.packageProductCategory = msdkInfoModel.getPackageProductCategory()
        msdkInfo.value?.isLDMEnabled = LDMManager.getInstance().isLDMEnabled.toString()
        msdkInfo.value?.isLDMLicenseLoaded = LDMManager.getInstance().isLDMLicenseLoaded.toString()
        msdkInfo.value?.coreInfo = msdkInfoModel.getCoreInfo()


        refreshMSDKInfo()
    }

    override fun onCleared() {
        removeListener()
    }

    fun refreshMSDKInfo() {
        msdkInfo.postValue(msdkInfo.value)
    }

    /**
     * 需要在register成功后，再调用
     */
    fun initListener() {
        if (!SDKManager.getInstance().isRegistered) {
            return
        }
        if (isInited.getAndSet(true)) {
            return
        }
        FlightControllerKey.KeyConnection.create().listen(this) {
            LogUtils.i(tag, "KeyConnection:$it")
            updateFirmwareVersion()
        }

        ProductKey.KeyProductType.create().listen(this) {
            LogUtils.i(tag, "KeyProductType:$it")
            it?.let {
                msdkInfo.value?.productType = it
                refreshMSDKInfo()
            }
        }
    }

    private fun removeListener() {
        KeyManager.getInstance().cancelListen(this)
    }

    private fun updateNetworkInfo(isAvailable: Boolean) {
        msdkInfo.value?.networkInfo = if(isAvailable) ONLINE_STR else NO_NETWORK_STR
//        viewModelScope.launch {
//            var isInInnerNetwork: Boolean
//            withContext(Dispatchers.IO) {
//                isInInnerNetwork = SDKConfig.getInstance().isInInnerNetwork
//            }
//            msdkInfo.value?.networkInfo =
//                if (isInInnerNetwork) IN_INNER_NETWORK_STR else IN_OUT_NETWORK_STR
//        }
    }

    private fun updateFirmwareVersion() {
        ProductKey.KeyFirmwareVersion.create().get({
            LogUtils.i(tag, "updateFirmwareVersion onSuccess:$it")
            msdkInfo.value?.firmwareVer = it ?: DEFAULT_STR
            refreshMSDKInfo()
        }) {
            LogUtils.i(tag, "updateFirmwareVersion onFailure:$it")
            msdkInfo.value?.firmwareVer = DEFAULT_STR
            refreshMSDKInfo()
        }
    }

    fun updateLDMStatus() {
        msdkInfo.value?.isLDMEnabled = LDMManager.getInstance().isLDMEnabled.toString()
        msdkInfo.value?.isLDMLicenseLoaded = LDMManager.getInstance().isLDMLicenseLoaded.toString()
        refreshMSDKInfo()
    }
}
