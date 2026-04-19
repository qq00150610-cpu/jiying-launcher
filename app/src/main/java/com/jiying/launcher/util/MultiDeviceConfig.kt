package com.jiying.launcher.util

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.view.WindowManager

/**
 * 极影桌面 - 多设备配置预设管理器
 * 
 * 功能说明：
 * - 预设多种车机设备配置
 * - 支持一键应用设备预设
 * - 保存用户自定义配置
 * - 兼容Android 6.0 (API 23) 及以上系统
 * 
 * 预设设备列表：
 * 1. 通用7寸车机 (1024x600)
 * 2. 通用10寸车机 (1280x800)
 * 3. 通用12寸车机 (1920x1200)
 * 4. 特斯拉风格大屏
 * 5. 竖屏车机
 * 6. 手机兼容模式
 * 
 * @author 极影桌面开发团队
 * @version 1.0.0
 */
object MultiDeviceConfig {
    
    // 设备预设ID
    object PresetId {
        const val AUTO_DETECT = 0           // 自动检测
        const val CAR_7_INCH = 1            // 7寸车机
        const val CAR_10_INCH = 2           // 10寸车机
        const val CAR_12_INCH = 3           // 12寸车机
        const val TESLA_STYLE = 4           // 特斯拉风格
        const val VERTICAL_SCREEN = 5       // 竖屏车机
        const val PHONE_COMPAT = 6          // 手机兼容
        const val CUSTOM = 99               // 自定义
    }
    
    // 设备预设数据类
    data class DevicePreset(
        val id: Int,                        // 预设ID
        val name: String,                    // 预设名称
        val description: String,             // 描述
        val minWidth: Int,                  // 最小宽度(dp)
        val minHeight: Int,                 // 最小高度(dp)
        val layoutMode: Int,                // 布局模式
        val dockHeight: Int,               // Dock栏高度(dp)
        val iconSize: Int,                  // 图标大小(dp)
        val statusBarHeight: Int,           // 状态栏高度(dp)
        val isLandscape: Boolean,           // 是否横屏
        val enableGesture: Boolean,          // 启用手势
        val enablePip: Boolean,             // 启用画中画
        val brightness: Float               // 默认亮度
    )
    
    // 设备预设列表
    val presets = listOf(
        // 自动检测
        DevicePreset(
            id = PresetId.AUTO_DETECT,
            name = "自动检测",
            description = "根据当前设备自动配置",
            minWidth = 0,
            minHeight = 0,
            layoutMode = ScreenAdapter.LayoutMode.MODE_DEFAULT,
            dockHeight = 80,
            iconSize = 48,
            statusBarHeight = 24,
            isLandscape = true,
            enableGesture = true,
            enablePip = true,
            brightness = 0.7f
        ),
        
        // 7寸车机
        DevicePreset(
            id = PresetId.CAR_7_INCH,
            name = "7寸车机",
            description = "适用于1024x600分辨率的车机",
            minWidth = 600,
            minHeight = 400,
            layoutMode = ScreenAdapter.LayoutMode.MODE_HYBRID,
            dockHeight = 72,
            iconSize = 44,
            statusBarHeight = 20,
            isLandscape = true,
            enableGesture = true,
            enablePip = true,
            brightness = 0.8f
        ),
        
        // 10寸车机
        DevicePreset(
            id = PresetId.CAR_10_INCH,
            name = "10寸车机",
            description = "适用于1280x800分辨率的车机",
            minWidth = 800,
            minHeight = 500,
            layoutMode = ScreenAdapter.LayoutMode.MODE_MAP_FOCUS,
            dockHeight = 76,
            iconSize = 48,
            statusBarHeight = 24,
            isLandscape = true,
            enableGesture = true,
            enablePip = true,
            brightness = 0.7f
        ),
        
        // 12寸车机
        DevicePreset(
            id = PresetId.CAR_12_INCH,
            name = "12寸车机",
            description = "适用于1920x1200分辨率的大屏车机",
            minWidth = 1000,
            minHeight = 600,
            layoutMode = ScreenAdapter.LayoutMode.MODE_CARPLAY,
            dockHeight = 84,
            iconSize = 56,
            statusBarHeight = 28,
            isLandscape = true,
            enableGesture = true,
            enablePip = true,
            brightness = 0.6f
        ),
        
        // 特斯拉风格
        DevicePreset(
            id = PresetId.TESLA_STYLE,
            name = "特斯拉风格",
            description = "模拟特斯拉大屏的极简风格",
            minWidth = 1200,
            minHeight = 600,
            layoutMode = ScreenAdapter.LayoutMode.MODE_MINIMAL,
            dockHeight = 64,
            iconSize = 64,
            statusBarHeight = 32,
            isLandscape = true,
            enableGesture = true,
            enablePip = false,
            brightness = 0.5f
        ),
        
        // 竖屏车机
        DevicePreset(
            id = PresetId.VERTICAL_SCREEN,
            name = "竖屏车机",
            description = "适用于竖屏安装的车机",
            minWidth = 400,
            minHeight = 700,
            layoutMode = ScreenAdapter.LayoutMode.MODE_DEFAULT,
            dockHeight = 80,
            iconSize = 52,
            statusBarHeight = 24,
            isLandscape = false,
            enableGesture = true,
            enablePip = true,
            brightness = 0.7f
        ),
        
        // 手机兼容
        DevicePreset(
            id = PresetId.PHONE_COMPAT,
            name = "手机兼容",
            description = "适用于手机竖屏使用",
            minWidth = 300,
            minHeight = 500,
            layoutMode = ScreenAdapter.LayoutMode.MODE_DEFAULT,
            dockHeight = 60,
            iconSize = 48,
            statusBarHeight = 24,
            isLandscape = false,
            enableGesture = true,
            enablePip = true,
            brightness = 0.8f
        )
    )
    
    // SharedPreferences键名
    private const val PREF_NAME = "jiying_device_config"
    private const val KEY_ACTIVE_PRESET = "active_preset"
    private const val KEY_CUSTOM_WIDTH = "custom_width"
    private const val KEY_CUSTOM_HEIGHT = "custom_height"
    private const val KEY_CUSTOM_DOCK_HEIGHT = "custom_dock_height"
    private const val KEY_CUSTOM_ICON_SIZE = "custom_icon_size"
    private const val KEY_CUSTOM_LAYOUT_MODE = "custom_layout_mode"
    
    private lateinit var prefs: SharedPreferences
    private lateinit var context: Context
    
    /**
     * 初始化多设备配置管理器
     * @param ctx 应用程序上下文
     */
    fun init(ctx: Context) {
        context = ctx.applicationContext
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * 获取当前激活的预设ID
     */
    fun getActivePresetId(): Int {
        return prefs.getInt(KEY_ACTIVE_PRESET, PresetId.AUTO_DETECT)
    }
    
    /**
     * 获取当前激活的预设
     */
    fun getActivePreset(): DevicePreset {
        val presetId = getActivePresetId()
        return if (presetId == PresetId.CUSTOM) {
            getCustomPreset()
        } else {
            presets.find { it.id == presetId } ?: presets[0]
        }
    }
    
    /**
     * 设置激活的预设
     */
    fun setActivePreset(presetId: Int) {
        prefs.edit().putInt(KEY_ACTIVE_PRESET, presetId).apply()
        applyPreset(presetId)
    }
    
    /**
     * 应用预设配置
     */
    fun applyPreset(presetId: Int) {
        val preset = if (presetId == PresetId.CUSTOM) {
            getCustomPreset()
        } else {
            presets.find { it.id == presetId } ?: presets[0]
        }
        
        // 应用布局模式
        LayoutModeManager.setLayoutMode(preset.layoutMode)
        
        // 应用布局配置
        val config = LayoutModeManager.getCurrentLayoutConfig()
        LayoutModeManager.setCustomDockHeight(preset.dockHeight)
        
        // 保存激活的预设
        prefs.edit().putInt(KEY_ACTIVE_PRESET, presetId).apply()
    }
    
    /**
     * 获取自定义预设
     */
    private fun getCustomPreset(): DevicePreset {
        return DevicePreset(
            id = PresetId.CUSTOM,
            name = "自定义",
            description = "用户自定义配置",
            minWidth = prefs.getInt(KEY_CUSTOM_WIDTH, 800),
            minHeight = prefs.getInt(KEY_CUSTOM_HEIGHT, 500),
            layoutMode = prefs.getInt(KEY_CUSTOM_LAYOUT_MODE, ScreenAdapter.LayoutMode.MODE_DEFAULT),
            dockHeight = prefs.getInt(KEY_CUSTOM_DOCK_HEIGHT, 80),
            iconSize = prefs.getInt(KEY_CUSTOM_ICON_SIZE, 48),
            statusBarHeight = 24,
            isLandscape = true,
            enableGesture = true,
            enablePip = true,
            brightness = 0.7f
        )
    }
    
    /**
     * 保存自定义配置
     */
    fun saveCustomConfig(
        width: Int? = null,
        height: Int? = null,
        dockHeight: Int? = null,
        iconSize: Int? = null,
        layoutMode: Int? = null
    ) {
        val editor = prefs.edit()
        editor.putInt(KEY_ACTIVE_PRESET, PresetId.CUSTOM)
        
        width?.let { editor.putInt(KEY_CUSTOM_WIDTH, it) }
        height?.let { editor.putInt(KEY_CUSTOM_HEIGHT, it) }
        dockHeight?.let { editor.putInt(KEY_CUSTOM_DOCK_HEIGHT, it) }
        iconSize?.let { editor.putInt(KEY_CUSTOM_ICON_SIZE, it) }
        layoutMode?.let { 
            editor.putInt(KEY_CUSTOM_LAYOUT_MODE, it)
            LayoutModeManager.setLayoutMode(it)
        }
        
        editor.apply()
    }
    
    /**
     * 自动检测并应用最匹配的预设
     */
    fun autoDetectAndApply(): DevicePreset {
        ScreenAdapter.init(context)
        val config = ScreenAdapter.getCurrentConfig()
        
        // 根据设备配置找到最匹配的预设
        val matchedPreset = presets.find { preset ->
            preset.id != PresetId.AUTO_DETECT &&
            preset.id != PresetId.CUSTOM &&
            config.widthDp >= preset.minWidth &&
            config.heightDp >= preset.minHeight &&
            config.isLandscape == preset.isLandscape
        } ?: presets[PresetId.CAR_10_INCH]
        
        // 自动应用匹配的预设
        setActivePreset(matchedPreset.id)
        
        return matchedPreset
    }
    
    /**
     * 获取推荐预设
     */
    fun getRecommendedPreset(): DevicePreset {
        ScreenAdapter.init(context)
        val config = ScreenAdapter.getCurrentConfig()
        
        return presets.find { preset ->
            preset.id != PresetId.AUTO_DETECT &&
            preset.id != PresetId.CUSTOM &&
            config.widthDp >= preset.minWidth &&
            config.heightDp >= preset.minHeight
        } ?: presets[0]
    }
    
    /**
     * 获取所有预设列表
     */
    fun getAllPresets(): List<DevicePreset> = presets
    
    /**
     * 获取预设名称
     */
    fun getPresetName(presetId: Int): String {
        return presets.find { it.id == presetId }?.name ?: "未知"
    }
    
    /**
     * 获取预设描述
     */
    fun getPresetDescription(presetId: Int): String {
        return presets.find { it.id == presetId }?.description ?: ""
    }
    
    /**
     * 检测当前设备是否与预设匹配
     */
    fun isPresetMatch(presetId: Int): Boolean {
        ScreenAdapter.init(context)
        val config = ScreenAdapter.getCurrentConfig()
        val preset = presets.find { it.id == presetId } ?: return false
        
        return config.widthDp >= preset.minWidth &&
               config.heightDp >= preset.minHeight &&
               config.isLandscape == preset.isLandscape
    }
    
    /**
     * 获取预设兼容性信息
     */
    fun getPresetCompatibility(presetId: Int): String {
        ScreenAdapter.init(context)
        val config = ScreenAdapter.getCurrentConfig()
        val preset = presets.find { it.id == presetId } ?: return "未知"
        
        val widthMatch = config.widthDp >= preset.minWidth
        val heightMatch = config.heightDp >= preset.minHeight
        val orientationMatch = config.isLandscape == preset.isLandscape
        
        return when {
            widthMatch && heightMatch && orientationMatch -> "✅ 完全兼容"
            widthMatch && heightMatch -> "⚠️ 方向不同，可调整"
            widthMatch || heightMatch -> "⚠️ 部分兼容"
            else -> "❌ 不兼容"
        }
    }
    
    /**
     * 重置为自动检测
     */
    fun resetToAuto() {
        setActivePreset(PresetId.AUTO_DETECT)
    }
    
    /**
     * 获取设备信息摘要
     */
    fun getDeviceSummary(): String {
        ScreenAdapter.init(context)
        val config = ScreenAdapter.getCurrentConfig()
        val activePreset = getActivePreset()
        
        return buildString {
            appendLine("=== 设备信息 ===")
            appendLine("分辨率: ${ScreenAdapter.getScreenWidth()}x${ScreenAdapter.getScreenHeight()}")
            appendLine("屏幕尺寸: ${String.format("%.1f", config.screenSize)}英寸")
            appendLine("屏幕方向: ${if (config.isLandscape) "横屏" else "竖屏"}")
            appendLine("密度: ${config.density.toInt()}dpi")
            appendLine()
            appendLine("=== 当前配置 ===")
            appendLine("预设: ${activePreset.name}")
            appendLine("布局模式: ${ScreenAdapter.getLayoutModeName(activePreset.layoutMode)}")
            appendLine("Dock高度: ${activePreset.dockHeight}dp")
            appendLine("图标大小: ${activePreset.iconSize}dp")
        }
    }
}
