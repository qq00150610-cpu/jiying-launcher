package com.jiying.launcher.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.jiying.launcher.R
import com.jiying.launcher.ui.main.AppManagerActivity
import com.jiying.launcher.ui.service.FileManagerActivity

/**
 * 极影桌面 - 系统设置页面
 * 
 * 功能说明：
 * - WiFi设置
 * - 蓝牙设置
 * - 显示设置
 * - 声音设置
 * - 应用管理
 * - 存储设置
 * - 位置设置
 * - 文件管理
 * - 悬浮球开关
 * - 布局模式设置
 * - 主题切换
 * - 设备配置
 * 
 * @author 极影桌面开发团队
 * @version 2.0.0
 */
class SystemSettingsActivity : AppCompatActivity() {

    private lateinit var floatingBallSwitch: Switch
    private lateinit var nightModeSwitch: Switch
    private lateinit var autoRotateSwitch: Switch
    private lateinit var bluetoothSwitch: Switch
    private lateinit var wifiSwitch: Switch

    companion object {
        private const val REQUEST_WRITE_SETTINGS = 1001
        private const val REQUEST_MANAGE_STORAGE = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_system_settings)
        hideSystemUI()
        initViews()
        loadSettings()
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        }
    }

    private fun initViews() {
        // 返回按钮
        findViewById<ImageButton>(R.id.back_button)?.setOnClickListener {
            finish()
        }

        // 悬浮球开关
        floatingBallSwitch = findViewById(R.id.floating_ball_switch)
        floatingBallSwitch.setOnCheckedChangeListener { _, isChecked ->
            saveFloatingBallSetting(isChecked)
            Toast.makeText(this, "悬浮球已${if (isChecked) "开启" else "关闭"}", Toast.LENGTH_SHORT).show()
        }

        // 夜间模式开关
        nightModeSwitch = findViewById(R.id.night_mode_switch)
        nightModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            saveNightModeSetting(isChecked)
            Toast.makeText(this, "夜间模式已${if (isChecked) "开启" else "关闭"}", Toast.LENGTH_SHORT).show()
        }

        // 自动旋转开关
        autoRotateSwitch = findViewById(R.id.auto_rotate_switch)
        autoRotateSwitch.setOnCheckedChangeListener { _, isChecked ->
            toggleAutoRotation(isChecked)
            Toast.makeText(this, "自动旋转已${if (isChecked) "开启" else "关闭"}", Toast.LENGTH_SHORT).show()
        }

        // ========== 设置项点击事件 ==========

        // WiFi设置
        findViewById<LinearLayout>(R.id.wifi_settings)?.setOnClickListener {
            openWifiSettings()
        }

        // 蓝牙设置
        findViewById<LinearLayout>(R.id.bluetooth_settings)?.setOnClickListener {
            openBluetoothSettings()
        }

        // 显示设置
        findViewById<LinearLayout>(R.id.display_settings)?.setOnClickListener {
            openDisplaySettings()
        }

        // 声音设置
        findViewById<LinearLayout>(R.id.sound_settings)?.setOnClickListener {
            openSoundSettings()
        }

        // 应用管理
        findViewById<LinearLayout>(R.id.app_management)?.setOnClickListener {
            openAppManager()
        }

        // 存储设置
        findViewById<LinearLayout>(R.id.storage_settings)?.setOnClickListener {
            openStorageSettings()
        }

        // 位置设置
        findViewById<LinearLayout>(R.id.location_settings)?.setOnClickListener {
            openLocationSettings()
        }

        // 文件管理
        findViewById<LinearLayout>(R.id.file_management)?.setOnClickListener {
            openFileManager()
        }

        // 所有设置
        findViewById<LinearLayout>(R.id.all_settings)?.setOnClickListener {
            openAllSettings()
        }

        // 悬浮球设置
        findViewById<LinearLayout>(R.id.floating_ball_settings)?.setOnClickListener {
            openFloatingBallSettings()
        }

        // 布局模式设置
        findViewById<LinearLayout>(R.id.layout_mode_settings)?.setOnClickListener {
            openLayoutModeSettings()
        }

        // 设备配置
        findViewById<LinearLayout>(R.id.device_config_settings)?.setOnClickListener {
            openDeviceConfig()
        }

        // USB设备管理
        findViewById<LinearLayout>(R.id.usb_device_settings)?.setOnClickListener {
            openUsbDeviceManager()
        }

        // 关于
        findViewById<LinearLayout>(R.id.about_settings)?.setOnClickListener {
            showAbout()
        }
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences("jiying_settings", MODE_PRIVATE)
        
        // 加载悬浮球设置
        floatingBallSwitch.isChecked = prefs.getBoolean("floating_ball", false)
        
        // 加载夜间模式设置
        nightModeSwitch.isChecked = prefs.getBoolean("night_mode", false)
        
        // 加载自动旋转设置
        try {
            val rotationEnabled = Settings.System.getInt(
                contentResolver,
                Settings.System.ACCELEROMETER_ROTATION, 0
            )
            autoRotateSwitch.isChecked = rotationEnabled == 1
        } catch (e: Exception) {
            autoRotateSwitch.isChecked = false
        }
    }

    private fun saveFloatingBallSetting(enabled: Boolean) {
        val prefs = getSharedPreferences("jiying_settings", MODE_PRIVATE)
        prefs.edit().putBoolean("floating_ball", enabled).apply()
        
        // 通知悬浮球服务
        if (enabled) {
            startFloatingBallService()
        } else {
            stopFloatingBallService()
        }
    }

    private fun saveNightModeSetting(enabled: Boolean) {
        val prefs = getSharedPreferences("jiying_settings", MODE_PRIVATE)
        prefs.edit().putBoolean("night_mode", enabled).apply()
    }

    private fun toggleAutoRotation(enabled: Boolean) {
        try {
            if (Settings.System.canWrite(this)) {
                Settings.System.putInt(
                    contentResolver,
                    Settings.System.ACCELEROMETER_ROTATION,
                    if (enabled) 1 else 0
                )
            } else {
                // 请求写入设置权限
                AlertDialog.Builder(this)
                    .setTitle("需要权限")
                    .setMessage("需要修改系统设置权限来更改自动旋转开关")
                    .setPositiveButton("去授权") { _, _ ->
                        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                        intent.data = Uri.parse("package:$packageName")
                        startActivityForResult(intent, REQUEST_WRITE_SETTINGS)
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "无法更改自动旋转设置", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openWifiSettings() {
        try {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开WiFi设置", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openBluetoothSettings() {
        try {
            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开蓝牙设置", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openDisplaySettings() {
        try {
            startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS))
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开显示设置", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openSoundSettings() {
        try {
            startActivity(Intent(Settings.ACTION_SOUND_SETTINGS))
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开声音设置", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openAppManager() {
        try {
            startActivity(Intent(this, AppManagerActivity::class.java))
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开应用管理", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openStorageSettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivityForResult(intent, REQUEST_MANAGE_STORAGE)
            } else {
                startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
            }
        } catch (e: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
            } catch (e2: Exception) {
                Toast.makeText(this, "无法打开存储设置", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openLocationSettings() {
        try {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开位置设置", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openFileManager() {
        try {
            startActivity(Intent(this, FileManagerActivity::class.java))
        } catch (e: Exception) {
            // 如果内置文件管理器不可用，尝试打开第三方文件管理器
            openThirdPartyFileManager()
        }
    }

    private fun openThirdPartyFileManager() {
        val fileManagers = listOf(
            "com.android.filemanager",           // 系统文件管理器
            "com.huawei.hidisk",                // 华为文件管理器
            "com.mi.android.globalFileexplorer", // 小米文件管理器
            "com.coloros.files",                // OPPO文件管理器
            "com.samsung.android.app.filemanager", // 三星文件管理器
            "com.oneplus.filemanager",          // 一加文件管理器
            "com.vivo.filemanager",              // Vivo文件管理器
            "com.lenovo.FileBrowser"            // 联想文件管理器
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
        
        // 如果没有文件管理器，打开存储设置
        Toast.makeText(this, "未找到文件管理器", Toast.LENGTH_SHORT).show()
    }

    private fun openAllSettings() {
        try {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开设置", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openFloatingBallSettings() {
        try {
            val intent = Intent(this, FloatBallSettingsActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开悬浮球设置", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openLayoutModeSettings() {
        try {
            val intent = Intent(this, com.jiying.launcher.ui.layout.LayoutModeSelectorActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开布局模式设置", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openDeviceConfig() {
        try {
            val intent = Intent(this, DeviceConfigActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开设备配置", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openUsbDeviceManager() {
        try {
            val intent = Intent(this, com.jiying.launcher.ui.usb.UsbDeviceManagerActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开USB设备管理", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAbout() {
        AlertDialog.Builder(this)
            .setTitle("关于极影桌面")
            .setMessage("""
                极影桌面 v2.0.0
                
                一款专为车机设计的智能桌面应用
                
                功能特点：
                • 布丁UI + 氢桌面双主题
                • 智能音乐播放
                • 画中画导航
                • 多种布局模式
                • 文件管理器
                • 视频播放器
                
                © 2024 极影桌面开发团队
            """.trimIndent())
            .setPositiveButton("确定", null)
            .show()
    }

    private fun startFloatingBallService() {
        try {
            val intent = Intent(this, com.jiying.launcher.service.FloatBallService::class.java)
            startService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopFloatingBallService() {
        try {
            val intent = Intent(this, com.jiying.launcher.service.FloatBallService::class.java)
            stopService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_WRITE_SETTINGS -> {
                // 用户从权限设置页面返回，重新加载设置
                loadSettings()
            }
            REQUEST_MANAGE_STORAGE -> {
                // 用户从存储权限页面返回
                Toast.makeText(this, "存储权限已更新", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}
