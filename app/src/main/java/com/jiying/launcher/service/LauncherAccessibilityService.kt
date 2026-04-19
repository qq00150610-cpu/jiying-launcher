package com.jiying.launcher.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.jiying.launcher.ui.main.MainActivity

/**
 * 无障碍服务
 * 支持辅助操作
 */
class LauncherAccessibilityService : AccessibilityService() {

    companion object {
        var instance: LauncherAccessibilityService? = null
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // 服务已连接
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            event ?: return
            
            when (event.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    handleWindowStateChanged(event)
                }
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                    handleWindowContentChanged(event)
                }
                AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                    handleNotificationChanged(event)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onInterrupt() {
        // 服务被中断
    }

    private fun handleWindowStateChanged(event: AccessibilityEvent) {
        try {
            val className = event.className?.toString()
            val packageName = event.packageName?.toString()
            
            // 检测应用切换
            if (className == "com.android.launcher.Launcher" || 
                className == "com.jiying.launcher.ui.main.MainActivity") {
                // 返回到桌面
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleWindowContentChanged(event: AccessibilityEvent) {
        try {
            val source = event.source
            source?.let { processNode(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleNotificationChanged(event: AccessibilityEvent) {
        try {
            val text = event.text
            text?.forEach {
                // 处理通知文本
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun processNode(node: AccessibilityNodeInfo) {
        try {
            // 递归处理子节点
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                child?.let {
                    processNode(it)
                    it.recycle()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 点击指定文本的按钮
     */
    fun clickText(text: String): Boolean {
        try {
            val rootNode = rootInActiveWindow ?: return false
            val result = findAndClickText(rootNode, text)
            rootNode.recycle()
            return result
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun findAndClickText(node: AccessibilityNodeInfo, text: String): Boolean {
        try {
            if (node.text?.toString()?.contains(text) == true) {
                val parent = node.parent
                if (parent?.isClickable == true) {
                    parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    parent.recycle()
                    return true
                }
                parent?.recycle()
            }
            
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                child?.let {
                    if (findAndClickText(it, text)) {
                        return true
                    }
                    it.recycle()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    /**
     * 执行返回操作
     */
    fun goBack(): Boolean {
        return try {
            performGlobalAction(GLOBAL_ACTION_BACK)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 执行Home操作
     */
    fun goHome(): Boolean {
        return try {
            performGlobalAction(GLOBAL_ACTION_HOME)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 打开通知栏
     */
    fun openNotifications(): Boolean {
        return try {
            performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 打开快速设置
     */
    fun openQuickSettings(): Boolean {
        return try {
            performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
