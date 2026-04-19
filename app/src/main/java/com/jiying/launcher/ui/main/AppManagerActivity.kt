package com.jiying.launcher.ui.main

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.jiying.launcher.R

class AppManagerActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var tabLayout: TabLayout
    private lateinit var appAdapter: AppAdapter
    
    private var appList = listOf<AppInfo>()
    private var currentFilter = FILTER_ALL
    
    data class AppInfo(
        val packageName: String,
        val appName: String,
        val icon: Drawable,
        val isSystemApp: Boolean
    )
    
    companion object {
        private const val FILTER_ALL = 0
        private const val FILTER_USER = 1
        private const val FILTER_SYSTEM = 2
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_manager)
        
        initViews()
        loadApps()
    }
    
    private fun initViews() {
        recyclerView = findViewById(R.id.app_list)
        recyclerView.layoutManager = GridLayoutManager(this, 4)
        
        searchView = findViewById(R.id.app_search)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterApps(query ?: "")
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                filterApps(newText ?: "")
                return true
            }
        })
        
        tabLayout = findViewById(R.id.app_tabs)
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> setFilter(FILTER_ALL)
                    1 -> setFilter(FILTER_USER)
                    2 -> setFilter(FILTER_SYSTEM)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        
        findViewById<ImageButton>(R.id.btn_sort).setOnClickListener {
            loadApps()
        }
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
        updateAppList()
    }
    
    private fun filterApps(query: String) {
        val filtered = appList.filter { 
            it.appName.contains(query, ignoreCase = true) ||
            it.packageName.contains(query, ignoreCase = true)
        }
        appAdapter.updateList(filtered)
    }
    
    private fun updateAppList() {
        val filtered = when (currentFilter) {
            FILTER_USER -> appList.filter { !it.isSystemApp }
            FILTER_SYSTEM -> appList.filter { it.isSystemApp }
            else -> appList
        }
        
        appAdapter = AppAdapter(filtered) { app ->
            showAppOptions(app)
        }
        recyclerView.adapter = appAdapter
    }
    
    private fun showAppOptions(app: AppInfo) {
        val options = arrayOf("打开应用", "应用详情", "卸载")
        android.app.AlertDialog.Builder(this)
            .setTitle(app.appName)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openApp(app.packageName)
                    1 -> openAppDetails(app.packageName)
                    2 -> uninstallApp(app.packageName)
                }
            }
            .show()
    }
    
    private fun openApp(packageName: String) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                startActivity(intent)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开应用", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun openAppDetails(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开应用详情", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun uninstallApp(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法卸载应用", Toast.LENGTH_SHORT).show()
        }
    }
    
    // 应用适配器
    inner class AppAdapter(
        private var apps: List<AppInfo>,
        private val onClick: (AppInfo) -> Unit
    ) : RecyclerView.Adapter<AppAdapter.ViewHolder>() {
        
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.app_icon)
            val name: TextView = view.findViewById(R.id.app_name)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_app_grid, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = apps[position]
            holder.icon.setImageDrawable(app.icon)
            holder.name.text = app.appName
            holder.itemView.setOnClickListener { onClick(app) }
        }
        
        override fun getItemCount() = apps.size
        
        fun updateList(newApps: List<AppInfo>) {
            apps = newApps
            notifyDataSetChanged()
        }
    }
}
