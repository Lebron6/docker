package com.aros.apron.app

import android.app.Application
import android.content.Context
import com.aros.apron.models.MSDKManagerVM
import com.aros.apron.models.globalViewModels
import com.aros.apron.xclog.CrashHandler
import com.aros.apron.xclog.XcFileLog
import com.aros.apron.xclog.XcLogConfig
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import com.orhanobut.logger.PrettyFormatStrategy


open class ApronApp : Application() {


    companion object {
        fun getApplication(): Context? {
            return context
        }
         var context: Context? = null
    }

    private val msdkManagerVM: MSDKManagerVM by globalViewModels()

    override fun onCreate() {
        super.onCreate()
        context=this
        initConfig()
        msdkManagerVM.initMobileSDK(this)

    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        com.cySdkyc.clx.Helper.install(this)    }

    /**
     * Logger 初始化配置
     */
    private fun initConfig() {
        var formatStrategy = PrettyFormatStrategy.newBuilder()
            .showThreadInfo(false) // 隐藏线程信息 默认：显示
            .methodCount(0) // 决定打印多少行（每一行代表一个方法）默认：2
            .methodOffset(7) // (Optional) Hides internal method calls up to offset. Default 5
//            .tag("Aros") // (Optional) Global tag for every log. Default PRETTY_LOGGER
            .build()
        Logger.addLogAdapter(object : AndroidLogAdapter(formatStrategy) {
            override fun isLoggable(priority: Int, tag: String): Boolean {
//                return super.isLoggable(priority, tag);
                return true
            }
        })
        XcFileLog.init(XcLogConfig())
        //打印崩溃日志
        CrashHandler.getInstance().init()

    }
}