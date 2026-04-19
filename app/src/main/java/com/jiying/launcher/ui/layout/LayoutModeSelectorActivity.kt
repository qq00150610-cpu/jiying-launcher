package com.jiying.launcher.ui.layout

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.jiying.launcher.R
import com.jiying.launcher.util.LayoutModeManager
import com.jiying.launcher.util.ScreenAdapter

/**
 * 极影桌面 - 布局模式选择器
 * 
 * 功能说明：
 * - 显示6种布局模式供选择
 * - 提供布局预览效果
 * - 支持一键切换布局
 * - 兼容Android 6.0 (API 23) 及以上系统
 * 
 * @author 极影桌面开发团队
 * @version 1.0.0
 */
class LayoutModeSelectorActivity : AppCompatActivity() {
    
    companion object {
        /**
         * 启动布局选择器
         */
        fun start(context: Context) {
            val intent = Intent(context, LayoutModeSelectorActivity::class.java)
            if (context !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
    
    // UI组件
    private lateinit var titleBar: LinearLayout
    private lateinit var backBtn: ImageView
    private lateinit var titleText: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var currentModeText: TextView
    private lateinit var applyBtn: Button
    
    // 数据
    private lateinit var adapter: LayoutModeAdapter
    private var selectedMode = -1
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_layout_mode_selector)
        
        // 初始化管理器
        LayoutModeManager.init(this)
        ScreenAdapter.init(this)
        
        // 获取当前布局模式
        selectedMode = LayoutModeManager.getCurrentLayoutMode()
        
        initViews()
        setupRecyclerView()
        updateCurrentModeDisplay()
    }
    
    private fun initViews() {
        titleBar = findViewById(R.id.title_bar)
        backBtn = findViewById(R.id.btn_back)
        titleText = findViewById(R.id.title_text)
        recyclerView = findViewById(R.id.layout_mode_list)
        currentModeText = findViewById(R.id.current_mode_text)
        applyBtn = findViewById(R.id.btn_apply)
        
        // 返回按钮
        backBtn.setOnClickListener {
            finish()
        }
        
        // 立即应用按钮
        applyBtn.setOnClickListener {
            if (selectedMode != -1) {
                LayoutModeManager.setLayoutMode(selectedMode)
                Toast.makeText(this, "布局已切换", Toast.LENGTH_SHORT).show()
                
                // 可选：重启主界面以应用新布局
                restartMainActivity()
            }
        }
    }
    
    private fun setupRecyclerView() {
        adapter = LayoutModeAdapter(this, selectedMode) { mode, isSelected ->
            selectedMode = mode
            adapter.updateSelectedMode(mode)
            updateCurrentModeDisplay()
        }
        recyclerView.adapter = adapter
        
        // 设置布局管理器
        val layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 2)
        recyclerView.layoutManager = layoutManager
    }
    
    private fun updateCurrentModeDisplay() {
        val currentMode = LayoutModeManager.getCurrentLayoutMode()
        val modeName = ScreenAdapter.getLayoutModeName(currentMode)
        currentModeText.text = "当前模式: $modeName"
    }
    
    private fun restartMainActivity() {
        val intent = Intent(this, com.jiying.launcher.ui.main.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
    
    /**
     * 布局模式适配器
     */
    inner class LayoutModeAdapter(
        private val context: Context,
        private var selectedMode: Int,
        private val onItemClick: (Int, Boolean) -> Unit
    ) : RecyclerView.Adapter<LayoutModeAdapter.ViewHolder>() {
        
        private val modes = LayoutModeManager.getAllLayoutModes()
        
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val cardView: CardView = itemView.findViewById(R.id.mode_card)
            val previewView: View = itemView.findViewById(R.id.layout_preview)
            val modeNameText: TextView = itemView.findViewById(R.id.mode_name)
            val modeDescText: TextView = itemView.findViewById(R.id.mode_description)
            val checkIcon: ImageView = itemView.findViewById(R.id.check_icon)
            val recommendedTag: TextView = itemView.findViewById(R.id.recommended_tag)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_layout_mode, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val (mode, name) = modes[position]
            val description = LayoutModeManager.getLayoutModeDescription(mode)
            val previewIcon = LayoutModeManager.getLayoutPreviewIcon(mode)
            val config = LayoutModeManager.getLayoutConfig(mode)
            
            holder.modeNameText.text = name
            holder.modeDescText.text = description
            
            // 设置预览图标
            try {
                val resId = context.resources.getIdentifier(previewIcon, "drawable", context.packageName)
                if (resId != 0) {
                    holder.previewView.setBackgroundResource(resId)
                }
            } catch (e: Exception) {
                // 使用默认预览
            }
            
            // 推荐标签
            val recommendedMode = ScreenAdapter.getCurrentConfig().recommendedLayoutMode
            holder.recommendedTag.visibility = if (mode == recommendedMode) View.VISIBLE else View.GONE
            
            // 选择状态
            val isSelected = mode == selectedMode
            holder.checkIcon.visibility = if (isSelected) View.VISIBLE else View.GONE
            holder.cardView.strokeWidth = if (isSelected) 3 else 0
            
            // 点击事件
            holder.cardView.setOnClickListener {
                onItemClick(mode, true)
            }
        }
        
        override fun getItemCount() = modes.size
        
        fun updateSelectedMode(mode: Int) {
            val oldSelected = selectedMode
            selectedMode = mode
            notifyItemChanged(modes.indexOfFirst { it.first == oldSelected })
            notifyItemChanged(modes.indexOfFirst { it.first == mode })
        }
    }
}
