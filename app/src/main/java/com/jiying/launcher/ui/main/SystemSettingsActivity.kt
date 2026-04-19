package com.jiying.launcher.ui.main

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.jiying.launcher.R

/**
 * 系统设置页面
 * 提供快速访问各种系统设置的入口
 */
class SystemSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_system_settings)
        
        initViews()
    }

    private fun initViews() {
        findViewById<ImageButton>(R.id.back_button).setOnClickListener { finish() }
        
        // 悬浮球开关
        val floatingBallSwitch = findViewById<SwitchCompat>(R.id.floating_ball_switch)
        // 读取保存的设置
        val prefs = getSharedPreferences("jiying_settings", MODE_PRIVATE)
        floatingBallSwitch.isChecked = prefs.getBoolean("floating_ball", false)
        
        floatingBallSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("floating_ball", isChecked).apply()
            Toast.makeText(this, if (isChecked) "悬浮球已开启" else "悬浮球已关闭", Toast.LENGTH_SHORT).show()
        }
        
        // 悬浮球设置
        findViewById<LinearLayout>(R.id.floating_ball_settings).setOnClickListener {
            Toast.makeText(this, "悬浮球设置", Toast.LENGTH_SHORT).show()
        }
        
        // WiFi设置
        findViewById<LinearLayout>(R.id.wifi_settings).setOnClickListener {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        }
        
        // 蓝牙设置
        findViewById<LinearLayout>(R.id.bluetooth_settings).setOnClickListener {
            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        }
        
        // 显示设置
        findViewById<LinearLayout>(R.id.display_settings).setOnClickListener {
            startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS))
        }
        
        // 声音设置
        findViewById<LinearLayout>(R.id.sound_settings).setOnClickListener {
            startActivity(Intent(Settings.ACTION_SOUND_SETTINGS))
        }
        
        // 应用管理
        findViewById<LinearLayout>(R.id.app_management).setOnClickListener {
            startActivity(Intent(this, AppManagerActivity::class.java))
        }
        
        // 存储设置
        findViewById<LinearLayout>(R.id.storage_settings).setOnClickListener {
            startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
        }
        
        // 位置设置
        findViewById<LinearLayout>(R.id.location_settings).setOnClickListener {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
        
        // 文件管理
        findViewById<LinearLayout>(R.id.file_management).setOnClickListener {
            openFileManager()
        }
        
        // 所有设置
        findViewById<LinearLayout>(R.id.all_settings).setOnClickListener {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun openFileManager() {
        val fileManagers = listOf(
            "com.android.filemanager",
            "com.huawei.hidisk",
            "com.mi.android.globalFileexplorer",
            "com.coloros.files",
            "com.samsung.android.app.filemanager",
            "com.oneplus.filemanager"
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
        Toast.makeText(this, "未找到文件管理器", Toast.LENGTH_SHORT).show()
    }
}
