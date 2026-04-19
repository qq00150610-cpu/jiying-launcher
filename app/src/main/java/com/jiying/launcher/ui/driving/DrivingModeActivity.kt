package com.jiying.launcher.ui.driving

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.jiying.launcher.R
import com.jiying.launcher.ui.apps.AppsCenterActivity
import com.jiying.launcher.ui.music.MusicPlayerActivity
import com.jiying.launcher.ui.navigation.NavigationFloatActivity
import com.jiying.launcher.ui.video.VideoPlayerActivity

/**
 * 驾驶模式Activity
 * 简化界面，大按钮，方便驾驶时操作
 */
class DrivingModeActivity : AppCompatActivity() {

    private lateinit var rootLayout: RelativeLayout
    private lateinit var voiceBtn: CardView
    private lateinit var navBtn: CardView
    private lateinit var musicBtn: CardView
    private lateinit var callBtn: CardView
    private lateinit var videoBtn: CardView
    private lateinit var appsBtn: CardView
    private lateinit var exitBtn: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driving_mode)
        
        try {
            initViews()
            setupClickListeners()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initViews() {
        try {
            rootLayout = findViewById(R.id.driving_root)
            voiceBtn = findViewById(R.id.btn_voice)
            navBtn = findViewById(R.id.btn_navigation)
            musicBtn = findViewById(R.id.btn_music)
            callBtn = findViewById(R.id.btn_call)
            videoBtn = findViewById(R.id.btn_video)
            appsBtn = findViewById(R.id.btn_apps)
            exitBtn = findViewById(R.id.btn_exit_driving)
            
            // 隐藏状态栏
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupClickListeners() {
        try {
            voiceBtn.setOnClickListener {
                // 启动语音助手
                Toast.makeText(this, "语音助手已启动", Toast.LENGTH_SHORT).show()
            }
            
            navBtn.setOnClickListener {
                startActivity(Intent(this, NavigationFloatActivity::class.java))
            }
            
            musicBtn.setOnClickListener {
                startActivity(Intent(this, MusicPlayerActivity::class.java))
            }
            
            callBtn.setOnClickListener {
                // 打开联系人或拨号
                Toast.makeText(this, "打开拨号界面", Toast.LENGTH_SHORT).show()
            }
            
            videoBtn.setOnClickListener {
                startActivity(Intent(this, VideoPlayerActivity::class.java))
            }
            
            appsBtn.setOnClickListener {
                startActivity(Intent(this, AppsCenterActivity::class.java))
            }
            
            exitBtn.setOnClickListener {
                finish()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBackPressed() {
        // 驾驶模式下禁用返回键
        Toast.makeText(this, "请点击退出按钮离开驾驶模式", Toast.LENGTH_SHORT).show()
    }
}
