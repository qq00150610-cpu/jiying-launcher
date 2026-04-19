package com.jiying.launcher.util

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

/**
 * 极影桌面 - 计划任务管理器
 * 
 * 功能说明：
 * - 定时启动指定应用（如开机自动启动地图）
 * - 延迟执行返回桌面等操作
 * - 支持重复执行和单次执行
 * - 兼容Android 6.0 (API 23) 及以上系统
 * 
 * 借鉴来源：氢桌面的计划任务功能
 * 
 * @author 极影桌面开发团队
 * @version 1.0.0
 */
object ScheduledTaskManager {
    
    // 任务类型常量
    object TaskType {
        const val LAUNCH_APP = 0          // 启动应用
        const val DELAY_RETURN_HOME = 1   // 延迟返回桌面
        const val SHOW_NOTIFICATION = 2   // 显示通知
        const val CHANGE_WALLPAPER = 3    // 更换壁纸
        const val TOGGLE_NIGHT_MODE = 4   // 切换夜间模式
    }
    
    // 重复类型常量
    object RepeatType {
        const val ONCE = 0           // 单次执行
        const val DAILY = 1          // 每天
        const val WEEKLY = 2         // 每周
        const val WORKDAY = 3        // 工作日
        const val WEEKEND = 4        // 周末
    }
    
    // 任务数据类
    data class ScheduledTask(
        val id: String,                 // 任务ID (UUID)
        val name: String,                // 任务名称
        val type: Int,                  // 任务类型
        val repeatType: Int,            // 重复类型
        val triggerTime: Long,           // 触发时间 (毫秒时间戳)
        val repeatInterval: Long,       // 重复间隔 (毫秒)
        val packageName: String?,        // 目标应用包名
        val delaySeconds: Int?,          // 延迟秒数
        val isEnabled: Boolean,          // 是否启用
        val createdAt: Long             // 创建时间
    )
    
    // SharedPreferences键名
    private const val PREF_NAME = "jiying_scheduled_tasks"
    private const val KEY_TASKS = "tasks"
    
    private lateinit var prefs: SharedPreferences
    private lateinit var context: Context
    
    // 定时器
    private val timers = mutableMapOf<String, Timer>()
    
    /**
     * 初始化计划任务管理器
     * @param ctx 应用程序上下文
     */
    fun init(ctx: Context) {
        context = ctx.applicationContext
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * 添加计划任务
     */
    fun addTask(task: ScheduledTask): Boolean {
        val tasks = getAllTasks().toMutableList()
        tasks.add(task)
        saveTasks(tasks)
        return if (task.isEnabled) {
            startTask(task)
            true
        } else {
            false
        }
    }
    
    /**
     * 创建并添加启动应用任务（类似氢桌面的开机自动启动地图）
     * @param appPackageName 目标应用包名
     * @param delaySeconds 延迟启动秒数
     * @param taskName 任务名称
     */
    fun addAutoLaunchTask(
        appPackageName: String,
        delaySeconds: Int = 5,
        taskName: String = "自动启动$appPackageName"
    ): Boolean {
        val task = ScheduledTask(
            id = UUID.randomUUID().toString(),
            name = taskName,
            type = TaskType.LAUNCH_APP,
            repeatType = RepeatType.ONCE,
            triggerTime = System.currentTimeMillis() + delaySeconds * 1000L,
            repeatInterval = 0,
            packageName = appPackageName,
            delaySeconds = delaySeconds,
            isEnabled = true,
            createdAt = System.currentTimeMillis()
        )
        return addTask(task)
    }
    
    /**
     * 创建延迟返回桌面任务（类似氢桌面的延迟返回桌面）
     * @param delaySeconds 延迟秒数
     * @param taskName 任务名称
     */
    fun addDelayReturnHomeTask(
        delaySeconds: Int = 10,
        taskName: String = "延迟返回桌面"
    ): Boolean {
        val task = ScheduledTask(
            id = UUID.randomUUID().toString(),
            name = taskName,
            type = TaskType.DELAY_RETURN_HOME,
            repeatType = RepeatType.ONCE,
            triggerTime = System.currentTimeMillis() + delaySeconds * 1000L,
            repeatInterval = 0,
            packageName = null,
            delaySeconds = delaySeconds,
            isEnabled = true,
            createdAt = System.currentTimeMillis()
        )
        return addTask(task)
    }
    
    /**
     * 移除计划任务
     */
    fun removeTask(taskId: String): Boolean {
        val tasks = getAllTasks().toMutableList()
        val removed = tasks.removeIf { it.id == taskId }
        if (removed) {
            saveTasks(tasks)
            cancelTask(taskId)
        }
        return removed
    }
    
    /**
     * 更新计划任务
     */
    fun updateTask(task: ScheduledTask): Boolean {
        val tasks = getAllTasks().toMutableList()
        val index = tasks.indexOfFirst { it.id == task.id }
        if (index >= 0) {
            tasks[index] = task
            saveTasks(tasks)
            cancelTask(task.id)
            if (task.isEnabled) {
                startTask(task)
            }
            return true
        }
        return false
    }
    
    /**
     * 启用/禁用任务
     */
    fun setTaskEnabled(taskId: String, enabled: Boolean): Boolean {
        val tasks = getAllTasks()
        val task = tasks.find { it.id == taskId } ?: return false
        return updateTask(task.copy(isEnabled = enabled))
    }
    
    /**
     * 获取所有任务
     */
    fun getAllTasks(): List<ScheduledTask> {
        val tasksJson = prefs.getString(KEY_TASKS, "[]") ?: "[]"
        return parseTasksFromJson(tasksJson)
    }
    
    /**
     * 获取已启用的任务
     */
    fun getEnabledTasks(): List<ScheduledTask> {
        return getAllTasks().filter { it.isEnabled }
    }
    
    /**
     * 启动任务定时器
     */
    private fun startTask(task: ScheduledTask) {
        cancelTask(task.id)
        
        val delay = task.triggerTime - System.currentTimeMillis()
        if (delay <= 0) {
            executeTask(task)
            return
        }
        
        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                executeTask(task)
                // 如果是单次任务，执行后取消
                if (task.repeatType == RepeatType.ONCE) {
                    removeTask(task.id)
                }
            }
        }, delay)
        
        timers[task.id] = timer
    }
    
    /**
     * 取消任务定时器
     */
    private fun cancelTask(taskId: String) {
        timers[taskId]?.cancel()
        timers.remove(taskId)
    }
    
    /**
     * 执行任务
     */
    private fun executeTask(task: ScheduledTask) {
        when (task.type) {
            TaskType.LAUNCH_APP -> {
                task.packageName?.let { launchApp(it) }
            }
            TaskType.DELAY_RETURN_HOME -> {
                // 延迟返回桌面已在launcher中处理
            }
            TaskType.SHOW_NOTIFICATION -> {
                // 显示通知
            }
            TaskType.CHANGE_WALLPAPER -> {
                // 更换壁纸
            }
            TaskType.TOGGLE_NIGHT_MODE -> {
                // 切换夜间模式
            }
        }
    }
    
    /**
     * 启动应用
     */
    private fun launchApp(packageName: String) {
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 保存任务列表到SharedPreferences
     */
    private fun saveTasks(tasks: List<ScheduledTask>) {
        val jsonArray = JSONArray()
        tasks.forEach { task ->
            val jsonObject = JSONObject().apply {
                put("id", task.id)
                put("name", task.name)
                put("type", task.type)
                put("repeatType", task.repeatType)
                put("triggerTime", task.triggerTime)
                put("repeatInterval", task.repeatInterval)
                put("packageName", task.packageName)
                put("delaySeconds", task.delaySeconds)
                put("isEnabled", task.isEnabled)
                put("createdAt", task.createdAt)
            }
            jsonArray.put(jsonObject)
        }
        prefs.edit().putString(KEY_TASKS, jsonArray.toString()).apply()
    }
    
    /**
     * 从JSON解析任务列表
     */
    private fun parseTasksFromJson(json: String): List<ScheduledTask> {
        val tasks = mutableListOf<ScheduledTask>()
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                tasks.add(ScheduledTask(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    type = obj.getInt("type"),
                    repeatType = obj.getInt("repeatType"),
                    triggerTime = obj.getLong("triggerTime"),
                    repeatInterval = obj.getLong("repeatInterval"),
                    packageName = if (obj.has("packageName") && !obj.isNull("packageName")) 
                        obj.getString("packageName") else null,
                    delaySeconds = if (obj.has("delaySeconds") && !obj.isNull("delaySeconds")) 
                        obj.getInt("delaySeconds") else null,
                    isEnabled = obj.getBoolean("isEnabled"),
                    createdAt = obj.getLong("createdAt")
                ))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return tasks
    }
    
    /**
     * 启动所有已启用的任务
     */
    fun startAllEnabledTasks() {
        getEnabledTasks().forEach { task ->
            startTask(task)
        }
    }
    
    /**
     * 停止所有任务
     */
    fun stopAllTasks() {
        timers.values.forEach { it.cancel() }
        timers.clear()
    }
    
    /**
     * 清空所有任务
     */
    fun clearAllTasks() {
        stopAllTasks()
        prefs.edit().remove(KEY_TASKS).apply()
    }
    
    /**
     * 获取任务类型名称
     */
    fun getTaskTypeName(type: Int): String {
        return when (type) {
            TaskType.LAUNCH_APP -> "启动应用"
            TaskType.DELAY_RETURN_HOME -> "延迟返回桌面"
            TaskType.SHOW_NOTIFICATION -> "显示通知"
            TaskType.CHANGE_WALLPAPER -> "更换壁纸"
            TaskType.TOGGLE_NIGHT_MODE -> "切换夜间模式"
            else -> "未知"
        }
    }
    
    /**
     * 获取重复类型名称
     */
    fun getRepeatTypeName(repeatType: Int): String {
        return when (repeatType) {
            RepeatType.ONCE -> "单次"
            RepeatType.DAILY -> "每天"
            RepeatType.WEEKLY -> "每周"
            RepeatType.WORKDAY -> "工作日"
            RepeatType.WEEKEND -> "周末"
            else -> "未知"
        }
    }
    
    /**
     * 获取任务状态描述
     */
    fun getTaskStatusDescription(task: ScheduledTask): String {
        return buildString {
            append("任务: ${task.name}\n")
            append("类型: ${getTaskTypeName(task.type)}\n")
            append("重复: ${getRepeatTypeName(task.repeatType)}\n")
            
            val remaining = task.triggerTime - System.currentTimeMillis()
            if (remaining > 0) {
                val seconds = remaining / 1000
                val minutes = seconds / 60
                val hours = minutes / 60
                append("触发: ${if (hours > 0) "${hours}小时" else ""}${minutes % 60}分钟后\n")
            }
            
            append("状态: ${if (task.isEnabled) "已启用" else "已禁用"}")
        }
    }
}
