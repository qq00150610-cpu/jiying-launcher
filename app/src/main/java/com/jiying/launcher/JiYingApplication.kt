package com.jiying.launcher

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import com.jiying.launcher.service.DesktopService
import com.jiying.launcher.util.ThemeManager

/**
 * 极影桌面 Application
 * 应用入口，负责全局初始化
 */
class JiYingApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // 初始化主题系统
        ThemeManager.init(this)
        
        // 启动桌面服务
        startDesktopService()
    }

    private fun startDesktopService() {
        // 暂不启动服务，避免前台服务通知问题
        // val serviceIntent = Intent(this, DesktopService::class.java)
        // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        //     startForegroundService(serviceIntent)
        // } else {
        //     startService(serviceIntent)
        // }
    }

    companion object {
        lateinit var instance: JiYingApplication
            private set
    }
}
