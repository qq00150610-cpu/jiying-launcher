package com.jiying.launcher.ui.settings

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jiying.launcher.R

class SystemSettingsActivity : AppCompatActivity() {
    
    private lateinit var floatingBallSwitch: Switch
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_system_settings)
        
        initViews()
    }
    
    private fun initViews() {
        // 返回按钮
        findViewById<ImageButton>(R.id.back_button).setOnClickListener {
            finish()
        }
        
        // 悬浮球开关
        floatingBallSwitch = findViewById(R.id.floating_ball_switch)
        loadSettings()
        
        floatingBallSwitch.setOnCheckedChangeListener { _, isChecked ->
            saveFloatingBallSetting(isChecked)
            Toast.makeText(this, "悬浮球已${if (isChecked) "开启" else "关闭"}", Toast.LENGTH_SHORT).show()
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
            startActivity(Intent(this, com.jiying.launcher.ui.main.AppManagerActivity::class.java))
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
    
    private fun loadSettings() {
        val prefs = getSharedPreferences("system_settings", MODE_PRIVATE)
        floatingBallSwitch.isChecked = prefs.getBoolean("floating_ball", false)
    }
    
    private fun saveFloatingBallSetting(enabled: Boolean) {
        val prefs = getSharedPreferences("system_settings", MODE_PRIVATE)
        prefs.edit().putBoolean("floating_ball", enabled).apply()
    }
    
    private fun openFileManager() {
        val fileManagers = listOf(
            "com.android.filemanager",
            "com.huawei.hidisk",
            "com.mi.android.globalFileexplorer",
            "com.coloros.files"
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
        startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
    }
}
