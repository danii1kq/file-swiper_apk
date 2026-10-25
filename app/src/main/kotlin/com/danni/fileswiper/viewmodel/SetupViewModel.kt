package com.danni.fileswiper.viewmodel

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.danni.fileswiper.FileSwiperApp
import com.danni.fileswiper.data.model.CleanMode
import com.danni.fileswiper.data.model.FileCategory
import com.danni.fileswiper.data.model.FileItem
import com.danni.fileswiper.data.model.MonthBucket
import com.danni.fileswiper.data.model.SessionRequest
import com.danni.fileswiper.data.prefs.ThemeMode
import com.danni.fileswiper.data.storage.StoragePermissions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

data class SetupUiState(
    val selectedCategories: Set<FileCategory> = setOf(FileCategory.PHOTOS),
    val mode: CleanMode = CleanMode.MONTHLY,
    val randomCount: Int = 20,
    val permissionDialogVisible: Boolean = false
)

class SetupViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as FileSwiperApp).container
    private val repo = container.mediaRepository
    private val settings = container.settingsRepository

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    private val _months = MutableStateFlow<List<MonthBucket>>(emptyList())
    val months: StateFlow<List<MonthBucket>> = _months.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    val clearedMonths get() = settings.clearedMonths

    val themeMode: StateFlow<ThemeMode> = settings.themeMode
    val language: StateFlow<String> = settings.language

    fun applyThemeMode(mode: ThemeMode) = settings.setThemeMode(mode)

    /** Saves the choice and applies it immediately (activities auto-recreate). */
    fun applyLanguage(tag: String) {
        settings.setLanguage(tag)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    /** One-shot request consumed by the UI to launch a swipe session. */
    private val _sessionRequests = MutableStateFlow<SessionRequest?>(null)
    val sessionRequests: StateFlow<SessionRequest?> = _sessionRequests.asStateFlow()

    fun requestSessionConsumed() {
        _sessionRequests.value = null
    }

    fun toggleCategory(category: FileCategory) {
        val target = _uiState.value.selectedCategories.toMutableSet()
        if (category in target) {
            if (target.size > 1) target.remove(category)
        } else {
            target += category
        }
        _uiState.update { it.copy(selectedCategories = target) }
        refreshMonths()
    }

    fun setMode(mode: CleanMode) {
        _uiState.update { it.copy(mode = mode) }
        if (mode == CleanMode.MONTHLY && _months.value.isEmpty()) refreshMonths()
    }

    fun setRandomCount(count: Int) {
        _uiState.update { it.copy(randomCount = count.coerceIn(5, 30)) }
    }

    fun refreshMonths() {
        val categories = _uiState.value.selectedCategories
        if (categories.isEmpty()) {
            _months.value = emptyList()
            return
        }
        viewModelScope.launch {
            _scanning.value = true
            val cleared = settings.clearedMonths.value
            val files = repo.scan(categories)
            _months.value = files.groupByMonth(categories, cleared)
            _scanning.value = false
        }
    }

    fun startSession(monthKey: String? = null) {
        if (!StoragePermissions.hasAccess(getApplication())) {
            _uiState.update { it.copy(permissionDialogVisible = true) }
            return
        }
        val state = _uiState.value
        viewModelScope.launch {
            when (state.mode) {
                CleanMode.MONTHLY -> {
                    val month = if (monthKey != null) {
                        _months.value.firstOrNull { it.id == monthKey && !it.isCleared }
                    } else {
                        _months.value.firstOrNull { !it.isCleared }
                    } ?: return@launch
                    _scanning.value = true
                    val files = repo.scan(state.selectedCategories)
                    _scanning.value = false
                    if (files.isEmpty()) return@launch
                    val monthStart = month.startMillis
                    val monthEnd = monthEnd(monthStart)
                    val monthFiles = files.filter { it.dateModified in monthStart until monthEnd }
                    if (monthFiles.isEmpty()) {
                        // Nothing left in the freshest scan for this month.
                        refreshMonths()
                        return@launch
                    }
                    emitSession(
                        SessionRequest(
                            files = monthFiles,
                            isMonthly = true,
                            monthKey = month.id,
                            monthLabel = month.label
                        )
                    )
                }

                CleanMode.QUICK -> {
                    _scanning.value = true
                    val files = repo.scan(state.selectedCategories)
                    _scanning.value = false
                    if (files.isEmpty()) return@launch
                    emitSession(
                        SessionRequest(
                            files = files.shuffled().take(state.randomCount),
                            isMonthly = false,
                            monthKey = null,
                            monthLabel = null
                        )
                    )
                }
            }
        }
    }

    private suspend fun emitSession(request: SessionRequest) {
        _sessionRequests.value = request
    }

    fun dismissPermissionDialog() {
        _uiState.update { it.copy(permissionDialogVisible = false) }
    }

    private fun List<FileItem>.groupByMonth(
        categories: Set<FileCategory>,
        cleared: Set<String>
    ): List<MonthBucket> {
        if (isEmpty()) return emptyList()
        val zone = ZoneId.systemDefault()

        fun keyOf(millis: Long): Pair<LocalDate, String> {
            val date = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
            return date to "%04d-%02d".format(date.year, date.monthValue)
        }

        fun mask(): String = categories.sorted().joinToString("-") { it.name }

        val byMonth = groupBy { keyOf(it.dateModified).second }
        return byMonth.mapNotNull { (key, list) ->
            if (list.isEmpty()) return@mapNotNull null
            val date = keyOf(list.first().dateModified).first
            val bucketKey = "$key::${mask()}"
            val start = date.withDayOfMonth(1).atStartOfDay(zone)
            MonthBucket(
                id = bucketKey,
                label = monthLabel(date),
                startMillis = start.toInstant().toEpochMilli(),
                count = list.size,
                totalSize = list.sumOf { it.size },
                isCleared = bucketKey in cleared
            )
        }.sortedByDescending { it.startMillis }
    }

    private fun monthEnd(startMillis: Long): Long {
        val zone = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(startMillis).atZone(zone).toLocalDate()
        return date.plusMonths(1).withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
    }

    private fun monthLabel(date: LocalDate): String {
        val locale: Locale = getApplication<Application>().resources.configuration.locales[0]
        val format = SimpleDateFormat("LLLL yyyy", locale)
        val zone = ZoneId.systemDefault()
        return format.format(Date.from(date.withDayOfMonth(1).atStartOfDay(zone).toInstant()))
    }
}
