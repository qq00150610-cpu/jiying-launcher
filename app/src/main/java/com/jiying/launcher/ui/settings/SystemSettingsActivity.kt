package com.jiying.launcher.ui.settings

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import com.jiying.launcher.R

/**
 * 系统设置Activity
 */
class SystemSettingsActivity : AppCompatActivity() {

    private lateinit var homeButtonSwitch: SwitchCompat
    private lateinit var statusBarSwitch: SwitchCompat
    private lateinit var dockBarSwitch: SwitchCompat
    private lateinit var gestureSwitch: SwitchCompat
    private lateinit var notificationSwitch: SwitchCompat
    private lateinit var autoStartSwitch: SwitchCompat
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_system_settings)
        
        try {
            initViews()
            loadSettings()
            setupListeners()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initViews() {
        try {
            homeButtonSwitch = findViewById(R.id.switch_home_button)
            statusBarSwitch = findViewById(R.id.switch_status_bar)
            dockBarSwitch = findViewById(R.id.switch_dock_bar)
            gestureSwitch = findViewById(R.id.switch_gesture)
            notificationSwitch = findViewById(R.id.switch_notification)
            autoStartSwitch = findViewById(R.id.switch_auto_start)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadSettings() {
        try {
            val prefs = getSharedPreferences("system_settings", MODE_PRIVATE)
            homeButtonSwitch.isChecked = prefs.getBoolean("home_button", true)
            statusBarSwitch.isChecked = prefs.getBoolean("status_bar", true)
            dockBarSwitch.isChecked = prefs.getBoolean("dock_bar", true)
            gestureSwitch.isChecked = prefs.getBoolean("gesture", false)
            notificationSwitch.isChecked = prefs.getBoolean("notification", true)
            autoStartSwitch.isChecked = prefs.getBoolean("auto_start", false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupListeners() {
        try {
            val prefs = getSharedPreferences("system_settings", MODE_PRIVATE)
            
            homeButtonSwitch.setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean("home_button", isChecked).apply()
                Toast.makeText(this, "Home键导航 ${if (isChecked) "开启" else "关闭"}", Toast.LENGTH_SHORT).show()
            }
            
            statusBarSwitch.setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean("status_bar", isChecked).apply()
                Toast.makeText(this, "状态栏 ${if (isChecked) "显示" else "隐藏"}", Toast.LENGTH_SHORT).show()
            }
            
            dockBarSwitch.setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean("dock_bar", isChecked).apply()
            }
            
            gestureSwitch.setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean("gesture", isChecked).apply()
                if (isChecked) {
                    Toast.makeText(this, "请授予手势操作权限", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            }
            
            notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean("notification", isChecked).apply()
                if (isChecked) {
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
            }
            
            autoStartSwitch.setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean("auto_start", isChecked).apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
