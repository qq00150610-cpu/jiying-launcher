package com.jiying.launcher.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import com.jiying.launcher.R
import com.jiying.launcher.ui.main.MainActivity

/**
 * 悬浮球服务
 * 支持桌面悬浮球显示
 */
class FloatBallService : Service() {

    private var windowManager: WindowManager? = null
    private var floatBallView: View? = null
    private var isLeftSide = true
    private var ballSize = 48
    private var ballOpacity = 100
    
    companion object {
        const val CHANNEL_ID = "float_ball_channel"
        const val NOTIFICATION_ID = 1002
    }
    
    override fun onCreate() {
        super.onCreate()
        try {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            loadSettings()
            createNotificationChannel()
            showFloatBall()
            startForeground(NOTIFICATION_ID, createNotification())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun loadSettings() {
        try {
            val prefs = getSharedPreferences("float_ball_settings", MODE_PRIVATE)
            isLeftSide = prefs.getInt("position", 0) == 0
            ballSize = prefs.getInt("size", 48)
            ballOpacity = prefs.getInt("opacity", 100)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "悬浮球服务",
                    NotificationManager.IMPORTANCE_MIN
                ).apply {
                    description = "极影桌面悬浮球服务"
                    setShowBadge(false)
                }
                
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.createNotificationChannel(channel)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotification(): android.app.Notification {
        try {
            val intent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("悬浮球已开启")
                .setContentText("点击打开设置")
                .setSmallIcon(R.drawable.ic_circle)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()
        } catch (e: Exception) {
            e.printStackTrace()
            return android.app.Notification()
        }
    }

    private fun showFloatBall() {
        try {
            // 移除已存在的悬浮球
            removeFloatBall()
            
            // 创建悬浮球视图
            floatBallView = LayoutInflater.from(this).inflate(R.layout.float_ball, null)
            
            val layoutParams = WindowManager.LayoutParams().apply {
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }
                format = PixelFormat.TRANSLUCENT
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                width = dpToPx(ballSize)
                height = dpToPx(ballSize)
                gravity = Gravity.TOP or Gravity.START
                x = if (isLeftSide) 0 else getScreenWidth() - dpToPx(ballSize)
                y = getScreenHeight() / 2
            }
            
            setupFloatBallTouch(floatBallView!!, layoutParams)
            windowManager?.addView(floatBallView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupFloatBallTouch(view: View, layoutParams: WindowManager.LayoutParams) {
        try {
            var initialX = 0
            var initialY = 0
            var initialTouchX = 0f
            var initialTouchY = 0f
            var isMoving = false
            
            view.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isMoving = false
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = (event.rawX - initialTouchX).toInt()
                        val deltaY = (event.rawY - initialTouchY).toInt()
                        
                        if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                            isMoving = true
                        }
                        
                        layoutParams.x = initialX + deltaX
                        layoutParams.y = initialY + deltaY
                        windowManager?.updateViewLayout(view, layoutParams)
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isMoving) {
                            // 点击事件
                            view.performClick()
                        }
                        
                        // 吸附到边缘
                        val halfWidth = view.width / 2
                        if (layoutParams.x < getScreenWidth() / 2 - halfWidth) {
                            layoutParams.x = 0
                        } else {
                            layoutParams.x = getScreenWidth() - view.width
                        }
                        windowManager?.updateViewLayout(view, layoutParams)
                        true
                    }
                    else -> false
                }
            }
            
            view.setOnClickListener {
                // 打开主界面
                try {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeFloatBall() {
        try {
            floatBallView?.let {
                windowManager?.removeView(it)
                floatBallView = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun getScreenWidth(): Int {
        return resources.displayMetrics.widthPixels
    }

    private fun getScreenHeight(): Int {
        return resources.displayMetrics.heightPixels
    }

    override fun onDestroy() {
        super.onDestroy()
        removeFloatBall()
    }
}
