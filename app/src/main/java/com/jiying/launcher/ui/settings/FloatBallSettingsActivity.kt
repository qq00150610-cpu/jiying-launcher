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
import com.jiying.launcher.service.FloatBallService

/**
 * 悬浮球设置Activity
 * 支持悬浮球开关、位置、功能配置
 */
class FloatBallSettingsActivity : AppCompatActivity() {

    private lateinit var enableSwitch: SwitchCompat
    private lateinit var positionRadioGroup: RadioGroup
    private lateinit var sizeSlider: SeekBar
    private lateinit var opacitySlider: SeekBar
    private lateinit var autoHideSwitch: SwitchCompat
    private lateinit var clickActionSpinner: Spinner
    private lateinit var doubleClickActionSpinner: Spinner
    private lateinit var longPressActionSpinner: Spinner
    
    private var isEnabled = false
    private var ballSize = 48
    private var ballOpacity = 100
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_float_ball_settings)
        
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
            enableSwitch = findViewById(R.id.float_ball_enable)
            positionRadioGroup = findViewById(R.id.float_ball_position)
            sizeSlider = findViewById(R.id.float_ball_size)
            opacitySlider = findViewById(R.id.float_ball_opacity)
            autoHideSwitch = findViewById(R.id.float_ball_auto_hide)
            clickActionSpinner = findViewById(R.id.float_ball_click_action)
            doubleClickActionSpinner = findViewById(R.id.float_ball_double_click_action)
            longPressActionSpinner = findViewById(R.id.float_ball_long_press_action)
            
            // 设置Spinner适配器
            val actions = arrayOf("打开主屏幕", "打开控制中心", "打开应用列表", "显示通知栏", "无操作")
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, actions)
            clickActionSpinner.adapter = adapter
            doubleClickActionSpinner.adapter = adapter
            longPressActionSpinner.adapter = adapter
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadSettings() {
        try {
            // 从SharedPreferences加载设置
            val prefs = getSharedPreferences("float_ball_settings", MODE_PRIVATE)
            isEnabled = prefs.getBoolean("enabled", false)
            ballSize = prefs.getInt("size", 48)
            ballOpacity = prefs.getInt("opacity", 100)
            
            enableSwitch.isChecked = isEnabled
            sizeSlider.progress = ballSize
            opacitySlider.progress = ballOpacity
            
            // 根据位置加载RadioButton
            val position = prefs.getInt("position", 0)
            when (position) {
                0 -> findViewById<RadioButton>(R.id.position_left).isChecked = true
                1 -> findViewById<RadioButton>(R.id.position_right).isChecked = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupListeners() {
        try {
            // 开关
            enableSwitch.setOnCheckedChangeListener { _, isChecked ->
                isEnabled = isChecked
                saveSettings()
                updateFloatBallService()
                
                val statusText = findViewById<TextView>(R.id.float_ball_status)
                statusText.text = if (isChecked) "悬浮球已开启" else "悬浮球已关闭"
            }
            
            // 位置
            positionRadioGroup.setOnCheckedChangeListener { _, checkedId ->
                saveSettings()
                updateFloatBallService()
            }
            
            // 大小
            sizeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        ballSize = progress
                        saveSettings()
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
            
            // 透明度
            opacitySlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        ballOpacity = progress
                        saveSettings()
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
            
            // 自动隐藏
            autoHideSwitch.setOnCheckedChangeListener { _, _ ->
                saveSettings()
            }
            
            // 操作设置
            clickActionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    saveSettings()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            
            doubleClickActionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    saveSettings()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            
            longPressActionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    saveSettings()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveSettings() {
        try {
            val prefs = getSharedPreferences("float_ball_settings", MODE_PRIVATE)
            prefs.edit().apply {
                putBoolean("enabled", isEnabled)
                putInt("size", ballSize)
                putInt("opacity", ballOpacity)
                
                val position = when (positionRadioGroup.checkedRadioButtonId) {
                    R.id.position_left -> 0
                    R.id.position_right -> 1
                    else -> 0
                }
                putInt("position", position)
                
                putBoolean("auto_hide", autoHideSwitch.isChecked)
                putInt("click_action", clickActionSpinner.selectedItemPosition)
                putInt("double_click_action", doubleClickActionSpinner.selectedItemPosition)
                putInt("long_press_action", longPressActionSpinner.selectedItemPosition)
                
                apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateFloatBallService() {
        try {
            val intent = Intent(this, FloatBallService::class.java)
            if (isEnabled) {
                startService(intent)
            } else {
                stopService(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
