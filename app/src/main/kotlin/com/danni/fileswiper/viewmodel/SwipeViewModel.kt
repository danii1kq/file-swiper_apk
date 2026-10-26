package com.danni.fileswiper.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.danni.fileswiper.FileSwiperApp
import com.danni.fileswiper.data.model.AudioPlayState
import com.danni.fileswiper.data.model.DeleteOutcome
import com.danni.fileswiper.data.model.FileItem
import com.danni.fileswiper.data.storage.StoragePermissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SwipeViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as FileSwiperApp).container

    private val _queue = MutableStateFlow<List<FileItem>>(emptyList())
    val queue: StateFlow<List<FileItem>> = _queue.asStateFlow()

    private val _trash = MutableStateFlow<List<FileItem>>(emptyList())
    val trash: StateFlow<List<FileItem>> = _trash.asStateFlow()

    private val _sessionActive = MutableStateFlow(false)
    val sessionActive: StateFlow<Boolean> = _sessionActive.asStateFlow()

    private val _isMonthly = MutableStateFlow(false)
    val isMonthly: StateFlow<Boolean> = _isMonthly.asStateFlow()

    private val _monthLabel = MutableStateFlow<String?>(null)
    val monthLabel: StateFlow<String?> = _monthLabel.asStateFlow()

    private val _currentMonthKey = MutableStateFlow<String?>(null)
    val currentMonthKey: StateFlow<String?> = _currentMonthKey.asStateFlow()

    private val _sessionDone = MutableStateFlow(false)
    val sessionDone: StateFlow<Boolean> = _sessionDone.asStateFlow()

    private val _generation = MutableStateFlow(0L)
    val generation: StateFlow<Long> = _generation.asStateFlow()

    private val _deleting = MutableStateFlow(false)
    val deleting: StateFlow<Boolean> = _deleting.asStateFlow()

    private val _deleteOutcome = MutableStateFlow<DeleteOutcome?>(null)
    val deleteOutcome: StateFlow<DeleteOutcome?> = _deleteOutcome.asStateFlow()

    val audioState: StateFlow<AudioPlayState> = container.audioPlayer.state

    val totalTrashSize: Long get() = _trash.value.sumOf { it.size }

    fun startSession(
        files: List<FileItem>,
        isMonthly: Boolean,
        monthKey: String?,
        monthLabel: String?
    ) {
        if (files.isEmpty()) return
        _isMonthly.value = isMonthly
        _currentMonthKey.value = monthKey
        _monthLabel.value = monthLabel
        _queue.value = files
        _trash.value = emptyList()
        _sessionActive.value = true
        _sessionDone.value = false
        _deleteOutcome.value = null
        _deleting.value = false
        bumpGeneration()
    }

    fun onSwipeLeft(item: FileItem) {
        container.audioPlayer.stop()
        _queue.value = _queue.value.filterNot { it.id == item.id }
        _trash.value = _trash.value + item
        checkFinished()
    }

    fun onSwipeRight(item: FileItem) {
        container.audioPlayer.stop()
        _queue.value = _queue.value.filterNot { it.id == item.id }
        checkFinished()
    }

    /** Skip: the card goes to the very end of the current queue. */
    fun onSkip() {
        container.audioPlayer.stop()
        val queue = _queue.value.toMutableList()
        val removed = queue.removeFirstOrNull() ?: return
        queue.add(removed)
        _queue.value = queue
        bumpGeneration()
    }

    fun toggleAudio(item: FileItem) {
        container.audioPlayer.toggle(item)
    }

    fun stopAudio() {
        container.audioPlayer.stop()
    }

    fun abandonSession() {
        _sessionActive.value = false
        _sessionDone.value = false
        _queue.value = emptyList()
        container.audioPlayer.stop()
    }

    fun completeSession() {
        if (_currentMonthKey.value != null) {
            _currentMonthKey.value?.let {
                container.settingsRepository.addClearedMonth(it)
            }
        }
        _sessionDone.value = true
    }

    fun deleteForever(items: List<FileItem>) {
        viewModelScope.launch(Dispatchers.IO) {
            _deleting.value = true
            val outcome = try {
                container.mediaRepository.delete(items)
            } finally {
                _deleting.value = false
            }
            _deleteOutcome.value = outcome
            _trash.value = emptyList()
        }
    }

    /** The system delete dialog (API 30+) was approved; commit the result. */
    fun onSystemDeleteApproved(items: List<FileItem>) {
        container.audioPlayer.stop()
        _deleteOutcome.value = DeleteOutcome(
            freedBytes = items.sumOf { it.size },
            failed = 0
        )
        _trash.value = emptyList()
    }

    fun reset() {
        _queue.value = emptyList()
        _trash.value = emptyList()
        _sessionActive.value = false
        _sessionDone.value = false
        _isMonthly.value = false
        _monthLabel.value = null
        _currentMonthKey.value = null
        _deleteOutcome.value = null
        _deleting.value = false
        container.audioPlayer.stop()
    }

    /**
     * True when the swipe screen was restored after a process death mid-session:
     * no live session, nothing finished, queue lost. The UI should escape.
     */
    fun isDeadEnd(): Boolean =
        !_sessionActive.value && !_sessionDone.value && _queue.value.isEmpty()

    private fun checkFinished() {
        if (_queue.value.isEmpty() && _sessionActive.value) {
            completeSession()
            _sessionActive.value = false
        }
        bumpGeneration()
    }

    private fun bumpGeneration() {
        _generation.value += 1
    }

    @Suppress("unused")
    private fun storageAvailable(): Boolean = StoragePermissions.hasAccess(getApplication())
}
