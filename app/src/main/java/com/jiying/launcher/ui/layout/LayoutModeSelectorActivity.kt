package com.jiying.launcher.ui.layout

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jiying.launcher.R
import com.jiying.launcher.util.LayoutModeManager
import com.jiying.launcher.util.ScreenAdapter

/**
 * 极影桌面 - 布局模式选择器
 * 
 * 功能说明：
 * - 6种布局预览图
 * - 实际切换布局逻辑
 * - 保存用户选择
 * - 布局模式：
 *   1. 默认模式
 *   2. 极简模式
 *   3. 地图优先
 *   4. 音乐优先
 *   5. CarPlay风格
 *   6. 混合模式
 * 
 * @author 极影桌面开发团队
 * @version 2.0.0
 */
class LayoutModeSelectorActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var confirmBtn: Button
    private lateinit var cancelBtn: Button
    private lateinit var previewContainer: FrameLayout
    private lateinit var selectedModeText: TextView
    
    private lateinit var layoutAdapter: LayoutModeAdapter
    private val layoutModes = mutableListOf<LayoutMode>()
    private var selectedMode = -1
    
    data class LayoutMode(
        val id: Int,
        val name: String,
        val description: String,
        val iconRes: Int,
        val mode: ScreenAdapter.LayoutMode
    )

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, LayoutModeSelectorActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_layout_mode_selector)
        hideSystemUI()
        initViews()
        loadLayoutModes()
        setupRecyclerView()
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
        recyclerView = findViewById(R.id.layout_mode_list)
        confirmBtn = findViewById(R.id.btn_confirm)
        cancelBtn = findViewById(R.id.btn_cancel)
        previewContainer = findViewById(R.id.preview_container)
        selectedModeText = findViewById(R.id.selected_mode_text)
        
        confirmBtn.setOnClickListener { applyAndFinish() }
        cancelBtn.setOnClickListener { finish() }
        
        // 获取当前选中的模式
        val currentMode = LayoutModeManager.getCurrentLayoutConfig()
        selectedMode = currentMode.mode.ordinal
    }

    private fun loadLayoutModes() {
        layoutModes.add(LayoutMode(
            id = 0,
            name = "默认模式",
            description = "标准车机布局，地图和音乐并排显示",
            iconRes = R.drawable.ic_layout_default,
            mode = ScreenAdapter.LayoutMode.MODE_DEFAULT
        ))
        
        layoutModes.add(LayoutMode(
            id = 1,
            name = "极简模式",
            description = "隐藏大部分卡片，只保留底部Dock",
            iconRes = R.drawable.ic_layout_minimal,
            mode = ScreenAdapter.LayoutMode.MODE_MINIMAL
        ))
        
        layoutModes.add(LayoutMode(
            id = 2,
            name = "地图优先",
            description = "放大地图卡片区域，导航更清晰",
            iconRes = R.drawable.ic_layout_map,
            mode = ScreenAdapter.LayoutMode.MODE_MAP_FOCUS
        ))
        
        layoutModes.add(LayoutMode(
            id = 3,
            name = "音乐优先",
            description = "放大音乐卡片区域，歌词更显眼",
            iconRes = R.drawable.ic_layout_music,
            mode = ScreenAdapter.LayoutMode.MODE_MUSIC_FOCUS
        ))
        
        layoutModes.add(LayoutMode(
            id = 4,
            name = "CarPlay风格",
            description = "仿CarPlay的大图标布局",
            iconRes = R.drawable.ic_layout_carplay,
            mode = ScreenAdapter.LayoutMode.MODE_CARPLAY
        ))
        
        layoutModes.add(LayoutMode(
            id = 5,
            name = "混合模式",
            description = "根据屏幕方向自动调整布局",
            iconRes = R.drawable.ic_layout_hybrid,
            mode = ScreenAdapter.LayoutMode.MODE_HYBRID
        ))
    }

    private fun setupRecyclerView() {
        layoutAdapter = LayoutModeAdapter(layoutModes) { position ->
            onLayoutSelected(position)
        }
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        recyclerView.adapter = layoutAdapter
        
        // 默认选中当前模式
        layoutAdapter.setSelectedPosition(selectedMode)
        updateSelectedText()
    }

    private fun onLayoutSelected(position: Int) {
        selectedMode = position
        layoutAdapter.setSelectedPosition(position)
        updateSelectedText()
        
        // 更新预览
        updatePreview(position)
    }

    private fun updateSelectedText() {
        if (selectedMode >= 0 && selectedMode < layoutModes.size) {
            selectedModeText.text = "已选择：${layoutModes[selectedMode].name}"
        }
    }

    private fun updatePreview(position: Int) {
        // 简单预览效果
        val mode = layoutModes[position]
        Toast.makeText(this, "预览：${mode.name}", Toast.LENGTH_SHORT).show()
    }

    private fun applyAndFinish() {
        if (selectedMode >= 0 && selectedMode < layoutModes.size) {
            val mode = layoutModes[selectedMode].mode
            
            // 保存选择 - 使用正确的参数类型
            LayoutModeManager.setLayoutMode(mode.ordinal)
            
            // 发送广播通知主界面更新
            val intent = Intent("com.jiying.launcher.LAYOUT_MODE_CHANGED")
            intent.putExtra("mode", mode.name)
            sendBroadcast(intent)
            
            // 提示重启生效
            AlertDialog.Builder(this)
                .setTitle("布局已切换")
                .setMessage("${layoutModes[selectedMode].name}已应用。部分更改需要重启桌面才能完全生效。")
                .setPositiveButton("重启桌面") { _, _ ->
                    restartDesktop()
                }
                .setNegativeButton("稍后") { _, _ ->
                    finish()
                }
                .show()
        } else {
            finish()
        }
    }

    private fun restartDesktop() {
        try {
            // 发送广播重启桌面
            val intent = Intent("android.intent.action.MAIN")
            intent.addCategory("android.intent.category.HOME")
            intent.addCategory("android.intent.category.DEFAULT")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            
            // 先启动主桌面
            val pm = packageManager
            val resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            
            // 如果有其他桌面，切换到它再切换回来
            if (resolveInfo != null && resolveInfo.activityInfo.packageName != packageName) {
                startActivity(intent)
                Handler(android.os.Looper.getMainLooper()).postDelayed({
                    // 重新启动当前桌面
                    val restartIntent = Intent(this, com.jiying.launcher.ui.main.MainActivity::class.java)
                    restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(restartIntent)
                }, 500)
            }
            
            finish()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "重启失败，请手动重启", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    // ========== 布局模式适配器 ==========
    
    inner class LayoutModeAdapter(
        private val items: List<LayoutMode>,
        private val onItemClick: (Int) -> Unit
    ) : RecyclerView.Adapter<LayoutModeAdapter.LayoutViewHolder>() {
        
        private var selectedPosition = -1
        
        fun setSelectedPosition(position: Int) {
            val oldPosition = selectedPosition
            selectedPosition = position
            if (oldPosition >= 0) notifyItemChanged(oldPosition)
            notifyItemChanged(position)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): LayoutViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_layout_mode, parent, false)
            return LayoutViewHolder(view)
        }

        override fun onBindViewHolder(holder: LayoutViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size

        inner class LayoutViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val cardView: CardView = itemView.findViewById(R.id.layout_card)
            private val iconView: ImageView = itemView.findViewById(R.id.layout_icon)
            private val nameView: TextView = itemView.findViewById(R.id.layout_name)
            private val descView: TextView = itemView.findViewById(R.id.layout_desc)
            private val checkView: ImageView = itemView.findViewById(R.id.check_icon)

            fun bind(mode: LayoutMode) {
                iconView.setImageResource(mode.iconRes)
                nameView.text = mode.name
                descView.text = mode.description
                
                // 选中状态
                if (adapterPosition == selectedPosition) {
                    cardView.setCardBackgroundColor(
                        resources.getColor(R.color.accent_primary_transparent, theme)
                    )
                    checkView.visibility = View.VISIBLE
                } else {
                    cardView.setCardBackgroundColor(
                        resources.getColor(R.color.card_background, theme)
                    )
                    checkView.visibility = View.GONE
                }
                
                itemView.setOnClickListener { onItemClick(adapterPosition) }
            }
        }
    }
}
