@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.danni.fileswiper.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.danni.fileswiper.R
import com.danni.fileswiper.data.model.CleanMode
import com.danni.fileswiper.data.model.FileCategory
import com.danni.fileswiper.data.model.SessionRequest
import com.danni.fileswiper.data.storage.StoragePermissions
import com.danni.fileswiper.ui.common.EmptyState
import com.danni.fileswiper.ui.common.LabeledChip
import com.danni.fileswiper.ui.common.MonthRow
import com.danni.fileswiper.ui.common.SectionHeader
import com.danni.fileswiper.ui.common.formatFileSize
import com.danni.fileswiper.viewmodel.SetupUiState
import com.danni.fileswiper.viewmodel.SetupViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetupScreen(
    onSessionReady: (SessionRequest) -> Unit,
    viewModel: SetupViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val months by viewModel.months.collectAsState()
    val scanning by viewModel.scanning.collectAsState()
    val sessionRequest by viewModel.sessionRequests.collectAsState()
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(sessionRequest) {
        if (sessionRequest != null && !scanning) {
            onSessionReady(sessionRequest!!)
            viewModel.requestSessionConsumed()
        }
    }

    val hasAccess = remember { StoragePermissions.hasAccess(context) }
    LaunchedEffect(Unit) {
        if (hasAccess) viewModel.refreshMonths()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.setup_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { showSettings = true }) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = stringResource(R.string.settings_title),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            SectionHeader(stringResource(R.string.select_types))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LabeledChip(
                    label = stringResource(R.string.cat_photos),
                    selected = FileCategory.PHOTOS in state.selectedCategories,
                    onClick = { viewModel.toggleCategory(FileCategory.PHOTOS) }
                )
                LabeledChip(
                    label = stringResource(R.string.cat_videos),
                    selected = FileCategory.VIDEOS in state.selectedCategories,
                    onClick = { viewModel.toggleCategory(FileCategory.VIDEOS) }
                )
                LabeledChip(
                    label = stringResource(R.string.cat_audio),
                    selected = FileCategory.AUDIO in state.selectedCategories,
                    onClick = { viewModel.toggleCategory(FileCategory.AUDIO) }
                )
                LabeledChip(
                    label = stringResource(R.string.cat_docs),
                    selected = FileCategory.DOCUMENTS in state.selectedCategories,
                    onClick = { viewModel.toggleCategory(FileCategory.DOCUMENTS) }
                )
            }
            AnimatedDocsHint(visible = FileCategory.DOCUMENTS in state.selectedCategories)

            SectionHeader(stringResource(R.string.mode_title))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ModeCard(
                    title = stringResource(R.string.mode_monthly),
                    hint = stringResource(R.string.mode_monthly_hint),
                    icon = Icons.Rounded.CalendarMonth,
                    selected = state.mode == CleanMode.MONTHLY,
                    onClick = { viewModel.setMode(CleanMode.MONTHLY) },
                    modifier = Modifier.weight(1f)
                )
                ModeCard(
                    title = stringResource(R.string.mode_quick),
                    hint = stringResource(R.string.mode_quick_hint),
                    icon = Icons.Rounded.Bolt,
                    selected = state.mode == CleanMode.QUICK,
                    onClick = { viewModel.setMode(CleanMode.QUICK) },
                    modifier = Modifier.weight(1f)
                )
            }

            when (state.mode) {
                CleanMode.MONTHLY -> MonthsSection(
                    months = months,
                    scanning = scanning,
                    context = context,
                    onClickMonth = { bucketId -> viewModel.startSession(bucketId) },
                    onRefresh = { viewModel.refreshMonths() }
                )
                CleanMode.QUICK -> QuickSection(
                    state = state,
                    onCountChange = { viewModel.setRandomCount(it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.startSession() },
                enabled = !scanning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = stringResource(R.string.btn_start),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (state.permissionDialogVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPermissionDialog() },
            title = { Text(stringResource(R.string.grant_title)) },
            text = { Text(stringResource(R.string.grant_text)) },
            confirmButton = {
                TextButton(onClick = {
                    StoragePermissions.request(context)
                    viewModel.dismissPermissionDialog()
                }) { Text(stringResource(R.string.grant_btn)) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPermissionDialog() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showSettings) {
        val themeMode by viewModel.themeMode.collectAsState()
        val language by viewModel.language.collectAsState()
        com.danni.fileswiper.ui.common.SettingsDialog(
            themeMode = themeMode,
            languageTag = language,
            onThemeModeChange = { viewModel.applyThemeMode(it) },
            onLanguageChange = { viewModel.applyLanguage(it) },
            onDismiss = { showSettings = false }
        )
    }
}
