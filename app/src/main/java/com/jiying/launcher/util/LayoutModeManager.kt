package com.jiying.launcher.util

import android.content.Context
import android.content.SharedPreferences

/**
 * 极影桌面 - 布局模式管理器
 * 
 * 功能说明：
 * - 管理6种首页布局模式（借鉴氢桌面）
 * - 支持布局模式切换和保存
 * - 提供布局相关配置参数
 * - 兼容Android 6.0 (API 23) 及以上系统
 * 
 * 布局模式说明：
 * 1. 默认模式（MODE_DEFAULT）- 布丁UI风格，地图+音乐双卡片
 * 2. 混合模式（MODE_HYBRID）- 地图小窗+快捷应用栏
 * 3. 地图优先（MODE_MAP_FOCUS）- 全屏地图+底部音乐
 * 4. 音乐优先（MODE_MUSIC_FOCUS）- 全屏音乐+底部地图
 * 5. 极简模式（MODE_MINIMAL）- 极简设计，只保留核心功能
 * 6. CarPlay风格（MODE_CARPLAY）- 类似CarPlay的大图标布局
 * 
 * @author 极影桌面开发团队
 * @version 1.0.0
 */
object LayoutModeManager {
    
    // 布局模式配置数据类
    data class LayoutConfig(
        val mode: Int,                    // 布局模式
        val mapRatio: Float,              // 地图区域占比 (0.0-1.0)
        val musicRatio: Float,            // 音乐区域占比 (0.0-1.0)
        val dockHeight: Int,              // 底部Dock栏高度(dp)
        val iconSize: Int,                // 图标大小(dp)
        val showWidget: Boolean,          // 是否显示小部件
        val enableGesture: Boolean,       // 是否启用手势
        val cardSpacing: Int              // 卡片间距(dp)
    )
    
    // 布局模式配置映射
    private val layoutConfigs = mapOf(
        ScreenAdapter.LayoutMode.MODE_DEFAULT to LayoutConfig(
            mode = ScreenAdapter.LayoutMode.MODE_DEFAULT,
            mapRatio = 0.5f,
            musicRatio = 0.5f,
            dockHeight = 80,
            iconSize = 48,
            showWidget = true,
            enableGesture = true,
            cardSpacing = 12
        ),
        ScreenAdapter.LayoutMode.MODE_HYBRID to LayoutConfig(
            mode = ScreenAdapter.LayoutMode.MODE_HYBRID,
            mapRatio = 0.4f,
            musicRatio = 0.3f,
            dockHeight = 72,
            iconSize = 44,
            showWidget = true,
            enableGesture = true,
            cardSpacing = 8
        ),
        ScreenAdapter.LayoutMode.MODE_MAP_FOCUS to LayoutConfig(
            mode = ScreenAdapter.LayoutMode.MODE_MAP_FOCUS,
            mapRatio = 0.7f,
            musicRatio = 0.2f,
            dockHeight = 68,
            iconSize = 40,
            showWidget = false,
            enableGesture = true,
            cardSpacing = 6
        ),
        ScreenAdapter.LayoutMode.MODE_MUSIC_FOCUS to LayoutConfig(
            mode = ScreenAdapter.LayoutMode.MODE_MUSIC_FOCUS,
            mapRatio = 0.25f,
            musicRatio = 0.6f,
            dockHeight = 76,
            iconSize = 52,
            showWidget = true,
            enableGesture = true,
            cardSpacing = 10
        ),
        ScreenAdapter.LayoutMode.MODE_MINIMAL to LayoutConfig(
            mode = ScreenAdapter.LayoutMode.MODE_MINIMAL,
            mapRatio = 0.0f,
            musicRatio = 0.0f,
            dockHeight = 64,
            iconSize = 56,
            showWidget = false,
            enableGesture = false,
            cardSpacing = 4
        ),
        ScreenAdapter.LayoutMode.MODE_CARPLAY to LayoutConfig(
            mode = ScreenAdapter.LayoutMode.MODE_CARPLAY,
            mapRatio = 0.35f,
            musicRatio = 0.35f,
            dockHeight = 84,
            iconSize = 64,
            showWidget = true,
            enableGesture = true,
            cardSpacing = 16
        )
    )
    
    // SharedPreferences键名
    private const val PREF_NAME = "jiying_layout"
    private const val KEY_LAYOUT_MODE = "layout_mode"
    private const val KEY_CUSTOM_MAP_RATIO = "custom_map_ratio"
    private const val KEY_CUSTOM_MUSIC_RATIO = "custom_music_ratio"
    private const val KEY_CUSTOM_DOCK_HEIGHT = "custom_dock_height"
    private const val KEY_SHOW_WIDGET = "show_widget"
    private const val KEY_ENABLE_GESTURE = "enable_gesture"
    private const val KEY_HORIZONTAL_MODE = "horizontal_mode"
    
    private lateinit var prefs: SharedPreferences
    private lateinit var context: Context
    
    /**
     * 初始化布局管理器
     * @param ctx 应用程序上下文
     */
    fun init(ctx: Context) {
        context = ctx.applicationContext
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * 获取当前布局模式
     * @return 当前布局模式，默认为设备推荐模式
     */
    fun getCurrentLayoutMode(): Int {
        return prefs.getInt(KEY_LAYOUT_MODE, getRecommendedMode())
    }
    
    /**
     * 设置布局模式
     * @param mode 布局模式
     */
    fun setLayoutMode(mode: Int) {
        if (layoutConfigs.containsKey(mode)) {
            prefs.edit().putInt(KEY_LAYOUT_MODE, mode).apply()
        }
    }
    
    /**
     * 获取推荐布局模式（根据设备类型）
     * @return 推荐布局模式
     */
    fun getRecommendedMode(): Int {
        ScreenAdapter.init(context)
        return ScreenAdapter.getCurrentConfig().recommendedLayoutMode
    }
    
    /**
     * 获取当前布局配置
     * @return 布局配置
     */
    fun getCurrentLayoutConfig(): LayoutConfig {
        val mode = getCurrentLayoutMode()
        val baseConfig = layoutConfigs[mode] ?: layoutConfigs[ScreenAdapter.LayoutMode.MODE_DEFAULT]!!
        
        // 如果启用了自定义配置，则返回自定义配置
        return if (hasCustomConfig()) {
            baseConfig.copy(
                mapRatio = prefs.getFloat(KEY_CUSTOM_MAP_RATIO, baseConfig.mapRatio),
                musicRatio = prefs.getFloat(KEY_CUSTOM_MUSIC_RATIO, baseConfig.musicRatio),
                dockHeight = prefs.getInt(KEY_CUSTOM_DOCK_HEIGHT, baseConfig.dockHeight),
                showWidget = prefs.getBoolean(KEY_SHOW_WIDGET, baseConfig.showWidget),
                enableGesture = prefs.getBoolean(KEY_ENABLE_GESTURE, baseConfig.enableGesture)
            )
        } else {
            baseConfig
        }
    }
    
    /**
     * 获取指定布局模式的配置
     * @param mode 布局模式
     * @return 布局配置
     */
    fun getLayoutConfig(mode: Int): LayoutConfig {
        return layoutConfigs[mode] ?: layoutConfigs[ScreenAdapter.LayoutMode.MODE_DEFAULT]!!
    }
    
    /**
     * 是否存在自定义配置
     */
    fun hasCustomConfig(): Boolean {
        return prefs.contains(KEY_CUSTOM_MAP_RATIO) || 
               prefs.contains(KEY_CUSTOM_MUSIC_RATIO) ||
               prefs.contains(KEY_CUSTOM_DOCK_HEIGHT)
    }
    
    /**
     * 保存自定义地图区域占比
     * @param ratio 占比 (0.0-1.0)
     */
    fun setCustomMapRatio(ratio: Float) {
        prefs.edit().putFloat(KEY_CUSTOM_MAP_RATIO, ratio.coerceIn(0.1f, 0.9f)).apply()
    }
    
    /**
     * 保存自定义音乐区域占比
     * @param ratio 占比 (0.0-1.0)
     */
    fun setCustomMusicRatio(ratio: Float) {
        prefs.edit().putFloat(KEY_CUSTOM_MUSIC_RATIO, ratio.coerceIn(0.1f, 0.9f)).apply()
    }
    
    /**
     * 保存自定义Dock栏高度
     * @param height 高度(dp)
     */
    fun setCustomDockHeight(height: Int) {
        prefs.edit().putInt(KEY_CUSTOM_DOCK_HEIGHT, height.coerceIn(48, 120)).apply()
    }
    
    /**
     * 设置是否显示小部件
     * @param show 是否显示
     */
    fun setShowWidget(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_WIDGET, show).apply()
    }
    
    /**
     * 设置是否启用手势
     * @param enable 是否启用
     */
    fun setEnableGesture(enable: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLE_GESTURE, enable).apply()
    }
    
    /**
     * 设置横屏模式
     * @param horizontal 是否横屏
     */
    fun setHorizontalMode(horizontal: Boolean) {
        prefs.edit().putBoolean(KEY_HORIZONTAL_MODE, horizontal).apply()
    }
    
    /**
     * 是否为横屏模式
     */
    fun isHorizontalMode(): Boolean {
        return prefs.getBoolean(KEY_HORIZONTAL_MODE, ScreenAdapter.isLandscape())
    }
    
    /**
     * 重置为默认配置
     */
    fun resetToDefault() {
        prefs.edit().clear().apply()
        setLayoutMode(getRecommendedMode())
    }
    
    /**
     * 获取所有布局模式列表
     * @return 布局模式列表
     */
    fun getAllLayoutModes(): List<Pair<Int, String>> {
        return listOf(
            ScreenAdapter.LayoutMode.MODE_DEFAULT to "默认模式",
            ScreenAdapter.LayoutMode.MODE_HYBRID to "混合模式",
            ScreenAdapter.LayoutMode.MODE_MAP_FOCUS to "地图优先",
            ScreenAdapter.LayoutMode.MODE_MUSIC_FOCUS to "音乐优先",
            ScreenAdapter.LayoutMode.MODE_MINIMAL to "极简模式",
            ScreenAdapter.LayoutMode.MODE_CARPLAY to "CarPlay风格"
        )
    }
    
    /**
     * 获取布局模式描述
     * @param mode 布局模式
     * @return 布局模式描述
     */
    fun getLayoutModeDescription(mode: Int): String {
        return when (mode) {
            ScreenAdapter.LayoutMode.MODE_DEFAULT -> "经典双卡片布局，地图与音乐并列显示"
            ScreenAdapter.LayoutMode.MODE_HYBRID -> "紧凑混合布局，适合大多数车机屏幕"
            ScreenAdapter.LayoutMode.MODE_MAP_FOCUS -> "地图占据主要视野，驾驶导航更清晰"
            ScreenAdapter.LayoutMode.MODE_MUSIC_FOCUS -> "音乐界面最大化，适合音乐爱好者"
            ScreenAdapter.LayoutMode.MODE_MINIMAL -> "极简设计，只保留最常用的功能入口"
            ScreenAdapter.LayoutMode.MODE_CARPLAY -> "类似CarPlay的大图标风格，简洁直观"
            else -> "未知布局模式"
        }
    }
    
    /**
     * 获取布局模式预览图标
     * @param mode 布局模式
     * @return 预览图标资源名
     */
    fun getLayoutPreviewIcon(mode: Int): String {
        return when (mode) {
            ScreenAdapter.LayoutMode.MODE_DEFAULT -> "ic_layout_default"
            ScreenAdapter.LayoutMode.MODE_HYBRID -> "ic_layout_hybrid"
            ScreenAdapter.LayoutMode.MODE_MAP_FOCUS -> "ic_layout_map"
            ScreenAdapter.LayoutMode.MODE_MUSIC_FOCUS -> "ic_layout_music"
            ScreenAdapter.LayoutMode.MODE_MINIMAL -> "ic_layout_minimal"
            ScreenAdapter.LayoutMode.MODE_CARPLAY -> "ic_layout_carplay"
            else -> "ic_layout_default"
        }
    }
    
    /**
     * 获取布局模式对应的布局文件
     * @param mode 布局模式
     * @return 布局文件资源名
     */
    fun getLayoutResourceName(mode: Int): String {
        return when (mode) {
            ScreenAdapter.LayoutMode.MODE_DEFAULT -> "activity_main"
            ScreenAdapter.LayoutMode.MODE_HYBRID -> "activity_main_hybrid"
            ScreenAdapter.LayoutMode.MODE_MAP_FOCUS -> "activity_main_map_focus"
            ScreenAdapter.LayoutMode.MODE_MUSIC_FOCUS -> "activity_main_music_focus"
            ScreenAdapter.LayoutMode.MODE_MINIMAL -> "activity_main_minimal"
            ScreenAdapter.LayoutMode.MODE_CARPLAY -> "activity_main_carplay"
            else -> "activity_main"
        }
    }
}
