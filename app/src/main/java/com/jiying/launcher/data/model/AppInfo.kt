package com.jiying.launcher.data.model

import android.graphics.drawable.Drawable

/**
 * 应用信息数据类
 */
data class AppInfo(
    val name: String,           // 应用名称
    val packageName: String,    // 包名
    val icon: Drawable,         // 应用图标
    val isSystemApp: Boolean = false,  // 是否系统应用
    val version: String = "",   // 版本号
    val apkPath: String = ""    // APK路径
)
