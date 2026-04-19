package com.jiying.launcher.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.jiying.launcher.R
import com.jiying.launcher.ui.music.MusicPlayerActivity

/**
 * 音乐播放服务
 * 支持后台音乐播放、媒体会话
 */
class MusicService : Service() {

    private val binder = MusicBinder()
    private var mediaPlayer: MediaPlayer? = null
    private var currentPath: String? = null
    private var isPlaying = false
    var isShuffleEnabled = false
    private var repeatMode = 0 // 0: 不重复, 1: 单曲循环, 2: 列表循环
    
    companion object {
        const val CHANNEL_ID = "music_service_channel"
        const val NOTIFICATION_ID = 1001
    }
    
    override fun onCreate() {
        super.onCreate()
        try {
            createNotificationChannel()
            initMediaPlayer()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            startForeground(NOTIFICATION_ID, createNotification())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "音乐播放",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "极影桌面音乐播放服务"
                    setShowBadge(false)
                }
                
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.createNotificationChannel(channel)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initMediaPlayer() {
        try {
            mediaPlayer = MediaPlayer().apply {
                setOnPreparedListener {
                    start()
                    this@MusicService.isPlaying = true
                }
                setOnCompletionListener {
                    when (repeatMode) {
                        1 -> {
                            mediaPlayer?.seekTo(0)
                            mediaPlayer?.start()
                        }
                        2 -> {
                            // 播放下一首
                        }
                        else -> {
                            this@MusicService.isPlaying = false
                        }
                    }
                }
                setOnErrorListener { _, _, _ ->
                    this@MusicService.isPlaying = false
                    true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playMusic(path: String) {
        try {
            currentPath = path
            mediaPlayer?.apply {
                reset()
                setDataSource(path)
                prepareAsync()
            }
            isPlaying = true
            updateNotification()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pause() {
        try {
            mediaPlayer?.pause()
            isPlaying = false
            updateNotification()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun resume() {
        try {
            mediaPlayer?.start()
            isPlaying = true
            updateNotification()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
            isPlaying = false
            currentPath = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun seekTo(position: Int) {
        try {
            mediaPlayer?.seekTo(position)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCurrentPosition(): Int {
        return try {
            mediaPlayer?.currentPosition ?: 0
        } catch (e: Exception) {
            0
        }
    }

    fun getDuration(): Int {
        return try {
            mediaPlayer?.duration ?: 0
        } catch (e: Exception) {
            0
        }
    }

    fun setShuffle(enabled: Boolean) {
        isShuffleEnabled = enabled
    }

    fun setRepeatMode(mode: Int) {
        repeatMode = mode
    }

    private fun createNotification(): Notification {
        try {
            val intent = Intent(this, MusicPlayerActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("极影桌面")
                .setContentText(if (isPlaying) "正在播放" else "已暂停")
                .setSmallIcon(R.drawable.ic_music_note)
                .setContentIntent(pendingIntent)
                .setOngoing(isPlaying)
                .build()
        } catch (e: Exception) {
            e.printStackTrace()
            return Notification()
        }
    }

    private fun updateNotification() {
        try {
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.notify(NOTIFICATION_ID, createNotification())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }
}
