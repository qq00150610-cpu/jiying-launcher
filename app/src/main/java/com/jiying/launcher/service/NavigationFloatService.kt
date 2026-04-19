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
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.jiying.launcher.R
import com.jiying.launcher.ui.navigation.NavigationFloatActivity

/**
 * 导航浮窗服务
 * 支持导航小窗悬浮显示
 */
class NavigationFloatService : Service() {

    private var windowManager: WindowManager? = null
    private var navFloatView: View? = null
    private var isExpanded = false
    
    companion object {
        const val CHANNEL_ID = "nav_float_channel"
        const val NOTIFICATION_ID = 1003
    }
    
    override fun onCreate() {
        super.onCreate()
        try {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            createNotificationChannel()
            showNavFloat()
            startForeground(NOTIFICATION_ID, createNotification())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun createNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "导航浮窗",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "极影桌面导航浮窗服务"
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
            val intent = Intent(this, NavigationFloatActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("导航中")
                .setContentText("点击查看详情")
                .setSmallIcon(R.drawable.ic_navigation)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()
        } catch (e: Exception) {
            e.printStackTrace()
            return android.app.Notification()
        }
    }

    private fun showNavFloat() {
        try {
            removeNavFloat()
            
            navFloatView = LayoutInflater.from(this).inflate(R.layout.nav_float_ball, null)
            
            val layoutParams = WindowManager.LayoutParams().apply {
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }
                format = PixelFormat.TRANSLUCENT
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                width = dpToPx(80)
                height = WindowManager.LayoutParams.WRAP_CONTENT
                gravity = Gravity.BOTTOM or Gravity.START
                x = dpToPx(16)
                y = dpToPx(120)
            }
            
            setupNavFloatTouch(navFloatView!!, layoutParams)
            windowManager?.addView(navFloatView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupNavFloatTouch(view: View, layoutParams: WindowManager.LayoutParams) {
        try {
            val distanceText = view.findViewById<TextView>(R.id.nav_distance)
            val etaText = view.findViewById<TextView>(R.id.nav_eta)
            
            // 模拟导航数据
            distanceText.text = "500米"
            etaText.text = "3分钟"
            
            var initialX = 0
            var initialY = 0
            var initialTouchX = 0f
            var initialTouchY = 0f
            
            view.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = (event.rawX - initialTouchX).toInt()
                        val deltaY = (event.rawY - initialTouchY).toInt()
                        
                        layoutParams.x = initialX + deltaX
                        layoutParams.y = initialY + deltaY
                        windowManager?.updateViewLayout(view, layoutParams)
                        true
                    }
                    MotionEvent.ACTION_UP -> true
                    else -> false
                }
            }
            
            view.setOnClickListener {
                // 打开完整导航界面
                try {
                    val intent = Intent(this, NavigationFloatActivity::class.java)
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

    private fun removeNavFloat() {
        try {
            navFloatView?.let {
                windowManager?.removeView(it)
                navFloatView = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeNavFloat()
    }
}
