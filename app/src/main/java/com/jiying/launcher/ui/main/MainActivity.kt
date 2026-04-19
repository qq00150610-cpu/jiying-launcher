package com.jiying.launcher.ui.main

import android.bluetooth.BluetoothAdapter
import android.content.*
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.*
import android.graphics.Color
import android.provider.Settings
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import com.jiying.launcher.R
import com.jiying.launcher.ui.settings.SystemSettingsActivity
import com.jiying.launcher.ui.layout.LayoutModeSelectorActivity
import com.jiying.launcher.ui.settings.DeviceConfigActivity
import com.jiying.launcher.ui.service.FileManagerActivity
import com.jiying.launcher.ui.video.VideoPlayerActivity
import com.jiying.launcher.ui.music.MusicPlayerActivity
import com.jiying.launcher.ui.navigation.NavigationFloatActivity
import com.jiying.launcher.ui.apps.AppsCenterActivity
import com.jiying.launcher.service.FloatingMapService
import com.jiying.launcher.util.ThemeManager
import com.jiying.launcher.util.ScreenAdapter
import com.jiying.launcher.util.LayoutModeManager
import java.text.SimpleDateFormat
import java.util.*

/**
 * 极影桌面 - 主界面
 * 
 * 复刻布丁UI风格的车机桌面，支持两种主题切换：
 * - 布丁UI风格（默认）
 * - 氢桌面风格（简洁卡片化）
 * 
 * 功能入口：
 * - 系统设置
 * - 文件管理
 * - 视频播放器
 * - 音乐播放器
 * - 画中画导航
 * - 布局模式选择
 * 
 * @author 极影桌面开发团队
 * @version 2.0.0
 */
class MainActivity : AppCompatActivity() {

    // UI组件
    private lateinit var rootLayout: RelativeLayout
    private lateinit var wallpaperImageView: ImageView
    private lateinit var dateTimeTextView: TextView
    
    // 顶部状态栏
    private lateinit var userAvatar: ImageView
    private lateinit var volumeBtn: ImageView
    private lateinit var notificationBtn: ImageView
    
    // 导航卡片
    private lateinit var navCard: CardView
    private lateinit var navClose: ImageView
    private lateinit var openMapBtn: LinearLayout
    
    // 音乐卡片
    private lateinit var musicCard: CardView
    private lateinit var musicClose: ImageView
    private lateinit var musicTitle: TextView
    private lateinit var musicArtist: TextView
    private lateinit var musicPlayPause: ImageButton
    private lateinit var musicPrev: ImageButton
    private lateinit var musicNext: ImageButton
    private lateinit var musicLock: ImageButton
    
    // 横屏快捷功能
    private lateinit var fileManagerLand: LinearLayout
    private lateinit var videoPlayerLand: LinearLayout
    private lateinit var pipModeLand: LinearLayout
    
    // 底部应用栏
    private lateinit var myAppsBtn: LinearLayout
    private lateinit var systemSettingsBtn: LinearLayout
    private lateinit var themeCenterBtn: LinearLayout
    private lateinit var miniMusicPlayer: LinearLayout
    private lateinit var kuwoMusicBtn: LinearLayout
    private lateinit var lightAppBtn: LinearLayout
    private lateinit var appStoreBtn: LinearLayout
    private lateinit var carServiceBtn: LinearLayout
    private lateinit var backToLauncherBtn: LinearLayout
    private lateinit var addAppBtn: LinearLayout
    
    // 底部功能栏
    private lateinit var homeButton: ImageView
    private lateinit var navButton: ImageView
    private lateinit var rotateButton: ImageView
    private lateinit var lockButton: ImageView
    private lateinit var compassButton: ImageView
    private lateinit var volumeButton: ImageView
    
    // 控制中心
    private lateinit var controlCenterLayout: View
    private lateinit var brightnessSlider: SeekBar
    private lateinit var nightModeSwitch: SwitchCompat
    private lateinit var closeControlCenter: ImageButton
    
    // 数据
    private val handler = Handler(Looper.getMainLooper())
    private var isControlCenterVisible = false
    private var isLandscape = false
    
    // 服务
    private lateinit var audioManager: AudioManager
    private lateinit var broadcastReceiver: BroadcastReceiver

    companion object {
        private const val REQUEST_BLUETOOTH = 1001
        private const val REQUEST_WRITE_SETTINGS = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 检测屏幕方向
        isLandscape = resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        
        setContentView(R.layout.activity_main)
        
        try {
            audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            
            // 初始化屏幕适配和布局管理器
            ScreenAdapter.init(this)
            LayoutModeManager.init(this)
            ThemeManager.init(this)
            
            // 应用当前布局模式
            applyLayoutMode()
            
            ThemeManager.applyTheme()
            hideSystemUI()
            initViews()
            initControlCenter()
            startTimeUpdate()
            registerReceivers()
            loadWallpaper()
            checkAndStartServices()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "启动错误: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * 检查并启动必要服务
     */
    private fun checkAndStartServices() {
        val prefs = getSharedPreferences("jiying_settings", MODE_PRIVATE)
        
        // 检查悬浮球设置
        if (prefs.getBoolean("floating_ball", false)) {
            try {
                val intent = Intent(this, com.jiying.launcher.service.FloatBallService::class.java)
                startService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 应用当前布局模式
     * 根据LayoutModeManager的配置动态调整界面元素
     */
    private fun applyLayoutMode() {
        val config = LayoutModeManager.getCurrentLayoutConfig()
        
        // 根据不同布局模式调整界面
        when (config.mode) {
            ScreenAdapter.LayoutMode.MODE_MINIMAL -> {
                // 极简模式：隐藏大部分卡片，只保留底部Dock
                navCard?.visibility = View.GONE
                musicCard?.visibility = View.GONE
            }
            ScreenAdapter.LayoutMode.MODE_MAP_FOCUS -> {
                // 地图优先：放大地图卡片
            }
            ScreenAdapter.LayoutMode.MODE_MUSIC_FOCUS -> {
                // 音乐优先：放大音乐卡片
            }
            ScreenAdapter.LayoutMode.MODE_CARPLAY -> {
                // CarPlay风格：大图标布局
            }
            else -> {
                // 默认模式和混合模式：标准布局
            }
        }
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun initViews() {
        rootLayout = findViewById(R.id.root_layout)
        wallpaperImageView = findViewById(R.id.wallpaper_background)
        
        // 顶部状态栏
        userAvatar = findViewById(R.id.user_avatar)
        volumeBtn = findViewById(R.id.volume_btn)
        notificationBtn = findViewById(R.id.notification_btn)
        dateTimeTextView = findViewById(R.id.date_time_text)
        
        // 导航卡片
        navCard = findViewById(R.id.nav_card)
        navClose = findViewById(R.id.nav_close)
        openMapBtn = findViewById(R.id.open_map_btn)
        
        // 音乐卡片
        musicCard = findViewById(R.id.music_card)
        musicClose = findViewById(R.id.music_close)
        musicTitle = findViewById(R.id.music_title)
        musicArtist = findViewById(R.id.music_artist)
        musicPlayPause = findViewById(R.id.music_play_pause)
        musicPrev = findViewById(R.id.music_prev)
        musicNext = findViewById(R.id.music_next)
        musicLock = findViewById(R.id.music_lock)
        
        // 横屏快捷功能
        fileManagerLand = findViewById(R.id.file_manager_land)
        videoPlayerLand = findViewById(R.id.video_player_land)
        pipModeLand = findViewById(R.id.pip_mode_land)
        
        // 底部应用栏
        myAppsBtn = findViewById(R.id.my_apps)
        systemSettingsBtn = findViewById(R.id.system_settings)
        themeCenterBtn = findViewById(R.id.theme_center)
        miniMusicPlayer = findViewById(R.id.mini_music_player)
        kuwoMusicBtn = findViewById(R.id.kuwo_music)
        lightAppBtn = findViewById(R.id.light_app)
        appStoreBtn = findViewById(R.id.app_store)
        carServiceBtn = findViewById(R.id.car_service)
        backToLauncherBtn = findViewById(R.id.back_to_launcher)
        addAppBtn = findViewById(R.id.add_app)
        
        // 底部功能栏
        homeButton = findViewById(R.id.home_button)
        navButton = findViewById(R.id.nav_button)
        rotateButton = findViewById(R.id.rotate_button)
        lockButton = findViewById(R.id.lock_button)
        compassButton = findViewById(R.id.compass_button)
        volumeButton = findViewById(R.id.volume_button)
        
        // 控制中心
        controlCenterLayout = findViewById(R.id.control_center_layout)
        brightnessSlider = findViewById(R.id.brightness_slider)
        nightModeSwitch = findViewById(R.id.night_mode_switch)
        closeControlCenter = findViewById(R.id.close_control_center)
        
        setupClickListeners()
    }

    private fun setupClickListeners() {
        // ========== 导航卡片 ==========
        navCard.setOnClickListener { openNavigationApp() }
        navClose.setOnClickListener { navCard.visibility = View.GONE }
        openMapBtn.setOnClickListener { openNavigationApp() }
        
        // ========== 音乐卡片 ==========
        musicCard.setOnClickListener { openMusicPlayer() }
        musicClose.setOnClickListener { musicCard.visibility = View.GONE }
        musicPlayPause.setOnClickListener { toggleMusicPlayback() }
        musicPrev.setOnClickListener { previousTrack() }
        musicNext.setOnClickListener { nextTrack() }
        musicLock.setOnClickListener { showMusicLockDialog() }
        
        // ========== 横屏快捷功能 ==========
        fileManagerLand?.setOnClickListener { openFileManager() }
        videoPlayerLand?.setOnClickListener { openVideoPlayer() }
        pipModeLand?.setOnClickListener { openPipMode() }
        
        // ========== 底部应用栏 ==========
        myAppsBtn.setOnClickListener { showAppsCenter() }
        systemSettingsBtn.setOnClickListener { openSystemSettings() }
        themeCenterBtn.setOnClickListener { showThemeCenter() }
        miniMusicPlayer.setOnClickListener { openMusicPlayer() }
        kuwoMusicBtn.setOnClickListener { showMusicAppsSelector() }
        lightAppBtn.setOnClickListener { showAppsCenter() }
        appStoreBtn.setOnClickListener { openAppStore() }
        carServiceBtn.setOnClickListener { openCarService() }
        backToLauncherBtn.setOnClickListener { backToSystemLauncher() }
        addAppBtn.setOnClickListener { showAddAppDialog() }
        
        // ========== 底部功能栏 ==========
        homeButton.setOnClickListener { 
            startActivity(Intent(this, MenuActivity::class.java))
        }
        homeButton.setOnLongClickListener {
            DeviceConfigActivity.start(this)
            true
        }
        
        navButton.setOnClickListener { openNavigationApp() }
        navButton.setOnLongClickListener {
            LayoutModeSelectorActivity.start(this)
            true
        }
        
        volumeButton.setOnClickListener { showControlCenter() }
        lockButton.setOnClickListener { toggleScreenLock() }
        rotateButton.setOnClickListener { toggleScreenRotation() }
        compassButton.setOnClickListener { openCompass() }
        
        // ========== 控制中心 ==========
        closeControlCenter?.setOnClickListener { hideControlCenter() }
        nightModeSwitch?.setOnCheckedChangeListener { _, isChecked -> toggleNightMode(isChecked) }
        
        // ========== 音量按钮 ==========
        volumeBtn.setOnClickListener { showControlCenter() }
        
        // ========== 通知按钮 ==========
        notificationBtn.setOnClickListener { openNotifications() }
    }
    
    // ========== 功能方法实现 ==========
    
    /**
     * 打开系统设置
     */
    private fun openSystemSettings() {
        try {
            startActivity(Intent(this, SystemSettingsActivity::class.java))
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开设置", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 打开文件管理器
     */
    private fun openFileManager() {
        try {
            startActivity(Intent(this, FileManagerActivity::class.java))
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开文件管理器", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 打开视频播放器
     */
    private fun openVideoPlayer() {
        try {
            startActivity(Intent(this, VideoPlayerActivity::class.java))
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开视频播放器", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 打开音乐播放器
     */
    private fun openMusicPlayer() {
        try {
            startActivity(Intent(this, MusicPlayerActivity::class.java))
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开音乐播放器", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 打开画中画模式
     */
    private fun openPipMode() {
        try {
            FloatingMapService.startService(this)
            Toast.makeText(this, "画中画导航已开启", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            // 如果悬浮地图不可用，尝试打开导航浮窗
            try {
                startActivity(Intent(this, NavigationFloatActivity::class.java))
            } catch (e2: Exception) {
                Toast.makeText(this, "无法开启画中画", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 显示音乐应用选择器
     * 识别并列出已安装的音乐应用
     */
    private fun showMusicAppsSelector() {
        val musicApps = listOf(
            "cn.kuwo.player" to "酷我音乐",
            "com.tencent.qqmusic" to "QQ音乐",
            "com.kugou.android" to "酷狗音乐",
            "com.netease.cloudmusic" to "网易云音乐",
            "com.xiami.music" to "虾米音乐",
            "com.sogou.music" to "搜狗音乐",
            "com.baidu.music" to "百度音乐",
            "com.lava.lava_music" to "lava音乐",
            "com.google.android.apps.youtube.music" to "YouTube Music",
            "com.spotify.music" to "Spotify",
            "com.amazon.mp3" to "Amazon Music"
        )
        
        val installedApps = mutableListOf<Pair<String, String>>()
        
        for ((pkg, name) in musicApps) {
            try {
                if (packageManager.getPackageInfo(pkg, 0) != null) {
                    installedApps.add(pkg to name)
                }
            } catch (e: Exception) {
                // 应用未安装
            }
        }
        
        if (installedApps.isEmpty()) {
            // 没有安装第三方音乐应用，打开本地音乐播放器
            openMusicPlayer()
            return
        }
        
        val appNames = installedApps.map { it.second }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("选择音乐应用")
            .setItems(appNames) { _, which ->
                val selectedPkg = installedApps[which].first
                try {
                    val intent = packageManager.getLaunchIntentForPackage(selectedPkg)
                    if (intent != null) {
                        startActivity(intent)
                    } else {
                        // 如果没有启动intent，打开本地播放器
                        openMusicPlayer()
                    }
                } catch (e: Exception) {
                    openMusicPlayer()
                }
            }
            .setPositiveButton("本地音乐") { _, _ ->
                openMusicPlayer()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 打开导航应用
     */
    private fun openNavigationApp() {
        val navApps = listOf(
            "com.autonavi.amap" to "高德地图",
            "com.baidu.BaiduMap" to "百度地图",
            "com.google.android.apps.maps" to "Google地图",
            "com.tencent.map" to "腾讯地图",
            "com.apple.Maps" to "Apple地图",
            "com.navinfo.navi" to "凯立德导航",
            "com.autonavi.xps" to "高德导航"
        )
        
        for ((pkg, name) in navApps) {
            try {
                val intent = packageManager.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    startActivity(intent)
                    return
                }
            } catch (e: Exception) {
                // 继续尝试下一个
            }
        }
        
        // 如果没有安装导航应用，尝试打开画中画
        openPipMode()
        Toast.makeText(this, "未检测到导航应用，已开启画中画模式", Toast.LENGTH_LONG).show()
    }
    
    /**
     * 打开应用中心
     */
    private fun showAppsCenter() {
        try {
            startActivity(Intent(this, AppsCenterActivity::class.java))
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开应用中心", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 显示主题中心
     */
    private fun showThemeCenter() {
        AlertDialog.Builder(this)
            .setTitle("选择主题")
            .setItems(arrayOf("布丁UI风格", "氢桌面风格")) { dialog, which ->
                if (which == 1) {
                    nightModeSwitch?.isChecked = true
                    toggleNightMode(true)
                    Toast.makeText(this, "已切换为氢桌面风格", Toast.LENGTH_SHORT).show()
                } else {
                    nightModeSwitch?.isChecked = false
                    toggleNightMode(false)
                    Toast.makeText(this, "已切换为布丁UI风格", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }
    
    /**
     * 打开应用商店
     */
    private fun openAppStore() {
        val stores = listOf(
            "com.xiaomi.market" to "小米商店",
            "com.tencent.android.qqdownloader" to "应用宝",
            "com.coolapk.market" to "酷安",
            "com.baidu.appsearch" to "百度手机助手",
            "com.huawei.appmarket" to "华为应用市场",
            "com.oppo.market" to "OPPO软件商店",
            "com.vivo.appstore" to "vivo应用商店"
        )
        
        for ((pkg, name) in stores) {
            try {
                val intent = packageManager.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    startActivity(intent)
                    return
                }
            } catch (e: Exception) {
                // 继续尝试
            }
        }
        
        // 如果都没有，打开设置
        try {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        } catch (e: Exception) {
            Toast.makeText(this, "未找到应用商店", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 打开车服务
     */
    private fun openCarService() {
        AlertDialog.Builder(this)
            .setTitle("车服务")
            .setItems(arrayOf("车辆设置", "驾驶模式", "车辆信息")) { _, which ->
                when (which) {
                    0 -> startActivity(Intent(Settings.ACTION_SETTINGS))
                    1 -> {
                        try {
                            startActivity(Intent(this, com.jiying.launcher.ui.driving.DrivingModeActivity::class.java))
                        } catch (e: Exception) {
                            Toast.makeText(this, "无法打开驾驶模式", Toast.LENGTH_SHORT).show()
                        }
                    }
                    2 -> showCarInfo()
                }
            }
            .show()
    }
    
    /**
     * 显示车辆信息
     */
    private fun showCarInfo() {
        val deviceName = Build.MODEL
        val manufacturer = Build.MANUFACTURER
        val androidVersion = Build.VERSION.RELEASE
        
        AlertDialog.Builder(this)
            .setTitle("车辆信息")
            .setMessage("""
                设备型号：$deviceName
                制造商：$manufacturer
                Android版本：$androidVersion
                桌面版本：2.0.0
            """.trimIndent())
            .setPositiveButton("确定", null)
            .show()
    }
    
    /**
     * 返回系统桌面
     */
    private fun backToSystemLauncher() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }
    
    /**
     * 打开通知面板
     */
    private fun openNotifications() {
        try {
            val intent = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            intent.putExtra("android.intent.extra.ACTION", "android.intent.action.CLOSE_SYSTEM_DIALOGS")
            
            // 发送展开通知面板的广播
            val expandIntent = Intent()
            expandIntent.action = "android.intent.action.EXPAND_STATUS_BAR"
            sendBroadcast(expandIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开通知面板", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 打开指南针
     */
    private fun openCompass() {
        Toast.makeText(this, "指南针功能", Toast.LENGTH_SHORT).show()
    }
    
    // ========== 音乐控制方法 ==========
    
    private fun toggleMusicPlayback() {
        Toast.makeText(this, "音乐控制", Toast.LENGTH_SHORT).show()
    }
    
    private fun previousTrack() {
        Toast.makeText(this, "上一首", Toast.LENGTH_SHORT).show()
    }
    
    private fun nextTrack() {
        Toast.makeText(this, "下一首", Toast.LENGTH_SHORT).show()
    }
    
    private fun showMusicLockDialog() {
        AlertDialog.Builder(this)
            .setTitle("音乐锁定")
            .setMessage("是否锁定音乐播放控件？锁定后音乐卡片将不可滑动。")
            .setPositiveButton("锁定") { _, _ ->
                Toast.makeText(this, "音乐已锁定", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    // ========== 控制中心方法 ==========
    
    private fun initControlCenter() {
        brightnessSlider?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) setScreenBrightness(progress.toFloat() / 255f)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        controlCenterLayout?.visibility = View.GONE
    }

    private fun showControlCenter() {
        try {
            val brightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            brightnessSlider?.progress = brightness
        } catch (e: Settings.SettingNotFoundException) {
            brightnessSlider?.progress = 128
        }
        controlCenterLayout?.visibility = View.VISIBLE
        isControlCenterVisible = true
    }

    private fun hideControlCenter() {
        controlCenterLayout?.visibility = View.GONE
        isControlCenterVisible = false
    }

    private fun setScreenBrightness(brightness: Float) {
        try {
            val layoutParams = window.attributes
            layoutParams.screenBrightness = brightness
            window.attributes = layoutParams
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // ========== 屏幕控制方法 ==========

    private fun toggleScreenRotation() {
        try {
            if (Settings.System.canWrite(this)) {
                val rotation = Settings.System.getInt(
                    contentResolver,
                    Settings.System.ACCELEROMETER_ROTATION, 0
                )
                if (rotation == 0) {
                    Settings.System.putInt(
                        contentResolver,
                        Settings.System.ACCELEROMETER_ROTATION,
                        1
                    )
                    Toast.makeText(this, "已开启自动旋转", Toast.LENGTH_SHORT).show()
                } else {
                    Settings.System.putInt(
                        contentResolver,
                        Settings.System.ACCELEROMETER_ROTATION,
                        0
                    )
                    Toast.makeText(this, "已关闭自动旋转", Toast.LENGTH_SHORT).show()
                }
            } else {
                AlertDialog.Builder(this)
                    .setTitle("需要权限")
                    .setMessage("需要修改系统设置权限才能更改旋转设置")
                    .setPositiveButton("去授权") { _, _ ->
                        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                        intent.data = Uri.parse("package:$packageName")
                        startActivityForResult(intent, REQUEST_WRITE_SETTINGS)
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "无法切换旋转设置", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleScreenLock() {
        AlertDialog.Builder(this)
            .setTitle("屏幕锁定")
            .setMessage("是否锁定屏幕？锁定后将需要解锁操作。")
            .setPositiveButton("锁定") { _, _ ->
                try {
                    val km = getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        km.requestDismissKeyguard(this, object : android.app.KeyguardManager.KeyguardDismissCallback() {
                            override fun onDismissError() {
                                Toast.makeText(this@MainActivity, "解锁失败", Toast.LENGTH_SHORT).show()
                            }
                            override fun onDismissSucceeded() {
                                Toast.makeText(this@MainActivity, "屏幕已锁定", Toast.LENGTH_SHORT).show()
                            }
                        })
                    } else {
                        @Suppress("DEPRECATION")
                        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
                        Toast.makeText(this, "屏幕已锁定", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "锁定功能暂不可用", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun toggleNightMode(enable: Boolean) {
        if (enable) {
            ThemeManager.setTheme(ThemeManager.THEME_HYDROGEN)
            rootLayout.setBackgroundColor(Color.parseColor("#0D1117"))
        } else {
            ThemeManager.setTheme(ThemeManager.THEME_PUDDING)
            rootLayout.setBackgroundColor(Color.parseColor("#1A1D21"))
        }
    }
    
    // ========== 添加应用相关方法 ==========

    private fun showAddAppDialog() {
        try {
            val pm = packageManager
            val apps = pm.getInstalledPackages(PackageManager.GET_ACTIVITIES)
                .filter { 
                    try {
                        val appInfo = it.applicationInfo
                        appInfo?.enabled == true && appInfo.loadLabel(pm).isNotBlank()
                    } catch (e: Exception) { false }
                }
                .sortedBy { it.applicationInfo.loadLabel(pm).toString() }

            val appNames = apps.map { it.applicationInfo.loadLabel(pm).toString() }.toTypedArray()

            AlertDialog.Builder(this)
                .setTitle("选择要添加的应用")
                .setItems(appNames) { _, which ->
                    val selectedApp = apps[which]
                    addAppToHome(selectedApp.packageName, selectedApp.applicationInfo.loadLabel(pm).toString())
                }
                .setNegativeButton("取消", null)
                .show()
        } catch (e: Exception) {
            Toast.makeText(this, "无法获取应用列表", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addAppToHome(packageName: String, appName: String) {
        val prefs = getSharedPreferences("jiying_home_apps", MODE_PRIVATE)
        val appList = prefs.getStringSet("apps", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        appList.add("$packageName|$appName")
        prefs.edit().putStringSet("apps", appList).apply()
        Toast.makeText(this, "已添加: $appName", Toast.LENGTH_SHORT).show()
    }

    private fun loadWallpaper() {
        wallpaperImageView.setImageResource(R.drawable.bg_wallpaper_default)
    }
    
    // ========== 时间更新 ==========
    
    private fun startTimeUpdate() {
        updateDateTime()
        handler.postDelayed(object : Runnable {
            override fun run() {
                updateDateTime()
                handler.postDelayed(this, 1000)
            }
        }, 1000)
    }
    
    private fun updateDateTime() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val weekDay = arrayOf("星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六")[calendar.get(Calendar.DAY_OF_WEEK) - 1]
        
        val timeStr = if (isLandscape) {
            String.format("%02d:%02d", hour, minute)
        } else {
            String.format("%d月%d日 %s %02d:%02d", month, day, weekDay, hour, minute)
        }
        
        dateTimeTextView.text = timeStr
    }
    
    // ========== 广播接收 ==========
    
    private fun registerReceivers() {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_CONFIGURATION_CHANGED -> {
                        // 屏幕方向改变
                        isLandscape = resources.configuration.orientation == 
                            android.content.res.Configuration.ORIENTATION_LANDSCAPE
                        recreate()
                    }
                }
            }
        }
        
        val filter = IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED)
        registerReceiver(broadcastReceiver, filter)
    }
    
    // ========== 生命周期 ==========
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_WRITE_SETTINGS) {
            // 权限授予后重新加载设置
        }
    }
    
    override fun onResume() {
        super.onResume()
        hideSystemUI()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        try {
            unregisterReceiver(broadcastReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    override fun onBackPressed() {
        // 不响应返回键，保持桌面常驻
    }
}
