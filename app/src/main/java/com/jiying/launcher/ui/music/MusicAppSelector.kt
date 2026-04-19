package com.jiying.launcher.ui.music

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

/**
 * 音乐应用选择器
 * 
 * 识别并启动已安装的音乐应用，包括共存版
 * 
 * @author 极影桌面开发团队
 * @version 2.0.0
 */
class MusicAppSelector(private val context: Context) {

    // 已知音乐应用列表（包含共存版）
    private val musicApps = listOf(
        MusicAppInfo("QQ音乐", "com.tencent.qqmusic", listOf("com.tencent.qqmusic.lite", "com.tencent.qqmusic.car")),
        MusicAppInfo("酷狗音乐", "com.kugou.android", listOf("com.kugou.android.lite", "com.kugou.android.car")),
        MusicAppInfo("酷我音乐", "cn.kuwo.player", listOf("cn.kuwo.player.lite", "cn.kuwo.player.car")),
        MusicAppInfo("网易云音乐", "com.netease.cloudmusic", listOf("com.netease.cloudmusic.lite")),
        MusicAppInfo("虾米音乐", "com.xiami.miplayer", listOf("com.xiami.miplayer.pad")),
        MusicAppInfo("咪咕音乐", "cmccwm.mobilemusic", listOf()),
        MusicAppInfo("千千音乐", "com.ting.mp3.android", listOf()),
        MusicAppInfo("豆瓣FM", "com.douban.radio", listOf()),
        MusicAppInfo("虾米音乐HD", "com.xiami.miplayer.pad", listOf()),
        MusicAppInfo("Apple Music", "com.apple.android.music", listOf())
    )

    data class MusicAppInfo(
        val name: String,
        val packageName: String,
        val coexistPackages: List<String> // 共存版包名列表
    )

    data class InstalledMusicApp(
        val name: String,
        val packageName: String,
        val isCoexist: Boolean
    )

    /**
     * 扫描已安装的音乐应用
     */
    fun scanInstalledMusicApps(): List<InstalledMusicApp> {
        val installed = mutableListOf<InstalledMusicApp>()
        
        for (app in musicApps) {
            // 检查主包名
            if (isPackageInstalled(app.packageName)) {
                installed.add(InstalledMusicApp(app.name, app.packageName, false))
            }
            
            // 检查共存版包名
            for (coexistPkg in app.coexistPackages) {
                if (isPackageInstalled(coexistPkg)) {
                    installed.add(InstalledMusicApp("${app.name}(共存版)", coexistPkg, true))
                }
            }
        }
        
        return installed
    }

    /**
     * 检查包是否已安装
     */
    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * 显示音乐应用选择对话框
     */
    fun showMusicAppSelector() {
        val apps = scanInstalledMusicApps()
        
        if (apps.isEmpty()) {
            Toast.makeText(context, "未找到音乐应用，请先安装音乐应用", Toast.LENGTH_LONG).show()
            return
        }
        
        val items = apps.map { "${it.name}${if (it.isCoexist) " 🔄" else ""}" }.toTypedArray()
        
        AlertDialog.Builder(context)
            .setTitle("选择音乐应用")
            .setMessage("检测到 ${apps.size} 个音乐应用")
            .setItems(items) { _, which ->
                launchMusicApp(apps[which].packageName)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 启动音乐应用
     */
    private fun launchMusicApp(packageName: String) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Toast.makeText(context, "已启动音乐应用", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "无法启动该应用", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "启动失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 获取默认音乐应用
     */
    fun getDefaultMusicApp(): InstalledMusicApp? {
        val apps = scanInstalledMusicApps()
        return if (apps.isNotEmpty()) apps[0] else null
    }

    /**
     * 快速启动默认音乐应用
     */
    fun quickLaunchDefaultMusicApp() {
        val app = getDefaultMusicApp()
        if (app != null) {
            launchMusicApp(app.packageName)
        } else {
            Toast.makeText(context, "未找到音乐应用", Toast.LENGTH_SHORT).show()
        }
    }
}
