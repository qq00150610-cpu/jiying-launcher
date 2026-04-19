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
 * 借鉴来源：布丁UI的悬浮模式 + 氢桌面的悬浮地图核心功能
 * 
 * @author 极影桌面开发团队
 * @version 1.0.0
 */
class FloatingMapService : Service() {
    
    // 服务唯一标识
    companion object {
        const val ACTION_START = "com.jiying.launcher.action.START_FLOAT_MAP"
        const val ACTION_STOP = "com.jiying.launcher.action.STOP_FLOAT_MAP"
        const val ACTION_MINIMIZE = "com.jiying.launcher.action.MINIMIZE_FLOAT_MAP"
        const val ACTION_EXPAND = "com.jiying.launcher.action.EXPAND_FLOAT_MAP"
        const val ACTION_RESIZE = "com.jiying.launcher.action.RESIZE_FLOAT_MAP"
        
        const val EXTRA_MAP_TYPE = "map_type"
        const val EXTRA_SIZE_MODE = "size_mode"
        
        const val MAP_AMAP = "amap"      // 高德地图
        const val MAP_BAIDU = "baidu"    // 百度地图
        
        const val SIZE_MINI = 0          // 迷你模式（固定在角落）
        const val SIZE_SMALL = 1         // 小窗口
        const val SIZE_MEDIUM = 2        // 中窗口
        const val SIZE_LARGE = 3         // 大窗口
        
        private var isRunning = false
        private var floatingWindow: WindowManager? = null
        private var floatingView: View? = null
        
        fun isServiceRunning(): Boolean = isRunning
        
        fun startService(context: Context, mapType: String = MAP_AMAP) {
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
    private lateinit var closeBtn: ImageButton
    private lateinit var resizeBtn: ImageButton
    private lateinit var homeBtn: ImageButton
    private lateinit var centerBtn: ImageButton
    private lateinit var zoomInBtn: ImageButton
    private lateinit var zoomOutBtn: ImageButton
    
    // 状态变量
    private var currentMapType = MAP_AMAP
    private var currentSizeMode = SIZE_MEDIUM
    private var isMinimized = false
    private var isDragging = false
    private var isExpanded = true
    
    // 尺寸配置
    private val sizeConfigs = mapOf(
        SIZE_MINI to Pair(120, 120),
        SIZE_SMALL to Pair(200, 180),
        SIZE_MEDIUM to Pair(320, 240),
        SIZE_LARGE to Pair(480, 360)
    )
    
    // 拖拽相关
    private var lastX = 0f
    private var lastY = 0f
    private var touchX = 0f
    private var touchY = 0f
    
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
                "悬浮地图",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "悬浮地图服务通知"
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
        
        return Notification.Builder(this, channelId)
            .setContentTitle("悬浮地图运行中")
            .setContentText("点击展开地图")
            .setSmallIcon(R.drawable.ic_amap_logo)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    /**
     * 创建悬浮窗口
     */
    @SuppressLint("InflateParams", "ClickableViewAccessibility", "WrongConstant")
    private fun createFloatingWindow() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        // 创建悬浮窗容器
        container = FrameLayout(this).apply {
            setBackgroundResource(R.drawable.bg_float_window)
        }
        
        // 创建地图容器
        mapContainer = FrameLayout(this).apply {
            id = View.generateViewId()
            setBackgroundColor(Color.parseColor("#1E2832"))
        }
        
        // 创建地图内容（WebView或App调用）
        val mapContent = createMapContent()
        mapContainer.addView(mapContent)
        
        // 创建控制栏
        controlBar = createControlBar()
        
        container.addView(mapContainer)
        container.addView(controlBar)
        
        // 设置窗口参数
        val (width, height) = sizeConfigs[currentSizeMode] ?: sizeConfigs[SIZE_MEDIUM]!!
        
        layoutParams = WindowManager.LayoutParams(
            width.dpToPx(),
            height.dpToPx(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = ScreenAdapter.getScreenWidth() - width.dpToPx() - 20.dpToPx()
            y = 100.dpToPx()
        }
        
        // 添加触摸监听器
        container.setOnTouchListener { _, event ->
            handleTouchEvent(event)
        }
        
        // 添加到窗口管理器
        windowManager.addView(container, layoutParams)
    }
    
    /**
     * 创建地图内容视图
     */
    private fun createMapContent(): View {
        // 创建地图占位视图，实际应用中会启动地图App或使用WebView
        val view = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            
            val icon = ImageView(context).apply {
                setImageResource(R.drawable.ic_amap_logo)
                layoutParams = LinearLayout.LayoutParams(60.dpToPx(), 60.dpToPx())
            }
            
            val text = TextView(context).apply {
                text = "高德地图"
                setTextColor(Color.WHITE)
                textSize = 14f
                gravity = Gravity.CENTER
            }
            
            val subText = TextView(context).apply {
                text = "悬浮模式"
                setTextColor(Color.parseColor("#AAAAAA"))
                textSize = 12f
                gravity = Gravity.CENTER
            }
            
            addView(icon)
            addView(text)
            addView(subText)
        }
        return view
    }
    
    /**
     * 创建控制栏
     */
    private fun createControlBar(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#CC1E2832"))
            setPadding(8.dpToPx(), 4.dpToPx(), 8.dpToPx(), 4.dpToPx())
            
            // 关闭按钮
            closeBtn = ImageButton(context).apply {
                setImageResource(R.drawable.ic_close)
                setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener { stopSelf() }
            }
            
            // 最小化按钮
            resizeBtn = ImageButton(context).apply {
                setImageResource(R.drawable.ic_minimize)
                setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener { minimizeWindow() }
            }
            
            // 回家按钮
            homeBtn = ImageButton(context).apply {
                setImageResource(R.drawable.ic_home)
                setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener { navigateHome() }
            }
            
            // 定位按钮
            centerBtn = ImageButton(context).apply {
                setImageResource(R.drawable.ic_location)
                setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener { centerOnCurrentLocation() }
            }
            
            // 放大按钮
            zoomInBtn = ImageButton(context).apply {
                setImageResource(R.drawable.ic_zoom_in)
                setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener { zoomIn() }
            }
            
            // 缩小按钮
            zoomOutBtn = ImageButton(context).apply {
                setImageResource(R.drawable.ic_zoom_out)
                setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener { zoomOut() }
            }
            
            addView(closeBtn, LinearLayout.LayoutParams(36.dpToPx(), 36.dpToPx()))
            addView(resizeBtn, LinearLayout.LayoutParams(36.dpToPx(), 36.dpToPx()))
            addView(homeBtn, LinearLayout.LayoutParams(36.dpToPx(), 36.dpToPx()))
            addView(centerBtn, LinearLayout.LayoutParams(36.dpToPx(), 36.dpToPx()))
            addView(zoomInBtn, LinearLayout.LayoutParams(36.dpToPx(), 36.dpToPx()))
            addView(zoomOutBtn, LinearLayout.LayoutParams(36.dpToPx(), 36.dpToPx()))
        }
    }
    
    /**
     * 处理触摸事件
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun handleTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.rawX
                lastY = event.rawY
                touchX = event.x
                touchY = event.y
                isDragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.rawX - lastX
                val deltaY = event.rawY - lastY
                
                // 如果移动距离超过阈值，则认为是拖拽
                if (!isDragging && (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10)) {
                    isDragging = true
                }
                
                if (isDragging) {
                    layoutParams.x = (layoutParams.x + deltaX).toInt()
                    layoutParams.y = (layoutParams.y + deltaY).toInt()
                    windowManager.updateViewLayout(container, layoutParams)
                    lastX = event.rawX
                    lastY = event.rawY
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!isDragging && event.x == touchX && event.y == touchY) {
                    // 点击事件，切换展开/折叠状态
                    toggleExpand()
                }
                // 吸附到边缘
                snapToEdge()
                return true
            }
        }
        return false
    }
    
    /**
     * 切换展开/折叠状态
     */
    private fun toggleExpand() {
        if (isExpanded) {
            minimizeWindow()
        } else {
            expandWindow()
        }
    }
    
    /**
     * 最小化窗口
     */
    private fun minimizeWindow() {
        if (!isMinimized) {
            isMinimized = true
            isExpanded = false
            
            // 动画缩小
            val targetSize = sizeConfigs[SIZE_MINI] ?: Pair(120, 120)
            animateResize(targetSize.first.dpToPx(), targetSize.second.dpToPx())
            
            // 隐藏控制栏
            controlBar.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    controlBar.visibility = View.GONE
                }
                .start()
        }
    }
    
    /**
     * 展开窗口
     */
    private fun expandWindow() {
        if (isMinimized || !isExpanded) {
            isMinimized = false
            isExpanded = true
            
            // 恢复到之前的尺寸
            val (width, height) = sizeConfigs[currentSizeMode] ?: sizeConfigs[SIZE_MEDIUM]!!
            animateResize(width.dpToPx(), height.dpToPx())
            
            // 显示控制栏
            controlBar.visibility = View.VISIBLE
            controlBar.animate()
                .alpha(1f)
                .setDuration(200)
                .start()
        }
    }
    
    /**
     * 调整窗口大小
     */
    private fun resizeWindow(mode: Int) {
        if (mode in sizeConfigs.keys) {
            currentSizeMode = mode
            val (width, height) = sizeConfigs[mode]!!
            
            if (isMinimized) {
                isMinimized = false
                isExpanded = true
                controlBar.visibility = View.VISIBLE
                controlBar.alpha = 1f
            }
            
            animateResize(width.dpToPx(), height.dpToPx())
        }
    }
    
    /**
     * 动画调整大小
     */
    private fun animateResize(targetWidth: Int, targetHeight: Int) {
        val startWidth = layoutParams.width
        val startHeight = layoutParams.height
        
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                val fraction = animation.animatedValue as Float
                layoutParams.width = (startWidth + (targetWidth - startWidth) * fraction).toInt()
                layoutParams.height = (startHeight + (targetHeight - startHeight) * fraction).toInt()
                windowManager.updateViewLayout(container, layoutParams)
            }
        }
        animator.start()
    }
    
    /**
     * 吸附到屏幕边缘
     */
    private fun snapToEdge() {
        val screenWidth = ScreenAdapter.getScreenWidth()
        val targetX = if (layoutParams.x < screenWidth / 2) {
            20.dpToPx()
        } else {
            screenWidth - layoutParams.width - 20.dpToPx()
        }
        
        val animator = ObjectAnimator.ofInt(this, "dummy", layoutParams.x, targetX).apply {
            duration = 150
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                layoutParams.x = animation.animatedValue as Int
                windowManager.updateViewLayout(container, layoutParams)
            }
        }
        animator.start()
    }
    
    /**
     * 导航回家
     */
    private fun navigateHome() {
        // 启动高德地图并导航回家
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setClassName("com.autonavi.minimap", "com.autonavi.minimap.search.searcharound.NearbySearchActivity")
                putExtra("keyword", "家")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "请安装高德地图", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 定位到当前位置
     */
    private fun centerOnCurrentLocation() {
        Toast.makeText(this, "正在定位...", Toast.LENGTH_SHORT).show()
        // 实际应用中调用定位服务
    }
    
    /**
     * 地图放大
     */
    private fun zoomIn() {
        Toast.makeText(this, "放大", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 地图缩小
     */
    private fun zoomOut() {
        Toast.makeText(this, "缩小", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 移除悬浮窗口
     */
    private fun removeFloatingWindow() {
        try {
            if (::container.isInitialized && container.parent != null) {
                windowManager.removeView(container)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Int.dp转px扩展函数
     */
    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
    
    // 用于ObjectAnimator的虚拟属性
    @Suppress("unused")
    private var dummy: Int = 0
}
