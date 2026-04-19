package com.jiying.launcher.service

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.*
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.cardview.widget.CardView
import com.jiying.launcher.R
import com.jiying.launcher.util.ScreenAdapter

/**
 * 极影桌面 - 悬浮地图服务（画中画导航）
 * 
 * 功能说明：
 * - 支持高德地图、百度地图悬浮显示
 * - 提供画中画模式（Picture-in-Picture）
 * - 可拖拽移动位置
 * - 支持折叠/展开切换
 * - 兼容Android 6.0 (API 23) 及以上系统
 * 
 * @author 极影桌面开发团队
 * @version 2.0.0
 */
class FloatingMapService : Service() {
    
    // 服务唯一标识
    companion object {
        const val ACTION_START = "com.jiying.launcher.action.START_FLOAT_MAP"
        const val ACTION_STOP = "com.jiying.launcher.action.STOP_FLOAT_MAP"
        const val ACTION_MINIMIZE = "com.jiying.launcher.action.MINIMIZE_FLOAT_MAP"
        const val ACTION_EXPAND = "com.jiying.launcher.action.EXPAND_FLOAT_MAP"
        const val ACTION_RESIZE = "com.jiying.launcher.action.RESIZE_FLOAT_MAP"
        const val ACTION_SWITCH_MAP = "com.jiying.launcher.action.SWITCH_MAP"
        
        const val EXTRA_MAP_TYPE = "map_type"
        const val EXTRA_SIZE_MODE = "size_mode"
        
        const val MAP_AMAP = "amap"      // 高德地图
        const val MAP_BAIDU = "baidu"    // 百度地图
        const val MAP_GOOGLE = "google" // Google地图
        
        const val SIZE_MINI = 0          // 迷你模式（固定在角落）
        const val SIZE_SMALL = 1         // 小窗口
        const val SIZE_MEDIUM = 2        // 中窗口
        const val SIZE_LARGE = 3         // 大窗口
        
        private var isRunning = false
        
        fun isServiceRunning(): Boolean = isRunning
        
        fun startService(context: Context, mapType: String = MAP_AMAP) {
            if (isRunning) return
            
            // 检查悬浮窗权限
            if (!Settings.canDrawOverlays(context)) {
                Toast.makeText(context, "需要悬浮窗权限才能使用画中画功能", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                intent.data = Uri.parse("package:${context.packageName}")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            }
            
            val intent = Intent(context, FloatingMapService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_MAP_TYPE, mapType)
            }
            context.startService(intent)
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, FloatingMapService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
    
    // 窗口管理器
    private lateinit var windowManager: WindowManager
    private lateinit var layoutParams: WindowManager.LayoutParams
    
    // 悬浮窗视图组件
    private lateinit var container: FrameLayout
    private lateinit var mapContainer: FrameLayout
    private lateinit var controlBar: LinearLayout
    private lateinit var titleBar: LinearLayout
    private lateinit var closeBtn: ImageButton
    private lateinit var minimizeBtn: ImageButton
    private lateinit var resizeBtn: ImageButton
    private lateinit var fullscreenBtn: ImageButton
    private lateinit var switchMapBtn: ImageButton
    private lateinit var titleText: TextView
    private lateinit var mapIcon: ImageView
    
    // 状态变量
    private var currentMapType = MAP_AMAP
    private var currentSizeMode = SIZE_MEDIUM
    private var isMinimized = false
    private var isDragging = false
    private var isExpanded = true
    
    // 尺寸配置
    private val sizeConfigs = mapOf(
        SIZE_MINI to Pair(dpToPx(100), dpToPx(100)),
        SIZE_SMALL to Pair(dpToPx(200), dpToPx(160)),
        SIZE_MEDIUM to Pair(dpToPx(320), dpToPx(240)),
        SIZE_LARGE to Pair(dpToPx(480), dpToPx(360))
    )
    
    // 拖拽相关
    private var lastX = 0f
    private var lastY = 0f
    private var touchX = 0f
    private var touchY = 0f
    private var startX = 0f
    private var startY = 0f
    
    // 通知相关
    private val notificationId = 1001
    private val channelId = "floating_map_channel"
    
    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()
        createFloatingWindow()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                currentMapType = intent.getStringExtra(EXTRA_MAP_TYPE) ?: MAP_AMAP
                currentSizeMode = intent.getIntExtra(EXTRA_SIZE_MODE, SIZE_MEDIUM)
                startForeground(notificationId, createNotification())
                updateMapDisplay()
            }
            ACTION_STOP -> {
                stopSelf()
            }
            ACTION_MINIMIZE -> {
                minimizeWindow()
            }
            ACTION_EXPAND -> {
                expandWindow()
            }
            ACTION_RESIZE -> {
                val mode = intent.getIntExtra(EXTRA_SIZE_MODE, SIZE_MEDIUM)
                resizeWindow(mode)
            }
            ACTION_SWITCH_MAP -> {
                switchMap()
            }
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        removeFloatingWindow()
    }
    
    /**
     * 创建通知渠道（Android 8.0+）
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "画中画导航",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "画中画导航服务通知"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * 创建前台服务通知
     */
    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, FloatingMapService::class.java).apply {
                action = ACTION_EXPAND
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, FloatingMapService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return Notification.Builder(this, channelId)
            .setContentTitle("画中画导航运行中")
            .setContentText("点击展开地图")
            .setSmallIcon(R.drawable.ic_navigation)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_close, "关闭", stopIntent)
            .setOngoing(true)
            .build()
    }
    
    /**
     * 创建悬浮窗口
     */
    @SuppressLint("InflateParams", "ClickableViewAccessibility", "WrongConstant")
    private fun createFloatingWindow() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        // 创建布局参数
        layoutParams = WindowManager.LayoutParams().apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            format = PixelFormat.RGBA_8888
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            width = sizeConfigs[currentSizeMode]?.first ?: dpToPx(320)
            height = sizeConfigs[currentSizeMode]?.second ?: dpToPx(240)
            x = dpToPx(20)
            y = dpToPx(100)
        }
        
        // 填充布局
        container = layoutInflater.inflate(R.layout.activity_navigation_float, null) as FrameLayout
        container.setBackgroundColor(Color.parseColor("#80000000"))
        container.radius = dpToPx(16).toFloat()
        
        // 获取组件
        mapContainer = container.findViewById(R.id.map_container)
        controlBar = container.findViewById(R.id.control_bar)
        titleBar = container.findViewById(R.id.title_bar)
        closeBtn = container.findViewById(R.id.btn_close)
        minimizeBtn = container.findViewById(R.id.btn_minimize)
        resizeBtn = container.findViewById(R.id.btn_resize)
        fullscreenBtn = container.findViewById(R.id.btn_fullscreen)
        switchMapBtn = container.findViewById(R.id.btn_switch_map)
        titleText = container.findViewById(R.id.map_title)
        mapIcon = container.findViewById(R.id.map_icon)
        
        // 设置按钮点击事件
        closeBtn.setOnClickListener { stopSelf() }
        minimizeBtn.setOnClickListener { minimizeWindow() }
        resizeBtn.setOnClickListener { cycleResizeMode() }
        fullscreenBtn.setOnClickListener { openFullscreenMap() }
        switchMapBtn.setOnClickListener { switchMap() }
        
        // 设置拖拽
        titleBar.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.rawX
                    lastY = event.rawY
                    touchX = event.rawX
                    touchY = event.rawY
                    startX = event.rawX
                    startY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - lastX
                    val dy = event.rawY - lastY
                    
                    if (!isDragging && (Math.abs(dx) > 10 || Math.abs(dy) > 10)) {
                        isDragging = true
                    }
                    
                    if (isDragging) {
                        layoutParams.x += dx.toInt()
                        layoutParams.y += dy.toInt()
                        windowManager.updateViewLayout(container, layoutParams)
                    }
                    
                    lastX = event.rawX
                    lastY = event.rawY
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val dx = event.rawX - startX
                    val dy = event.rawY - startY
                    
                    if (!isDragging || (Math.abs(dx) < 10 && Math.abs(dy) < 10)) {
                        // 点击事件
                        openFullscreenMap()
                    } else {
                        // 吸附到边缘
                        snapToEdge()
                    }
                    true
                }
                else -> false
            }
        }
        
        // 添加到窗口
        windowManager.addView(container, layoutParams)
        
        // 显示地图
        updateMapDisplay()
    }
    
    /**
     * 更新地图显示
     */
    private fun updateMapDisplay() {
        val mapInfo = when (currentMapType) {
            MAP_AMAP -> "高德地图" to R.drawable.ic_amap_logo
            MAP_BAIDU -> "百度地图" to R.drawable.ic_baidu_logo
            MAP_GOOGLE -> "Google地图" to R.drawable.ic_google_logo
            else -> "导航" to R.drawable.ic_navigation
        }
        
        titleText.text = mapInfo.first
        mapIcon.setImageResource(mapInfo.second)
        
        // 显示地图应用提示
        Toast.makeText(this, "正在启动${mapInfo.first}...", Toast.LENGTH_SHORT).show()
        
        // 延迟启动地图应用
        Handler(Looper.getMainLooper()).postDelayed({
            openMapApp()
        }, 1000)
    }
    
    /**
     * 打开地图应用
     */
    private fun openMapApp() {
        val pkg = when (currentMapType) {
            MAP_AMAP -> "com.autonavi.amap"
            MAP_BAIDU -> "com.baidu.BaiduMap"
            MAP_GOOGLE -> "com.google.android.apps.maps"
            else -> null
        }
        
        pkg?.let {
            try {
                val intent = packageManager.getLaunchIntentForPackage(it)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "未安装${currentMapType}地图", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "无法启动地图应用", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 切换地图
     */
    private fun switchMap() {
        currentMapType = when (currentMapType) {
            MAP_AMAP -> MAP_BAIDU
            MAP_BAIDU -> MAP_GOOGLE
            MAP_GOOGLE -> MAP_AMAP
            else -> MAP_AMAP
        }
        updateMapDisplay()
    }
    
    /**
     * 最小化窗口
     */
    private fun minimizeWindow() {
        if (!isMinimized) {
            // 缩小到迷你模式
            val miniSize = sizeConfigs[SIZE_MINI] ?: Pair(dpToPx(100), dpToPx(100))
            animateResize(miniSize.first, miniSize.second)
            isMinimized = true
            minimizeBtn.setImageResource(R.drawable.ic_expand)
        } else {
            // 恢复到之前的模式
            val size = sizeConfigs[currentSizeMode] ?: sizeConfigs[SIZE_MEDIUM]!!
            animateResize(size.first, size.second)
            isMinimized = false
            minimizeBtn.setImageResource(R.drawable.ic_minimize)
        }
    }
    
    /**
     * 展开窗口
     */
    private fun expandWindow() {
        if (isMinimized) {
            minimizeWindow()
        }
        
        // 最大化尺寸
        val displayMetrics = resources.displayMetrics
        val maxWidth = (displayMetrics.widthPixels * 0.9).toInt()
        val maxHeight = (displayMetrics.heightPixels * 0.7).toInt()
        animateResize(maxWidth, maxHeight)
    }
    
    /**
     * 调整窗口大小
     */
    private fun resizeWindow(mode: Int) {
        currentSizeMode = mode
        val size = sizeConfigs[mode] ?: sizeConfigs[SIZE_MEDIUM]!!
        animateResize(size.first, size.second)
    }
    
    /**
     * 循环调整大小
     */
    private fun cycleResizeMode() {
        val nextMode = (currentSizeMode + 1) % 4
        resizeWindow(nextMode)
    }
    
    /**
     * 动画调整大小
     */
    private fun animateResize(targetWidth: Int, targetHeight: Int) {
        val startWidth = layoutParams.width
        val startHeight = layoutParams.height
        
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 300
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { animation ->
            val fraction = animation.animatedValue as Float
            layoutParams.width = (startWidth + (targetWidth - startWidth) * fraction).toInt()
            layoutParams.height = (startHeight + (targetHeight - startHeight) * fraction).toInt()
            windowManager.updateViewLayout(container, layoutParams)
        }
        animator.start()
    }
    
    /**
     * 吸附到边缘
     */
    private fun snapToEdge() {
        val screenWidth = resources.displayMetrics.widthPixels
        val targetX = if (layoutParams.x < screenWidth / 2) dpToPx(20) else screenWidth - layoutParams.width - dpToPx(20)
        
        val animator = ValueAnimator.ofInt(layoutParams.x, targetX)
        animator.duration = 200
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { animation ->
            layoutParams.x = animation.animatedValue as Int
            windowManager.updateViewLayout(container, layoutParams)
        }
        animator.start()
    }
    
    /**
     * 打开全屏地图
     */
    private fun openFullscreenMap() {
        openMapApp()
    }
    
    /**
     * 移除悬浮窗口
     */
    private fun removeFloatingWindow() {
        try {
            if (::container.isInitialized) {
                windowManager.removeView(container)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * dp转px
     */
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
