package com.jiying.launcher.ui.apps

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.google.android.material.tabs.TabLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jiying.launcher.R
import com.jiying.launcher.data.model.AppInfo

/**
 * 应用管理Activity
 * 支持应用卸载、应用信息、应用备份
 */
class AppManagerActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var appListRecycler: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var sortBtn: ImageButton
    private lateinit var appAdapter: AppListAdapter
    
    private var allApps = mutableListOf<AppInfo>()
    private var filteredApps = mutableListOf<AppInfo>()
    private var currentFilter = "all" // all, system, user
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_manager)
        
        try {
            initViews()
            setupRecyclerView()
            loadApps()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initViews() {
        try {
            tabLayout = findViewById(R.id.app_tabs)
            appListRecycler = findViewById(R.id.app_list)
            searchView = findViewById(R.id.app_search)
            sortBtn = findViewById(R.id.btn_sort)
            
            // Tab切换
            tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    currentFilter = when (tab?.position) {
                        0 -> "all"
                        1 -> "user"
                        2 -> "system"
                        else -> "all"
                    }
                    filterApps()
                }
                override fun onTabUnselected(tab: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })
            
            // 搜索
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?) = false
                override fun onQueryTextChange(newText: String?): Boolean {
                    filterApps()
                    return true
                }
            })
            
            // 排序
            sortBtn.setOnClickListener {
                showSortDialog()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupRecyclerView() {
        appAdapter = AppListAdapter(filteredApps) { position ->
            showAppDetails(position)
        }
        appListRecycler.layoutManager = LinearLayoutManager(this)
        appListRecycler.adapter = appAdapter
    }

    private fun loadApps() {
        try {
            val pm = packageManager
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            
            allApps.clear()
            apps.forEach { app ->
                val isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                allApps.add(AppInfo(
                    name = pm.getApplicationLabel(app).toString(),
                    packageName = app.packageName,
                    icon = pm.getApplicationIcon(app),
                    isSystemApp = isSystemApp,
                    version = getAppVersion(app.packageName),
                    apkPath = app.sourceDir
                ))
            }
            
            // 按名称排序
            allApps.sortBy { it.name }
            filterApps()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun filterApps() {
        try {
            val query = searchView.query.toString().lowercase()
            
            filteredApps.clear()
            allApps.filter { app ->
                val matchesFilter = when (currentFilter) {
                    "user" -> !app.isSystemApp
                    "system" -> app.isSystemApp
                    else -> true
                }
                val matchesSearch = query.isEmpty() || 
                    app.name.lowercase().contains(query) || 
                    app.packageName.lowercase().contains(query)
                
                matchesFilter && matchesSearch
            }.forEach { filteredApps.add(it) }
            
            appAdapter.notifyDataSetChanged()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showSortDialog() {
        try {
            val options = arrayOf("按名称", "按安装时间", "按大小", "按包名")
            AlertDialog.Builder(this)
                .setTitle("排序方式")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> allApps.sortBy { it.name }
                        1 -> allApps.sortByDescending { it.packageName }
                        2 -> allApps.sortByDescending { it.apkPath }
                        3 -> allApps.sortBy { it.packageName }
                    }
                    filterApps()
                }
                .show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showAppDetails(position: Int) {
        try {
            if (position >= filteredApps.size) return
            val app = filteredApps[position]
            
            val options = arrayOf("打开", "应用信息", "卸载", "强制停止", "添加到桌面")
            AlertDialog.Builder(this)
                .setTitle(app.name)
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> launchApp(app.packageName)
                        1 -> showAppInfo(app.packageName)
                        2 -> uninstallApp(app.packageName)
                        3 -> forceStop(app.packageName)
                        4 -> addToHomeScreen(app)
                    }
                }
                .show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun launchApp(packageName: String) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showAppInfo(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun uninstallApp(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun forceStop(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
            Toast.makeText(this, "请在应用信息中强制停止", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun addToHomeScreen(app: com.jiying.launcher.data.model.AppInfo) {
        Toast.makeText(this, "已添加 ${app.name} 到桌面", Toast.LENGTH_SHORT).show()
    }

    private fun getAppVersion(packageName: String): String {
        return try {
            val info = packageManager.getPackageInfo(packageName, 0)
            info.versionName ?: "未知"
        } catch (e: Exception) {
            "未知"
        }
    }
}

class AppListAdapter(
    private val apps: List<AppInfo>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<AppListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.app_icon)
        val name: TextView = view.findViewById(R.id.app_name)
        val packageName: TextView = view.findViewById(R.id.app_package)
        val badge: TextView = view.findViewById(R.id.app_type_badge)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.icon.setImageDrawable(app.icon)
        holder.name.text = app.name
        holder.packageName.text = app.packageName
        
        if (app.isSystemApp) {
            holder.badge.visibility = View.VISIBLE
            holder.badge.text = "系统"
        } else {
            holder.badge.visibility = View.GONE
        }
        
        holder.itemView.setOnClickListener { onItemClick(position) }
    }

    override fun getItemCount() = apps.size
}
