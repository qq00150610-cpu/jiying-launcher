package com.jiying.launcher.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jiying.launcher.ui.main.MainActivity

/**
 * 极影桌面 - 开机广播接收器
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == "android.intent.action.QUICKBOOT_POWERON") {
            // 启动桌面
            val launchIntent = Intent(context, MainActivity::class.java)
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context?.startActivity(launchIntent)
        }
    }
}
