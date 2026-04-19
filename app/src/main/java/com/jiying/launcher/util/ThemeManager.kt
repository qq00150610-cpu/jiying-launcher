package com.jiying.launcher.util

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

/**
 * 极影桌面 - 主题管理器
 * 支持布丁UI风格和氢桌面风格两种主题切换
 */
object ThemeManager {

    // 主题常量
    const val THEME_BUDING = "buding"      // 布丁UI风格（默认）
    const val THEME_HYDROGEN = "hydrogen"   // 氢桌面风格

    // 主题键名
    private const val PREF_NAME = "jiying_theme"
    private const val KEY_THEME = "current_theme"
    
    // 布丁UI风格特点
    // - 现代化卡片设计
    // - 活泼配色
    // - 圆角适中
    // - 功能丰富的快捷入口
    
    // 氢桌面风格特点
    // - 简洁卡片化设计
    // - 大面积留白
    // - 圆角卡片
    // - 清爽的配色

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 获取当前主题
     */
    fun getCurrentTheme(): String {
        return prefs.getString(KEY_THEME, THEME_BUDING) ?: THEME_BUDING
    }

    /**
     * 设置主题
     */
    fun setTheme(theme: String) {
        prefs.edit().putString(KEY_THEME, theme).apply()
    }

    /**
     * 切换主题
     */
    fun toggleTheme() {
        val currentTheme = getCurrentTheme()
        val newTheme = if (currentTheme == THEME_BUDING) THEME_HYDROGEN else THEME_BUDING
        setTheme(newTheme)
        applyTheme(newTheme)
    }

    /**
     * 应用主题 - 不使用AppCompatDelegate，避免闪屏
     */
    fun applyTheme(theme: String = getCurrentTheme()) {
        // 不调用AppCompatDelegate.setDefaultNightMode，避免Activity重建闪屏
        // 主题切换由MainActivity手动处理背景颜色
    }

    /**
     * 获取布丁UI风格资源
     */
    fun getBudingStyle(): BudingStyle {
        return BudingStyle()
    }

    /**
     * 获取氢桌面风格资源
     */
    fun getHydrogenStyle(): HydrogenStyle {
        return HydrogenStyle()
    }

    /**
     * 布丁UI风格配置
     */
    class BudingStyle {
        // 主色调
        val primaryColor = 0xFF2196F3.toInt()      // 蓝色
        val accentColor = 0xFFFF5722.toInt()        // 橙红色
        val backgroundColor = 0xFFF5F5F5.toInt()   // 浅灰背景
        val cardColor = 0xFFFFFFFF.toInt()          // 白色卡片
        
        // 圆角大小
        val cardCornerRadius = 16f
        val buttonCornerRadius = 12f
        
        // 阴影
        val cardElevation = 8f
        
        // 文字大小
        val titleSize = 24f
        val bodySize = 16f
        val captionSize = 14f
        
        // 图标大小
        val iconSize = 48f
        val largeIconSize = 64f
    }

    /**
     * 氢桌面风格配置
     */
    class HydrogenStyle {
        // 主色调 - 更加清爽
        val primaryColor = 0xFF4CAF50.toInt()       // 绿色
        val accentColor = 0xFF03A9F4.toInt()       // 浅蓝色
        val backgroundColor = 0xFFFAFAFA.toInt()   // 近乎白色背景
        val cardColor = 0xFFFFFFFF.toInt()         // 纯白卡片
        
        // 圆角大小 - 更大圆角
        val cardCornerRadius = 24f
        val buttonCornerRadius = 16f
        
        // 阴影 - 更柔和
        val cardElevation = 4f
        
        // 文字大小 - 略小
        val titleSize = 22f
        val bodySize = 14f
        val captionSize = 12f
        
        // 图标大小 - 适中
        val iconSize = 40f
        val largeIconSize = 56f
    }
}
