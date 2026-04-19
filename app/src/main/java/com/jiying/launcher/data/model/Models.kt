package com.jiying.launcher.data.model

import android.graphics.drawable.Drawable

/**
 * 应用信息数据模型
 */
data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable,
    val isSystemApp: Boolean = false,
    val category: String = "其他",
    val version: String = "",
    val apkPath: String = ""
)

/**
 * 壁纸信息数据模型
 */
data class WallpaperInfo(
    val name: String,
    val path: String,
    val isLive: Boolean = false,
    val isDefault: Boolean = false,
    val thumbnail: String? = null
)

/**
 * 快捷插件信息
 */
data class QuickPluginInfo(
    val id: String,
    val name: String,
    val icon: Int,
    val type: PluginType,
    val packageName: String? = null,
    val action: String? = null
)

enum class PluginType {
    NAVIGATION,
    MUSIC,
    WEATHER,
    SETTINGS
}

/**
 * 轻应用信息
 */
data class LightAppInfo(
    val id: String,
    val name: String,
    val icon: Int,
    val packageName: String,
    val category: AppCategory
)

enum class AppCategory {
    VIDEO,      // 影音
    PODCAST,    // 播客
    GAME,       // 游戏
    NEWS,       // 资讯
    LEARNING,   // 学习
    OTHER       // 其他
}

/**
 * 音乐信息
 */
data class MusicInfo(
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val albumArt: String? = null,
    val isPlaying: Boolean = false
)
