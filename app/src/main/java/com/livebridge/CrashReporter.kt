package com.livebridge

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashReporter {
    private const val TAG = "LiveBridgeCrash"
    private const val LOG_FILE = "livebridge_session.log"
    private const val CRASH_FILE = "livebridge_crash.txt"
    @Volatile private var installed = false

    @Synchronized
    fun install(context: Context) {
        if (installed) return
        installed = true
        val appContext = context.applicationContext
        log(appContext, "PROCESS_START")
        log(appContext, "Android=${Build.VERSION.RELEASE} SDK=${Build.VERSION.SDK_INT} device=${Build.MANUFACTURER} ${Build.MODEL}")
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val report = buildString {
                    appendLine("LIVEBRIDGE CRASH REPORT")
                    appendLine("time=${timestamp()}")
                    appendLine("thread=${thread.name}")
                    appendLine("android=${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
                    appendLine("device=${Build.MANUFACTURER} ${Build.MODEL}")
                    appendLine("app=${appVersion(appContext)}")
                    appendLine()
                    appendLine("EXCEPTION")
                    appendLine(throwable.stackTraceToString())
                    appendLine()
                    appendLine("RECENT SESSION LOG")
                    appendLine(readSessionLog(appContext))
                }
                appContext.openFileOutput(CRASH_FILE, Context.MODE_PRIVATE).use { it.write(report.toByteArray(Charsets.UTF_8)) }
                Log.e(TAG, "Fatal crash saved to $CRASH_FILE", throwable)
            } catch (_: Throwable) {}
            previous?.uncaughtException(thread, throwable)
                ?: run { android.os.Process.killProcess(android.os.Process.myPid()) }
        }
    }

    fun log(context: Context, message: String) {
        try {
            val line = "${timestamp()} | $message\n"
            val file = File(context.applicationContext.filesDir, LOG_FILE)
            synchronized(this) {
                file.appendText(line, Charsets.UTF_8)
                if (file.length() > 256 * 1024) {
                    val text = file.readText(Charsets.UTF_8)
                    file.writeText(text.takeLast(128 * 1024), Charsets.UTF_8)
                }
            }
            Log.d(TAG, message)
        } catch (_: Throwable) {}
    }

    fun hasCrash(context: Context): Boolean = File(context.applicationContext.filesDir, CRASH_FILE).exists()

    fun readCrash(context: Context): String = try {
        File(context.applicationContext.filesDir, CRASH_FILE).readText(Charsets.UTF_8).takeLast(120_000)
    } catch (e: Throwable) { "Impossible de lire le rapport: ${e.message}" }

    fun clearCrash(context: Context) {
        File(context.applicationContext.filesDir, CRASH_FILE).delete()
        log(context, "CRASH_REPORT_CLEARED")
    }

    fun copyCrash(context: Context): Boolean = try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("LiveBridge crash report", readCrash(context)))
        log(context, "CRASH_REPORT_COPIED")
        true
    } catch (e: Throwable) {
        log(context, "CRASH_REPORT_COPY_FAILED ${e.message}")
        false
    }

    private fun readSessionLog(context: Context): String = try {
        File(context.applicationContext.filesDir, LOG_FILE).readText(Charsets.UTF_8).takeLast(80_000)
    } catch (_: Throwable) { "(session log unavailable)" }

    private fun appVersion(context: Context): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
    } catch (_: Throwable) { "unknown" }

    private fun timestamp(): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US).format(Date())
}