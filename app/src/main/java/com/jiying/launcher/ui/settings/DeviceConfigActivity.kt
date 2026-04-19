package com.jiying.launcher.ui.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.jiying.launcher.R
import com.jiying.launcher.ui.layout.LayoutModeSelectorActivity
import com.jiying.launcher.util.MultiDeviceConfig
import com.jiying.launcher.util.ScreenAdapter

/**
 * 极影桌面 - 多设备配置与屏幕适配设置界面
 * 
 * 功能说明：
 * - 显示当前设备信息
 * - 提供设备预设选择
 * - 支持屏幕适配配置
 * - 兼容Android 6.0 (API 23) 及以上系统
 * 
 * @author 极影桌面开发团队
 * @version 1.0.0
 */
class DeviceConfigActivity : AppCompatActivity() {
    
    companion object {
        /**
         * 启动多设备配置界面
         */
        fun start(context: Context) {
            val intent = Intent(context, DeviceConfigActivity::class.java)
            if (context !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
    
    // UI组件
    private lateinit var titleBar: LinearLayout
    private lateinit var backBtn: ImageView
    private lateinit var titleText: TextView
    private lateinit var deviceInfoCard: CardView
    private lateinit var deviceInfoText: TextView
    private lateinit var presetSpinner: Spinner
    private lateinit var layoutModeBtn: Button
    private lateinit var screenOrientationToggle: ToggleButton
    private lateinit var applyPresetBtn: Button
    private lateinit var autoDetectBtn: Button
    private lateinit var resetBtn: Button
    
    // 数据
    private lateinit var presets: List<MultiDeviceConfig.DevicePreset>
    private lateinit var presetNames: List<String>
    private var selectedPresetIndex = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_config)
        
        // 初始化管理器
        MultiDeviceConfig.init(this)
        ScreenAdapter.init(this)
        
        initViews()
        loadPresets()
        updateDeviceInfo()
        updateCurrentPreset()
    }
    
    private fun initViews() {
        titleBar = findViewById(R.id.title_bar)
        backBtn = findViewById(R.id.btn_back)
        titleText = findViewById(R.id.title_text)
        deviceInfoCard = findViewById(R.id.device_info_card)
        deviceInfoText = findViewById(R.id.device_info_text)
        presetSpinner = findViewById(R.id.preset_spinner)
        layoutModeBtn = findViewById(R.id.btn_layout_mode)
        screenOrientationToggle = findViewById(R.id.screen_orientation_toggle)
        applyPresetBtn = findViewById(R.id.btn_apply_preset)
        autoDetectBtn = findViewById(R.id.btn_auto_detect)
        resetBtn = findViewById(R.id.btn_reset)
        
        // 返回按钮
        backBtn.setOnClickListener { finish() }
        
        // 布局模式按钮
        layoutModeBtn.setOnClickListener {
            LayoutModeSelectorActivity.start(this)
        }
        
        // 应用预设按钮
        applyPresetBtn.setOnClickListener {
            applySelectedPreset()
        }
        
        // 自动检测按钮
        autoDetectBtn.setOnClickListener {
            autoDetectDevice()
        }
        
        // 重置按钮
        resetBtn.setOnClickListener {
            resetConfig()
        }
        
        // 横竖屏切换
        screenOrientationToggle.setOnCheckedChangeListener { _, isChecked ->
            MultiDeviceConfig.saveCustomConfig(layoutMode = if (isChecked) {
                ScreenAdapter.LayoutMode.MODE_HYBRID
            } else {
                ScreenAdapter.LayoutMode.MODE_DEFAULT
            })
        }
    }
    
    private fun loadPresets() {
        presets = MultiDeviceConfig.getAllPresets()
        presetNames = presets.map { it.name }
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, presetNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        presetSpinner.adapter = adapter
        
        presetSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedPresetIndex = position
                updatePresetCompatibility(position)
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun updateDeviceInfo() {
        val info = ScreenAdapter.getDeviceDescription()
        deviceInfoText.text = info
    }
    
    private fun updateCurrentPreset() {
        val currentPresetId = MultiDeviceConfig.getActivePresetId()
        val index = presets.indexOfFirst { it.id == currentPresetId }
        if (index >= 0) {
            presetSpinner.setSelection(index)
        }
        
        // 更新横竖屏状态
        screenOrientationToggle.isChecked = ScreenAdapter.isLandscape()
    }
    
    private fun updatePresetCompatibility(position: Int) {
        val preset = presets[position]
        val compatibility = MultiDeviceConfig.getPresetCompatibility(preset.id)
        
        // 可以在这里更新UI显示兼容性信息
        Toast.makeText(this, compatibility, Toast.LENGTH_SHORT).show()
    }
    
    private fun applySelectedPreset() {
        val preset = presets[selectedPresetIndex]
        MultiDeviceConfig.setActivePreset(preset.id)
        Toast.makeText(this, "已应用预设: ${preset.name}", Toast.LENGTH_SHORT).show()
        
        // 重启主界面
        restartMainActivity()
    }
    
    private fun autoDetectDevice() {
        val detectedPreset = MultiDeviceConfig.autoDetectAndApply()
        Toast.makeText(this, "已自动配置: ${detectedPreset.name}", Toast.LENGTH_SHORT).show()
        
        // 更新UI
        updateCurrentPreset()
        updateDeviceInfo()
        
        // 重启主界面
        restartMainActivity()
    }
    
    private fun resetConfig() {
        MultiDeviceConfig.resetToAuto()
        Toast.makeText(this, "已重置为自动检测", Toast.LENGTH_SHORT).show()
        
        // 更新UI
        updateCurrentPreset()
        updateDeviceInfo()
    }
    
    private fun restartMainActivity() {
        val intent = Intent(this, com.jiying.launcher.ui.main.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
    
    override fun onResume() {
        super.onResume()
        // 刷新设备信息
        updateDeviceInfo()
    }
}
