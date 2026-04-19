package com.jiying.launcher.ui.main

import android.bluetooth.BluetoothAdapter
import android.content.*
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.*
import android.provider.Settings
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jiying.launcher.R
import com.jiying.launcher.data.model.AppInfo
import com.jiying.launcher.ui.apps.AppsCenterActivity
import com.jiying.launcher.ui.assistant.AssistantActivity
import com.jiying.launcher.ui.service.FileManagerActivity
import com.jiying.launcher.ui.wallpaper.WallpaperPreviewActivity
import com.jiying.launcher.util.ThemeManager
import kotlinx.coroutines.*
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
    private lateinit var appGridRecyclerView: RecyclerView
    private lateinit var dateTimeTextView: TextView
    private lateinit var dateTextView: TextView
    
    // 快捷插件区域
    private lateinit var quickPanelCard: CardView
    private lateinit var navigationCard: CardView
    private lateinit var musicCard: CardView
    private lateinit var navAppName: TextView
    private lateinit var musicTitle: TextView
    private lateinit var musicArtist: TextView
    private lateinit var musicPlayPause: ImageButton
    private lateinit var musicPrev: ImageButton
    private lateinit var musicNext: ImageButton
    
    // 左上角智能助理
    private lateinit var assistantButton: ImageButton
    
    // 底部导航栏
    private lateinit var bottomNavigation: LinearLayout
    private lateinit var homeButton: ImageButton
    private lateinit var wallpaperButton: ImageButton
    private lateinit var appsButton: ImageButton
    private lateinit var serviceButton: ImageButton
    private lateinit var settingsButton: ImageButton
    private lateinit var themeSwitchButton: ImageButton
    
    // 控制中心
    private lateinit var controlCenterLayout: RelativeLayout
    private lateinit var brightnessSlider: SeekBar
    private lateinit var volumeSlider: SeekBar
    private lateinit var wifiSwitch: Switch
    private lateinit var bluetoothSwitch: Switch
    private lateinit var nightModeSwitch: Switch
    private lateinit var closeControlCenter: ImageButton
    
    // 数据
    private lateinit var appAdapter: AppGridAdapter
    private val installedApps = mutableListOf<AppInfo>()
    private val handler = Handler(Looper.getMainLooper())
    private var isControlCenterVisible = false
    
    // 服务
    private lateinit var audioManager: AudioManager
    private lateinit var broadcastReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.applyTheme()
        hideSystemUI()
        setContentView(R.layout.activity_main)
        initViews()
        initControlCenter()
        startTimeUpdate()
        registerReceivers()
        loadInstalledApps()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        loadWallpaper()
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
        quickPanelCard = findViewById(R.id.quick_panel_card)
        navigationCard = findViewById(R.id.navigation_card)
        musicCard = findViewById(R.id.music_card)
        navAppName = findViewById(R.id.nav_app_name)
        musicTitle = findViewById(R.id.music_title)
        musicArtist = findViewById(R.id.music_artist)
        musicPlayPause = findViewById(R.id.music_play_pause)
        musicPrev = findViewById(R.id.music_prev)
        musicNext = findViewById(R.id.music_next)
        assistantButton = findViewById(R.id.assistant_button)
        bottomNavigation = findViewById(R.id.bottom_navigation)
        homeButton = findViewById(R.id.home_button)
        wallpaperButton = findViewById(R.id.wallpaper_button)
        appsButton = findViewById(R.id.apps_button)
        serviceButton = findViewById(R.id.service_button)
        settingsButton = findViewById(R.id.settings_button)
        themeSwitchButton = findViewById(R.id.theme_switch_button)
        appGridRecyclerView = findViewById(R.id.app_grid_recycler_view)
        dateTimeTextView = findViewById(R.id.date_time_text)
        dateTextView = findViewById(R.id.date_text)
        controlCenterLayout = findViewById(R.id.control_center_layout)
        brightnessSlider = findViewById(R.id.brightness_slider)
        volumeSlider = findViewById(R.id.volume_slider)
        wifiSwitch = findViewById(R.id.wifi_switch)
        bluetoothSwitch = findViewById(R.id.bluetooth_switch)
        nightModeSwitch = findViewById(R.id.night_mode_switch)
        closeControlCenter = findViewById(R.id.close_control_center)
        
        setupClickListeners()
        applyThemeStyle()
    }

    private fun applyThemeStyle() {
        when (ThemeManager.getCurrentTheme()) {
            ThemeManager.THEME_BUDING -> {
                val style = ThemeManager.getBudingStyle()
                quickPanelCard.cardElevation = style.cardElevation
                navigationCard.cardElevation = style.cardElevation
                musicCard.cardElevation = style.cardElevation
            }
            ThemeManager.THEME_HYDROGEN -> {
                val style = ThemeManager.getHydrogenStyle()
                quickPanelCard.cardElevation = style.cardElevation
                navigationCard.cardElevation = style.cardElevation
                musicCard.cardElevation = style.cardElevation
            }
        }
    }

    private fun setupClickListeners() {
        assistantButton.setOnClickListener { startActivity(Intent(this, AssistantActivity::class.java)) }
        navigationCard.setOnClickListener { openNavigationApp() }
        musicPlayPause.setOnClickListener { toggleMusicPlayback() }
        musicPrev.setOnClickListener { previousTrack() }
        musicNext.setOnClickListener { nextTrack() }
        homeButton.setOnClickListener { showHomePage() }
        wallpaperButton.setOnClickListener { showWallpaperCenter() }
        appsButton.setOnClickListener { showAppsCenter() }
        serviceButton.setOnClickListener { showServiceCenter() }
        settingsButton.setOnClickListener { showControlCenter() }
        themeSwitchButton.setOnClickListener { switchTheme() }
        closeControlCenter.setOnClickListener { hideControlCenter() }
        dateTimeTextView.setOnClickListener { toggleControlCenter() }
        dateTextView.setOnClickListener { toggleControlCenter() }
        quickPanelCard.setOnClickListener { toggleQuickPanel() }
    }

    private fun loadWallpaper() {
        val prefs = getSharedPreferences("jiying_settings", MODE_PRIVATE)
        val wallpaperPath = prefs.getString("current_wallpaper", null)
        if (wallpaperPath != null) {
            try {
                wallpaperImageView.setImageURI(Uri.parse(wallpaperPath))
            } catch (e: Exception) {
                wallpaperImageView.setBackgroundResource(R.drawable.default_wallpaper)
            }
        } else {
            wallpaperImageView.setBackgroundResource(R.drawable.default_wallpaper)
        }
    }

    private fun initAppList() {
        appAdapter = AppGridAdapter(installedApps) { appInfo -> launchApp(appInfo) }
        appGridRecyclerView.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 6)
            adapter = appAdapter
            setHasFixedSize(true)
        }
    }

    private fun loadInstalledApps() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val pm = packageManager
                val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
                val apps = pm.queryIntentActivities(intent, 0)
                installedApps.clear()
                for (resolveInfo in apps) {
                    installedApps.add(AppInfo(
                        name = resolveInfo.loadLabel(pm).toString(),
                        packageName = resolveInfo.activityInfo.packageName,
                        icon = resolveInfo.loadIcon(pm),
                        isSystemApp = false
                    ))
                }
                installedApps.sortBy { it.name }
                withContext(Dispatchers.Main) {
                    initAppList()
                    appAdapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun launchApp(appInfo: AppInfo) {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
            if (launchIntent != null) startActivity(launchIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法启动应用", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initControlCenter() {
        brightnessSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) setScreenBrightness(progress.toFloat() / 255f)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        volumeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) setVolume(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        wifiSwitch.setOnCheckedChangeListener { _, isChecked -> toggleWifi(isChecked) }
        bluetoothSwitch.setOnCheckedChangeListener { _, isChecked -> toggleBluetooth(isChecked) }
        nightModeSwitch.setOnCheckedChangeListener { _, isChecked -> toggleNightMode(isChecked) }
        updateControlCenterState()
        controlCenterLayout.visibility = View.GONE
    }

    private fun updateControlCenterState() {
        try {
            val brightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            brightnessSlider.progress = brightness
        } catch (e: Settings.SettingNotFoundException) {
            brightnessSlider.progress = 128
        }
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        volumeSlider.max = maxVolume
        volumeSlider.progress = currentVolume
        wifiSwitch.isChecked = isWifiEnabled()
        bluetoothSwitch.isChecked = isBluetoothEnabled()
        nightModeSwitch.isChecked = ThemeManager.getCurrentTheme() == ThemeManager.THEME_HYDROGEN
    }

    private fun toggleControlCenter() {
        if (isControlCenterVisible) hideControlCenter() else showControlCenter()
    }

    private fun showControlCenter() {
        updateControlCenterState()
        controlCenterLayout.visibility = View.VISIBLE
        controlCenterLayout.alpha = 0f
        controlCenterLayout.animate().alpha(1f).setDuration(300).setInterpolator(AccelerateDecelerateInterpolator()).start()
        isControlCenterVisible = true
    }

    private fun hideControlCenter() {
        controlCenterLayout.animate().alpha(0f).setDuration(300).setInterpolator(AccelerateDecelerateInterpolator())
            .setListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    controlCenterLayout.visibility = View.GONE
                }
            }).start()
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
            Toast.makeText(this, "已切换到氢桌面风格", Toast.LENGTH_SHORT).show()
        } else {
            ThemeManager.setTheme(ThemeManager.THEME_BUDING)
            Toast.makeText(this, "已切换到布丁UI风格", Toast.LENGTH_SHORT).show()
        }
        recreate()
    }

    private fun switchTheme() {
        ThemeManager.toggleTheme()
        Toast.makeText(this, if (ThemeManager.getCurrentTheme() == ThemeManager.THEME_HYDROGEN) "已切换到氢桌面风格" else "已切换到布丁UI风格", Toast.LENGTH_SHORT).show()
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
        val hourFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("yyyy年MM月dd日 E", Locale.getDefault())
        dateTimeTextView.text = hourFormat.format(calendar.time)
        dateTextView.text = dateFormat.format(calendar.time)
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
        appGridRecyclerView.visibility = View.VISIBLE
        quickPanelCard.visibility = View.VISIBLE
        assistantButton.visibility = View.VISIBLE
    }

    private fun showWallpaperCenter() = startActivity(Intent(this, WallpaperPreviewActivity::class.java))
    private fun showAppsCenter() = startActivity(Intent(this, AppsCenterActivity::class.java))

    private fun showServiceCenter() {
        val items = arrayOf("文件管理", "应用备份", "远程传输", "垃圾清理", "流量监控")
        AlertDialog.Builder(this).setTitle("车主服务")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> startActivity(Intent(this, FileManagerActivity::class.java))
                    else -> Toast.makeText(this, "功能开发中", Toast.LENGTH_SHORT).show()
                }
            }.setNegativeButton("取消", null).show()
    }

    private fun toggleQuickPanel() {
        quickPanelCard.visibility = if (quickPanelCard.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
        loadInstalledApps()
        updateControlCenterState()
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
}

/**
 * 应用网格适配器
 */
class AppGridAdapter(
    private val apps: List<AppInfo>,
    private val onAppClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppGridAdapter.AppViewHolder>() {

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iconImageView: ImageView = itemView.findViewById(R.id.app_icon)
        val nameTextView: TextView = itemView.findViewById(R.id.app_name)
        val cardView: CardView = itemView.findViewById(R.id.app_card)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app_grid, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        holder.iconImageView.setImageDrawable(app.icon)
        holder.nameTextView.text = app.name
        holder.cardView.setOnClickListener { onAppClick(app) }
    }

    override fun getItemCount(): Int = apps.size
}
