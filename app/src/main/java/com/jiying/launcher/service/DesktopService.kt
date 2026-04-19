package com.jiying.launcher.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * 极影桌面 - 桌面服务
 * 负责桌面相关的后台任务
 */
class DesktopService : Service() {

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
