package com.jiying.launcher.ui.media

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jiying.launcher.R
import java.io.File

/**
 * 图片查看器Activity
 */
class ImageViewerActivity : AppCompatActivity() {

    private lateinit var imageGrid: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var imagePreview: ImageView
    private lateinit var previewCard: CardView
    private lateinit var closePreviewBtn: ImageButton
    
    private val images = mutableListOf<File>()
    private var currentIndex = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)
        
        try {
            initViews()
            checkPermissions()
            loadImages()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initViews() {
        try {
            imageGrid = findViewById(R.id.image_grid)
            emptyView = findViewById(R.id.empty_view)
            imagePreview = findViewById(R.id.image_preview)
            previewCard = findViewById(R.id.preview_card)
            closePreviewBtn = findViewById(R.id.btn_close_preview)
            
            closePreviewBtn.setOnClickListener {
                previewCard.visibility = View.GONE
            }
            
            previewCard.setOnClickListener {
                // 全屏查看
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkPermissions() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) 
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES), 102)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadImages() {
        try {
            val directories = listOf(
                File("/storage/emulated/0/DCIM"),
                File("/storage/emulated/0/Pictures"),
                File("/storage/emulated/0/Download")
            )
            
            directories.forEach { dir ->
                if (dir.exists()) {
                    searchImages(dir)
                }
            }
            
            if (images.isEmpty()) {
                emptyView.visibility = View.VISIBLE
                imageGrid.visibility = View.GONE
            } else {
                emptyView.visibility = View.GONE
                imageGrid.visibility = View.VISIBLE
                setupRecyclerView()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun searchImages(dir: File) {
        try {
            dir.listFiles()?.filter { 
                it.extension.lowercase() in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp")
            }?.forEach { images.add(it) }
            
            // 递归搜索子目录
            dir.listFiles()?.filter { it.isDirectory }?.forEach { subDir ->
                searchImages(subDir)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupRecyclerView() {
        val adapter = ImageGridAdapter(images) { position ->
            showPreview(position)
        }
        imageGrid.layoutManager = GridLayoutManager(this, 3)
        imageGrid.adapter = adapter
    }

    private fun showPreview(position: Int) {
        try {
            currentIndex = position
            val file = images[position]
            
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                imagePreview.setImageBitmap(bitmap)
                previewCard.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class ImageGridAdapter(
    private val images: List<File>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<ImageGridAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.grid_image)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image_grid, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = images[position]
        try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            holder.image.setImageBitmap(bitmap)
        } catch (e: Exception) {
            holder.image.setImageResource(R.drawable.ic_image)
        }
        holder.itemView.setOnClickListener { onItemClick(position) }
    }

    override fun getItemCount() = images.size
}
