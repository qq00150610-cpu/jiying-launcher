package com.jiying.launcher.ui.apps

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.jiying.launcher.R
import com.jiying.launcher.data.model.AppInfo
import com.jiying.launcher.data.model.AppCategory
import com.jiying.launcher.data.model.AppStoreConfig
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * 极影桌面 - 应用商店
 * 从阿里云OSS服务器获取应用列表并提供下载服务
 */
class AppStoreActivity : AppCompatActivity() {

    // UI组件
    private lateinit var searchEditText: EditText
    private lateinit var searchBtn: ImageView
    private lateinit var categoryTabs: LinearLayout
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var appsRecyclerView: RecyclerView
    private lateinit var loadingLayout: LinearLayout
    private lateinit var emptyLayout: LinearLayout
    private lateinit var backBtn: ImageView
    
    // 数据
    private val appList = mutableListOf<AppInfo>()
    private val filteredList = mutableListOf<AppInfo>()
    private val categories = mutableListOf<AppCategory>()
    private var currentCategory = "all"
    private var currentSearchQuery = ""
    
    // 适配器
    private lateinit var appAdapter: AppStoreAdapter
    
    // 线程池
    private val executor = Executors.newCachedThreadPool()
    private val handler = Handler(Looper.getMainLooper())
    
    // 下载状态
    private val downloadProgressMap = mutableMapOf<String, Int>()
    private var downloadReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_store)
        
        hideSystemUI()
        initViews()
        setupListeners()
        registerDownloadReceiver()
        loadAppsFromServer()
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
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
        searchEditText = findViewById(R.id.search_edit_text)
        searchBtn = findViewById(R.id.search_btn)
        categoryTabs = findViewById(R.id.category_tabs)
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)
        appsRecyclerView = findViewById(R.id.apps_recycler_view)
        loadingLayout = findViewById(R.id.loading_layout)
        emptyLayout = findViewById(R.id.empty_layout)
        backBtn = findViewById(R.id.back_btn)
        
        // 设置RecyclerView
        appsRecyclerView.layoutManager = GridLayoutManager(this, 3)
        appAdapter = AppStoreAdapter(appList, this)
        appsRecyclerView.adapter = appAdapter
        
        // 设置下拉刷新
        swipeRefreshLayout.setColorSchemeResources(R.color.primary_color)
    }

    private fun setupListeners() {
        backBtn.setOnClickListener { finish() }
        
        searchBtn.setOnClickListener {
            currentSearchQuery = searchEditText.text.toString().trim()
            filterApps()
        }
        
        searchEditText.setOnEditorActionListener { _, _, _ ->
            currentSearchQuery = searchEditText.text.toString().trim()
            filterApps()
            true
        }
        
        swipeRefreshLayout.setOnRefreshListener {
            loadAppsFromServer()
        }
    }

    /**
     * 从服务器加载应用列表
     */
    private fun loadAppsFromServer() {
        showLoading()
        
        executor.execute {
            try {
                val url = URL("${AppStoreConfig.DEFAULT_BASE_URL}${AppStoreConfig.ENDPOINT_APPS}")
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = AppStoreConfig().timeout
                connection.readTimeout = AppStoreConfig().timeout
                
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.readText()
                    reader.close()
                    
                    parseAndDisplayApps(response)
                } else {
                    // 使用本地示例数据
                    loadLocalApps()
                }
                
                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
                // 网络失败时加载本地示例数据
                loadLocalApps()
            }
        }
    }

    /**
     * 解析并显示应用列表
     */
    private fun parseAndDisplayApps(jsonString: String) {
        try {
            val json = JSONObject(jsonString)
            val appsArray = json.optJSONArray("apps") ?: return
            
            val apps = mutableListOf<AppInfo>()
            
            for (i in 0 until appsArray.length()) {
                val appJson = appsArray.getJSONObject(i)
                val app = AppInfo(
                    id = appJson.optString("id"),
                    name = appJson.optString("name"),
                    packageName = appJson.optString("packageName"),
                    versionName = appJson.optString("versionName", "1.0.0"),
                    versionCode = appJson.optLong("versionCode", 1),
                    size = appJson.optLong("size", 0),
                    downloadUrl = appJson.optString("downloadUrl"),
                    iconUrl = appJson.optString("iconUrl"),
                    description = appJson.optString("description", ""),
                    category = appJson.optString("category", "other"),
                    developer = appJson.optString("developer", ""),
                    rating = appJson.optDouble("rating", 0.0).toFloat(),
                    downloadCount = appJson.optLong("downloadCount", 0),
                    updateTime = appJson.optString("updateTime", ""),
                    md5 = appJson.optString("md5", "")
                )
                apps.add(app)
            }
            
            handler.post {
                appList.clear()
                appList.addAll(apps)
                checkInstalledApps()
                filterApps()
                hideLoading()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            loadLocalApps()
        }
    }

    /**
     * 加载本地示例应用数据
     */
    private fun loadLocalApps() {
        val localApps = listOf(
            AppInfo(
                id = "1",
                name = "高德地图",
                packageName = "com.autonavi.minimap",
                versionName = "11.0.0",
                versionCode = 1100,
                size = 85 * 1024 * 1024,
                downloadUrl = "${AppStoreConfig.DEFAULT_BASE_URL}/apps/amap.apk",
                iconUrl = "${AppStoreConfig.DEFAULT_BASE_URL}/icons/amap.png",
                description = "中国领先的地图导航应用，提供精准导航服务",
                category = "navigation",
                developer = "高德软件",
                rating = 4.8f,
                downloadCount = 500000000,
                updateTime = "2026-04-01"
            ),
            AppInfo(
                id = "2",
                name = "酷我音乐",
                packageName = "cn.kuwo.player",
                versionName = "10.5.0",
                versionCode = 1050,
                size = 45 * 1024 * 1024,
                downloadUrl = "${AppStoreConfig.DEFAULT_BASE_URL}/apps/kuwo.apk",
                iconUrl = "${AppStoreConfig.DEFAULT_BASE_URL}/icons/kuwo.png",
                description = "海量正版音乐，无损音质播放",
                category = "music",
                developer = "酷我音乐",
                rating = 4.6f,
                downloadCount = 300000000,
                updateTime = "2026-03-28"
            ),
            AppInfo(
                id = "3",
                name = "网易云音乐",
                packageName = "com.netease.cloudmusic",
                versionName = "8.8.0",
                versionCode = 880,
                size = 55 * 1024 * 1024,
                downloadUrl = "${AppStoreConfig.DEFAULT_BASE_URL}/apps/netease_music.apk",
                iconUrl = "${AppStoreConfig.DEFAULT_BASE_URL}/icons/netease_music.png",
                description = "音乐社区，发现好音乐",
                category = "music",
                developer = "网易",
                rating = 4.9f,
                downloadCount = 400000000,
                updateTime = "2026-04-05"
            ),
            AppInfo(
                id = "4",
                name = "QQ音乐",
                packageName = "com.tencent.qqmusic",
                versionName = "12.0.0",
                versionCode = 1200,
                size = 50 * 1024 * 1024,
                downloadUrl = "${AppStoreConfig.DEFAULT_BASE_URL}/apps/qqmusic.apk",
                iconUrl = "${AppStoreConfig.DEFAULT_BASE_URL}/icons/qqmusic.png",
                description = "腾讯旗下音乐播放器",
                category = "music",
                developer = "腾讯",
                rating = 4.7f,
                downloadCount = 450000000,
                updateTime = "2026-04-02"
            ),
            AppInfo(
                id = "5",
                name = "喜马拉雅",
                packageName = "com.ximalaya.ting.android",
                versionName = "9.0.0",
                versionCode = 900,
                size = 60 * 1024 * 1024,
                downloadUrl = "${AppStoreConfig.DEFAULT_BASE_URL}/apps/ximalaya.apk",
                iconUrl = "${AppStoreConfig.DEFAULT_BASE_URL}/icons/ximalaya.png",
                description = "有声书、相声、电台",
                category = "audio",
                developer = "喜马拉雅",
                rating = 4.7f,
                downloadCount = 350000000,
                updateTime = "2026-03-30"
            ),
            AppInfo(
                id = "6",
                name = "腾讯视频",
                packageName = "com.tencent.qqlive",
                versionName = "8.9.0",
                versionCode = 890,
                size = 75 * 1024 * 1024,
                downloadUrl = "${AppStoreConfig.DEFAULT_BASE_URL}/apps/tencent_video.apk",
                iconUrl = "${AppStoreConfig.DEFAULT_BASE_URL}/icons/tencent_video.png",
                description = "热门影视剧、综艺、动漫",
                category = "video",
                developer = "腾讯",
                rating = 4.6f,
                downloadCount = 600000000,
                updateTime = "2026-04-08"
            ),
            AppInfo(
                id = "7",
                name = "爱奇艺",
                packageName = "com.qiyi.video",
                versionName = "15.0.0",
                versionCode = 1500,
                size = 70 * 1024 * 1024,
                downloadUrl = "${AppStoreConfig.DEFAULT_BASE_URL}/apps/iqiyi.apk",
                iconUrl = "${AppStoreConfig.DEFAULT_BASE_URL}/icons/iqiyi.png",
                description = "高清视频，海量影视剧",
                category = "video",
                developer = "爱奇艺",
                rating = 4.5f,
                downloadCount = 550000000,
                updateTime = "2026-04-06"
            ),
            AppInfo(
                id = "8",
                name = "百度网盘",
                packageName = "com.baidu.netdisk",
                versionName = "11.0.0",
                versionCode = 1100,
                size = 40 * 1024 * 1024,
                downloadUrl = "${AppStoreConfig.DEFAULT_BASE_URL}/apps/baidu_netdisk.apk",
                iconUrl = "${AppStoreConfig.DEFAULT_BASE_URL}/icons/baidu_netdisk.png",
                description = "超大存储空间，文件同步备份",
                category = "tools",
                developer = "百度",
                rating = 4.4f,
                downloadCount = 700000000,
                updateTime = "2026-04-10"
            ),
            AppInfo(
                id = "9",
                name = "微信",
                packageName = "com.tencent.mm",
                versionName = "8.0.0",
                versionCode = 800,
                size = 180 * 1024 * 1024,
                downloadUrl = "${AppStoreConfig.DEFAULT_BASE_URL}/apps/wechat.apk",
                iconUrl = "${AppStoreConfig.DEFAULT_BASE_URL}/icons/wechat.png",
                description = "社交通讯，朋友圈，小程序",
                category = "social",
                developer = "腾讯",
                rating = 4.8f,
                downloadCount = 1200000000,
                updateTime = "2026-04-15"
            )
        )
        
        handler.post {
            appList.clear()
            appList.addAll(localApps)
            checkInstalledApps()
            filterApps()
            hideLoading()
        }
    }

    /**
     * 检查已安装的应用
     */
    private fun checkInstalledApps() {
        for (app in appList) {
            try {
                packageManager.getPackageInfo(app.packageName, 0)
                app.isInstalled = true
            } catch (e: PackageManager.NameNotFoundException) {
                app.isInstalled = false
            }
        }
        appAdapter.notifyDataSetChanged()
    }

    /**
     * 过滤应用列表
     */
    private fun filterApps() {
        filteredList.clear()
        
        for (app in appList) {
            val matchCategory = currentCategory == "all" || app.category == currentCategory
            val matchSearch = currentSearchQuery.isEmpty() || 
                app.name.contains(currentSearchQuery, ignoreCase = true) ||
                app.description.contains(currentSearchQuery, ignoreCase = true)
            
            if (matchCategory && matchSearch) {
                filteredList.add(app)
            }
        }
        
        appAdapter.updateList(filteredList)
        
        if (filteredList.isEmpty()) {
            showEmpty()
        } else {
            hideEmpty()
        }
    }

    /**
     * 显示加载中
     */
    private fun showLoading() {
        loadingLayout.visibility = View.VISIBLE
        appsRecyclerView.visibility = View.GONE
        emptyLayout.visibility = View.GONE
    }

    /**
     * 隐藏加载中
     */
    private fun hideLoading() {
        swipeRefreshLayout.isRefreshing = false
        loadingLayout.visibility = View.GONE
        appsRecyclerView.visibility = View.VISIBLE
    }

    /**
     * 显示空状态
     */
    private fun showEmpty() {
        emptyLayout.visibility = View.VISIBLE
    }

    /**
     * 隐藏空状态
     */
    private fun hideEmpty() {
        emptyLayout.visibility = View.GONE
    }

    /**
     * 注册下载完成广播接收器
     */
    private fun registerDownloadReceiver() {
        downloadReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val downloadId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: -1
                if (downloadId != -1L) {
                    installDownloadedApk(downloadId)
                }
            }
        }
        
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(downloadReceiver, filter)
        }
    }

    /**
     * 下载应用
     */
    fun downloadApp(app: AppInfo) {
        if (app.isInstalled) {
            // 已安装，打开应用
            try {
                val intent = packageManager.getLaunchIntentForPackage(app.packageName)
                if (intent != null) {
                    startActivity(intent)
                    return
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // 使用系统下载管理器
        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        
        val request = DownloadManager.Request(Uri.parse(app.downloadUrl))
            .setTitle(app.name)
            .setDescription("正在下载 ${app.name} v${app.versionName}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, "${app.packageName}.apk")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
        
        val downloadId = downloadManager.enqueue(request)
        
        // 保存下载ID和应用包名的对应关系
        val prefs = getSharedPreferences("downloads", MODE_PRIVATE)
        prefs.edit().putLong(downloadId.toString(), System.currentTimeMillis()).apply()
        
        Toast.makeText(this, "开始下载 ${app.name}", Toast.LENGTH_SHORT).show()
    }

    /**
     * 安装下载完成的APK
     */
    private fun installDownloadedApk(downloadId: Long) {
        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = downloadManager.getUriForDownloadedFile(downloadId)
        
        if (uri != null) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val apkUri = FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    File(uri.path ?: return)
                )
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
            } else {
                intent.setDataAndType(uri, "application/vnd.android.package-archive")
            }
            
            startActivity(intent)
        }
    }

    /**
     * 显示应用详情
     */
    fun showAppDetail(app: AppInfo) {
        AlertDialog.Builder(this)
            .setTitle(app.name)
            .setMessage("""
                版本: ${app.versionName}
                大小: ${app.getFormattedSize()}
                开发者: ${app.developer}
                评分: ${app.rating}⭐
                下载: ${app.getFormattedDownloadCount()}次
                
                ${app.description}
            """.trimIndent())
            .setPositiveButton(if (app.isInstalled) "打开" else "下载") { _, _ ->
                downloadApp(app)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        downloadReceiver?.let { unregisterReceiver(it) }
        executor.shutdown()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}
