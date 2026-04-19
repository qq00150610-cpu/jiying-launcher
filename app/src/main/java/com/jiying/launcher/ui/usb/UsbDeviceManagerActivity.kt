package com.jiying.launcher.ui.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jiying.launcher.R
import com.jiying.launcher.receiver.UsbDeviceReceiver

/**
 * USB设备管理Activity
 */
class UsbDeviceManagerActivity : AppCompatActivity() {

    private lateinit var deviceList: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var refreshBtn: Button
    
    private val usbDevices = mutableListOf<UsbDeviceInfo>()
    private lateinit var deviceAdapter: UsbDeviceAdapter
    private var usbManager: UsbManager? = null
    
    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    loadUsbDevices()
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    loadUsbDevices()
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usb_device_manager)
        
        try {
            initViews()
            setupRecyclerView()
            loadUsbDevices()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initViews() {
        try {
            usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
            deviceList = findViewById(R.id.usb_device_list)
            emptyView = findViewById(R.id.empty_view)
            refreshBtn = findViewById(R.id.btn_refresh)
            
            refreshBtn.setOnClickListener {
                loadUsbDevices()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupRecyclerView() {
        deviceAdapter = UsbDeviceAdapter(usbDevices) { position ->
            showDeviceOptions(position)
        }
        deviceList.layoutManager = LinearLayoutManager(this)
        deviceList.adapter = deviceAdapter
    }

    private fun loadUsbDevices() {
        try {
            usbDevices.clear()
            
            // 获取已连接的USB设备
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                val deviceMap = usbManager?.deviceList
                deviceMap?.forEach { (_, device) ->
                    usbDevices.add(UsbDeviceInfo(
                        name = device.deviceName,
                        deviceId = device.deviceId,
                        vendorId = device.vendorId,
                        productId = device.productId,
                        isConnected = true
                    ))
                }
            }
            
            // 模拟设备（测试用）
            if (usbDevices.isEmpty()) {
                usbDevices.add(UsbDeviceInfo(
                    name = "模拟USB存储设备",
                    deviceId = 1,
                    vendorId = 0x1234,
                    productId = 0x5678,
                    isConnected = true
                ))
            }
            
            updateUI()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateUI() {
        try {
            if (usbDevices.isEmpty()) {
                emptyView.visibility = View.VISIBLE
                deviceList.visibility = View.GONE
            } else {
                emptyView.visibility = View.GONE
                deviceList.visibility = View.VISIBLE
                deviceAdapter.notifyDataSetChanged()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showDeviceOptions(position: Int) {
        try {
            if (position >= usbDevices.size) return
            val device = usbDevices[position]
            
            val options = arrayOf("打开", "弹出", "设备详情")
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(device.name)
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> openDevice(device)
                        1 -> ejectDevice(device)
                        2 -> showDeviceInfo(device)
                    }
                }
                .show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openDevice(device: UsbDeviceInfo) {
        Toast.makeText(this, "打开设备: ${device.name}", Toast.LENGTH_SHORT).show()
    }

    private fun ejectDevice(device: UsbDeviceInfo) {
        Toast.makeText(this, "弹出设备: ${device.name}", Toast.LENGTH_SHORT).show()
    }

    private fun showDeviceInfo(device: UsbDeviceInfo) {
        val info = "设备名称: ${device.name}\n" +
                   "设备ID: ${device.deviceId}\n" +
                   "厂商ID: ${String.format("0x%04X", device.vendorId)}\n" +
                   "产品ID: ${String.format("0x%04X", device.productId)}"
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("设备详情")
            .setMessage(info)
            .setPositiveButton("确定", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        try {
            val filter = IntentFilter().apply {
                addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            }
            registerReceiver(usbReceiver, filter)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(usbReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

data class UsbDeviceInfo(
    val name: String,
    val deviceId: Int,
    val vendorId: Int,
    val productId: Int,
    val isConnected: Boolean
)

class UsbDeviceAdapter(
    private val devices: List<UsbDeviceInfo>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<UsbDeviceAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.device_icon)
        val name: TextView = view.findViewById(R.id.device_name)
        val status: TextView = view.findViewById(R.id.device_status)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_usb_device, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = devices[position]
        holder.name.text = device.name
        holder.status.text = if (device.isConnected) "已连接" else "已断开"
        holder.status.setTextColor(
            if (device.isConnected) 0xFF4CAF50.toInt() else 0xFFFF5252.toInt()
        )
        holder.itemView.setOnClickListener { onItemClick(position) }
    }

    override fun getItemCount() = devices.size
}
