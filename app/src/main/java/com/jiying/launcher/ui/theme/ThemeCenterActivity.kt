package com.jiying.launcher.ui.theme

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jiying.launcher.R

/**
 * 主题中心Activity
 * 支持主题切换、主题预览、主题下载
 */
class ThemeCenterActivity : AppCompatActivity() {

    private lateinit var themeList: RecyclerView
    private lateinit var themeAdapter: ThemeGridAdapter
    private lateinit var currentThemeCard: CardView
    private lateinit var currentThemeName: TextView
    
    private val themeItems = mutableListOf<ThemeItem>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_theme_center)
        
        try {
            initViews()
            setupRecyclerView()
            loadThemes()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initViews() {
        try {
            themeList = findViewById(R.id.theme_list)
            currentThemeCard = findViewById(R.id.current_theme_card)
            currentThemeName = findViewById(R.id.current_theme_name)
            
            // 显示当前主题
            currentThemeName.text = "布丁UI主题"
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupRecyclerView() {
        themeAdapter = ThemeGridAdapter(themeItems) { position ->
            applyTheme(position)
        }
        themeList.layoutManager = GridLayoutManager(this, 2)
        themeList.adapter = themeAdapter
    }

    private fun loadThemes() {
        try {
            // 添加预设主题
            themeItems.add(ThemeItem(
                name = "布丁UI主题",
                preview = "pudding_theme",
                isApplied = true
            ))
            themeItems.add(ThemeItem(
                name = "氢桌面主题",
                preview = "hydrogen_theme",
                isApplied = false
            ))
            themeItems.add(ThemeItem(
                name = "星空主题",
                preview = "star_theme",
                isApplied = false
            ))
            themeItems.add(ThemeItem(
                name = "极简主题",
                preview = "minimal_theme",
                isApplied = false
            ))
            themeItems.add(ThemeItem(
                name = "赛博朋克",
                preview = "cyberpunk_theme",
                isApplied = false
            ))
            themeItems.add(ThemeItem(
                name = "赛车主题",
                preview = "racing_theme",
                isApplied = false
            ))
            
            // 在线主题（模拟）
            themeItems.add(ThemeItem(
                name = "在线主题1",
                preview = "online_theme_1",
                isDownloaded = false
            ))
            themeItems.add(ThemeItem(
                name = "在线主题2",
                preview = "online_theme_2",
                isDownloaded = false
            ))
            
            themeAdapter.notifyDataSetChanged()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun applyTheme(position: Int) {
        try {
            if (position < themeItems.size) {
                val theme = themeItems[position]
                
                if (theme.isDownloaded == false && theme.isApplied == false) {
                    Toast.makeText(this, "正在下载主题: ${theme.name}", Toast.LENGTH_SHORT).show()
                    return
                }
                
                // 更新主题应用状态
                themeItems.forEach { it.isApplied = false }
                theme.isApplied = true
                themeAdapter.notifyDataSetChanged()
                
                currentThemeName.text = theme.name
                Toast.makeText(this, "已应用主题: ${theme.name}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

data class ThemeItem(
    val name: String,
    val preview: String,
    var isApplied: Boolean = false,
    var isDownloaded: Boolean = true
)

class ThemeGridAdapter(
    private val themes: List<ThemeItem>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<ThemeGridAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val preview: ImageView = view.findViewById(R.id.theme_preview)
        val name: TextView = view.findViewById(R.id.theme_name)
        val appliedBadge: TextView = view.findViewById(R.id.theme_applied_badge)
        val downloadBtn: Button = view.findViewById(R.id.btn_download)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_theme_grid, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = themes[position]
        holder.name.text = item.name
        
        // 设置预览图背景色（根据主题名称）
        val bgColor = when {
            item.name.contains("布丁") -> "#FF6B6B"
            item.name.contains("氢") -> "#4ECDC4"
            item.name.contains("星空") -> "#1A1A2E"
            item.name.contains("极简") -> "#FFFFFF"
            item.name.contains("赛博") -> "#00FF00"
            item.name.contains("赛车") -> "#FF4500"
            else -> "#2D2D44"
        }
        holder.preview.setBackgroundColor(android.graphics.Color.parseColor(bgColor))
        
        // 显示应用状态
        if (item.isApplied) {
            holder.appliedBadge.visibility = View.VISIBLE
            holder.downloadBtn.visibility = View.GONE
        } else {
            holder.appliedBadge.visibility = View.GONE
            holder.downloadBtn.visibility = if (item.isDownloaded) View.GONE else View.VISIBLE
        }
        
        holder.itemView.setOnClickListener { onItemClick(position) }
    }

    override fun getItemCount() = themes.size
}
