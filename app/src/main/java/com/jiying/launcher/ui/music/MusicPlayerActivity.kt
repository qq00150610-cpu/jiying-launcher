package com.jiying.launcher.ui.music

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jiying.launcher.R
import com.jiying.launcher.service.MusicService
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * 极影桌面 - 音乐播放器
 * 
 * 功能说明：
 * - 本地音乐扫描
 * - 播放控制
 * - 播放列表
 * - 随机/循环模式
 * - 后台播放
 * - LRC歌词显示（基础）
 * - 音乐应用选择（支持共存版识别）
 * 
 * @author 极影桌面开发团队
 * @version 2.1.0
 */
class MusicPlayerActivity : AppCompatActivity() {

    // ========== 音乐应用识别数据类 ==========
    
    data class MusicAppInfo(
        val name: String,
        val packageName: String,
        val coexistPackage: String? = null // 共存版包名
    )
    
    // 已知音乐应用包名列表
    private val musicApps = listOf(
        MusicAppInfo("QQ音乐", "com.tencent.qqmusic", "com.tencent.qqmusic.lite"),
        MusicAppInfo("酷狗音乐", "com.kugou.android", "com.kugou.android.lite"),
        MusicAppInfo("酷我音乐", "cn.kuwo.player", "cn.kuwo.player.lite"),
        MusicAppInfo("网易云音乐", "com.netease.cloudmusic", "com.netease.cloudmusic.lite"),
        MusicAppInfo("虾米音乐", "com.xiami.miplayer", "com.xiami.miplayer.pad"),
        MusicAppInfo("百度音乐", "com.baidu.music", null),
        MusicAppInfo("搜狗音乐", "com.sogou.music", null),
        MusicAppInfo("Spotify", "com.spotify.music", null),
        MusicAppInfo("YouTube Music", "com.google.android.apps.youtube.music", null),
        MusicAppInfo("Amazon Music", "com.amazon.mp3", null)
    )

    private lateinit var rootLayout: RelativeLayout
    private lateinit var titleText: TextView
    private lateinit var artistText: TextView
    private lateinit var albumArt: ImageView
    private lateinit var progressBar: SeekBar
    private lateinit var currentTime: TextView
    private lateinit var totalTime: TextView
    private lateinit var playPauseBtn: ImageButton
    private lateinit var prevBtn: ImageButton
    private lateinit var nextBtn: ImageButton
    private lateinit var shuffleBtn: ImageButton
    private lateinit var repeatBtn: ImageButton
    private lateinit var listBtn: ImageButton
    private lateinit var backBtn: ImageButton
    private lateinit var musicListRecycler: RecyclerView
    private lateinit var nowPlayingCard: CardView
    private lateinit var emptyView: TextView
    private lateinit var lyricView: TextView
    
    private var musicService: MusicService? = null
    private var isBound = false
    private var isPlaying = false
    private var currentSongIndex = 0
    private var isShuffle = false
    private var repeatMode = RepeatMode.OFF
    private val musicList = mutableListOf<MusicItem>()
    private lateinit var musicAdapter: MusicListAdapter
    
    enum class RepeatMode { OFF, ONE, ALL }
    
    companion object {
        private const val REQUEST_PERMISSION = 1001
        private val MUSIC_EXTENSIONS = listOf("mp3", "wav", "ogg", "flac", "m4a", "aac", "opus")
    }
    
    data class MusicItem(
        val title: String,
        val artist: String,
        val album: String,
        val path: String,
        val duration: Long
    )

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            isBound = true
            updateUI()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_player)
        hideSystemUI()
        initViews()
        setupRecyclerView()
        checkPermissions()
        bindMusicService()
        
        // 检查是否指定了音乐路径
        intent.getStringExtra("music_path")?.let { path ->
            playSpecificMusic(path)
        }
    }

    private fun hideSystemUI() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
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
        rootLayout = findViewById(R.id.music_player_root)
        titleText = findViewById(R.id.music_title)
        artistText = findViewById(R.id.music_artist)
        albumArt = findViewById(R.id.music_album_art)
        progressBar = findViewById(R.id.music_progress)
        currentTime = findViewById(R.id.music_current_time)
        totalTime = findViewById(R.id.music_total_time)
        playPauseBtn = findViewById(R.id.btn_play_pause)
        prevBtn = findViewById(R.id.btn_prev)
        nextBtn = findViewById(R.id.btn_next)
        shuffleBtn = findViewById(R.id.btn_shuffle)
        repeatBtn = findViewById(R.id.btn_repeat)
        listBtn = findViewById(R.id.btn_list)
        backBtn = findViewById(R.id.btn_back)
        musicListRecycler = findViewById(R.id.music_list_recycler)
        nowPlayingCard = findViewById(R.id.now_playing_card)
        emptyView = findViewById(R.id.empty_view)
        lyricView = findViewById(R.id.lyric_highlight)
        
        backBtn.setOnClickListener { finish() }
        playPauseBtn.setOnClickListener { togglePlayPause() }
        prevBtn.setOnClickListener { playPrevious() }
        nextBtn.setOnClickListener { playNext() }
        shuffleBtn.setOnClickListener { toggleShuffle() }
        repeatBtn.setOnClickListener { toggleRepeat() }
        listBtn.setOnClickListener { togglePlaylist() }
        
        // 选择音乐应用按钮点击事件
        findViewById<Button>(R.id.btn_select_app)?.setOnClickListener { showMusicAppSelector() }
        
        progressBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    musicService?.seekTo(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 设置默认专辑封面
        albumArt.setImageResource(R.drawable.ic_album_placeholder)
        
        // 默认显示播放列表
        musicListRecycler.visibility = View.VISIBLE
    }

    private fun setupRecyclerView() {
        musicAdapter = MusicListAdapter(musicList) { position ->
            playSong(position)
        }
        musicListRecycler.layoutManager = LinearLayoutManager(this)
        musicListRecycler.adapter = musicAdapter
    }

    private fun checkPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                    arrayOf(Manifest.permission.READ_MEDIA_AUDIO), REQUEST_PERMISSION)
            } else {
                loadMusicFiles()
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_PERMISSION)
            } else {
                loadMusicFiles()
            }
        }
    }

    private fun loadMusicFiles() {
        val dirs = listOf(
            File(android.os.Environment.getExternalStorageDirectory(), "Music"),
            File(android.os.Environment.getExternalStorageDirectory(), "Download"),
            File(android.os.Environment.getExternalStorageDirectory(), "netease/cloudmusic"),
            File(android.os.Environment.getExternalStorageDirectory(), "QQMusic"),
            File(android.os.Environment.getExternalStorageDirectory(), "KuGou"),
            File(android.os.Environment.getExternalStorageDirectory(), "Tencent/QQfile_recv")
        )
        
        dirs.filter { it.exists() }.forEach { searchMusic(it) }
        
        // 按艺术家排序
        musicList.sortBy { it.artist + it.title }
        
        musicAdapter.notifyDataSetChanged()
        
        if (musicList.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            musicListRecycler.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            musicListRecycler.visibility = View.VISIBLE
        }
    }

    private fun searchMusic(dir: File) {
        try {
            dir.listFiles()?.forEach { file ->
                if (file.isFile && file.extension.lowercase() in MUSIC_EXTENSIONS) {
                    val metadata = getMusicMetadata(file)
                    
                    musicList.add(MusicItem(
                        title = metadata["title"] ?: file.nameWithoutExtension,
                        artist = metadata["artist"] ?: "未知艺术家",
                        album = metadata["album"] ?: "未知专辑",
                        path = file.absolutePath,
                        duration = metadata["duration"]?.toLongOrNull() ?: 0
                    ))
                } else if (file.isDirectory && !file.name.startsWith(".")) {
                    searchMusic(file)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun getMusicMetadata(file: File): Map<String, String> {
        val metadata = mutableMapOf<String, String>()
        
        try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            
            metadata["title"] = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE)
            metadata["artist"] = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST)
            metadata["album"] = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM)
            metadata["duration"] = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            
            retriever.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return metadata
    }

    private fun bindMusicService() {
        try {
            val intent = Intent(this, MusicService::class.java)
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playSong(index: Int) {
        if (index < 0 || index >= musicList.size) return
        
        currentSongIndex = index
        val song = musicList[index]
        titleText.text = song.title
        artistText.text = song.artist
        
        musicService?.playMusic(song.path)
        isPlaying = true
        playPauseBtn.setImageResource(R.drawable.ic_pause)
        
        // 更新播放列表高亮
        musicAdapter.setCurrentPlaying(index)
        
        // 更新进度条
        progressBar.max = song.duration.toInt()
        totalTime.text = formatTime(song.duration)
        
        // 隐藏播放列表
        musicListRecycler.visibility = View.GONE
        nowPlayingCard.visibility = View.VISIBLE
    }
    
    private fun playSpecificMusic(path: String) {
        val index = musicList.indexOfFirst { it.path == path }
        if (index >= 0) {
            playSong(index)
        } else {
            val file = File(path)
            musicList.add(0, MusicItem(
                title = file.nameWithoutExtension,
                artist = "未知艺术家",
                album = "未知专辑",
                path = path,
                duration = 0
            ))
            musicAdapter.notifyItemInserted(0)
            playSong(0)
        }
    }

    private fun togglePlayPause() {
        isPlaying = !isPlaying
        if (isPlaying) {
            musicService?.resume()
            playPauseBtn.setImageResource(R.drawable.ic_pause)
        } else {
            musicService?.pause()
            playPauseBtn.setImageResource(R.drawable.ic_play)
        }
    }

    private fun playPrevious() {
        val newIndex = when {
            isShuffle -> (0 until musicList.size).random()
            currentSongIndex > 0 -> currentSongIndex - 1
            else -> musicList.size - 1
        }
        playSong(newIndex)
    }

    private fun playNext() {
        val newIndex = when {
            isShuffle -> (0 until musicList.size).random()
            currentSongIndex < musicList.size - 1 -> currentSongIndex + 1
            repeatMode == RepeatMode.ALL -> 0
            else -> currentSongIndex
        }
        playSong(newIndex)
    }

    private fun toggleShuffle() {
        isShuffle = !isShuffle
        shuffleBtn.alpha = if (isShuffle) 1.0f else 0.5f
        Toast.makeText(this, if (isShuffle) "随机播放已开启" else "随机播放已关闭", Toast.LENGTH_SHORT).show()
    }

    private fun toggleRepeat() {
        repeatMode = when (repeatMode) {
            RepeatMode.OFF -> {
                repeatBtn.setImageResource(R.drawable.ic_repeat)
                Toast.makeText(this, "列表循环", Toast.LENGTH_SHORT).show()
                RepeatMode.ALL
            }
            RepeatMode.ALL -> {
                repeatBtn.setImageResource(R.drawable.ic_repeat_one)
                Toast.makeText(this, "单曲循环", Toast.LENGTH_SHORT).show()
                RepeatMode.ONE
            }
            RepeatMode.ONE -> {
                repeatBtn.setImageResource(R.drawable.ic_repeat)
                repeatBtn.alpha = 0.5f
                Toast.makeText(this, "循环已关闭", Toast.LENGTH_SHORT).show()
                RepeatMode.OFF
            }
        }
    }

    private fun togglePlaylist() {
        if (musicListRecycler.visibility == View.VISIBLE) {
            musicListRecycler.visibility = View.GONE
        } else {
            musicListRecycler.visibility = View.VISIBLE
        }
    }
    
    private fun updateUI() {
        if (musicList.isNotEmpty() && currentSongIndex < musicList.size) {
            val song = musicList[currentSongIndex]
            titleText.text = song.title
            artistText.text = song.artist
            totalTime.text = formatTime(song.duration)
        }
    }

    private fun formatTime(millis: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadMusicFiles()
            } else {
                Toast.makeText(this, "需要存储权限才能访问音乐", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }
    
    // ========== 音乐列表适配器 ==========
    
    inner class MusicListAdapter(
        private val items: List<MusicItem>,
        private val onItemClick: (Int) -> Unit
    ) : RecyclerView.Adapter<MusicListAdapter.MusicViewHolder>() {
        
        private var currentPlayingIndex = -1
        
        fun setCurrentPlaying(index: Int) {
            val oldIndex = currentPlayingIndex
            currentPlayingIndex = index
            if (oldIndex >= 0) notifyItemChanged(oldIndex)
            notifyItemChanged(currentPlayingIndex)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): MusicViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_music_list, parent, false)
            return MusicViewHolder(view)
        }

        override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size

        inner class MusicViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val musicIcon: ImageView = itemView.findViewById(R.id.music_icon)
            private val titleView: TextView = itemView.findViewById(R.id.music_title)
            private val artistView: TextView = itemView.findViewById(R.id.music_artist)
            private val durationView: TextView = itemView.findViewById(R.id.music_duration)
            private val playingIndicator: ImageView = itemView.findViewById(R.id.playing_indicator)

            fun bind(music: MusicItem) {
                titleView.text = music.title
                artistView.text = music.artist
                durationView.text = formatTime(music.duration)
                
                // 当前播放高亮
                if (adapterPosition == currentPlayingIndex) {
                    playingIndicator.visibility = View.VISIBLE
                    titleView.setTextColor(ContextCompat.getColor(itemView.context, R.color.accent_primary))
                } else {
                    playingIndicator.visibility = View.GONE
                    titleView.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_primary))
                }
                
                itemView.setOnClickListener { onItemClick(adapterPosition) }
            }
        }
    }
    
    // ========== 音乐应用选择功能 ==========
    
    /**
     * 扫描已安装的音乐应用
     * 支持识别共存版应用
     */
    private fun scanInstalledMusicApps(): List<MusicAppInfo> {
        val installed = mutableListOf<MusicAppInfo>()
        for (app in musicApps) {
            // 检查主版本
            if (isPackageInstalled(app.packageName)) {
                installed.add(app)
            }
            // 检查共存版
            app.coexistPackage?.let { coexistPkg ->
                if (isPackageInstalled(coexistPkg)) {
                    // 添加共存版，名称标注
                    installed.add(MusicAppInfo(
                        name = "${app.name}(共存版)",
                        packageName = coexistPkg,
                        coexistPackage = null
                    ))
                }
            }
        }
        return installed
    }
    
    /**
     * 检查指定包名的应用是否已安装
     */
    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 显示音乐应用选择对话框
     */
    private fun showMusicAppSelector() {
        val apps = scanInstalledMusicApps()
        
        if (apps.isEmpty()) {
            Toast.makeText(this, "未找到已安装的音乐应用", Toast.LENGTH_SHORT).show()
            return
        }
        
        val items = apps.map { "${it.name}" }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("选择音乐应用")
            .setItems(items) { _, which ->
                launchMusicApp(apps[which].packageName)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 启动指定包名的音乐应用
     */
    private fun launchMusicApp(packageName: String) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                startActivity(intent)
                Toast.makeText(this, "正在启动...", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "无法启动应用", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "启动失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
