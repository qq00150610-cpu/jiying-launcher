package com.jiying.launcher.ui.apps

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.jiying.launcher.R
import com.jiying.launcher.data.model.AppInfo
import java.io.InputStream
import java.net.URL
import java.util.concurrent.Executors

/**
 * 应用商店适配器
 */
class AppStoreAdapter(
    private var appList: List<AppInfo>,
    private val activity: AppStoreActivity
) : RecyclerView.Adapter<AppStoreAdapter.AppViewHolder>() {

    private val executor = Executors.newCachedThreadPool()
    private val iconCache = mutableMapOf<String, Drawable>()

    class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iconImageView: ImageView = itemView.findViewById(R.id.app_icon)
        val nameTextView: TextView = itemView.findViewById(R.id.app_name)
        val sizeTextView: TextView = itemView.findViewById(R.id.app_size)
        val statusBtn: Button = itemView.findViewById(R.id.status_btn)
        val ratingTextView: TextView = itemView.findViewById(R.id.app_rating)
        val cardView: androidx.cardview.widget.CardView = itemView.findViewById(R.id.app_card)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_store, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = appList[position]
        
        holder.nameTextView.text = app.name
        holder.sizeTextView.text = app.getFormattedSize()
        holder.ratingTextView.text = "${app.rating}⭐"
        
        // 设置状态按钮
        if (app.isInstalled) {
            holder.statusBtn.text = "打开"
            holder.statusBtn.setBackgroundResource(R.drawable.bg_btn_primary)
        } else {
            holder.statusBtn.text = "下载"
            holder.statusBtn.setBackgroundResource(R.drawable.bg_btn_outline)
        }
        
        // 加载图标
        loadIcon(app, holder.iconImageView)
        
        // 点击事件
        holder.cardView.setOnClickListener {
            activity.showAppDetail(app)
        }
        
        holder.statusBtn.setOnClickListener {
            activity.downloadApp(app)
        }
    }

    override fun getItemCount(): Int = appList.size

    /**
     * 更新列表
     */
    fun updateList(newList: List<AppInfo>) {
        appList = newList
        notifyDataSetChanged()
    }

    /**
     * 加载图标
     */
    private fun loadIcon(app: AppInfo, imageView: ImageView) {
        // 先检查缓存
        iconCache[app.packageName]?.let {
            imageView.setImageDrawable(it)
            return
        }
        
        // 先显示默认图标
        imageView.setImageResource(R.drawable.ic_app_store)
        
        // 尝试从已安装应用获取图标
        try {
            val appInfo = activity.packageManager.getApplicationInfo(app.packageName, 0)
            val icon = activity.packageManager.getApplicationIcon(appInfo)
            imageView.setImageDrawable(icon)
            iconCache[app.packageName] = icon
            return
        } catch (e: Exception) {
            // 应用未安装，从网络加载
        }
        
        // 从网络加载图标
        if (app.iconUrl.isNotEmpty()) {
            executor.execute {
                try {
                    val inputStream: InputStream = URL(app.iconUrl).openStream()
                    val drawable = Drawable.createFromStream(inputStream, null)
                    inputStream.close()
                    
                    if (drawable != null) {
                        iconCache[app.packageName] = drawable
                        imageView.post {
                            imageView.setImageDrawable(drawable)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
