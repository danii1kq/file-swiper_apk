package com.danni.fileswiper

import android.app.Application
import android.content.Context
import android.os.Environment
import com.danni.fileswiper.data.MediaStoreRepository
import com.danni.fileswiper.data.MediaRepository
import com.danni.fileswiper.data.audio.AudioPlayer
import com.danni.fileswiper.data.prefs.SettingsRepository
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppContainer(context: Context) {
    val settingsRepository = SettingsRepository(context)
    val mediaRepository: MediaRepository = MediaStoreRepository(context)
    val audioPlayer = AudioPlayer(context)
}

class FileSwiperApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        installCrashLogger()
    }

    /**
     * Saves the last uncaught exception so a crash can be diagnosed without
     * logcat: always into the app-private external dir, and into public
     * Downloads when "All files access" is granted (openable with any
     * file manager).
     */
    private fun installCrashLogger() {
        val system = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                val body = buildString {
                    appendLine("FileSwiper crash at $timestamp (thread: ${thread.name})")
                    appendLine()
                    appendLine(error.stackTraceToString())
                }
                runCatching {
                    File(getExternalFilesDir(null), "logs/crash.txt").writeText(body)
                }
                runCatching {
                    val downloads = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS
                    )
                    if (Environment.getExternalStorageState(downloads) == Environment.MEDIA_MOUNTED) {
                        File(downloads, "FileSwiper-crash.txt").writeText(body)
                    }
                }
            } catch (_: Throwable) {
            }
            system?.uncaughtException(thread, error)
        }
    }
}
