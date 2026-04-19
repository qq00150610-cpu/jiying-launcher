package com.jiying.launcher.util

import android.content.Context
import android.content.res.Configuration
import android.util.DisplayMetrics
import android.view.WindowManager

/**
 * 极影桌面 - 屏幕适配工具类
 * 
 * 功能说明：
 * - 处理不同屏幕尺寸的适配
 * - 支持横屏/竖屏切换检测
 * - 提供不同分辨率设备（7寸、10寸、12寸车机）的尺寸计算
 * - 兼容Android 6.0 (API 23) 及以上系统
 * 
 * @author 极影桌面开发团队
 * @version 1.0.0
 */
object ScreenAdapter {
    
    // 屏幕尺寸常量（单位：dp）
    object ScreenSize {
        const val SMALL = 7      // 7寸屏 (1024x600)
        const val MEDIUM = 10    // 10寸屏 (1280x800)
        const val LARGE = 12     // 12寸屏 (1920x1200)
    }
    
    // 设备类型常量
    object DeviceType {
        const val PHONE = 0          // 手机
        const val TABLET_7_INCH = 1  // 7寸平板/车机
        const val TABLET_10_INCH = 2 // 10寸平板/车机
        const val TABLET_12_INCH = 3  // 12寸平板/车机
        const val CAR_MACHINE = 4     // 通用车机
    }
    
    // 布局模式常量（类似氢桌面的6种布局）
    object LayoutMode {
        const val MODE_DEFAULT = 0      // 默认模式（布丁UI风格）
        const val MODE_HYBRID = 1       // 混合模式（地图+音乐）
        const val MODE_MAP_FOCUS = 2    // 地图优先模式
        const val MODE_MUSIC_FOCUS = 3  // 音乐优先模式
        const val MODE_MINIMAL = 4      // 极简模式
        const val MODE_CARPLAY = 5      // CarPlay风格模式
    }
    
    // 设备配置数据类
    data class DeviceConfig(
        val deviceType: Int,           // 设备类型
        val screenSize: Float,          // 屏幕尺寸（英寸）
        val widthDp: Int,              // 屏幕宽度(dp)
        val heightDp: Int,             // 屏幕高度(dp)
        val density: Float,            // 屏幕密度
        val isLandscape: Boolean,     // 是否横屏
        val recommendedLayoutMode: Int // 推荐布局模式
    )
    
    // 当前设备配置
    private var currentConfig: DeviceConfig? = null
    
    // 上下文引用
    private lateinit var context: Context
    
    /**
     * 初始化屏幕适配器
     * @param ctx 应用程序上下文
     */
    fun init(ctx: Context) {
        context = ctx.applicationContext
        updateDeviceConfig()
    }
    
    /**
     * 更新设备配置信息
     */
    fun updateDeviceConfig() {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        
        // 计算屏幕尺寸（英寸）
        val widthPixels = displayMetrics.widthPixels
        val heightPixels = displayMetrics.heightPixels
        val density = displayMetrics.densityDpi
        
        // 计算dp值
        val widthDp = (widthPixels / displayMetrics.density).toInt()
        val heightDp = (heightPixels / displayMetrics.density).toInt()
        
        // 计算屏幕尺寸（英寸）- 使用勾股定理
        val screenSize = calculateScreenSize(widthPixels, heightPixels, displayMetrics.xdpi, displayMetrics.ydpi)
        
        // 判断是否横屏
        val isLandscape = widthDp > heightDp
        
        // 判断设备类型
        val deviceType = determineDeviceType(screenSize, widthDp, heightDp)
        
        // 确定推荐布局模式
        val recommendedLayout = getRecommendedLayoutMode(deviceType, isLandscape)
        
        currentConfig = DeviceConfig(
            deviceType = deviceType,
            screenSize = screenSize,
            widthDp = widthDp,
            heightDp = heightDp,
            density = density.toFloat(),
            isLandscape = isLandscape,
            recommendedLayoutMode = recommendedLayout
        )
    }
    
    /**
     * 计算屏幕尺寸（英寸）
     */
    private fun calculateScreenSize(widthPx: Int, heightPx: Int, xdpi: Float, ydpi: Float): Float {
        val widthInches = widthPx / xdpi
        val heightInches = heightPx / ydpi
        return Math.sqrt((widthInches * widthInches + heightInches * heightInches).toDouble()).toFloat()
    }
    
    /**
     * 判断设备类型
     */
    private fun determineDeviceType(screenSize: Float, widthDp: Int, heightDp: Int): Int {
        return when {
            // 车机判断：宽度大于600dp且宽度小于高度*1.5倍
            widthDp > 600 && widthDp < Math.max(widthDp, heightDp) * 1.5 -> {
                when {
                    screenSize <= 8 -> DeviceType.TABLET_7_INCH
                    screenSize <= 11 -> DeviceType.TABLET_10_INCH
                    else -> DeviceType.TABLET_12_INCH
                }
            }
            screenSize <= 7 -> DeviceType.TABLET_7_INCH
            screenSize <= 10 -> DeviceType.TABLET_10_INCH
            screenSize <= 13 -> DeviceType.TABLET_12_INCH
            else -> DeviceType.PHONE
        }
    }
    
    /**
     * 根据设备类型和屏幕方向获取推荐布局模式
     */
    private fun getRecommendedLayoutMode(deviceType: Int, isLandscape: Boolean): Int {
        return when (deviceType) {
            DeviceType.TABLET_7_INCH -> if (isLandscape) LayoutMode.MODE_HYBRID else LayoutMode.MODE_DEFAULT
            DeviceType.TABLET_10_INCH -> if (isLandscape) LayoutMode.MODE_MAP_FOCUS else LayoutMode.MODE_DEFAULT
            DeviceType.TABLET_12_INCH -> LayoutMode.MODE_CARPLAY
            DeviceType.CAR_MACHINE -> LayoutMode.MODE_HYBRID
            else -> LayoutMode.MODE_DEFAULT
        }
    }
    
    /**
     * 获取当前设备配置
     */
    fun getCurrentConfig(): DeviceConfig {
        return currentConfig ?: run {
            updateDeviceConfig()
            currentConfig!!
        }
    }
    
    /**
     * 获取屏幕宽度（像素）
     */
    fun getScreenWidth(): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        return displayMetrics.widthPixels
    }
    
    /**
     * 获取屏幕高度（像素）
     */
    fun getScreenHeight(): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }
    
    /**
     * 获取屏幕宽度（dp）
     */
    fun getScreenWidthDp(): Int {
        return getScreenWidth() / context.resources.displayMetrics.density.toInt()
    }
    
    /**
     * 获取屏幕高度（dp）
     */
    fun getScreenHeightDp(): Int {
        return getScreenHeight() / context.resources.displayMetrics.density.toInt()
    }
    
    /**
     * dp转px
     */
    fun dp2px(dp: Float): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
    
    /**
     * px转dp
     */
    fun px2dp(px: Int): Float {
        return px / context.resources.displayMetrics.density
    }
    
    /**
     * sp转px
     */
    fun sp2px(sp: Float): Float {
        return sp * context.resources.displayMetrics.scaledDensity
    }
    
    /**
     * 判断是否横屏
     */
    fun isLandscape(): Boolean {
        return context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }
    
    /**
     * 判断是否竖屏
     */
    fun isPortrait(): Boolean {
        return context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    }
    
    /**
     * 根据设备类型获取适配的尺寸
     * @param size7Inch 7寸设备的尺寸
     * @param size10Inch 10寸设备的尺寸
     * @param size12Inch 12寸设备的尺寸
     * @return 适配后的尺寸（像素）
     */
    fun getAdaptiveSize(size7Inch: Float, size10Inch: Float, size12Inch: Float): Int {
        val config = getCurrentConfig()
        val baseSize = when (config.deviceType) {
            DeviceType.TABLET_7_INCH -> size7Inch
            DeviceType.TABLET_10_INCH -> size10Inch
            DeviceType.TABLET_12_INCH -> size12Inch
            else -> size10Inch
        }
        return dp2px(baseSize)
    }
    
    /**
     * 获取状态栏高度
     */
    fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }
    
    /**
     * 获取导航栏高度
     */
    fun getNavigationBarHeight(): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }
    
    /**
     * 判断是否为车机设备
     */
    fun isCarDevice(): Boolean {
        val config = getCurrentConfig()
        return config.deviceType in listOf(
            DeviceType.TABLET_7_INCH,
            DeviceType.TABLET_10_INCH,
            DeviceType.TABLET_12_INCH,
            DeviceType.CAR_MACHINE
        )
    }
    
    /**
     * 获取设备描述信息
     */
    fun getDeviceDescription(): String {
        val config = getCurrentConfig()
        return buildString {
            append("设备类型: ")
            append(when (config.deviceType) {
                DeviceType.PHONE -> "手机"
                DeviceType.TABLET_7_INCH -> "7寸平板/车机"
                DeviceType.TABLET_10_INCH -> "10寸平板/车机"
                DeviceType.TABLET_12_INCH -> "12寸平板/车机"
                DeviceType.CAR_MACHINE -> "通用车机"
                else -> "未知设备"
            })
            append("\n屏幕尺寸: ${String.format("%.1f", config.screenSize)}英寸")
            append("\n分辨率: ${getScreenWidth()}x${getScreenHeight()} (${config.widthDp}x${config.heightDp}dp)")
            append("\n屏幕方向: ${if (config.isLandscape) "横屏" else "竖屏"}")
            append("\n推荐布局: ${getLayoutModeName(config.recommendedLayoutMode)}")
        }
    }
    
    /**
     * 获取布局模式名称
     */
    fun getLayoutModeName(mode: Int): String {
        return when (mode) {
            LayoutMode.MODE_DEFAULT -> "默认模式"
            LayoutMode.MODE_HYBRID -> "混合模式"
            LayoutMode.MODE_MAP_FOCUS -> "地图优先"
            LayoutMode.MODE_MUSIC_FOCUS -> "音乐优先"
            LayoutMode.MODE_MINIMAL -> "极简模式"
            LayoutMode.MODE_CARPLAY -> "CarPlay风格"
            else -> "未知模式"
        }
    }
}
