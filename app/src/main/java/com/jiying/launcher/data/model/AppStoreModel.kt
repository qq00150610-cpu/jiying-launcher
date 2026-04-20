package com.jiying.launcher.data.model

import android.graphics.drawable.Drawable

/**
 * 应用商店 - 应用信息模型
 */
data class AppInfo(
    val id: String,                      // 应用ID
    val name: String,                    // 应用名称
    val packageName: String,             // 包名
    val versionName: String,             // 版本名
    val versionCode: Long,               // 版本号
    val size: Long,                      // 文件大小(字节)
    val downloadUrl: String,             // 下载地址
    val iconUrl: String,                 // 图标URL
    val description: String,             // 应用描述
    val category: String,                // 分类
    val developer: String,               // 开发者
    val rating: Float,                   // 评分
    val downloadCount: Long,             // 下载次数
    val updateTime: String,              // 更新时间
    val md5: String,                     // MD5校验值
    var icon: Drawable? = null,          // 已加载的图标
    var isInstalled: Boolean = false,    // 是否已安装
    var isDownloading: Boolean = false,  // 是否正在下载
    var downloadProgress: Int = 0        // 下载进度
) {
    /**
     * 格式化文件大小
     */
    fun getFormattedSize(): String {
        val kb = size / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1 -> String.format("%.1f GB", gb)
            mb >= 1 -> String.format("%.1f MB", mb)
            kb >= 1 -> String.format("%.1f KB", kb)
            else -> "$size B"
        }
    }
    
    /**
     * 格式化下载次数
     */
    fun getFormattedDownloadCount(): String {
        val wan = downloadCount / 10000.0
        val yi = wan / 10000.0
        return when {
            yi >= 1 -> String.format("%.1f亿", yi)
            wan >= 1 -> String.format("%.1f万", wan)
            else -> "$downloadCount"
        }
    }
}

/**
 * 应用分类
 */
data class AppCategory(
    val id: String,
    val name: String,
    val icon: String,
    val apps: List<AppInfo> = emptyList()
)

/**
 * 应用商店配置
 */
data class AppStoreConfig(
    val baseUrl: String,                 // 服务器基础URL
    val apiVersion: String,              // API版本
    val timeout: Int = 30000,            // 超时时间(毫秒)
    val enableCache: Boolean = true      // 是否启用缓存
) {
    companion object {
        // 阿里云OSS服务器地址
        const val DEFAULT_BASE_URL = "https://jiying-appstore.oss-cn-beijing.aliyuncs.com"
        const val API_VERSION = "v1"
        
        // API端点
        const val ENDPOINT_APPS = "/apps/list.json"
        const val ENDPOINT_CATEGORIES = "/categories.json"
        const val ENDPOINT_SEARCH = "/apps/search.json"
    }
}
