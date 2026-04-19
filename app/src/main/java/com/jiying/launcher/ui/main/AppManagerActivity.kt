package com.jiying.launcher.ui.main

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jiying.launcher.R

/**
 * 应用管理页面
 * 显示设备上所有已安装应用，支持分类筛选和搜索
 */
class AppManagerActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchEditText: android.widget.EditText
    private lateinit var tabAll: TextView
    private lateinit var tabUser: TextView
    private lateinit var tabSystem: TextView
    private lateinit var appCountText: TextView
    private lateinit var backButton: ImageButton
    private lateinit var refreshButton: ImageButton

    private var currentFilter = FILTER_ALL
    private var appList: List<AppInfo> = emptyList()

    companion object {
        const val FILTER_ALL = 0
        const val FILTER_USER = 1
        const val FILTER_SYSTEM = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_manager)
        
        initViews()
        loadApps()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.app_recycler_view)
        recyclerView.layoutManager = GridLayoutManager(this, 4)
        
        searchEditText = findViewById(R.id.search_edit)
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterApps(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        tabAll = findViewById(R.id.tab_all)
        tabUser = findViewById(R.id.tab_user)
        tabSystem = findViewById(R.id.tab_system)
        appCountText = findViewById(R.id.app_count)
        
        tabAll.setOnClickListener { setFilter(FILTER_ALL) }
        tabUser.setOnClickListener { setFilter(FILTER_USER) }
        tabSystem.setOnClickListener { setFilter(FILTER_SYSTEM) }
        
        backButton = findViewById(R.id.back_button)
        backButton.setOnClickListener { finish() }
        
        refreshButton = findViewById(R.id.refresh_button)
        refreshButton.setOnClickListener { loadApps() }
        
        updateTabStyles()
    }
    
    private fun loadApps() {
        val pm = packageManager
        val packages = pm.getInstalledPackages(PackageManager.GET_ACTIVITIES)
        
        appList = packages.mapNotNull { pkg ->
            try {
                AppInfo(
                    packageName = pkg.packageName,
                    appName = pkg.applicationInfo.loadLabel(pm).toString(),
                    icon = pkg.applicationInfo.loadIcon(pm),
                    isSystemApp = (pkg.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                )
            } catch (e: Exception) { null }
        }.sortedBy { it.appName.lowercase() }
        
        updateAppList()
    }
    
    private fun setFilter(filter: Int) {
        currentFilter = filter
        updateTabStyles()
        updateAppList()
    }
    
    private fun updateTabStyles() {
        val selectedColor = resources.getColor(R.color.teal_200, null)
        val normalColor = resources.getColor(android.R.color.darker_gray, null)
        
        tabAll.setTextColor(if (currentFilter == FILTER_ALL) selectedColor else normalColor)
        tabUser.setTextColor(if (currentFilter == FILTER_USER) selectedColor else normalColor)
        tabSystem.setTextColor(if (currentFilter == FILTER_SYSTEM) selectedColor else normalColor)
    }
    
    private fun updateAppList() {
        val filtered = when (currentFilter) {
            FILTER_USER -> appList.filter { !it.isSystemApp }
            FILTER_SYSTEM -> appList.filter { it.isSystemApp }
            else -> appList
        }
        
        // 更新计数
        appCountText.text = when (currentFilter) {
            FILTER_USER -> "用户应用(${filtered.size})"
            FILTER_SYSTEM -> "系统应用(${filtered.size})"
            else -> "全部应用(${filtered.size})"
        }
        
        recyclerView.adapter = AppAdapter(filtered) { app ->
            // 点击启动应用
            val intent = packageManager.getLaunchIntentForPackage(app.packageName)
            intent?.let { startActivity(it) }
        }
    }
    
    private fun filterApps(query: String) {
        if (query.isEmpty()) {
            updateAppList()
            return
        }
        
        val baseFiltered = when (currentFilter) {
            FILTER_USER -> appList.filter { !it.isSystemApp }
            FILTER_SYSTEM -> appList.filter { it.isSystemApp }
            else -> appList
        }
        
        val filtered = baseFiltered.filter { 
            it.appName.contains(query, ignoreCase = true) 
        }
        
        appCountText.text = "搜索结果(${filtered.size})"
        
        recyclerView.adapter = AppAdapter(filtered) { app ->
            val intent = packageManager.getLaunchIntentForPackage(app.packageName)
            intent?.let { startActivity(it) }
        }
    }
    
    data class AppInfo(
        val packageName: String,
        val appName: String,
        val icon: android.graphics.drawable.Drawable,
        val isSystemApp: Boolean
    )
    
    inner class AppAdapter(
        private val apps: List<AppInfo>,
        private val onItemClick: (AppInfo) -> Unit
    ) : RecyclerView.Adapter<AppAdapter.ViewHolder>() {
        
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.app_icon)
            val name: TextView = view.findViewById(R.id.app_name)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_app_manager, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = apps[position]
            holder.icon.setImageDrawable(app.icon)
            holder.name.text = app.appName
            holder.itemView.setOnClickListener { onItemClick(app) }
            
            holder.itemView.setOnLongClickListener {
                showAppOptions(app)
                true
            }
        }
        
        override fun getItemCount() = apps.size
        
        private fun showAppOptions(app: AppInfo) {
            AlertDialog.Builder(this@AppManagerActivity)
                .setTitle(app.appName)
                .setItems(arrayOf("打开应用", "应用详情", "卸载")) { _, which ->
                    when (which) {
                        0 -> {
                            val intent = packageManager.getLaunchIntentForPackage(app.packageName)
                            intent?.let { startActivity(it) }
                        }
                        1 -> {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            intent.data = Uri.parse("package:${app.packageName}")
                            startActivity(intent)
                        }
                        2 -> {
                            val intent = Intent(Intent.ACTION_DELETE)
                            intent.data = Uri.parse("package:${app.packageName}")
                            startActivity(intent)
                        }
                    }
                }
                .show()
        }
    }
}
