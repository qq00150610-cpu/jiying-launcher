package com.jiying.launcher.ui.main

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jiying.launcher.R
import android.content.pm.ApplicationInfo

/**
 * 自定义应用页面
 * 允许用户选择要添加到桌面的应用
 */
class CustomAppActivity : AppCompatActivity() {

    private lateinit var allAppsGrid: RecyclerView
    private lateinit var selectedAppsGrid: RecyclerView
    private lateinit var pageIndicator: LinearLayout
    private lateinit var doneButton: Button
    private lateinit var allAppsAdapter: AppGridAdapter
    private lateinit var selectedAppsAdapter: SelectedAppAdapter
    
    private val allApps = mutableListOf<AppInfo>()
    private val selectedApps = mutableListOf<AppInfo>()
    private var currentPage = 0
    private val appsPerPage = 24
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_app)
        
        initViews()
        loadApps()
        loadSavedApps()
    }
    
    private fun initViews() {
        findViewById<ImageButton>(R.id.back_button).setOnClickListener { finish() }
        
        doneButton = findViewById(R.id.done_button)
        doneButton.setOnClickListener { 
            saveSelectedApps()
            setResult(RESULT_OK)
            finish()
        }
        
        allAppsGrid = findViewById(R.id.all_apps_grid)
        selectedAppsGrid = findViewById(R.id.selected_apps_grid)
        pageIndicator = findViewById(R.id.page_indicator)
        
        // 设置所有应用网格 (4列)
        allAppsAdapter = AppGridAdapter(allApps, true) { app, position ->
            addApp(app)
        }
        allAppsGrid.layoutManager = GridLayoutManager(this, 4)
        allAppsGrid.adapter = allAppsAdapter
        
        // 设置已选应用网格 (4列)
        selectedAppsAdapter = SelectedAppAdapter(selectedApps) { app ->
            removeApp(app)
        }
        selectedAppsGrid.layoutManager = GridLayoutManager(this, 4)
        selectedAppsGrid.adapter = selectedAppsAdapter
    }
    
    private fun loadApps() {
        val pm = packageManager
        val packages = pm.getInstalledPackages(PackageManager.GET_ACTIVITIES)
        
        allApps.clear()
        for (pkg in packages) {
            try {
                val appInfo = pkg.applicationInfo ?: continue
                if (appInfo.enabled && appInfo.loadLabel(pm).isNotBlank()) {
                    // 排除系统关键应用
                    val packageName = appInfo.packageName
                    if (!isSystemApp(appInfo) || isUserApp(appInfo)) {
                        allApps.add(AppInfo(
                            packageName = packageName,
                            appName = appInfo.loadLabel(pm).toString(),
                            icon = appInfo.loadIcon(pm)
                        ))
                    }
                }
            } catch (e: Exception) {
                // 跳过无法获取信息的应用
            }
        }
        
        // 按名称排序
        allApps.sortBy { it.appName }
        allAppsAdapter.notifyDataSetChanged()
        updatePageIndicator()
    }
    
    private fun isSystemApp(appInfo: ApplicationInfo): Boolean {
        return (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
    }
    
    private fun isUserApp(appInfo: ApplicationInfo): Boolean {
        return (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0 ||
               (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
    }
    
    private fun loadSavedApps() {
        val prefs = getSharedPreferences("jiying_custom_apps", MODE_PRIVATE)
        val savedPackages = prefs.getStringSet("selected_apps", emptySet()) ?: emptySet()
        
        val pm = packageManager
        selectedApps.clear()
        
        for (pkg in savedPackages) {
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                if (appInfo.enabled) {
                    selectedApps.add(AppInfo(
                        packageName = pkg,
                        appName = appInfo.loadLabel(pm).toString(),
                        icon = appInfo.loadIcon(pm)
                    ))
                }
            } catch (e: Exception) {
                // 应用已卸载，跳过
            }
        }
        selectedAppsAdapter.notifyDataSetChanged()
    }
    
    private fun saveSelectedApps() {
        val prefs = getSharedPreferences("jiying_custom_apps", MODE_PRIVATE)
        val packages = selectedApps.map { it.packageName }.toSet()
        prefs.edit().putStringSet("selected_apps", packages).apply()
    }
    
    private fun addApp(app: AppInfo) {
        if (!selectedApps.any { it.packageName == app.packageName }) {
            selectedApps.add(app)
            selectedAppsAdapter.notifyDataSetChanged()
            Toast.makeText(this, "已添加: ${app.appName}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun removeApp(app: AppInfo) {
        selectedApps.removeAll { it.packageName == app.packageName }
        selectedAppsAdapter.notifyDataSetChanged()
        Toast.makeText(this, "已移除: ${app.appName}", Toast.LENGTH_SHORT).show()
    }
    
    private fun updatePageIndicator() {
        pageIndicator.removeAllViews()
        val totalPages = (allApps.size + appsPerPage - 1) / appsPerPage
        
        for (i in 0 until totalPages) {
            val dot = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(16, 16).apply {
                    marginStart = 8
                    marginEnd = 8
                }
                setBackgroundResource(if (i == currentPage) R.drawable.ic_circle else R.drawable.ic_circle)
                alpha = if (i == currentPage) 1.0f else 0.3f
            }
            pageIndicator.addView(dot)
        }
    }
    
    data class AppInfo(
        val packageName: String,
        val appName: String,
        val icon: android.graphics.drawable.Drawable
    )
    
    // 所有应用适配器
    inner class AppGridAdapter(
        private val apps: List<AppInfo>,
        private val showAddButton: Boolean,
        private val onAddClick: (AppInfo, Int) -> Unit
    ) : RecyclerView.Adapter<AppGridAdapter.ViewHolder>() {
        
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val iconView: ImageView = itemView.findViewById(R.id.app_icon)
            val nameView: TextView = itemView.findViewById(R.id.app_name)
            val addButton: ImageView = itemView.findViewById(R.id.add_button)
            val itemLayout: LinearLayout = itemView.findViewById(R.id.item_layout)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_custom_app, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = apps[position]
            holder.iconView.setImageDrawable(app.icon)
            holder.nameView.text = app.appName
            
            holder.itemLayout.setOnClickListener {
                onAddClick(app, position)
            }
            
            holder.addButton.setOnClickListener {
                onAddClick(app, position)
            }
            
            // 如果已添加，显示不同状态
            if (selectedApps.any { it.packageName == app.packageName }) {
                holder.addButton.setImageResource(R.drawable.ic_remove_circle_red)
            } else {
                holder.addButton.setImageResource(R.drawable.ic_add_circle_blue)
            }
        }
        
        override fun getItemCount() = apps.size
    }
    
    // 已选应用适配器
    inner class SelectedAppAdapter(
        private val apps: List<AppInfo>,
        private val onRemoveClick: (AppInfo) -> Unit
    ) : RecyclerView.Adapter<SelectedAppAdapter.ViewHolder>() {
        
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val iconView: ImageView = itemView.findViewById(R.id.app_icon)
            val nameView: TextView = itemView.findViewById(R.id.app_name)
            val removeButton: ImageView = itemView.findViewById(R.id.add_button)
            val itemLayout: LinearLayout = itemView.findViewById(R.id.item_layout)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_custom_app, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = apps[position]
            holder.iconView.setImageDrawable(app.icon)
            holder.nameView.text = app.appName
            holder.removeButton.setImageResource(R.drawable.ic_remove_circle_red)
            
            holder.itemLayout.setOnClickListener {
                onRemoveClick(app)
            }
            
            holder.removeButton.setOnClickListener {
                onRemoveClick(app)
            }
        }
        
        override fun getItemCount() = apps.size
    }
}
