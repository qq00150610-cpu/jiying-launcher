package com.jiying.launcher.ui.music

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.IBinder
import android.view.View
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

/**
 * 音乐播放器Activity
 * 支持本地音乐播放、播放列表、播放控制
 */
class MusicPlayerActivity : AppCompatActivity() {

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
    private lateinit var musicListRecycler: RecyclerView
    private lateinit var nowPlayingCard: CardView
    
    private var musicService: MusicService? = null
    private var isBound = false
    private var isPlaying = false
    private var currentSongIndex = 0
    private val musicList = mutableListOf<MusicItem>()
    private lateinit var musicAdapter: MusicListAdapter
    
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
        
        try {
            initViews()
            setupRecyclerView()
            checkPermissions()
            loadMusicFiles()
            bindMusicService()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initViews() {
        try {
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
            musicListRecycler = findViewById(R.id.music_list_recycler)
            nowPlayingCard = findViewById(R.id.now_playing_card)
            
            playPauseBtn.setOnClickListener { togglePlayPause() }
            prevBtn.setOnClickListener { playPrevious() }
            nextBtn.setOnClickListener { playNext() }
            shuffleBtn.setOnClickListener { toggleShuffle() }
            repeatBtn.setOnClickListener { toggleRepeat() }
            listBtn.setOnClickListener { togglePlaylist() }
            
            progressBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        musicService?.seekTo(progress)
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupRecyclerView() {
        musicAdapter = MusicListAdapter(musicList) { position ->
            playSong(position)
        }
        musicListRecycler.layoutManager = LinearLayoutManager(this)
        musicListRecycler.adapter = musicAdapter
    }

    private fun checkPermissions() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                    arrayOf(Manifest.permission.READ_MEDIA_AUDIO), 100)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadMusicFiles() {
        try {
            val musicDir = File("/storage/emulated/0/Music")
            if (musicDir.exists()) {
                musicDir.listFiles()?.filter { 
                    it.extension.lowercase() in listOf("mp3", "wav", "ogg", "flac", "m4a", "aac")
                }?.forEach { file ->
                    musicList.add(MusicItem(
                        title = file.nameWithoutExtension,
                        artist = "未知艺术家",
                        path = file.absolutePath,
                        duration = 0L
                    ))
                }
            }
            
            val downloadDir = File("/storage/emulated/0/Download")
            if (downloadDir.exists()) {
                downloadDir.listFiles()?.filter { 
                    it.extension.lowercase() in listOf("mp3", "wav", "ogg", "flac", "m4a", "aac")
                }?.forEach { file ->
                    musicList.add(MusicItem(
                        title = file.nameWithoutExtension,
                        artist = "未知艺术家",
                        path = file.absolutePath,
                        duration = 0L
                    ))
                }
            }
            
            musicAdapter.notifyDataSetChanged()
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
        try {
            currentSongIndex = index
            if (index < musicList.size) {
                val song = musicList[index]
                titleText.text = song.title
                artistText.text = song.artist
                musicService?.playMusic(song.path)
                isPlaying = true
                playPauseBtn.setImageResource(R.drawable.ic_pause)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun togglePlayPause() {
        try {
            isPlaying = !isPlaying
            if (isPlaying) {
                musicService?.resume()
                playPauseBtn.setImageResource(R.drawable.ic_pause)
            } else {
                musicService?.pause()
                playPauseBtn.setImageResource(R.drawable.ic_play)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playPrevious() {
        try {
            currentSongIndex = if (currentSongIndex > 0) currentSongIndex - 1 else musicList.size - 1
            playSong(currentSongIndex)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playNext() {
        try {
            currentSongIndex = if (currentSongIndex < musicList.size - 1) currentSongIndex + 1 else 0
            playSong(currentSongIndex)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun toggleShuffle() {
        try {
            Toast.makeText(this, "随机播放: " + (!musicService!!.isShuffleEnabled), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun toggleRepeat() {
        try {
            Toast.makeText(this, "循环模式已切换", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun togglePlaylist() {
        try {
            if (musicListRecycler.visibility == View.VISIBLE) {
                musicListRecycler.visibility = View.GONE
            } else {
                musicListRecycler.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateUI() {
        try {
            if (musicList.isNotEmpty()) {
                playSong(0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (isBound) {
                unbindService(serviceConnection)
                isBound = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

data class MusicItem(
    val title: String,
    val artist: String,
    val path: String,
    val duration: Long
)

class MusicListAdapter(
    private val musicList: List<MusicItem>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<MusicListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.music_item_title)
        val artist: TextView = view.findViewById(R.id.music_item_artist)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_music_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = musicList[position]
        holder.title.text = item.title
        holder.artist.text = item.artist
        holder.itemView.setOnClickListener { onItemClick(position) }
    }

    override fun getItemCount() = musicList.size
}
