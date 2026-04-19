package com.jiying.launcher.ui.main

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.jiying.launcher.R
import com.jiying.launcher.ui.settings.SystemSettingsActivity

/**
 * 菜单页面（横屏）
 * 提供各类功能入口的快捷菜单
 */
class MenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)
        
        initViews()
    }

    private fun initViews() {
        findViewById<ImageButton>(R.id.back_button).setOnClickListener { finish() }
        
        // 左侧图标矩阵 - 2行3列
        findViewById<LinearLayout>(R.id.basic_settings).setOnClickListener {
            startActivity(Intent(this, SystemSettingsActivity::class.java))
        }
        
        findViewById<LinearLayout>(R.id.plugin_settings).setOnClickListener {
            Toast.makeText(this, "插件设置", Toast.LENGTH_SHORT).show()
        }
        
        findViewById<LinearLayout>(R.id.theme_wallpaper).setOnClickListener {
            showThemeCenter()
        }
        
        findViewById<LinearLayout>(R.id.map_settings).setOnClickListener {
            openNavigationApp()
        }
        
        findViewById<LinearLayout>(R.id.music_plugin).setOnClickListener {
            openMusicApp()
        }
        
        findViewById<LinearLayout>(R.id.advanced_settings).setOnClickListener {
            showAdvancedMenu()
        }
        
        // 右侧功能列表
        findViewById<LinearLayout>(R.id.video_player).setOnClickListener {
            openVideoPlayer()
        }
        
        findViewById<LinearLayout>(R.id.music_player).setOnClickListener {
            openMusicApp()
        }
        
        findViewById<LinearLayout>(R.id.car_connect).setOnClickListener {
            openCarConnect()
        }
        
        findViewById<LinearLayout>(R.id.other_settings).setOnClickListener {
            startActivity(Intent(this, SystemSettingsActivity::class.java))
        }
        
        findViewById<LinearLayout>(R.id.file_manager).setOnClickListener {
            openFileManager()
        }
        
        findViewById<LinearLayout>(R.id.usb_device).setOnClickListener {
            openUsbSettings()
        }
    }

    private fun showThemeCenter() {
        AlertDialog.Builder(this)
            .setTitle("选择主题")
            .setItems(arrayOf("布丁UI风格", "氢桌面风格", "经典风格")) { _, which ->
                Toast.makeText(this, "已切换到主题${which + 1}", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showAdvancedMenu() {
        AlertDialog.Builder(this)
            .setTitle("高级功能")
            .setItems(arrayOf("开发者选项", "安全设置", "关于平板", "恢复默认")) { _, which ->
                when (which) {
                    0 -> startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                    1 -> startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                    2 -> startActivity(Intent(Settings.ACTION_DEVICE_INFO_SETTINGS))
                    3 -> Toast.makeText(this, "已恢复默认设置", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun openNavigationApp() {
        val navApps = listOf(
            "com.autonavi.amapauto",  // 高德地图车机版
            "com.autonavi.minimap",    // 高德地图
            "com.baidu.navi",          // 百度导航
            "com.sogou.map.android"    // 搜狗地图
        )
        for (pkg in navApps) {
            try {
                val intent = packageManager.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    startActivity(intent)
                    return
                }
            } catch (e: Exception) { /* continue */ }
        }
        Toast.makeText(this, "未安装导航应用", Toast.LENGTH_SHORT).show()
    }

    private fun openMusicApp() {
        val musicApps = listOf(
            "cn.kuwo.player",           // 酷我音乐
            "com.kugou.android",        // 酷狗音乐
            "com.netease.cloudmusic",   // 网易云音乐
            "com.tencent.qqmusic",       // QQ音乐
            "com.android.music"         // 系统音乐
        )
        for (pkg in musicApps) {
            try {
                val intent = packageManager.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    startActivity(intent)
                    return
                }
            } catch (e: Exception) { /* continue */ }
        }
        Toast.makeText(this, "未安装音乐应用", Toast.LENGTH_SHORT).show()
    }

    private fun openVideoPlayer() {
        val videoApps = listOf(
            "com.mxtech.videoplayer",      // MX Player
            "com.tencent.qqlive",          // 腾讯视频
            "tv.danmaku.bili",             // B站
            "com.youku.phone",             // 优酷
            "com.iqiyi.iQiyi"              // 爱奇艺
        )
        for (pkg in videoApps) {
            try {
                val intent = packageManager.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    startActivity(intent)
                    return
                }
            } catch (e: Exception) { /* continue */ }
        }
        Toast.makeText(this, "未安装视频应用", Toast.LENGTH_SHORT).show()
    }

    private fun openCarConnect() {
        val carApps = listOf(
            "com.baidu.carlife",           // 百度CarLife
            "com.tencent.qqlivecar",        // 腾讯车联
            "com.vivo.connectedcarservice", // vivo车联
            "com.huawei.carkit",            // 华为车联
            "com.xiaomi.mi_carlife"         // 小米CarLife
        )
        for (pkg in carApps) {
            try {
                val intent = packageManager.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    startActivity(intent)
                    return
                }
            } catch (e: Exception) { /* continue */ }
        }
        Toast.makeText(this, "未安装车机互联应用", Toast.LENGTH_SHORT).show()
    }

    private fun openFileManager() {
        val fileManagers = listOf(
            "com.android.filemanager",
            "com.huawei.hidisk",
            "com.mi.android.globalFileexplorer",
            "com.coloros.files",
            "com.samsung.android.app.filemanager"
        )
        for (pkg in fileManagers) {
            try {
                val intent = packageManager.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    startActivity(intent)
                    return
                }
            } catch (e: Exception) { /* continue */ }
        }
        startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
    }

    private fun openUsbSettings() {
        try {
            @Suppress("DEPRECATION")
            val intent = Intent("android.settings.USB_SETTINGS")
            startActivity(intent)
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_DEVICE_INFO_SETTINGS))
        }
    }
}
