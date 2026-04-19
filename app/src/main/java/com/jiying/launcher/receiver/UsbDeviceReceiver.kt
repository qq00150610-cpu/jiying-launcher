package com.jiying.launcher.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.jiying.launcher.service.DesktopService
import com.jiying.launcher.service.FloatBallService

/**
 * USB设备接收器
 * 监听USB设备的连接和断开
 */
class UsbDeviceReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_USB_PERMISSION = "android.hardware.usb.action.USB_DEVICE_ATTACHED"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        try {
            context ?: return
            intent ?: return
            
            when (intent.action) {
                "android.hardware.usb.action.USB_DEVICE_ATTACHED" -> {
                    handleDeviceAttached(context, intent)
                }
                "android.hardware.usb.action.USB_DEVICE_DETACHED" -> {
                    handleDeviceDetached(context, intent)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleDeviceAttached(context: Context, intent: Intent) {
        try {
            val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra("device", android.hardware.usb.UsbDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra("device")
            }
            
            device?.let {
                // 发送广播通知UI
                val broadcastIntent = Intent(UsbDeviceAction.ACTION_USB_DEVICE_CONNECTED).apply {
                    putExtra("device_name", it.deviceName)
                    putExtra("vendor_id", it.vendorId)
                    putExtra("product_id", it.productId)
                    setPackage(context.packageName)
                }
                context.sendBroadcast(broadcastIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleDeviceDetached(context: Context, intent: Intent) {
        try {
            val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra("device", android.hardware.usb.UsbDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra("device")
            }
            
            device?.let {
                // 发送广播通知UI
                val broadcastIntent = Intent(UsbDeviceAction.ACTION_USB_DEVICE_DISCONNECTED).apply {
                    putExtra("device_name", it.deviceName)
                    putExtra("vendor_id", it.vendorId)
                    putExtra("product_id", it.productId)
                    setPackage(context.packageName)
                }
                context.sendBroadcast(broadcastIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

object UsbDeviceAction {
    const val ACTION_USB_DEVICE_CONNECTED = "com.jiying.launcher.USB_DEVICE_CONNECTED"
    const val ACTION_USB_DEVICE_DISCONNECTED = "com.jiying.launcher.USB_DEVICE_DISCONNECTED"
}
