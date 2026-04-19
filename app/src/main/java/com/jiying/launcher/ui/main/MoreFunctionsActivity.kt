package com.jiying.launcher.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.jiying.launcher.R
import com.jiying.launcher.ui.apps.AppManagerActivity
import com.jiying.launcher.ui.driving.DrivingModeActivity
import com.jiying.launcher.ui.media.ImageViewerActivity
import com.jiying.launcher.ui.music.MusicPlayerActivity
import com.jiying.launcher.ui.settings.FloatBallSettingsActivity
import com.jiying.launcher.ui.settings.SystemSettingsActivity
import com.jiying.launcher.ui.theme.ThemeCenterActivity
import com.jiying.launcher.ui.usb.UsbDeviceManagerActivity
import com.jiying.launcher.ui.video.VideoPlayerActivity

/**
 * 更多功能Activity
 * 功能入口集合
 */
class MoreFunctionsActivity : AppCompatActivity() {

    private lateinit var functionGrid: GridLayout
    
    private val functions = listOf(
        FunctionItem("驾驶模式", "ic_car", DrivingModeActivity::class.java),
        FunctionItem("主题中心", "ic_theme", ThemeCenterActivity::class.java),
        FunctionItem("应用管理", "ic_apps", AppManagerActivity::class.java),
        FunctionItem("悬浮球设置", "ic_circle", FloatBallSettingsActivity::class.java),
        FunctionItem("音乐播放", "ic_music_note", MusicPlayerActivity::class.java),
        FunctionItem("视频播放", "ic_video_file", VideoPlayerActivity::class.java),
        FunctionItem("图片查看", "ic_image", ImageViewerActivity::class.java),
        FunctionItem("USB管理", "ic_usb", UsbDeviceManagerActivity::class.java),
        FunctionItem("系统设置", "ic_settings", SystemSettingsActivity::class.java),
        FunctionItem("文件管理", "ic_folder", null),
        FunctionItem("浏览器", "ic_web", null),
        FunctionItem("计算器", "ic_calculator", null)
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_more_functions)
        
        try {
            initViews()
            loadFunctions()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initViews() {
        try {
            functionGrid = findViewById(R.id.function_grid)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadFunctions() {
        try {
            functionGrid.removeAllViews()
            
            functions.forEachIndexed { index, function ->
                val view = layoutInflater.inflate(R.layout.item_function, functionGrid, false)
                
                val icon = view.findViewById<ImageView>(R.id.function_icon)
                val title = view.findViewById<TextView>(R.id.function_title)
                
                title.text = function.title
                
                // 设置图标
                val iconRes = when (function.iconName) {
                    "ic_car" -> R.drawable.ic_car
                    "ic_theme" -> R.drawable.ic_theme
                    "ic_apps" -> R.drawable.ic_apps
                    "ic_circle" -> R.drawable.ic_circle
                    "ic_music_note" -> R.drawable.ic_music_note
                    "ic_video_file" -> R.drawable.ic_video_file
                    "ic_image" -> R.drawable.ic_image
                    "ic_usb" -> R.drawable.ic_usb
                    "ic_settings" -> R.drawable.ic_settings
                    "ic_folder" -> R.drawable.ic_folder
                    "ic_web" -> R.drawable.ic_web
                    "ic_calculator" -> R.drawable.ic_calculator
                    else -> R.drawable.ic_apps
                }
                icon.setImageResource(iconRes)
                
                view.setOnClickListener {
                    if (function.activity != null) {
                        startActivity(Intent(this, function.activity))
                    } else {
                        Toast.makeText(this, "${function.title} 功能开发中", Toast.LENGTH_SHORT).show()
                    }
                }
                
                functionGrid.addView(view)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

data class FunctionItem(
    val title: String,
    val iconName: String,
    val activity: Class<*>?
)
