package com.danni.fileswiper

import android.app.Application
import android.content.Context
import com.danni.fileswiper.data.MediaStoreRepository
import com.danni.fileswiper.data.MediaRepository
import com.danni.fileswiper.data.audio.AudioPlayer
import com.danni.fileswiper.data.prefs.SettingsRepository

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
    }
}
