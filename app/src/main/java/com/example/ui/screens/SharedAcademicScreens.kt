package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audio.RingtonePlayer
import com.example.data.AouRoadmapsData
import com.example.data.AppStrings
import com.example.data.LocalAppPreferences
import com.example.data.LocalizationData
import com.example.model.CalendarEvent
import com.example.model.EmailPriority
import com.example.model.EventType
import com.example.model.TextScaleOption
import com.example.model.UiState
import com.example.model.UserRole
import com.example.ui.components.PlatformDownloadsDialog
import com.example.ui.components.UserAvatarChip
import com.example.ui.theme.PriorityCriticalBg
import com.example.ui.theme.PriorityHighBg
import com.example.ui.theme.PriorityLowBg
import com.example.ui.theme.PriorityNormalBg
import com.example.ui.theme.SoftOffWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatCountdown(targetMillis: Long, nowMillis: Long): String {
    val diff = (targetMillis - nowMillis).coerceAtLeast(0L)
    if (diff == 0L) return "Starting Now"
    val totalSeconds = diff / 1000L
    val days = totalSeconds / 86400L
    val hours = (totalSeconds % 86400L) / 3600L
    val mins = (totalSeconds % 3600L) / 60L
    val secs = totalSeconds % 60L
    return if (days > 0) {
        "${days}d ${hours}h ${mins}m ${secs}s"
    } else {
        String.format(Locale.US, "%02dh %02dm %02ds", hours, mins, secs)
    }
}

fun formatEventDate(epochMillis: Long): String {
    val fmt = SimpleDateFormat("EEE, MMM d · HH:mm", Locale.getDefault())
    return fmt.format(Date(epochMillis))
}

fun priorityIcon(priority: EmailPriority) = when (priority) {
    EmailPriority.CRITICAL -> Icons.Default.Error
    EmailPriority.HIGH -> Icons.Default.WarningAmber
    EmailPriority.NORMAL -> Icons.Default.Info
    EmailPriority.LOW -> Icons.Default.ArrowDownward
}

fun priorityColor(priority: EmailPriority) = when (priority) {
    EmailPriority.CRITICAL -> PriorityCriticalBg
    EmailPriority.HIGH -> PriorityHighBg
    EmailPriority.NORMAL -> PriorityNormalBg
    EmailPriority.LOW -> PriorityLowBg
}

@Composable
fun PriorityBadge(priority: EmailPriority) {
    val bg = priorityColor(priority)
    val icon = priorityIcon(priority)
    Surface(
        shape = RoundedCornerShape(50),
        color = bg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = priority.name,
                tint = SoftOffWhite,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = priority.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall,
                color = SoftOffWhite,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun EmailPriorityFilterAndSortBar(
    selectedFilter: EmailPriority?,
    criticalFirst: Boolean,
    allLabel: String = "All",
    onSelectFilter: (EmailPriority?) -> Unit,
    onToggleSort: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = !criticalFirst,
            onClick = onToggleSort,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = "Sort priority",
                    modifier = Modifier.size(15.dp)
                )
            },
            label = {
                Text(
                    text = if (criticalFirst) "Critical → Low" else "Low → Critical",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            },
            modifier = Modifier.testTag("email_priority_sort_chip")
        )

        FilterChip(
            selected = selectedFilter == null,
            onClick = { onSelectFilter(null) },
            label = { Text(allLabel, style = MaterialTheme.typography.labelSmall, maxLines = 1) },
            modifier = Modifier.testTag("email_filter_all")
        )

        EmailPriority.entries.forEach { prio ->
            val tint = priorityColor(prio)
            FilterChip(
                selected = selectedFilter == prio,
                onClick = { onSelectFilter(if (selectedFilter == prio) null else prio) },
                leadingIcon = {
                    Icon(
                        imageVector = priorityIcon(prio),
                        contentDescription = prio.name,
                        tint = if (selectedFilter == prio) MaterialTheme.colorScheme.primary else tint,
                        modifier = Modifier.size(14.dp)
                    )
                },
                label = {
                    Text(
                        text = prio.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                },
                modifier = Modifier.testTag("email_filter_${prio.name.lowercase()}")
            )
        }
    }
}

@Composable
fun NextUpCountdownCard(
    events: List<CalendarEvent>,
    currentTimeMillis: Long,
    sectionTitle: String,
    modifier: Modifier = Modifier
) {
    val nextEvent = remember(events, currentTimeMillis) {
        events.filter { it.triggerEpochMillis >= currentTimeMillis - 60_000L }
            .minByOrNull { it.triggerEpochMillis }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = sectionTitle.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f)
                    )
                }
                if (nextEvent != null) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = nextEvent.eventType,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (nextEvent == null) {
                Text(
                    text = "No upcoming deadlines or lectures scheduled.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    text = if (nextEvent.courseCode.isNotBlank()) "${nextEvent.courseCode} · ${nextEvent.title}" else nextEvent.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatCountdown(nextEvent.triggerEpochMillis, currentTimeMillis),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "${formatEventDate(nextEvent.triggerEpochMillis)}${if (nextEvent.notes.isNotBlank()) " · ${nextEvent.notes}" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.86f)
                )
            }
        }
    }
}

@Composable
fun CalendarAndSmartAlarmScreen(
    calendarState: UiState<List<CalendarEvent>>,
    currentTimeMillis: Long,
    strings: AppStrings,
    onAddEvent: (String, String, EventType, Long, Boolean, String, Int, String) -> Unit,
    onDeleteEvent: (String) -> Unit,
    onTestOverlayAlert: (CalendarEvent) -> Unit
) {
    val scope = rememberCoroutineScope()
    var showCreateDialog by remember { mutableStateOf(false) }
    var createAsAlarm by remember { mutableStateOf(false) }

    val events = (calendarState as? UiState.Success)?.data.orEmpty()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(6.dp))
            NextUpCountdownCard(
                events = events,
                currentTimeMillis = currentTimeMillis,
                sectionTitle = strings.nextUp
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        createAsAlarm = false
                        showCreateDialog = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("add_calendar_event_button")
                ) {
                    Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.addEvent, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }

                OutlinedButton(
                    onClick = {
                        createAsAlarm = true
                        showCreateDialog = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("add_manual_alarm_button")
                ) {
                    Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.addAlarm, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
            }
        }

        item {
            Text(
                text = strings.calendarSmartAlarm,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (events.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "No scheduled events or alarms yet.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        } else {
            items(events, key = { it.id }) { ev ->
                val toneLabel = RingtonePlayer.options.firstOrNull { it.id == ev.ringtoneId }?.label ?: "Crystal Chime"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (ev.courseCode.isNotBlank()) "${ev.courseCode} · ${ev.title}" else ev.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = formatEventDate(ev.triggerEpochMillis),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onDeleteEvent(ev.id) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete event",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (ev.notes.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = ev.notes,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (ev.alarmEnabled) "$toneLabel · ${ev.volumePercent}%" else "Alarm muted",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(
                                    onClick = {
                                        RingtonePlayer.playTone(ev.ringtoneId, ev.volumePercent.toInt(), scope)
                                    }
                                ) {
                                    Text("Preview", style = MaterialTheme.typography.labelLarge)
                                }
                                OutlinedButton(
                                    onClick = { onTestOverlayAlert(ev) },
                                    modifier = Modifier.testTag("test_overlay_alert_${ev.id}")
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Full Alert", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }

    if (showCreateDialog) {
        CreateEventOrAlarmDialog(
            isAlarmDefault = createAsAlarm,
            onDismiss = { showCreateDialog = false },
            onSave = { title, code, type, delayMinutes, alarmOn, ringtoneId, volume, notes ->
                onAddEvent(
                    title,
                    code,
                    type,
                    System.currentTimeMillis() + delayMinutes * 60_000L,
                    alarmOn,
                    ringtoneId,
                    volume,
                    notes
                )
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun CreateEventOrAlarmDialog(
    isAlarmDefault: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, EventType, Long, Boolean, String, Int, String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf(if (isAlarmDefault) "Study Reminder Alarm" else "") }
    var courseCode by remember { mutableStateOf("M251") }
    var eventType by remember { mutableStateOf(if (isAlarmDefault) EventType.ALARM else EventType.LECTURE) }
    var delayMinutesText by remember { mutableStateOf(if (isAlarmDefault) "1" else "60") }
    var alarmEnabled by remember { mutableStateOf(true) }
    var selectedRingtone by remember { mutableStateOf(RingtonePlayer.options.first().id) }
    var volumePercent by remember { mutableFloatStateOf(85f) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isAlarmDefault) "Create Manual Smart Alarm" else "Create Academic Event") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = courseCode,
                    onValueChange = { courseCode = it },
                    label = { Text("Course Code (e.g. M251)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = delayMinutesText,
                    onValueChange = { delayMinutesText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Triggers in (minutes from now)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Event Type", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(EventType.LECTURE, EventType.QUIZ, EventType.ASSIGNMENT, EventType.ALARM).forEach { t ->
                        FilterChip(
                            selected = eventType == t,
                            onClick = { eventType = t },
                            label = { Text(t.name.take(6), style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Smart Alarm Enabled", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = alarmEnabled, onCheckedChange = { alarmEnabled = it })
                }

                Text("Ringtone (6 Options)", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    RingtonePlayer.options.forEach { opt ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = selectedRingtone == opt.id,
                                onClick = { selectedRingtone = opt.id },
                                label = { Text(opt.label) }
                            )
                            TextButton(onClick = {
                                RingtonePlayer.playTone(opt.id, volumePercent.toInt(), scope)
                            }) {
                                Text("Preview")
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Volume: ${volumePercent.toInt()}%", style = MaterialTheme.typography.bodyMedium)
                }
                Slider(
                    value = volumePercent,
                    onValueChange = { volumePercent = it },
                    valueRange = 10f..100f
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Location / Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val mins = delayMinutesText.toLongOrNull()?.coerceAtLeast(1L) ?: 15L
                    onSave(
                        title.ifBlank { "AOU Academic Reminder" },
                        courseCode,
                        eventType,
                        mins,
                        alarmEnabled,
                        selectedRingtone,
                        volumePercent.toInt(),
                        notes
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun DrawerAccountSettingsContent(
    prefs: LocalAppPreferences,
    strings: AppStrings,
    isCloudAuthenticated: Boolean,
    onCloseDrawer: () -> Unit,
    onRoleSwitch: (UserRole) -> Unit,
    onLanguageSelected: (String) -> Unit,
    onToggleDarkMode: () -> Unit,
    onTextScaleSelected: (TextScaleOption) -> Unit,
    onBranchSelected: (String) -> Unit,
    onMajorSelected: (String) -> Unit,
    onProfilePhotoPicked: (Uri) -> Unit,
    onOpenGpaCalculator: (() -> Unit)? = null,
    onOpenPortalsHub: (() -> Unit)? = null,
    onOpenSupportDirectory: () -> Unit = {},
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var branchMenuExpanded by remember { mutableStateOf(false) }
    var majorMenuExpanded by remember { mutableStateOf(false) }
    var langMenuExpanded by remember { mutableStateOf(false) }
    var showDownloadsDialog by remember { mutableStateOf(false) }

    val profilePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onProfilePhotoPicked(uri)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Non-blocking internal avatar upload
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatarChip(
                profilePhotoUri = prefs.profilePhotoUri,
                size = 54.dp,
                showCameraBadge = true,
                testTag = "upload_profile_photo_button",
                onClick = {
                    profilePhotoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(strings.profilePhoto, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${if (isCloudAuthenticated) strings.cloudLabel else strings.demoLabel} · ${strings.profileTapHint}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Data Protection badge
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = strings.dataProtectionLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Quick AOU Utilities
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onOpenGpaCalculator != null) {
                OutlinedButton(onClick = {
                    onCloseDrawer()
                    onOpenGpaCalculator()
                }) {
                    Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.navGpaCalc, maxLines = 1)
                }
            }
            if (onOpenPortalsHub != null) {
                OutlinedButton(onClick = {
                    onCloseDrawer()
                    onOpenPortalsHub()
                }) {
                    Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.navPortals, maxLines = 1)
                }
            }
            OutlinedButton(onClick = {
                onCloseDrawer()
                onOpenSupportDirectory()
            }) {
                Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text(strings.navSupport, maxLines = 1)
            }
        }

        // Role Switch
        Text(strings.chooseRole, style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = prefs.role == UserRole.STUDENT,
                onClick = { onRoleSwitch(UserRole.STUDENT) },
                label = { Text(strings.studentRole, maxLines = 1) }
            )
            FilterChip(
                selected = prefs.role == UserRole.INSTRUCTOR,
                onClick = { onRoleSwitch(UserRole.INSTRUCTOR) },
                label = { Text(strings.instructorRole, maxLines = 1) }
            )
        }

        // AOU Branch
        androidx.compose.foundation.layout.Box {
            OutlinedButton(
                onClick = { branchMenuExpanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("${strings.aouBranch}: ${prefs.aouBranch}", maxLines = 1)
            }
            DropdownMenu(expanded = branchMenuExpanded, onDismissRequest = { branchMenuExpanded = false }) {
                AouRoadmapsData.aouBranches.forEach { branch ->
                    DropdownMenuItem(
                        text = { Text("${strings.aouPrefix} · $branch") },
                        onClick = {
                            branchMenuExpanded = false
                            onBranchSelected(branch)
                        }
                    )
                }
            }
        }

        if (prefs.role == UserRole.STUDENT) {
            val activeMajor = AouRoadmapsData.allMajors.firstOrNull { it.id == prefs.majorId } ?: AouRoadmapsData.allMajors.first()
            androidx.compose.foundation.layout.Box {
                OutlinedButton(
                    onClick = { majorMenuExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("${strings.majorLabel}: ${activeMajor.name}", maxLines = 1)
                }
                DropdownMenu(expanded = majorMenuExpanded, onDismissRequest = { majorMenuExpanded = false }) {
                    AouRoadmapsData.allMajors.forEach { major ->
                        DropdownMenuItem(
                            text = { Text(major.name) },
                            onClick = {
                                majorMenuExpanded = false
                                onMajorSelected(major.id)
                            }
                        )
                    }
                }
            }
        }

        // Language (20 Languages)
        val currentLang = LocalizationData.languages.firstOrNull { it.code == prefs.languageCode }
            ?: LocalizationData.languages.first()
        androidx.compose.foundation.layout.Box {
            OutlinedButton(
                onClick = { langMenuExpanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("${strings.language}: ${currentLang.nativeName} (${currentLang.englishName})", maxLines = 1)
            }
            DropdownMenu(expanded = langMenuExpanded, onDismissRequest = { langMenuExpanded = false }) {
                LocalizationData.languages.forEach { lang ->
                    DropdownMenuItem(
                        text = { Text("${lang.nativeName} (${lang.englishName})") },
                        onClick = {
                            langMenuExpanded = false
                            onLanguageSelected(lang.code)
                        }
                    )
                }
            }
        }

        // Font Size Scaling (Medium, Large, Extra Large)
        Text(strings.fontSizeLabel, style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextScaleOption.entries.forEach { option ->
                FilterChip(
                    selected = prefs.textScale == option,
                    onClick = { onTextScaleSelected(option) },
                    label = { Text(option.label, maxLines = 1) },
                    modifier = Modifier.testTag("text_scale_${option.name.lowercase()}")
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(strings.darkMode, style = MaterialTheme.typography.titleMedium)
            Switch(checked = prefs.darkMode, onCheckedChange = { onToggleDarkMode() })
        }

        OutlinedButton(
            onClick = { showDownloadsDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_platform_downloads_button")
        ) {
            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(strings.getAppsLabel, maxLines = 1)
        }

        Button(
            onClick = {
                onCloseDrawer()
                onSignOut()
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("sign_out_button")
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(strings.signOut)
        }

        Spacer(Modifier.height(14.dp))
    }

    if (showDownloadsDialog) {
        PlatformDownloadsDialog(strings = strings, onDismiss = { showDownloadsDialog = false })
    }
}
