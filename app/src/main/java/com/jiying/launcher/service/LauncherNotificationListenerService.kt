package com.jiying.launcher.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.content.Intent

/**
 * 通知监听服务
 * 支持通知显示
 */
class LauncherNotificationListenerService : NotificationListenerService() {

    companion object {
        var instance: LauncherNotificationListenerService? = null
            private set
        
        val notificationList = mutableListOf<NotificationInfo>()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        try {
            sbn ?: return
            
            val notification = NotificationInfo(
                packageName = sbn.packageName,
                title = sbn.notification.extras.getCharSequence("android.title")?.toString() ?: "",
                content = sbn.notification.extras.getCharSequence("android.text")?.toString() ?: "",
                time = sbn.postTime,
                isOngoing = sbn.isOngoing
            )
            
            // 添加到列表顶部
            notificationList.add(0, notification)
            
            // 保持最多50条通知
            if (notificationList.size > 50) {
                notificationList.removeAt(notificationList.size - 1)
            }
            
            // 发送广播通知UI更新
            val broadcastIntent = Intent(NotificationAction.ACTION_NOTIFICATION_POSTED).apply {
                putExtra("package_name", notification.packageName)
                putExtra("title", notification.title)
            }
            sendBroadcast(broadcastIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        try {
            sbn ?: return
            
            val packageName = sbn.packageName
            val title = sbn.notification.extras.getCharSequence("android.title")?.toString() ?: ""
            
            // 从列表中移除
            notificationList.removeAll { 
                it.packageName == packageName && it.title == title 
            }
            
            // 发送广播通知UI更新
            val broadcastIntent = Intent(NotificationAction.ACTION_NOTIFICATION_REMOVED).apply {
                putExtra("package", packageName)
                putExtra("title", title)
            }
            sendBroadcast(broadcastIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 获取所有通知
     */
    fun getNotifications(): List<NotificationInfo> {
        return notificationList.toList()
    }

    /**
     * 清除所有通知
     */
    fun clearAllNotifications() {
        try {
            cancelAllNotifications()
            notificationList.clear()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 清除指定应用的通知
     */
    fun clearNotifications(packageName: String) {
        try {
            val activeNotifications = activeNotifications
            activeNotifications?.forEach { sbn ->
                if (sbn.packageName == packageName) {
                    cancelNotification(sbn.key)
                }
            }
            notificationList.removeAll { it.packageName == packageName }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 打开指定通知
     */
    fun openNotification(sbn: StatusBarNotification?) {
        try {
            sbn ?: return
            val pendingIntent = sbn.notification.contentIntent
            pendingIntent?.send()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 判断是否为重要的通知
     */
    fun isImportantNotification(sbn: StatusBarNotification?): Boolean {
        try {
            sbn ?: return false
            return sbn.notification.priority >= 0
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}

data class NotificationInfo(
    val packageName: String,
    val title: String,
    val content: String,
    val time: Long,
    val isOngoing: Boolean
)

object NotificationAction {
    const val ACTION_NOTIFICATION_POSTED = "com.jiying.launcher.NOTIFICATION_POSTED"
    const val ACTION_NOTIFICATION_REMOVED = "com.jiying.launcher.NOTIFICATION_REMOVED"
}
