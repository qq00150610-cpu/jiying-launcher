package com.jiying.launcher.ui.music

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jiying.launcher.R

/**
 * 本地音乐列表Activity
 */
class LocalMusicListActivity : AppCompatActivity() {

    private lateinit var musicList: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var shufflePlayBtn: Button
    private lateinit var sortBtn: ImageButton
    
    private val musicItems = mutableListOf<MusicItem>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_local_music_list)
        
        try {
            initViews()
            setupRecyclerView()
            loadMusic()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initViews() {
        try {
            musicList = findViewById(R.id.local_music_list)
            emptyView = findViewById(R.id.empty_view)
            shufflePlayBtn = findViewById(R.id.btn_shuffle_play)
            sortBtn = findViewById(R.id.btn_sort)
            
            shufflePlayBtn.setOnClickListener {
                if (musicItems.isNotEmpty()) {
                    Toast.makeText(this, "随机播放全部", Toast.LENGTH_SHORT).show()
                }
            }
            
            sortBtn.setOnClickListener {
                showSortOptions()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupRecyclerView() {
        val adapter = MusicListAdapter(musicItems) { position ->
            Toast.makeText(this, "播放: ${musicItems[position].title}", Toast.LENGTH_SHORT).show()
        }
        musicList.layoutManager = LinearLayoutManager(this)
        musicList.adapter = adapter
    }

    private fun loadMusic() {
        try {
            // 加载本地音乐
            if (musicItems.isEmpty()) {
                emptyView.visibility = View.VISIBLE
                musicList.visibility = View.GONE
            } else {
                emptyView.visibility = View.GONE
                musicList.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showSortOptions() {
        val options = arrayOf("按名称", "按艺术家", "按时长", "按添加时间")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("排序方式")
            .setItems(options) { _, which ->
                Toast.makeText(this, "已按${options[which]}排序", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
}
