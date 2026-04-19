package com.jiying.launcher.ui.navigation

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.jiying.launcher.R
import com.jiying.launcher.service.NavigationFloatService

/**
 * 导航浮窗Activity
 * 支持导航小窗显示
 */
class NavigationFloatActivity : AppCompatActivity() {

    private lateinit var navContainer: CardView
    private lateinit var navTitle: TextView
    private lateinit var navDistance: TextView
    private lateinit var navEta: TextView
    private lateinit var startNavBtn: Button
    private lateinit var closeBtn: ImageButton
    
    private var isNavigating = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation_float)
        
        try {
            initViews()
            setupListeners()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initViews() {
        try {
            navContainer = findViewById(R.id.nav_container)
            navTitle = findViewById(R.id.nav_title)
            navDistance = findViewById(R.id.nav_distance)
            navEta = findViewById(R.id.nav_eta)
            startNavBtn = findViewById(R.id.btn_start_navigation)
            closeBtn = findViewById(R.id.btn_close_nav)
            
            // 默认状态
            navTitle.text = "选择一个目的地"
            navDistance.text = "--"
            navEta.text = "--"
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupListeners() {
        try {
            startNavBtn.setOnClickListener {
                if (isNavigating) {
                    stopNavigation()
                } else {
                    startNavigation()
                }
            }
            
            closeBtn.setOnClickListener {
                finish()
            }
            
            // 点击地图区域
            navContainer.setOnClickListener {
                if (!isNavigating) {
                    Toast.makeText(this, "正在打开地图...", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startNavigation() {
        try {
            isNavigating = true
            startNavBtn.text = "停止导航"
            
            // 模拟导航数据
            navTitle.text = "正在导航"
            navDistance.text = "前方 500 米"
            navEta.text = "约 3 分钟"
            
            // 启动导航浮窗服务
            val intent = Intent(this, NavigationFloatService::class.java)
            startService(intent)
            
            Toast.makeText(this, "开始导航", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopNavigation() {
        try {
            isNavigating = false
            startNavBtn.text = "开始导航"
            
            navTitle.text = "选择一个目的地"
            navDistance.text = "--"
            navEta.text = "--"
            
            // 停止导航浮窗服务
            val intent = Intent(this, NavigationFloatService::class.java)
            stopService(intent)
            
            Toast.makeText(this, "导航已停止", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
