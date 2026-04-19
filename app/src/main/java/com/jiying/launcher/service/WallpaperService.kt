package com.jiying.launcher.service

import android.service.wallpaper.WallpaperService

/**
 * 极影桌面 - 壁纸服务
 * 支持动态壁纸
 */
class WallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return JiYingWallpaperEngine()
    }

    inner class JiYingWallpaperEngine : Engine() {
        override fun onCreate(surfaceHolder: android.view.SurfaceHolder?) {
            super.onCreate(surfaceHolder)
        }

        override fun onDestroy() {
            super.onDestroy()
        }
    }
}
