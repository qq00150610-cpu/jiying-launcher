package com.jiying.launcher.ui.service

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jiying.launcher.R
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * 极影桌面 - 文件管理器
 * 
 * 功能说明：
 * - 文件/文件夹浏览
 * - 文件复制、移动、删除
 * - APK安装
 * - 图片/视频预览
 * - 排序功能
 * - 多选操作
 * 
 * @author 极影桌面开发团队
 * @version 2.0.0
 */
class FileManagerActivity : AppCompatActivity() {

    private lateinit var currentPathText: TextView
    private lateinit var fileListRecycler: RecyclerView
    private lateinit var backButton: ImageButton
    private lateinit var homeButton: ImageButton
    private lateinit var sortButton: ImageButton
    private lateinit var selectButton: ImageButton
    private lateinit var emptyView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var bottomActionBar: LinearLayout
    private lateinit var selectedCount: TextView
    private lateinit var btnCopy: Button
    private lateinit var btnMove: Button
    private lateinit var btnDelete: Button
    
    private lateinit var fileAdapter: FileListAdapter
    private var currentPath: String = Environment.getExternalStorageDirectory().absolutePath
    private var fileList = mutableListOf<FileItem>()
    private var isSelectionMode = false
    private val selectedFiles = mutableSetOf<String>()
    
    private var sortMode = SortMode.NAME_ASC
    private var showHiddenFiles = false
    
    companion object {
        private const val REQUEST_PERMISSION = 1001
        private const val REQUEST_MANAGE_STORAGE = 1002
    }
    
    enum class SortMode {
        NAME_ASC, NAME_DESC, DATE_ASC, DATE_DESC, SIZE_ASC, SIZE_DESC, TYPE
    }
    
    data class FileItem(
        val file: File,
        val name: String,
        val path: String,
        val isDirectory: Boolean,
        val size: Long,
        val lastModified: Long,
        val iconRes: Int,
        val extension: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_manager)
        hideSystemUI()
        initViews()
        checkPermissions()
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
            )
        }
    }

    private fun initViews() {
        currentPathText = findViewById(R.id.current_path)
        fileListRecycler = findViewById(R.id.file_list)
        backButton = findViewById(R.id.back_button)
        homeButton = findViewById(R.id.btn_home)
        sortButton = findViewById(R.id.btn_sort)
        selectButton = findViewById(R.id.btn_select)
        emptyView = findViewById(R.id.empty_view)
        progressBar = findViewById(R.id.progress_bar)
        
        // 底部操作栏
        bottomActionBar = findViewById(R.id.bottom_action_bar)
        selectedCount = findViewById(R.id.selected_count)
        btnCopy = findViewById(R.id.btn_copy)
        btnMove = findViewById(R.id.btn_move)
        btnDelete = findViewById(R.id.btn_delete)
        
        fileAdapter = FileListAdapter(fileList) { item, position ->
            onFileClicked(item, position)
        }
        
        fileListRecycler.layoutManager = LinearLayoutManager(this)
        fileListRecycler.adapter = fileAdapter
        
        backButton.setOnClickListener { navigateBack() }
        homeButton.setOnClickListener { navigateToHome() }
        sortButton.setOnClickListener { showSortDialog() }
        selectButton.setOnClickListener { toggleSelectionMode() }
        
        // 底部操作栏按钮
        btnCopy.setOnClickListener { copySelectedFiles() }
        btnMove.setOnClickListener { moveSelectedFiles() }
        btnDelete.setOnClickListener { deleteSelectedFiles() }
        
        currentPathText.text = currentPath
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                AlertDialog.Builder(this)
                    .setTitle("需要权限")
                    .setMessage("需要存储管理权限才能访问所有文件")
                    .setPositiveButton("去授权") { _, _ ->
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        intent.data = Uri.parse("package:$packageName")
                        startActivityForResult(intent, REQUEST_MANAGE_STORAGE)
                    }
                    .setNegativeButton("取消") { _, _ -> finish() }
                    .show()
            } else {
                loadFiles()
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_PERMISSION)
            } else {
                loadFiles()
            }
        }
    }

    private fun loadFiles() {
        progressBar.visibility = View.VISIBLE
        
        Thread {
            try {
                val dir = File(currentPath)
                val files = dir.listFiles()?.filter { file ->
                    if (showHiddenFiles) true else !file.name.startsWith(".")
                } ?: emptyList()
                
                fileList.clear()
                
                // 添加父目录（如果不是根目录）
                if (currentPath != Environment.getExternalStorageDirectory().absolutePath) {
                    fileList.add(FileItem(
                        file = dir.parentFile ?: dir,
                        name = "..",
                        path = dir.parentFile?.absolutePath ?: currentPath,
                        isDirectory = true,
                        size = 0,
                        lastModified = 0,
                        iconRes = R.drawable.ic_folder,
                        extension = ""
                    ))
                }
                
                // 添加文件和文件夹
                files.forEach { file ->
                    val iconRes = getFileIcon(file)
                    val extension = if (file.isFile) file.extension.lowercase() else ""
                    
                    fileList.add(FileItem(
                        file = file,
                        name = file.name,
                        path = file.absolutePath,
                        isDirectory = file.isDirectory,
                        size = if (file.isFile) file.length() else 0,
                        lastModified = file.lastModified(),
                        iconRes = iconRes,
                        extension = extension
                    ))
                }
                
                // 排序
                sortFiles()
                
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    updateUI()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "无法访问该目录", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun sortFiles() {
        when (sortMode) {
            SortMode.NAME_ASC -> fileList.sortBy { it.name.lowercase() }
            SortMode.NAME_DESC -> fileList.sortByDescending { it.name.lowercase() }
            SortMode.DATE_ASC -> fileList.sortBy { it.lastModified }
            SortMode.DATE_DESC -> fileList.sortByDescending { it.lastModified }
            SortMode.SIZE_ASC -> fileList.sortBy { it.size }
            SortMode.SIZE_DESC -> fileList.sortByDescending { it.size }
            SortMode.TYPE -> fileList.sortWith(compareBy({ !it.isDirectory }, { it.extension }, { it.name }))
        }
    }

    private fun getFileIcon(file: File): Int {
        return when {
            file.isDirectory -> R.drawable.ic_folder
            file.extension.lowercase() in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp") -> R.drawable.ic_image
            file.extension.lowercase() in listOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "3gp") -> R.drawable.ic_video
            file.extension.lowercase() in listOf("mp3", "wav", "ogg", "flac", "m4a", "aac") -> R.drawable.ic_music
            file.extension.lowercase() == "pdf" -> R.drawable.ic_pdf
            file.extension.lowercase() in listOf("doc", "docx", "txt", "rtf") -> R.drawable.ic_document
            file.extension.lowercase() == "apk" -> R.drawable.ic_apk
            file.extension.lowercase() in listOf("zip", "rar", "7z", "tar", "gz") -> R.drawable.ic_archive
            else -> R.drawable.ic_file
        }
    }

    private fun updateUI() {
        if (fileList.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            fileListRecycler.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            fileListRecycler.visibility = View.VISIBLE
            fileAdapter.notifyDataSetChanged()
        }
        currentPathText.text = currentPath
    }

    private fun onFileClicked(item: FileItem, position: Int) {
        if (isSelectionMode) {
            toggleSelection(item.path)
            fileAdapter.updateSelectedFiles(selectedFiles)
            return
        }
        
        if (item.isDirectory) {
            currentPath = item.path
            loadFiles()
        } else {
            openFile(item)
        }
    }

    private fun openFile(item: FileItem) {
        try {
            when (item.extension) {
                "apk" -> installApk(item.path)
                "jpg", "jpeg", "png", "gif", "bmp", "webp" -> openImage(item.path)
                "mp4", "mkv", "avi", "mov", "wmv", "flv", "3gp" -> openVideo(item.path)
                "mp3", "wav", "ogg", "flac", "m4a", "aac" -> openMusic(item.path)
                "pdf" -> openPdf(item.path)
                else -> openWithDefaultApp(item.path)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开该文件", Toast.LENGTH_SHORT).show()
        }
    }

    private fun installApk(path: String) {
        try {
            val file = File(path)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法安装APK", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openImage(path: String) {
        try {
            val intent = Intent(this, com.jiying.launcher.ui.media.ImageViewerActivity::class.java).apply {
                putExtra("image_path", path)
            }
            startActivity(intent)
        } catch (e: Exception) {
            openWithDefaultApp(path)
        }
    }

    private fun openVideo(path: String) {
        try {
            val intent = Intent(this, com.jiying.launcher.ui.video.VideoPlayerActivity::class.java).apply {
                putExtra("video_path", path)
            }
            startActivity(intent)
        } catch (e: Exception) {
            openWithDefaultApp(path)
        }
    }

    private fun openMusic(path: String) {
        try {
            val intent = Intent(this, com.jiying.launcher.ui.music.MusicPlayerActivity::class.java).apply {
                putExtra("music_path", path)
            }
            startActivity(intent)
        } catch (e: Exception) {
            openWithDefaultApp(path)
        }
    }

    private fun openPdf(path: String) {
        openWithDefaultApp(path)
    }

    private fun openWithDefaultApp(path: String) {
        try {
            val file = File(path)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                setDataAndType(Uri.fromFile(file), "*/*")
            }
            startActivity(Intent.createChooser(intent, "打开文件"))
        } catch (e: Exception) {
            Toast.makeText(this, "没有可打开该文件的应用", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateBack() {
        val parent = File(currentPath).parentFile
        if (parent != null) {
            currentPath = parent.absolutePath
            loadFiles()
        }
    }

    private fun navigateToHome() {
        currentPath = Environment.getExternalStorageDirectory().absolutePath
        loadFiles()
    }

    private fun showSortDialog() {
        val sortOptions = arrayOf("名称升序", "名称降序", "时间升序", "时间降序", "大小升序", "大小降序", "类型")
        
        AlertDialog.Builder(this)
            .setTitle("排序方式")
            .setItems(sortOptions) { _, which ->
                sortMode = SortMode.values()[which]
                sortFiles()
                fileAdapter.notifyDataSetChanged()
            }
            .show()
    }

    private fun toggleSelectionMode() {
        isSelectionMode = !isSelectionMode
        if (!isSelectionMode) {
            selectedFiles.clear()
            bottomActionBar.visibility = View.GONE
        } else {
            bottomActionBar.visibility = View.VISIBLE
        }
        fileAdapter.setSelectionMode(isSelectionMode)
        fileAdapter.updateSelectedFiles(selectedFiles)
        updateSelectionUI()
    }
    
    private fun updateSelectionUI() {
        selectedCount.text = "已选择 ${selectedFiles.size} 个文件"
        btnCopy.isEnabled = selectedFiles.isNotEmpty()
        btnMove.isEnabled = selectedFiles.isNotEmpty()
        btnDelete.isEnabled = selectedFiles.isNotEmpty()
    }

    private fun toggleSelection(path: String) {
        if (selectedFiles.contains(path)) {
            selectedFiles.remove(path)
        } else {
            selectedFiles.add(path)
        }
        
        if (selectedFiles.isEmpty()) {
            Toast.makeText(this, "未选择文件", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "已选择 ${selectedFiles.size} 个项目", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteSelectedFiles() {
        if (selectedFiles.isEmpty()) return
        
        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定要删除选中的 ${selectedFiles.size} 个项目吗？此操作不可撤销。")
            .setPositiveButton("删除") { _, _ ->
                Thread {
                    selectedFiles.forEach { path ->
                        try {
                            val file = File(path)
                            if (file.isDirectory) {
                                file.deleteRecursively()
                            } else {
                                file.delete()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    
                    runOnUiThread {
                        selectedFiles.clear()
                        isSelectionMode = false
                        loadFiles()
                        Toast.makeText(this, "删除完成", Toast.LENGTH_SHORT).show()
                    }
                }.start()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 复制选中的文件
     */
    private fun copySelectedFiles() {
        if (selectedFiles.isEmpty()) {
            Toast.makeText(this, "请先选择文件", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(this, "复制功能开发中", Toast.LENGTH_SHORT).show()
        // TODO: 实现文件复制功能
    }
    
    /**
     * 移动选中的文件
     */
    private fun moveSelectedFiles() {
        if (selectedFiles.isEmpty()) {
            Toast.makeText(this, "请先选择文件", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(this, "移动功能开发中", Toast.LENGTH_SHORT).show()
        // TODO: 实现文件移动功能
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadFiles()
            } else {
                Toast.makeText(this, "需要存储权限才能访问文件", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_MANAGE_STORAGE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
                loadFiles()
            } else {
                finish()
            }
        }
    }

    override fun onBackPressed() {
        if (isSelectionMode) {
            isSelectionMode = false
            selectedFiles.clear()
            fileAdapter.setSelectionMode(false)
            fileAdapter.updateSelectedFiles(selectedFiles)
            return
        }
        
        val parent = File(currentPath).parentFile
        if (parent != null && currentPath != Environment.getExternalStorageDirectory().absolutePath) {
            currentPath = parent.absolutePath
            loadFiles()
        } else {
            super.onBackPressed()
            finish()
        }
    }
    
    // ========== 内部适配器类 ==========
    
    inner class FileListAdapter(
        private val items: List<FileItem>,
        private val onItemClick: (FileItem, Int) -> Unit
    ) : RecyclerView.Adapter<FileListAdapter.FileViewHolder>() {
        
        private var selectionMode = false
        private val selectedPaths = mutableSetOf<String>()
        
        fun setSelectionMode(enabled: Boolean) {
            selectionMode = enabled
            notifyDataSetChanged()
        }
        
        fun updateSelectedFiles(selected: Set<String>) {
            selectedPaths.clear()
            selectedPaths.addAll(selected)
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): FileViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_file_list, parent, false)
            return FileViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
            val item = items[position]
            holder.bind(item)
        }
        
        override fun getItemCount() = items.size
        
        inner class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val iconView: ImageView = itemView.findViewById(R.id.file_icon)
            private val nameView: TextView = itemView.findViewById(R.id.file_name)
            private val infoView: TextView = itemView.findViewById(R.id.file_info)
            private val checkBox: CheckBox = itemView.findViewById(R.id.file_checkbox)
            
            fun bind(item: FileItem) {
                iconView.setImageResource(item.iconRes)
                nameView.text = item.name
                
                if (item.isDirectory) {
                    infoView.text = ""
                    checkBox.visibility = View.GONE
                } else {
                    infoView.text = formatFileSize(item.size)
                    checkBox.visibility = if (selectionMode) View.VISIBLE else View.GONE
                    checkBox.isChecked = selectedPaths.contains(item.path)
                }
                
                itemView.setOnClickListener { onItemClick(item, adapterPosition) }
                checkBox.setOnCheckedChangeListener { _, _ ->
                    if (selectionMode) {
                        if (checkBox.isChecked) {
                            selectedPaths.add(item.path)
                        } else {
                            selectedPaths.remove(item.path)
                        }
                    }
                }
            }
            
            private fun formatFileSize(size: Long): String {
                return when {
                    size < 1024 -> "$size B"
                    size < 1024 * 1024 -> "${size / 1024} KB"
                    size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
                    else -> "${size / (1024 * 1024 * 1024)} GB"
                }
            }
        }
    }
}
