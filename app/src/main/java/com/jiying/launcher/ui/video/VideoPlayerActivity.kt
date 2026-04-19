package com.jiying.launcher.ui.video

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jiying.launcher.R
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * 极影桌面 - 视频播放器
 * 
 * 功能说明：
 * - 本地视频扫描
 * - 视频缩略图显示
 * - 播放控制
 * - 全屏模式
 * - 多种格式支持
 * 
 * @author 极影桌面开发团队
 * @version 2.0.0
 */
class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private lateinit var titleText: TextView
    private lateinit var playPauseBtn: ImageButton
    private lateinit var prevBtn: ImageButton
    private lateinit var nextBtn: ImageButton
    private lateinit var progressBar: SeekBar
    private lateinit var currentTime: TextView
    private lateinit var totalTime: TextView
    private lateinit var fullscreenBtn: ImageButton
    private lateinit var videoListRecycler: RecyclerView
    private lateinit var videoContainer: CardView
    private lateinit var backButton: ImageButton
    private lateinit var emptyView: TextView
    
    private var videoList = mutableListOf<VideoItem>()
    private lateinit var videoAdapter: VideoGridAdapter
    private var currentIndex = 0
    private var isFullscreen = false
    private var isListVisible = true
    
    companion object {
        private const val REQUEST_PERMISSION = 1001
        private val VIDEO_EXTENSIONS = listOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "3gp", "webm", "m4v")
    }
    
    data class VideoItem(
        val title: String,
        val path: String,
        val duration: Long,
        val thumbnail: Bitmap?,
        val size: Long
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)
        hideSystemUI()
        initViews()
        setupVideoView()
        setupRecyclerView()
        checkPermissions()
        
        // 检查是否指定了视频路径
        intent.getStringExtra("video_path")?.let { path ->
            playSpecificVideo(path)
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
    }

    private fun initViews() {
        videoView = findViewById(R.id.video_view)
        titleText = findViewById(R.id.video_title)
        playPauseBtn = findViewById(R.id.btn_video_play_pause)
        prevBtn = findViewById(R.id.btn_video_prev)
        nextBtn = findViewById(R.id.btn_video_next)
        progressBar = findViewById(R.id.video_progress)
        currentTime = findViewById(R.id.video_current_time)
        totalTime = findViewById(R.id.video_total_time)
        fullscreenBtn = findViewById(R.id.btn_fullscreen)
        videoListRecycler = findViewById(R.id.video_list_recycler)
        videoContainer = findViewById(R.id.video_container)
        backButton = findViewById(R.id.btn_back)
        emptyView = findViewById(R.id.empty_view)
        
        backButton.setOnClickListener { finish() }
        playPauseBtn.setOnClickListener { togglePlayPause() }
        prevBtn.setOnClickListener { playPrevious() }
        nextBtn.setOnClickListener { playNext() }
        fullscreenBtn.setOnClickListener { toggleFullscreen() }
        
        // 长按视频列表项删除
        videoListRecycler.setOnLongClickListener {
            showVideoOptions()
            true
        }
        
        progressBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    videoView.seekTo(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupVideoView() {
        videoView.setOnPreparedListener { mp ->
            mp.isLooping = false
            val duration = videoView.duration
            progressBar.max = duration
            totalTime.text = formatTime(duration.toLong())
            playPauseBtn.setImageResource(R.drawable.ic_pause)
        }
        
        videoView.setOnCompletionListener {
            playPauseBtn.setImageResource(R.drawable.ic_play)
            playNext()
        }
        
        videoView.setOnErrorListener { _, what, extra ->
            Toast.makeText(this, "视频播放错误: $what", Toast.LENGTH_SHORT).show()
            true
        }
        
        // 进度更新
        Thread {
            while (true) {
                try {
                    if (videoView.isPlaying) {
                        runOnUiThread {
                            progressBar.progress = videoView.currentPosition
                            currentTime.text = formatTime(videoView.currentPosition.toLong())
                        }
                    }
                    Thread.sleep(1000)
                } catch (e: Exception) {
                    break
                }
            }
        }.start()
    }

    private fun setupRecyclerView() {
        videoAdapter = VideoGridAdapter(videoList) { position ->
            playVideo(position)
        }
        videoListRecycler.layoutManager = GridLayoutManager(this, 3)
        videoListRecycler.adapter = videoAdapter
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                    arrayOf(Manifest.permission.READ_MEDIA_VIDEO), REQUEST_PERMISSION)
            } else {
                loadVideos()
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_PERMISSION)
            } else {
                loadVideos()
            }
        }
    }

    private fun loadVideos() {
        val dirs = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            File(Environment.getExternalStorageDirectory(), "Tencent/QQfile_recv"),
            File(Environment.getExternalStorageDirectory(), "WeiXin")
        )
        
        dirs.filter { it.exists() }.forEach { searchVideos(it) }
        
        // 按时间排序
        videoList.sortByDescending { it.duration }
        
        videoAdapter.notifyDataSetChanged()
        
        if (videoList.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            videoListRecycler.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            videoListRecycler.visibility = View.VISIBLE
        }
    }

    private fun searchVideos(dir: File) {
        try {
            dir.listFiles()?.forEach { file ->
                if (file.isFile && file.extension.lowercase() in VIDEO_EXTENSIONS) {
                    val thumbnail = getVideoThumbnail(file)
                    val duration = getVideoDuration(file)
                    
                    videoList.add(VideoItem(
                        title = file.nameWithoutExtension,
                        path = file.absolutePath,
                        duration = duration,
                        thumbnail = thumbnail,
                        size = file.length()
                    ))
                } else if (file.isDirectory && !file.name.startsWith(".")) {
                    searchVideos(file)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun getVideoThumbnail(file: File): Bitmap? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val bitmap = retriever.getFrameAtTime(1000000) // 1秒处
            retriever.release()
            bitmap
        } catch (e: Exception) {
            null
        }
    }
    
    private fun getVideoDuration(file: File): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0
            retriever.release()
            duration
        } catch (e: Exception) {
            0
        }
    }

    private fun playVideo(index: Int) {
        if (index < 0 || index >= videoList.size) return
        
        currentIndex = index
        val video = videoList[index]
        titleText.text = video.title
        
        try {
            val uri = Uri.parse(video.path)
            videoView.setVideoURI(uri)
            videoView.start()
            
            // 隐藏列表显示播放器
            if (isListVisible) {
                toggleVideoList()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "无法播放该视频", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun playSpecificVideo(path: String) {
        try {
            titleText.text = File(path).nameWithoutExtension
            val uri = Uri.parse(path)
            videoView.setVideoURI(uri)
            videoView.start()
        } catch (e: Exception) {
            Toast.makeText(this, "无法播放该视频", Toast.LENGTH_SHORT).show()
        }
    }

    private fun togglePlayPause() {
        if (videoView.isPlaying) {
            videoView.pause()
            playPauseBtn.setImageResource(R.drawable.ic_play)
        } else {
            videoView.start()
            playPauseBtn.setImageResource(R.drawable.ic_pause)
        }
    }

    private fun playPrevious() {
        val newIndex = if (currentIndex > 0) currentIndex - 1 else videoList.size - 1
        playVideo(newIndex)
    }

    private fun playNext() {
        val newIndex = if (currentIndex < videoList.size - 1) currentIndex + 1 else 0
        playVideo(newIndex)
    }
    
    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        
        if (isFullscreen) {
            // 全屏模式
            videoListRecycler.visibility = View.GONE
            val params = videoContainer.layoutParams
            params.width = MatchParent
            params.height = MatchParent
            videoContainer.layoutParams = params
            fullscreenBtn.setImageResource(R.drawable.ic_fullscreen_exit)
        } else {
            // 退出全屏
            videoListRecycler.visibility = View.VISIBLE
            fullscreenBtn.setImageResource(R.drawable.ic_fullscreen)
        }
    }
    
    private fun toggleVideoList() {
        isListVisible = !isListVisible
        videoListRecycler.visibility = if (isListVisible) View.VISIBLE else View.GONE
    }
    
    private fun showVideoOptions() {
        if (videoList.isEmpty()) return
        
        AlertDialog.Builder(this)
            .setTitle("视频选项")
            .setItems(arrayOf("刷新列表", "按名称排序", "按大小排序", "按时间排序")) { _, which ->
                when (which) {
                    0 -> loadVideos()
                    1 -> {
                        videoList.sortBy { it.title }
                        videoAdapter.notifyDataSetChanged()
                    }
                    2 -> {
                        videoList.sortByDescending { it.size }
                        videoAdapter.notifyDataSetChanged()
                    }
                    3 -> {
                        videoList.sortByDescending { it.duration }
                        videoAdapter.notifyDataSetChanged()
                    }
                }
            }
            .show()
    }

    private fun formatTime(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadVideos()
            } else {
                Toast.makeText(this, "需要存储权限才能访问视频", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (videoView.isPlaying) {
            videoView.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        videoView.stopPlayback()
    }
    
    // ========== 视频网格适配器 ==========
    
    inner class VideoGridAdapter(
        private val items: List<VideoItem>,
        private val onItemClick: (Int) -> Unit
    ) : RecyclerView.Adapter<VideoGridAdapter.VideoViewHolder>() {

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VideoViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_video_grid, parent, false)
            return VideoViewHolder(view)
        }

        override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size

        inner class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val thumbnailView: ImageView = itemView.findViewById(R.id.video_thumbnail)
            private val titleView: TextView = itemView.findViewById(R.id.video_title)
            private val durationView: TextView = itemView.findViewById(R.id.video_duration)
            private val playIcon: ImageView = itemView.findViewById(R.id.play_icon)

            fun bind(video: VideoItem) {
                if (video.thumbnail != null) {
                    thumbnailView.setImageBitmap(video.thumbnail)
                } else {
                    thumbnailView.setImageResource(R.drawable.ic_video_placeholder)
                }
                
                titleView.text = video.title
                durationView.text = formatTime(video.duration)
                
                itemView.setOnClickListener { onItemClick(adapterPosition) }
                
                // 播放中的视频高亮
                if (adapterPosition == currentIndex && videoView.isPlaying) {
                    playIcon.visibility = View.VISIBLE
                } else {
                    playIcon.visibility = View.GONE
                }
            }
        }
    }
}
