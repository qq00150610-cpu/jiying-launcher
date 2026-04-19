package com.jiying.launcher.ui.main

import android.bluetooth.BluetoothAdapter
import android.content.*
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.*
import android.graphics.Color
import android.provider.Settings
import android.content.pm.PackageManager
import androidx.appcompat.app.AlertDialog
import android.view.inputmethod.InputMethodManager
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import com.jiying.launcher.R
import com.jiying.launcher.ui.main.AppManagerActivity
import com.jiying.launcher.ui.main.MenuActivity
import com.jiying.launcher.ui.main.SystemSettingsActivity
import com.jiying.launcher.util.ThemeManager
import java.text.SimpleDateFormat
import java.util.*

/**
 * 极影桌面 - 主界面
 * 
 * 复刻布丁UI风格的车机桌面，支持两种主题切换：
 * - 布丁UI风格（默认）
 * - 氢桌面风格（简洁卡片化）
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
    private lateinit var controlCenterLayout: LinearLayout
    private lateinit var brightnessSlider: SeekBar
    private lateinit var nightModeSwitch: SwitchCompat
    private lateinit var closeControlCenter: ImageButton
    
    // 数据
    private val handler = Handler(Looper.getMainLooper())
    private var isControlCenterVisible = false
    
    // 服务
    private lateinit var audioManager: AudioManager
    private lateinit var broadcastReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        try {
            audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            ThemeManager.applyTheme()
            hideSystemUI()
            initViews()
            initControlCenter()
            startTimeUpdate()
            registerReceivers()
            loadWallpaper()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "启动错误: ${e.message}", Toast.LENGTH_LONG).show()
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
        // 导航卡片
        navCard.setOnClickListener { openNavigationApp() }
        navClose.setOnClickListener { navCard.visibility = View.GONE }
        openMapBtn.setOnClickListener { openNavigationApp() }
        
        // 音乐卡片
        musicCard.setOnClickListener { openMusicApp() }
        musicClose.setOnClickListener { musicCard.visibility = View.GONE }
        musicPlayPause.setOnClickListener { toggleMusicPlayback() }
        musicPrev.setOnClickListener { previousTrack() }
        musicNext.setOnClickListener { nextTrack() }
        
        // 底部应用栏
        myAppsBtn.setOnClickListener { showAppsCenter() }
        systemSettingsBtn.setOnClickListener { 
            startActivity(Intent(this, SystemSettingsActivity::class.java))
        }
        themeCenterBtn.setOnClickListener { showThemeCenter() }
        miniMusicPlayer.setOnClickListener { openMusicApp() }
        kuwoMusicBtn.setOnClickListener { openMusicApp() }
        lightAppBtn.setOnClickListener { showAppsCenter() }
        appStoreBtn.setOnClickListener { openAppStore() }
        carServiceBtn.setOnClickListener { openCarService() }
        backToLauncherBtn.setOnClickListener { backToSystemLauncher() }
        addAppBtn.setOnClickListener { showAddAppDialog() }
        
        // 底部功能栏
        homeButton.setOnClickListener { 
            startActivity(Intent(this, MenuActivity::class.java))
        }
        navButton.setOnClickListener { openNavigationApp() }
        volumeButton.setOnClickListener { showControlCenter() }
        lockButton.setOnClickListener { toggleScreenLock() }
        rotateButton.setOnClickListener { toggleScreenRotation() }
        compassButton.setOnClickListener { openCompass() }
        
        // 添加应用按钮
        addAppBtn.setOnClickListener { showAddAppDialog() }
        
        // 系统设置按钮 -> SystemSettingsActivity
        systemSettingsBtn.setOnClickListener { 
            startActivity(Intent(this, SystemSettingsActivity::class.java))
        }
        
        // 主页按钮 -> MenuActivity
        homeButton.setOnClickListener { 
            startActivity(Intent(this, MenuActivity::class.java))
        }
        
        // 控制中心
        closeControlCenter.setOnClickListener { hideControlCenter() }
        nightModeSwitch.setOnCheckedChangeListener { _, isChecked -> toggleNightMode(isChecked) }
    }

    private fun openSettings() {
        try {
            val intent = Intent(Settings.ACTION_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开设置", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openAppStore() {
        val stores = listOf(
            "com.xiaomi.market",      // 小米商店
            "com.tencent.android.qqdownloader", // 应用宝
            "com.coolapk.market",     // 酷安
            "com.baidu.appsearch"     // 百度手机助手
        )
        for (pkg in stores) {
            try {
                val intent = packageManager.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    startActivity(intent)
                    return
                }
            } catch (e: Exception) { /* 继续尝试下一个 */ }
        }
        // 如果都没有，打开设置
        startActivity(Intent(Settings.ACTION_SETTINGS))
    }

    private fun openCarService() {
        try {
            val intent = Intent(Settings.ACTION_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开", Toast.LENGTH_SHORT).show()
        }
    }

    private fun backToSystemLauncher() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    private fun openCompass() {
        Toast.makeText(this, "指南针", Toast.LENGTH_SHORT).show()
    }

    // 打开语音助手
    private fun openVoiceAssistant() {
        val voiceApps = listOf(
            "com.baidu.duer.dcs",           // 小度
            "com.iflytek.aiges",           // 讯飞语音
            "com.sogou.map.android.voice", // 搜狗语音
            "com.alibaba.ailabs.genie",    // 天猫精灵
            "com.xiaomi.smarthome",         // 米家语音
            "com.google.android.googlequicksearchbox", // Google搜索
            "com.speech reco",              // 通用语音识别
            "android.assist.vi"             // Android语音助手
        )
        for (pkg in voiceApps) {
            try {
                val intent = packageManager.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    startActivity(intent)
                    return
                }
            } catch (e: Exception) { /* 继续尝试 */ }
        }
        // 如果都没有，尝试打开Google语音搜索
        try {
            val intent = Intent(Intent.ACTION_ASSIST)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "未找到语音助手", Toast.LENGTH_SHORT).show()
        }
    }

    // 打开位置设置
    private fun openLocationSettings() {
        try {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            // 如果位置设置不可用，打开一般设置
            try {
                val intent = Intent(Settings.ACTION_SETTINGS)
                startActivity(intent)
            } catch (e2: Exception) {
                Toast.makeText(this, "无法打开设置", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 屏幕旋转控制
    private fun toggleScreenRotation() {
        try {
            val rotation = window.attributes.preferredDisplayModeId
            if (rotation == 0) {
                // 启用自动旋转
                Settings.System.putInt(
                    contentResolver,
                    Settings.System.ACCELEROMETER_ROTATION,
                    1
                )
                Toast.makeText(this, "已开启自动旋转", Toast.LENGTH_SHORT).show()
            } else {
                // 禁用自动旋转
                Settings.System.putInt(
                    contentResolver,
                    Settings.System.ACCELEROMETER_ROTATION,
                    0
                )
                Toast.makeText(this, "已关闭自动旋转", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "无法切换旋转设置", Toast.LENGTH_SHORT).show()
        }
    }

    // 锁定屏幕
    private fun toggleScreenLock() {
        AlertDialog.Builder(this)
            .setTitle("屏幕锁定")
            .setMessage("是否锁定屏幕？锁定后将需要解锁操作。")
            .setPositiveButton("锁定") { _, _ ->
                // 发送广播或使用KeyguardManager锁定
                try {
                    val km = getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        km.requestDismissKeyguard(this, object : android.app.KeyguardManager.KeyguardDismissCallback() {
                            override fun onDismissError() {
                                Toast.makeText(this@MainActivity, "解锁失败", Toast.LENGTH_SHORT).show()
                            }
                            override fun onDismissSucceeded() {
                                // 锁定成功后可以添加全屏遮罩
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

    // 显示添加应用对话框
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

    // 添加应用到首页
    private fun addAppToHome(packageName: String, appName: String) {
        val prefs = getSharedPreferences("jiying_home_apps", MODE_PRIVATE)
        val appList = prefs.getStringSet("apps", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        appList.add("$packageName|$appName")
        prefs.edit().putStringSet("apps", appList).apply()
        Toast.makeText(this, "已添加: $appName", Toast.LENGTH_SHORT).show()
    }

    private fun showThemeCenter() {
        AlertDialog.Builder(this)
            .setTitle("选择主题")
            .setItems(arrayOf("布丁UI风格", "氢桌面风格")) { dialog, which ->
                if (which == 1) {
                    nightModeSwitch.isChecked = true
                    toggleNightMode(true)
                } else {
                    nightModeSwitch.isChecked = false
                    toggleNightMode(false)
                }
            }
            .show()
    }

    private fun openMusicApp() {
        try {
            val intent = packageManager.getLaunchIntentForPackage("cn.kuwo.player")
            if (intent != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "未安装酷我音乐", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开音乐应用", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadWallpaper() {
        // 使用默认壁纸
        wallpaperImageView.setImageResource(R.drawable.bg_wallpaper_default)
    }

    private fun initControlCenter() {
        brightnessSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) setScreenBrightness(progress.toFloat() / 255f)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        controlCenterLayout.visibility = View.GONE
    }

    private fun showControlCenter() {
        try {
            val brightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            brightnessSlider.progress = brightness
        } catch (e: Settings.SettingNotFoundException) {
            brightnessSlider.progress = 128
        }
        controlCenterLayout.visibility = View.VISIBLE
        isControlCenterVisible = true
    }

    private fun hideControlCenter() {
        controlCenterLayout.visibility = View.GONE
        isControlCenterVisible = false
    }

    private fun setScreenBrightness(brightness: Float) {
        try {
            val layoutParams = window.attributes
            layoutParams.screenBrightness = brightness
            window.attributes = layoutParams
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun setVolume(volume: Int) {
        try {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun toggleWifi(enable: Boolean) {
        try {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiManager.isWifiEnabled = enable
        } catch (e: Exception) {
            Toast.makeText(this, "无法切换WiFi", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isWifiEnabled(): Boolean {
        return try {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiManager.isWifiEnabled
        } catch (e: Exception) { false }
    }

    private fun toggleBluetooth(enable: Boolean) {
        try {
            if (enable) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_BLUETOOTH)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "无法切换蓝牙", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isBluetoothEnabled(): Boolean {
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            adapter?.isEnabled == true
        } catch (e: Exception) { false }
    }

    private fun toggleNightMode(enable: Boolean) {
        if (enable) {
            ThemeManager.setTheme(ThemeManager.THEME_HYDROGEN)
            // 直接修改背景色，不重建Activity
            rootLayout.setBackgroundColor(Color.parseColor("#0D1117"))
            navCard.setCardBackgroundColor(Color.parseColor("#161B22"))
            musicCard.setCardBackgroundColor(Color.parseColor("#161B22"))
        } else {
            ThemeManager.setTheme(ThemeManager.THEME_BUDING)
            rootLayout.setBackgroundColor(Color.parseColor("#0A0E14"))
            navCard.setCardBackgroundColor(Color.parseColor("#1E2832"))
            musicCard.setCardBackgroundColor(Color.parseColor("#1E2832"))
        }
        // 移除 recreate() 调用，避免闪屏
    }

    private fun switchTheme() {
        ThemeManager.toggleTheme()
        recreate()
    }

    private fun startTimeUpdate() {
        val runnable = object : Runnable {
            override fun run() {
                updateTimeDisplay()
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(runnable)
    }

    private fun updateTimeDisplay() {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("M月dd日 E HH:mm", Locale.getDefault())
        dateTimeTextView.text = dateFormat.format(calendar.time)
    }

    private fun registerReceivers() {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_TIME_TICK -> updateTimeDisplay()
                    "com.jiying.launcher.MUSIC_UPDATE" -> updateMusicInfo()
                    "android.net.conn.CONNECTIVITY_CHANGE" -> {}
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction("com.jiying.launcher.MUSIC_UPDATE")
            addAction("android.net.conn.CONNECTIVITY_CHANGE")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(broadcastReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(broadcastReceiver, filter)
        }
    }

    private fun updateMusicInfo() {
        val prefs = getSharedPreferences("jiying_music", MODE_PRIVATE)
        musicTitle.text = prefs.getString("title", "未知歌曲")
        musicArtist.text = prefs.getString("artist", "未知艺术家")
        musicPlayPause.setImageResource(if (prefs.getBoolean("isPlaying", false)) R.drawable.ic_pause else R.drawable.ic_play)
    }

    private fun openNavigationApp() {
        val prefs = getSharedPreferences("jiying_settings", MODE_PRIVATE)
        val navPackage = prefs.getString("navigation_app", "com.autonavi.amapauto")
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(navPackage!!)
            if (launchIntent != null) startActivity(launchIntent)
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW)
            webIntent.data = Uri.parse("https://uri.amap.com/navigation")
            startActivity(webIntent)
        }
    }

    private fun toggleMusicPlayback() {
        val intent = Intent("com.jiying.launcher.MUSIC_TOGGLE")
        sendBroadcast(intent)
        val prefs = getSharedPreferences("jiying_music", MODE_PRIVATE)
        val isPlaying = !prefs.getBoolean("isPlaying", false)
        prefs.edit().putBoolean("isPlaying", isPlaying).apply()
        updateMusicInfo()
    }

    private fun previousTrack() = sendBroadcast(Intent("com.jiying.launcher.MUSIC_PREV"))
    private fun nextTrack() = sendBroadcast(Intent("com.jiying.launcher.MUSIC_NEXT"))

    private fun showHomePage() {
        // 返回主界面
    }

    private fun showAppsCenter() {
        val intent = Intent(this, AppManagerActivity::class.java)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        try { unregisterReceiver(broadcastReceiver) } catch (e: Exception) { e.printStackTrace() }
    }

    override fun onBackPressed() {
        if (isControlCenterVisible) hideControlCenter()
        else {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_HOME)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }

    companion object {
        private const val REQUEST_BLUETOOTH = 1001
    }
    
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // 处理其他onActivityResult回调
    }
}
