package com.jiying.launcher.ui.video

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
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

/**
 * 视频播放器Activity
 * 支持本地视频播放
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
    
    private var videoList = mutableListOf<VideoItem>()
    private lateinit var videoAdapter: VideoGridAdapter
    private var currentIndex = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)
        
        try {
            initViews()
            setupVideoView()
            setupRecyclerView()
            checkPermissions()
            loadVideos()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initViews() {
        try {
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
            
            playPauseBtn.setOnClickListener { togglePlayPause() }
            prevBtn.setOnClickListener { playPrevious() }
            nextBtn.setOnClickListener { playNext() }
            fullscreenBtn.setOnClickListener { toggleFullscreen() }
            
            progressBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        videoView.seekTo(progress)
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupVideoView() {
        try {
            videoView.setOnPreparedListener { mp ->
                mp.isLooping = false
                progressBar.max = videoView.duration
                totalTime.text = formatTime(videoView.duration.toLong())
            }
            
            videoView.setOnCompletionListener {
                playNext()
            }
            
            videoView.setOnErrorListener { _, _, _ ->
                Toast.makeText(this, "视频播放错误", Toast.LENGTH_SHORT).show()
                true
            }
            
            // 进度更新
            Thread {
                while (true) {
                    try {
                        runOnUiThread {
                            if (videoView.isPlaying) {
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupRecyclerView() {
        videoAdapter = VideoGridAdapter(videoList) { position ->
            playVideo(position)
        }
        videoListRecycler.layoutManager = GridLayoutManager(this, 3)
        videoListRecycler.adapter = videoAdapter
    }

    private fun checkPermissions() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) 
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                    arrayOf(Manifest.permission.READ_MEDIA_VIDEO), 101)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadVideos() {
        try {
            val videoDir = File("/storage/emulated/0/DCIM")
            if (videoDir.exists()) {
                searchVideos(videoDir)
            }
            
            val downloadDir = File("/storage/emulated/0/Download")
            if (downloadDir.exists()) {
                searchVideos(downloadDir)
            }
            
            val moviesDir = File("/storage/emulated/0/Movies")
            if (moviesDir.exists()) {
                searchVideos(moviesDir)
            }
            
            videoAdapter.notifyDataSetChanged()
            
            if (videoList.isNotEmpty()) {
                playVideo(0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun searchVideos(dir: File) {
        try {
            dir.listFiles()?.filter { 
                it.extension.lowercase() in listOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "3gp")
            }?.forEach { file ->
                videoList.add(VideoItem(
                    title = file.nameWithoutExtension,
                    path = file.absolutePath,
                    thumbnail = null
                ))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playVideo(index: Int) {
        try {
            currentIndex = index
            if (index < videoList.size) {
                val video = videoList[index]
                titleText.text = video.title
                val uri = Uri.parse(video.path)
                videoView.setVideoURI(uri)
                videoView.start()
                playPauseBtn.setImageResource(R.drawable.ic_pause)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun togglePlayPause() {
        try {
            if (videoView.isPlaying) {
                videoView.pause()
                playPauseBtn.setImageResource(R.drawable.ic_play)
            } else {
                videoView.start()
                playPauseBtn.setImageResource(R.drawable.ic_pause)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playPrevious() {
        try {
            currentIndex = if (currentIndex > 0) currentIndex - 1 else videoList.size - 1
            playVideo(currentIndex)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playNext() {
        try {
            currentIndex = if (currentIndex < videoList.size - 1) currentIndex + 1 else 0
            playVideo(currentIndex)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun toggleFullscreen() {
        try {
            if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                setRequestedOrientation(android.content.res.Configuration.ORIENTATION_PORTRAIT)
            } else {
                setRequestedOrientation(android.content.res.Configuration.ORIENTATION_LANDSCAPE)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun formatTime(millis: Long): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
        } else {
            String.format("%02d:%02d", minutes, seconds % 60)
        }
    }
}

data class VideoItem(
    val title: String,
    val path: String,
    val thumbnail: android.graphics.Bitmap?
)

class VideoGridAdapter(
    private val videos: List<VideoItem>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<VideoGridAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnail: ImageView = view.findViewById(R.id.video_thumbnail)
        val title: TextView = view.findViewById(R.id.video_item_title)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video_grid, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = videos[position]
        holder.title.text = item.title
        holder.thumbnail.setImageResource(R.drawable.ic_video_file)
        holder.itemView.setOnClickListener { onItemClick(position) }
    }

    override fun getItemCount() = videos.size
}
