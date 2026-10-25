package com.danni.fileswiper.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.danni.fileswiper.data.model.AudioPlayState
import com.danni.fileswiper.data.model.FileItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Provides local audio playback for the swipe cards (mp3 / wav). */
class AudioPlayer(context: Context) {

    private val appContext = context.applicationContext

    private val player = MediaPlayer().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var ticker: Job? = null
    private var currentPfd: ParcelFileDescriptor? = null

    private val _state = MutableStateFlow(AudioPlayState())
    val state: StateFlow<AudioPlayState> = _state.asStateFlow()

    init {
        player.setOnCompletionListener {
            _state.update { it.copy(isPlaying = false, positionMs = it.durationMs) }
        }
    }

    fun toggle(item: FileItem) {
        scope.launch {
            val current = _state.value
            if (current.fileId == item.id && current.durationMs > 0) {
                runCatching {
                    if (player.isPlaying) player.pause() else player.start()
                }
                _state.update { it.copy(isPlaying = runCatching { player.isPlaying }.getOrDefault(false)) }
            } else {
                playNew(item)
            }
        }
    }

    fun stop() {
        scope.launch { reset() }
    }

    private fun playNew(item: FileItem) {
        val fd = try {
            appContext.contentResolver.openFileDescriptor(Uri.parse(item.uri), "r")
        } catch (_: Exception) {
            null
        }
        if (fd == null) {
            _state.value = AudioPlayState()
            return
        }
        try {
            closePfd()
            player.reset()
            player.setDataSource(fd.fileDescriptor)
            player.prepare()
            currentPfd = fd
            _state.value = AudioPlayState(
                fileId = item.id,
                isPlaying = true,
                positionMs = 0,
                durationMs = player.duration
            )
            player.start()
            startTicker(item.id)
        } catch (_: Exception) {
            runCatching { fd.close() }
            _state.value = AudioPlayState()
        }
    }

    private fun startTicker(fileId: Long) {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive && _state.value.fileId == fileId) {
                delay(250)
                val playing = runCatching { player.isPlaying }.getOrDefault(false)
                val pos = runCatching { player.currentPosition }.getOrDefault(0)
                if (_state.value.isPlaying != playing || _state.value.positionMs != pos) {
                    _state.update { it.copy(isPlaying = playing, positionMs = pos) }
                }
            }
        }
    }

    private suspend fun reset() {
        ticker?.cancel()
        ticker = null
        runCatching { player.reset() }
        closePfd()
        _state.value = AudioPlayState()
    }

    private fun closePfd() {
        currentPfd?.let { runCatching { it.close() } }
        currentPfd = null
    }

    fun release() {
        scope.cancel()
        closePfd()
        runCatching { player.release() }
    }
}
