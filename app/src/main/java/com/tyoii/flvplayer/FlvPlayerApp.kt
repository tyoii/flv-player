package com.tyoii.flvplayer

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import com.tyoii.flvplayer.ui.CrashActivity
import java.io.PrintWriter
import java.io.StringWriter

@HiltAndroidApp
class FlvPlayerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            Log.e("FLVPlayerCrash", "Uncaught exception", throwable)
            
            CrashActivity.start(this, sw.toString())
            
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(1)
        }
    }
}
