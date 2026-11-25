package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.data.AppStrings
import com.example.focus.FocusLockBridge
import com.example.model.CalendarEvent
import com.example.model.CourseMaterial
import com.example.model.DirectChatMessage
import com.example.model.EmailPriority
import com.example.model.FlashcardItem
import com.example.model.ForumPost
import com.example.model.GeneralChatMessage
import com.example.model.InboxEmail
import com.example.model.MindMapResult
import com.example.model.PersonalMaterial
import com.example.model.PersonalMaterialType
import com.example.model.PomodoroCharacter
import com.example.model.StudyTutorMessage
import com.example.model.UiState
import com.example.model.VoiceNoteItem
import com.example.model.safeEnumValue
import com.example.ui.components.PomodoroCompanionCanvas
import com.example.ui.theme.InstructorPrimaryPurple
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PriorityCriticalBg
import com.example.ui.theme.SoftOffWhite
import com.example.ui.theme.StudentDarkBackground
import com.example.ui.theme.StudentPrimaryNavy
import com.example.ui.theme.StudentSoftSkyBlue
import com.example.viewmodel.PomodoroState
import java.io.File
import java.util.Locale

internal fun resolveDisplayName(context: Context, uri: Uri, fallback: String): String {
    var result = fallback
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) {
                result = cursor.getString(idx) ?: fallback
            }
        }
    } catch (_: Exception) {
    }
    return result
}

internal fun takePersistablePermission(context: Context, uri: Uri) {
    try {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } catch (_: Exception) {
    }
}

@Composable
internal fun AttachmentPreviewBlock(
    attachmentName: String,
    attachmentUri: String,
    attachmentType: String,
    itemId: String,
    playingAudioId: String?,
    onToggleAudio: (String, String) -> Unit,
    inverseSurface: Boolean = false
) {
    if (attachmentUri.isBlank() && attachmentName.isBlank()) return

    val fgColor = if (inverseSurface) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val chipBg = if (inverseSurface) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    }

    when (attachmentType.lowercase()) {
        "image" -> {
            if (attachmentUri.isNotBlank()) {
                val model: Any = if (attachmentUri.startsWith("/")) File(attachmentUri) else attachmentUri
                AsyncImage(
                    model = model,
                    contentDescription = attachmentName.ifBlank { "Image attachment" },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(155.dp)
                        .clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.Crop
                )
            }
        }
        "audio" -> {
            val isPlaying = playingAudioId == itemId
            Surface(
                shape = MaterialTheme.shapes.small,
                color = chipBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (attachmentUri.isNotBlank()) onToggleAudio(itemId, attachmentUri)
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause audio" else "Play audio",
                        tint = fgColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = fgColor.copy(alpha = 0.85f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = attachmentName.ifBlank { "Voice Recording" },
                        style = MaterialTheme.typography.labelMedium,
                        color = fgColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        else -> {
            Surface(shape = MaterialTheme.shapes.small, color = chipBg) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = null,
                        tint = fgColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = attachmentName.ifBlank { "Attached Document" },
                        style = MaterialTheme.typography.labelMedium,
                        color = fgColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
internal fun AttachmentActionPickerRow(
    attachmentName: String,
    attachmentType: String,
    isRecording: Boolean,
    onPickImage: () -> Unit,
    onPickFile: () -> Unit,
    onToggleVoiceRecord: () -> Unit,
    onClearAttachment: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (attachmentName.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Attached ($attachmentType): $attachmentName",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = onClearAttachment, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove attachment",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(
                onClick = onPickImage,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Image, contentDescription = "Attach image", modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("Image", style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
            OutlinedButton(
                onClick = onPickFile,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.AttachFile, contentDescription = "Attach file", modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("File", style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
            OutlinedButton(
                onClick = onToggleVoiceRecord,
                colors = if (isRecording) {
                    ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.outlinedButtonColors()
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = "Record audio",
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(if (isRecording) "Stop" else "Audio", style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

@Composable
fun StudentHomeScreen(
    calendarState: UiState<List<CalendarEvent>>,
    currentTimeMillis: Long,
    strings: AppStrings,
    onOpenFullCalendar: () -> Unit
) {
    val events = (calendarState as? UiState.Success)?.data.orEmpty()
    val upcomingAlarms = remember(events, currentTimeMillis) {
        events.filter { it.alarmEnabled && it.triggerEpochMillis >= currentTimeMillis - 60_000L }
            .take(3)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = strings.calendarSmartAlarm,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        TextButton(
                            onClick = onOpenFullCalendar,
                            modifier = Modifier.testTag("open_full_calendar_button")
                        ) {
                            Text(strings.manageAllLabel, style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    if (upcomingAlarms.isEmpty()) {
                        Text(
                            text = "All academic deadlines synced. Tap Manage All to create events or smart alarms.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        upcomingAlarms.forEach { ev ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (ev.courseCode.isNotBlank()) "${ev.courseCode} · ${ev.title}" else ev.title,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = formatEventDate(ev.triggerEpochMillis),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        Icons.Default.NotificationsActive,
                                        contentDescription = "Alarm active",
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
fun StudentInboxScreen(
    inboxState: UiState<List<InboxEmail>>,
    analyzingEmailId: String?,
    isRecordingAudio: Boolean,
    playingAudioId: String?,
    strings: AppStrings,
    onSummarizeAi: (InboxEmail) -> Unit,
    onAddDeadlineToCalendar: (InboxEmail) -> Unit,
    onComposeEmail: (String, String, String, String, EmailPriority, String, String, String) -> Unit,
    onStartAudioRecord: () -> Boolean,
    onStopAudioRecord: () -> VoiceNoteItem?,
    onTogglePlayAudio: (String, String) -> Unit
) {
    var showCompose by remember { mutableStateOf(false) }
    var priorityFilter by remember { mutableStateOf<EmailPriority?>(null) }
    var criticalFirst by remember { mutableStateOf(true) }

    val rawEmails = (inboxState as? UiState.Success)?.data.orEmpty()
    val emails = remember(rawEmails, priorityFilter, criticalFirst) {
        rawEmails
            .filter { priorityFilter == null || safeEnumValue(it.priority, EmailPriority.NORMAL) == priorityFilter }
            .let { list ->
                if (criticalFirst) {
                    list.sortedBy { safeEnumValue(it.priority, EmailPriority.NORMAL).weight }
                } else {
                    list.sortedByDescending { safeEnumValue(it.priority, EmailPriority.NORMAL).weight }
                }
            }
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCompose = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("student_compose_email_fab")
            ) {
                Icon(Icons.Default.Edit, contentDescription = strings.composeEmail)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = strings.priorityInbox,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Filter & sort by priority · AI summary & deadline sync",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                EmailPriorityFilterAndSortBar(
                    selectedFilter = priorityFilter,
                    criticalFirst = criticalFirst,
                    allLabel = strings.allLabel,
                    onSelectFilter = { priorityFilter = it },
                    onToggleSort = { criticalFirst = !criticalFirst }
                )
            }

            if (inboxState is UiState.Loading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (inboxState is UiState.Error) {
                item {
                    Text(inboxState.message, color = MaterialTheme.colorScheme.error)
                }
            } else {
                items(emails, key = { it.id }) { email ->
                    val prio = safeEnumValue(email.priority, EmailPriority.NORMAL)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    PriorityBadge(priority = prio)
                                    Text(
                                        text = email.courseCode,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                                Text(
                                    text = email.senderName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = email.subject,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = email.body,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            AttachmentPreviewBlock(
                                attachmentName = email.attachmentName,
                                attachmentUri = email.attachmentUri,
                                attachmentType = email.attachmentType,
                                itemId = "email_${email.id}",
                                playingAudioId = playingAudioId,
                                onToggleAudio = onTogglePlayAudio
                            )

                            if (!email.aiSummary.isNullOrBlank()) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.tertiaryContainer
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "AI Summary: ${email.aiSummary}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                        if (!email.extractedDeadline.isNullOrBlank()) {
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = "Deadline: ${email.extractedDeadline}",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onSummarizeAi(email) },
                                    enabled = analyzingEmailId != email.id,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (analyzingEmailId == email.id) {
                                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.summarizeAi, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }

                                Button(
                                    onClick = { onAddDeadlineToCalendar(email) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Sync Alarm", maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    if (showCompose) {
        ComposeEmailDialog(
            recipientLabel = "Instructor / Faculty Name",
            isRecordingAudio = isRecordingAudio,
            onStartAudioRecord = onStartAudioRecord,
            onStopAudioRecord = onStopAudioRecord,
            onDismiss = { showCompose = false },
            onSend = { to, code, subject, body, prio, attName, attUri, attType ->
                onComposeEmail(to, code, subject, body, prio, attName, attUri, attType)
                showCompose = false
            }
        )
    }
}

@Composable
fun ComposeEmailDialog(
    recipientLabel: String,
    isRecordingAudio: Boolean = false,
    onStartAudioRecord: () -> Boolean = { false },
    onStopAudioRecord: () -> VoiceNoteItem? = { null },
    onDismiss: () -> Unit,
    onSend: (String, String, String, String, EmailPriority, String, String, String) -> Unit
) {
    val context = LocalContext.current
    var recipient by remember { mutableStateOf("") }
    var courseCode by remember { mutableStateOf("M251") }
    var subject by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(EmailPriority.NORMAL) }
    var attachmentName by remember { mutableStateOf("") }
    var attachmentUri by remember { mutableStateOf("") }
    var attachmentType by remember { mutableStateOf("") }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            takePersistablePermission(context, uri)
            attachmentName = resolveDisplayName(context, uri, "Email_Image.jpg")
            attachmentUri = uri.toString()
            attachmentType = "image"
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            takePersistablePermission(context, uri)
            attachmentName = resolveDisplayName(context, uri, "Document.pdf")
            attachmentUri = uri.toString()
            attachmentType = "file"
        }
    }

    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onStartAudioRecord()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Compose Academic Email") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = recipient,
                    onValueChange = { recipient = it },
                    label = { Text(recipientLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = courseCode,
                    onValueChange = { courseCode = it },
                    label = { Text("Course Code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Message") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EmailPriority.entries.forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p.name) }
                        )
                    }
                }
                AttachmentActionPickerRow(
                    attachmentName = attachmentName,
                    attachmentType = attachmentType,
                    isRecording = isRecordingAudio,
                    onPickImage = {
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onPickFile = {
                        filePicker.launch(arrayOf("*/*"))
                    },
                    onToggleVoiceRecord = {
                        if (isRecordingAudio) {
                            val saved = onStopAudioRecord()
                            if (saved != null) {
                                attachmentName = "${saved.title} (${saved.durationLabel})"
                                attachmentUri = saved.filePath
                                attachmentType = "audio"
                            }
                        } else {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) onStartAudioRecord() else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onClearAttachment = {
                        attachmentName = ""
                        attachmentUri = ""
                        attachmentType = ""
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (subject.isNotBlank() && body.isNotBlank()) {
                        onSend(
                            recipient.ifBlank { "AOU Faculty" },
                            courseCode,
                            subject,
                            body,
                            priority,
                            attachmentName,
                            attachmentUri,
                            attachmentType
                        )
                    }
                }
            ) {
                Text("Send")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun StudentPomodoroScreen(
    pomodoroState: PomodoroState,
    character: PomodoroCharacter,
    focusedStudyMinutes: Int,
    strings: AppStrings,
    onSelectCharacter: (PomodoroCharacter) -> Unit,
    onSelectPreset: (Int, Int) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onReopenAppLock: () -> Unit,
    onRecordDistractionAttempt: () -> Unit,
    onDismissFocusExitWarning: () -> Unit,
    onDismissMilestoneReward: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var platformState by remember { mutableStateOf(FocusLockBridge.readPlatformState(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                platformState = FocusLockBridge.readPlatformState(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        platformState = FocusLockBridge.readPlatformState(context)
        if (!granted) {
            FocusLockBridge.openNotificationSettings(context)
        }
    }

    val totalSec = (if (pomodoroState.isFocusPhase) pomodoroState.focusMinutes else pomodoroState.breakMinutes) * 60
    val progress = if (totalSec > 0) pomodoroState.remainingSeconds.toFloat() / totalSec.toFloat() else 0f
    val mins = pomodoroState.remainingSeconds / 60
    val secs = pomodoroState.remainingSeconds % 60
    val timeText = String.format(Locale.US, "%02d:%02d", mins, secs)

    val milestoneCount = focusedStudyMinutes / 30
    val minutesIntoCurrentMilestone = focusedStudyMinutes % 30
    val milestoneProgress = minutesIntoCurrentMilestone / 30f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (pomodoroState.isFocusPhase) strings.focusMode else strings.breakTime,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Completed sessions today: ${pomodoroState.completedSessions}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Character Toggle (Girl Sky Dress / Boy)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = character == PomodoroCharacter.GIRL,
                    onClick = { onSelectCharacter(PomodoroCharacter.GIRL) },
                    label = { Text("${strings.girlCompanion} (Sky Dress)") },
                    modifier = Modifier.weight(1f).testTag("character_girl_chip")
                )
                FilterChip(
                    selected = character == PomodoroCharacter.BOY,
                    onClick = { onSelectCharacter(PomodoroCharacter.BOY) },
                    label = { Text("${strings.boyCompanion} (Blue & Purple)") },
                    modifier = Modifier.weight(1f).testTag("character_boy_chip")
                )
            }
        }

        // Main Pomodoro Card: Cute Cartoon Companion strictly to the LEFT of the Clock
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            PomodoroCompanionCanvas(
                                character = character,
                                isRunning = pomodoroState.isRunning,
                                isFocusPhase = pomodoroState.isFocusPhase,
                                size = 136.dp
                            )
                            Text(
                                text = when {
                                    pomodoroState.isRunning && pomodoroState.isFocusPhase -> "Studying hard..."
                                    pomodoroState.isRunning && !pomodoroState.isFocusPhase -> "Break sip & rest!"
                                    else -> "Ready to focus"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (pomodoroState.isFocusPhase) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.tertiaryContainer
                                }
                            ) {
                                Text(
                                    text = if (pomodoroState.isFocusPhase) strings.focusMode else strings.breakTime,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = timeText,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 25.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(25 to 5, 50 to 10, 15 to 3).forEach { (f, b) ->
                            FilterChip(
                                selected = pomodoroState.focusMinutes == f,
                                onClick = { onSelectPreset(f, b) },
                                label = { Text("${f}m / ${b}m") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { if (pomodoroState.isRunning) onPause() else onStart() },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("pomodoro_start_pause_button")
                        ) {
                            Icon(
                                if (pomodoroState.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (pomodoroState.isRunning) strings.pause else strings.startFocus)
                        }

                        OutlinedButton(
                            onClick = onReset,
                            modifier = Modifier.height(52.dp).testTag("pomodoro_reset_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = strings.reset)
                        }
                    }

                    if (pomodoroState.isRunning && pomodoroState.isFocusPhase && !pomodoroState.isAppLockVisible) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onReopenAppLock,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Enter Full-Screen App Lock")
                        }
                    }
                }
            }
        }

        // 30-Minute Milestone Rewards Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("pomodoro_milestone_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("30-Minute Milestone Rewards", style = MaterialTheme.typography.titleMedium)
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "$milestoneCount Badge${if (milestoneCount == 1) "" else "s"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (pomodoroState.latestMilestoneReached != null) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Milestone Unlocked: ${pomodoroState.latestMilestoneReached} focused minutes completed! Keep going!",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = onDismissMilestoneReward,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Dismiss milestone reward",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "Cumulative deep focus: $focusedStudyMinutes min · ${30 - minutesIntoCurrentMilestone} min until next reward",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LinearProgressIndicator(
                        progress = { milestoneProgress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Device Focus Permissions Card (shown only outside active focus lock)
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("pomodoro_permissions_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Focus Lock & Device Permissions", style = MaterialTheme.typography.titleMedium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Overlay, Pop-up & Alarm Alerts", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    text = if (platformState.alertsGranted) "Alerts Active" else "Alerts Needed",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (!platformState.alertsGranted) {
                            OutlinedButton(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        FocusLockBridge.openNotificationSettings(context)
                                    }
                                },
                                modifier = Modifier.testTag("grant_pomodoro_alerts_button")
                            ) {
                                Text("Enable Alerts")
                            }
                        } else if (!platformState.overlayHookGranted) {
                            OutlinedButton(
                                onClick = {
                                    if (!FocusLockBridge.openOverlaySettings(context)) {
                                        FocusLockBridge.openNotificationSettings(context)
                                    }
                                },
                                modifier = Modifier.testTag("grant_pomodoro_overlay_hook_button")
                            ) {
                                Text("Overlay Hook")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.PermMedia,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Photos & Files Permission", style = MaterialTheme.typography.labelLarge)
                            Text(
                                text = "Android Photo Picker & Document SAF Ready",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(72.dp))
        }
    }

    // Clean Full-Screen Focus Lock Overlay — strictly timer, status, milestone, warning (if triggered), and companion until timer completes
    if (pomodoroState.isAppLockVisible && pomodoroState.isRunning && pomodoroState.isFocusPhase) {
        Dialog(
            onDismissRequest = { onRecordDistractionAttempt() },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            BackHandler {
                onRecordDistractionAttempt()
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(StudentDarkBackground, StudentPrimaryNavy, InstructorPrimaryPurple)
                        )
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = SoftOffWhite.copy(alpha = 0.14f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.DoNotDisturbOn,
                                contentDescription = null,
                                tint = StudentSoftSkyBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = strings.appLockActive,
                                style = MaterialTheme.typography.labelLarge,
                                color = SoftOffWhite
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = SoftOffWhite.copy(alpha = 0.10f)
                        ) {
                            Text(
                                text = "Distractions blocked: ${pomodoroState.distractionAttemptsBlocked}",
                                style = MaterialTheme.typography.labelMedium,
                                color = SoftOffWhite,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = SoftOffWhite.copy(alpha = 0.10f)
                        ) {
                            Text(
                                text = "Milestone: $minutesIntoCurrentMilestone/30m",
                                style = MaterialTheme.typography.labelMedium,
                                color = StudentSoftSkyBlue,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (!pomodoroState.focusExitWarning.isNullOrBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = PriorityCriticalBg.copy(alpha = 0.92f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = SoftOffWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = pomodoroState.focusExitWarning,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SoftOffWhite,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = onDismissFocusExitWarning) {
                                    Text("OK", color = SoftOffWhite)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(28.dp))

                    // Companion strictly to the LEFT of the clock
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        PomodoroCompanionCanvas(
                            character = character,
                            isRunning = true,
                            isFocusPhase = true,
                            size = 152.dp
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = timeText,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = SoftOffWhite
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Deep Focus Session",
                                style = MaterialTheme.typography.titleMedium,
                                color = StudentSoftSkyBlue
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentChatScreen(
    directChatState: UiState<List<DirectChatMessage>>,
    generalChatState: UiState<List<GeneralChatMessage>>,
    currentUid: String?,
    isCloudAuthenticated: Boolean,
    displayName: String,
    isRecordingAudio: Boolean,
    playingAudioId: String?,
    strings: AppStrings,
    onSendDirectChat: (String, String, String, String, String, String, String) -> Unit,
    onSendGeneralChat: (String, String, String, String, String, String) -> Unit,
    onDeleteGeneralChat: (String) -> Unit,
    onStartAudioRecord: () -> Boolean,
    onStopAudioRecord: () -> VoiceNoteItem?,
    onTogglePlayAudio: (String, String) -> Unit
) {
    var selectedChannel by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedChannel,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = selectedChannel == 0,
                onClick = { selectedChannel = 0 },
                icon = { Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp)) },
                text = { Text(strings.directInstructorChat, maxLines = 1) },
                modifier = Modifier.testTag("chat_tab_direct_instructor")
            )
            Tab(
                selected = selectedChannel == 1,
                onClick = { selectedChannel = 1 },
                icon = { Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(18.dp)) },
                text = { Text(strings.generalStudentChat, maxLines = 1) },
                modifier = Modifier.testTag("chat_tab_general_students")
            )
        }

        Spacer(Modifier.height(8.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedChannel) {
                0 -> DirectAcademicChatTab(
                    messages = (directChatState as? UiState.Success)?.data.orEmpty(),
                    defaultPeerName = "Dr. Tariq Al-Mansoor",
                    isRecordingAudio = isRecordingAudio,
                    playingAudioId = playingAudioId,
                    onSendMessage = onSendDirectChat,
                    onStartAudioRecord = onStartAudioRecord,
                    onStopAudioRecord = onStopAudioRecord,
                    onTogglePlayAudio = onTogglePlayAudio
                )
                1 -> GeneralStudentChatTab(
                    chatState = generalChatState,
                    currentUid = currentUid,
                    isCloudAuthenticated = isCloudAuthenticated,
                    displayName = displayName,
                    isRecordingAudio = isRecordingAudio,
                    playingAudioId = playingAudioId,
                    onSendMessage = onSendGeneralChat,
                    onDeleteMessage = onDeleteGeneralChat,
                    onStartAudioRecord = onStartAudioRecord,
                    onStopAudioRecord = onStopAudioRecord,
                    onTogglePlayAudio = onTogglePlayAudio
                )
            }
        }
    }
}

@Composable
private fun GeneralStudentChatTab(
    chatState: UiState<List<GeneralChatMessage>>,
    currentUid: String?,
    isCloudAuthenticated: Boolean,
    displayName: String,
    isRecordingAudio: Boolean,
    playingAudioId: String?,
    onSendMessage: (String, String, String, String, String, String) -> Unit,
    onDeleteMessage: (String) -> Unit,
    onStartAudioRecord: () -> Boolean,
    onStopAudioRecord: () -> VoiceNoteItem?,
    onTogglePlayAudio: (String, String) -> Unit
) {
    val context = LocalContext.current
    var selectedTag by remember { mutableStateOf("General") }
    var inputText by remember { mutableStateOf("") }
    var pendingName by remember { mutableStateOf("") }
    var pendingUri by remember { mutableStateOf("") }
    var pendingType by remember { mutableStateOf("") }

    val courseTags = listOf("General", "M251", "TM112", "TM351", "BUS110", "EL111")
    val messages = (chatState as? UiState.Success)?.data.orEmpty()

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            takePersistablePermission(context, uri)
            pendingName = resolveDisplayName(context, uri, "Image.jpg")
            pendingUri = uri.toString()
            pendingType = "image"
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            takePersistablePermission(context, uri)
            pendingName = resolveDisplayName(context, uri, "File.pdf")
            pendingUri = uri.toString()
            pendingType = "file"
        }
    }

    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onStartAudioRecord()
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.tertiaryContainer
        ) {
            Text(
                text = "AOU General Student Community Chat · Respectful Academic Peers",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.fillMaxWidth().padding(10.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            courseTags.forEach { tag ->
                FilterChip(
                    selected = selectedTag == tag,
                    onClick = { selectedTag = tag },
                    label = { Text(tag, maxLines = 1) }
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        if (chatState is UiState.Loading) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    val isMine = (!isCloudAuthenticated && msg.authorUid == "demo_user") ||
                        (isCloudAuthenticated && msg.authorUid == currentUid)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth(0.86f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${if (isMine) "You" else msg.authorName} · AOU ${msg.authorBranch} · ${msg.courseTag}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isMine) {
                                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                        } else {
                                            MaterialTheme.colorScheme.tertiary
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isMine) {
                                        IconButton(
                                            onClick = { onDeleteMessage(msg.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete message",
                                                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                                AttachmentPreviewBlock(
                                    attachmentName = msg.attachmentName,
                                    attachmentUri = msg.attachmentUri,
                                    attachmentType = msg.attachmentType,
                                    itemId = "gen_${msg.id}",
                                    playingAudioId = playingAudioId,
                                    onToggleAudio = onTogglePlayAudio,
                                    inverseSurface = isMine
                                )
                                if (msg.messageText.isNotBlank()) {
                                    Text(
                                        text = msg.messageText,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        AttachmentActionPickerRow(
            attachmentName = pendingName,
            attachmentType = pendingType,
            isRecording = isRecordingAudio,
            onPickImage = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onPickFile = { filePicker.launch(arrayOf("*/*")) },
            onToggleVoiceRecord = {
                if (isRecordingAudio) {
                    val saved = onStopAudioRecord()
                    if (saved != null) {
                        pendingName = "${saved.title} (${saved.durationLabel})"
                        pendingUri = saved.filePath
                        pendingType = "audio"
                    }
                } else {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) onStartAudioRecord() else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            onClearAttachment = {
                pendingName = ""
                pendingUri = ""
                pendingType = ""
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Message AOU students ($selectedTag)...") },
                modifier = Modifier.weight(1f).testTag("general_chat_input"),
                maxLines = 2
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    val trimmed = inputText.trim()
                    if (trimmed.isNotEmpty() || pendingUri.isNotBlank()) {
                        onSendMessage(displayName, selectedTag, trimmed, pendingName, pendingUri, pendingType)
                        inputText = ""
                        pendingName = ""
                        pendingUri = ""
                        pendingType = ""
                    }
                },
                enabled = inputText.isNotBlank() || pendingUri.isNotBlank(),
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .testTag("general_chat_send_button")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send to general chat",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun StudentHubScreen(
    tutorMessages: List<StudyTutorMessage>,
    tutorStreaming: Boolean,
    mindMapState: UiState<MindMapResult>?,
    flashcardsState: UiState<List<FlashcardItem>>?,
    studyVisualState: UiState<Bitmap>?,
    forumState: UiState<List<ForumPost>>,
    personalMaterialsState: UiState<List<PersonalMaterial>>,
    courseMaterialsState: UiState<List<CourseMaterial>>,
    voiceNotes: List<VoiceNoteItem>,
    isRecordingVoice: Boolean,
    playingVoiceNoteId: String?,
    transcribingNoteId: String?,
    displayName: String,
    strings: AppStrings,
    onSendTutorPrompt: (String) -> Unit,
    onGenerateMindMap: (String) -> Unit,
    onGenerateFlashcards: (String) -> Unit,
    onGenerateStudyVisual: (String) -> Unit,
    onCreateForumPost: (String, String, String, String, String, String, String) -> Unit,
    onReplyForumPost: (ForumPost, String) -> Unit,
    onAddPersonalMaterial: (String, String, String, String, PersonalMaterialType) -> Unit,
    onDeletePersonalMaterial: (String) -> Unit,
    onStartVoiceRecord: () -> Boolean,
    onStopVoiceRecord: () -> VoiceNoteItem?,
    onToggleVoicePlay: (VoiceNoteItem) -> Unit,
    onToggleAudioPath: (String, String) -> Unit,
    onTranscribeVoiceNote: (VoiceNoteItem) -> Unit,
    onDeleteVoiceNote: (VoiceNoteItem) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        strings.studyAiBooksTab,
        strings.voiceNotes,
        strings.freshmenForum
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEachIndexed { idx, label ->
                FilterChip(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    label = { Text(label, maxLines = 1) },
                    leadingIcon = {
                        Icon(
                            when (idx) {
                                0 -> Icons.Default.AutoAwesome
                                1 -> Icons.Default.Mic
                                else -> Icons.Default.Forum
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTab) {
                0 -> UnifiedStudyHubTab(
                    tutorMessages = tutorMessages,
                    tutorStreaming = tutorStreaming,
                    mindMapState = mindMapState,
                    flashcardsState = flashcardsState,
                    studyVisualState = studyVisualState,
                    personalMaterialsState = personalMaterialsState,
                    courseMaterialsState = courseMaterialsState,
                    strings = strings,
                    onSendPrompt = onSendTutorPrompt,
                    onGenerateMindMap = onGenerateMindMap,
                    onGenerateFlashcards = onGenerateFlashcards,
                    onGenerateStudyVisual = onGenerateStudyVisual,
                    onAddPersonalMaterial = onAddPersonalMaterial,
                    onDeletePersonalMaterial = onDeletePersonalMaterial
                )
                1 -> VoiceNotesTab(
                    voiceNotes = voiceNotes,
                    isRecording = isRecordingVoice,
                    playingId = playingVoiceNoteId,
                    transcribingNoteId = transcribingNoteId,
                    onStartRecord = { onStartVoiceRecord() },
                    onStopRecord = { onStopVoiceRecord() },
                    onTogglePlay = onToggleVoicePlay,
                    onTranscribe = onTranscribeVoiceNote,
                    onDelete = onDeleteVoiceNote
                )
                2 -> FreshmenForumTab(
                    forumState = forumState,
                    displayName = displayName,
                    isRecordingAudio = isRecordingVoice,
                    playingAudioId = playingVoiceNoteId,
                    onCreatePost = onCreateForumPost,
                    onReply = onReplyForumPost,
                    onStartAudioRecord = onStartVoiceRecord,
                    onStopAudioRecord = onStopVoiceRecord,
                    onTogglePlayAudio = onToggleAudioPath
                )
            }
        }
    }
}

@Composable
private fun UnifiedStudyHubTab(
    tutorMessages: List<StudyTutorMessage>,
    tutorStreaming: Boolean,
    mindMapState: UiState<MindMapResult>?,
    flashcardsState: UiState<List<FlashcardItem>>?,
    studyVisualState: UiState<Bitmap>?,
    personalMaterialsState: UiState<List<PersonalMaterial>>,
    courseMaterialsState: UiState<List<CourseMaterial>>,
    strings: AppStrings,
    onSendPrompt: (String) -> Unit,
    onGenerateMindMap: (String) -> Unit,
    onGenerateFlashcards: (String) -> Unit,
    onGenerateStudyVisual: (String) -> Unit,
    onAddPersonalMaterial: (String, String, String, String, PersonalMaterialType) -> Unit,
    onDeletePersonalMaterial: (String) -> Unit
) {
    val context = LocalContext.current
    var showMaterialsDrawer by remember { mutableStateOf(false) }
    var noteTitle by remember { mutableStateOf("") }
    var noteBody by remember { mutableStateOf("") }
    var input by remember { mutableStateOf("") }

    val personalList = (personalMaterialsState as? UiState.Success)?.data.orEmpty()
    val instructorMaterials = (courseMaterialsState as? UiState.Success)?.data.orEmpty()

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            takePersistablePermission(context, uri)
            val name = resolveDisplayName(context, uri, "Study_Material.pdf")
            onAddPersonalMaterial(name, "", uri.toString(), name, PersonalMaterialType.FILE)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // Unified Books / Notes / Materials Header Card (Collapsible)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showMaterialsDrawer = !showMaterialsDrawer },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Book, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "${strings.myBooksCardTitle} (${personalList.size + instructorMaterials.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = strings.myBooksCardSubtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        if (showMaterialsDrawer) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (showMaterialsDrawer) "Collapse materials" else "Expand materials",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                if (showMaterialsDrawer) {
                    OutlinedTextField(
                        value = noteTitle,
                        onValueChange = { noteTitle = it },
                        label = { Text("Note / Book Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = noteBody,
                        onValueChange = { noteBody = it },
                        label = { Text("Paste book excerpt or write study notes...") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (noteTitle.isNotBlank() || noteBody.isNotBlank()) {
                                    onAddPersonalMaterial(
                                        noteTitle.trim().ifBlank { "Study Note" },
                                        noteBody.trim(),
                                        "",
                                        "",
                                        PersonalMaterialType.NOTE
                                    )
                                    noteTitle = ""
                                    noteBody = ""
                                }
                            },
                            enabled = noteTitle.isNotBlank() || noteBody.isNotBlank(),
                            modifier = Modifier.weight(1f).testTag("add_manual_note_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Save Note", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = { filePicker.launch(arrayOf("*/*")) },
                            modifier = Modifier.weight(1f).testTag("upload_manual_file_button")
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Upload Book/File", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    if (personalList.isEmpty() && instructorMaterials.isEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Add study notes, upload a PDF/TXT book, or transcribe a voice memo to unlock book-grounded AI.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        personalList.take(4).forEach { mat ->
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(mat.title, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (mat.noteContent.isNotBlank()) {
                                            Text(mat.noteContent, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                    IconButton(onClick = { onDeletePersonalMaterial(mat.id) }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                        instructorMaterials.take(3).forEach { cm ->
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            ) {
                                Text(
                                    text = "${cm.courseCode}: ${cm.title}",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Quick Book-Only Generators: Mind Map, Flashcards, Study Visual
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onGenerateMindMap(input) },
                enabled = mindMapState !is UiState.Loading,
                modifier = Modifier.testTag("generate_mind_map_button")
            ) {
                Icon(Icons.Default.AccountTree, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(5.dp))
                Text(strings.mindMapLabel, style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(
                onClick = { onGenerateFlashcards(input) },
                enabled = flashcardsState !is UiState.Loading,
                modifier = Modifier.testTag("generate_flashcards_button")
            ) {
                Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(5.dp))
                Text(strings.flashcardsLabel, style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(
                onClick = { onGenerateStudyVisual(input) },
                enabled = studyVisualState !is UiState.Loading,
                modifier = Modifier.testTag("generate_study_visual_button")
            ) {
                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(5.dp))
                Text(strings.studyVisualLabel, style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Generated Mind Map Card
            if (mindMapState != null) {
                item {
                    MindMapResultCard(state = mindMapState)
                }
            }

            // Generated Interactive Flashcards Card
            if (flashcardsState != null) {
                item {
                    FlashcardsCarouselCard(state = flashcardsState)
                }
            }

            // Generated Study Diagram Visual
            if (studyVisualState != null) {
                item {
                    StudyVisualResultCard(state = studyVisualState)
                }
            }

            items(tutorMessages, key = { it.id }) { msg ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (msg.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        Text(
                            text = msg.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (msg.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text(strings.askBooksPlaceholder) },
                modifier = Modifier.weight(1f).testTag("study_hub_prompt_input"),
                maxLines = 3
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    val prompt = input.trim()
                    if (prompt.isNotBlank()) {
                        input = ""
                        onSendPrompt(prompt)
                    }
                },
                enabled = !tutorStreaming && input.isNotBlank(),
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .testTag("study_hub_send_button")
            ) {
                if (tutorStreaming) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
private fun MindMapResultCard(state: UiState<MindMapResult>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountTree, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Book Mind Map", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            when (state) {
                is UiState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                is UiState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                is UiState.Success -> {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = state.data.centerTopic,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    state.data.branches.forEach { branch ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "• ${branch.title}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            branch.subtopics.forEach { sub ->
                                Text(
                                    text = "   – $sub",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlashcardsCarouselCard(state: UiState<List<FlashcardItem>>) {
    var currentIndex by remember(state) { mutableIntStateOf(0) }
    var showAnswer by remember(state) { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Quiz, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Interactive Flashcards", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            when (state) {
                is UiState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                is UiState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                is UiState.Success -> {
                    val items = state.data
                    if (items.isEmpty()) {
                        Text("No flashcards extracted.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        val safeIdx = currentIndex.coerceIn(0, items.lastIndex)
                        val card = items[safeIdx]
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAnswer = !showAnswer }
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (showAnswer) "ANSWER (${safeIdx + 1}/${items.size})" else "QUESTION (${safeIdx + 1}/${items.size}) · Tap to flip",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (showAnswer) card.back else card.front,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(
                                onClick = {
                                    currentIndex = (safeIdx - 1).coerceAtLeast(0)
                                    showAnswer = false
                                },
                                enabled = safeIdx > 0
                            ) { Text("Prev") }
                            TextButton(onClick = { showAnswer = !showAnswer }) {
                                Text(if (showAnswer) "Show Question" else "Reveal Answer")
                            }
                            TextButton(
                                onClick = {
                                    currentIndex = (safeIdx + 1).coerceAtMost(items.lastIndex)
                                    showAnswer = false
                                },
                                enabled = safeIdx < items.lastIndex
                            ) { Text("Next") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudyVisualResultCard(state: UiState<Bitmap>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Generated Study Diagram", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            when (state) {
                is UiState.Loading -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("Generating diagram from your materials...", style = MaterialTheme.typography.bodySmall)
                }
                is UiState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                is UiState.Success -> {
                    Image(
                        bitmap = state.data.asImageBitmap(),
                        contentDescription = "Study diagram generated from course materials",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(MaterialTheme.shapes.small),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

@Composable
private fun FreshmenForumTab(
    forumState: UiState<List<ForumPost>>,
    displayName: String,
    isRecordingAudio: Boolean,
    playingAudioId: String?,
    onCreatePost: (String, String, String, String, String, String, String) -> Unit,
    onReply: (ForumPost, String) -> Unit,
    onStartAudioRecord: () -> Boolean,
    onStopAudioRecord: () -> VoiceNoteItem?,
    onTogglePlayAudio: (String, String) -> Unit
) {
    val context = LocalContext.current
    var showNewPost by remember { mutableStateOf(false) }
    var replyingPost by remember { mutableStateOf<ForumPost?>(null) }
    var replyText by remember { mutableStateOf("") }
    val posts = (forumState as? UiState.Success)?.data.orEmpty()

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Button(
            onClick = { showNewPost = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Forum, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Ask Freshmen & Orientation Community")
        }
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(posts, key = { it.id }) { post ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(post.category, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary)
                            Text("${post.repliesCount} replies", style = MaterialTheme.typography.labelMedium)
                        }
                        Text(post.questionTitle, style = MaterialTheme.typography.titleMedium)
                        Text(post.questionBody, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        AttachmentPreviewBlock(
                            attachmentName = post.attachmentName,
                            attachmentUri = post.attachmentUri,
                            attachmentType = post.attachmentType,
                            itemId = "forum_${post.id}",
                            playingAudioId = playingAudioId,
                            onToggleAudio = onTogglePlayAudio
                        )
                        if (post.latestReply.isNotBlank()) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = "Latest reply: ${post.latestReply}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                        TextButton(onClick = { replyingPost = post }) {
                            Text("Reply")
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }

    if (showNewPost) {
        var title by remember { mutableStateOf("") }
        var body by remember { mutableStateOf("") }
        var cat by remember { mutableStateOf("Orientation") }
        var attName by remember { mutableStateOf("") }
        var attUri by remember { mutableStateOf("") }
        var attType by remember { mutableStateOf("") }

        val imagePicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                takePersistablePermission(context, uri)
                attName = resolveDisplayName(context, uri, "Thread_Image.jpg")
                attUri = uri.toString()
                attType = "image"
            }
        }
        val filePicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                takePersistablePermission(context, uri)
                attName = resolveDisplayName(context, uri, "Attachment.pdf")
                attUri = uri.toString()
                attType = "file"
            }
        }
        val micLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) onStartAudioRecord()
        }

        AlertDialog(
            onDismissRequest = { showNewPost = false },
            title = { Text("New Orientation Q&A Thread") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(value = cat, onValueChange = { cat = it }, label = { Text("Category") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Question Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Details") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                    AttachmentActionPickerRow(
                        attachmentName = attName,
                        attachmentType = attType,
                        isRecording = isRecordingAudio,
                        onPickImage = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        onPickFile = { filePicker.launch(arrayOf("*/*")) },
                        onToggleVoiceRecord = {
                            if (isRecordingAudio) {
                                val saved = onStopAudioRecord()
                                if (saved != null) {
                                    attName = "${saved.title} (${saved.durationLabel})"
                                    attUri = saved.filePath
                                    attType = "audio"
                                }
                            } else {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (granted) onStartAudioRecord() else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onClearAttachment = {
                            attName = ""
                            attUri = ""
                            attType = ""
                        }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (title.isNotBlank() && body.isNotBlank()) {
                        onCreatePost(displayName, cat, title, body, attName, attUri, attType)
                        showNewPost = false
                    }
                }) { Text("Post") }
            },
            dismissButton = { TextButton(onClick = { showNewPost = false }) { Text("Cancel") } }
        )
    }

    replyingPost?.let { post ->
        AlertDialog(
            onDismissRequest = { replyingPost = null },
            title = { Text("Reply to ${post.authorName}") },
            text = {
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    label = { Text("Your helpful reply") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (replyText.isNotBlank()) {
                        onReply(post, replyText.trim())
                        replyText = ""
                        replyingPost = null
                    }
                }) { Text("Submit") }
            },
            dismissButton = { TextButton(onClick = { replyingPost = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun VoiceNotesTab(
    voiceNotes: List<VoiceNoteItem>,
    isRecording: Boolean,
    playingId: String?,
    transcribingNoteId: String?,
    onStartRecord: () -> Unit,
    onStopRecord: () -> Unit,
    onTogglePlay: (VoiceNoteItem) -> Unit,
    onTranscribe: (VoiceNoteItem) -> Unit,
    onDelete: (VoiceNoteItem) -> Unit
) {
    val context = LocalContext.current
    var permissionDeniedMsg by remember { mutableStateOf(false) }

    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDeniedMsg = false
            onStartRecord()
        } else {
            permissionDeniedMsg = true
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (isRecording) "Recording Voice Note..." else "Voice Study Recorder",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        if (isRecording) "Tap Stop to save M4A memo" else "Record lecture audio and transcribe into Study Hub notes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = {
                        if (isRecording) {
                            onStopRecord()
                        } else {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) onStartRecord() else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("voice_record_toggle_button")
                ) {
                    Icon(if (isRecording) Icons.Default.Stop else Icons.Default.Mic, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (isRecording) "Stop" else "Record")
                }
            }
        }

        if (permissionDeniedMsg) {
            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.small) {
                Text(
                    "Microphone permission is required to record voice notes.",
                    modifier = Modifier.padding(10.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(voiceNotes, key = { it.id }) { note ->
                val playing = playingId == note.id
                val transcribing = transcribingNoteId == note.id
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(note.title, style = MaterialTheme.typography.titleMedium)
                                Text(note.durationLabel, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = { onTranscribe(note) },
                                enabled = !transcribing,
                                modifier = Modifier.testTag("transcribe_voice_${note.id}")
                            ) {
                                if (transcribing) {
                                    CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                                Spacer(Modifier.width(4.dp))
                                Text("Transcribe", style = MaterialTheme.typography.labelSmall)
                            }
                            IconButton(onClick = { onTogglePlay(note) }) {
                                Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play/Pause")
                            }
                            IconButton(onClick = { onDelete(note) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DirectAcademicChatTab(
    messages: List<DirectChatMessage>,
    defaultPeerName: String,
    isRecordingAudio: Boolean,
    playingAudioId: String?,
    onSendMessage: (String, String, String, String, String, String, String) -> Unit,
    onStartAudioRecord: () -> Boolean,
    onStopAudioRecord: () -> VoiceNoteItem?,
    onTogglePlayAudio: (String, String) -> Unit
) {
    val context = LocalContext.current
    var peerName by remember { mutableStateOf(defaultPeerName) }
    var courseCode by remember { mutableStateOf("M251") }
    var inputText by remember { mutableStateOf("") }
    var pendingName by remember { mutableStateOf("") }
    var pendingUri by remember { mutableStateOf("") }
    var pendingType by remember { mutableStateOf("") }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            takePersistablePermission(context, uri)
            pendingName = resolveDisplayName(context, uri, "Chat_Image.jpg")
            pendingUri = uri.toString()
            pendingType = "image"
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            takePersistablePermission(context, uri)
            pendingName = resolveDisplayName(context, uri, "Attachment.pdf")
            pendingUri = uri.toString()
            pendingType = "file"
        }
    }

    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onStartAudioRecord()
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.tertiaryContainer
        ) {
            Text(
                text = "Private Academic Chat · Text, Image, File & Voice",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.fillMaxWidth().padding(10.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = peerName,
                onValueChange = { peerName = it },
                label = { Text("Recipient") },
                singleLine = true,
                modifier = Modifier.weight(1.5f)
            )
            OutlinedTextField(
                value = courseCode,
                onValueChange = { courseCode = it },
                label = { Text("Course") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (msg.senderIsSelf) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (msg.senderIsSelf) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth(0.84f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "${if (msg.senderIsSelf) "You" else msg.peerName} · ${msg.courseCode}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (msg.senderIsSelf) {
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                } else {
                                    MaterialTheme.colorScheme.tertiary
                                }
                            )
                            if (msg.imageUri.isNotBlank()) {
                                AsyncImage(
                                    model = msg.imageUri,
                                    contentDescription = "Chat image attachment",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                        .clip(MaterialTheme.shapes.small),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                AttachmentPreviewBlock(
                                    attachmentName = msg.attachmentName,
                                    attachmentUri = msg.attachmentUri,
                                    attachmentType = msg.attachmentType,
                                    itemId = "direct_${msg.id}",
                                    playingAudioId = playingAudioId,
                                    onToggleAudio = onTogglePlayAudio,
                                    inverseSurface = msg.senderIsSelf
                                )
                            }
                            if (msg.messageText.isNotBlank()) {
                                Text(
                                    text = msg.messageText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (msg.senderIsSelf) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        AttachmentActionPickerRow(
            attachmentName = pendingName,
            attachmentType = pendingType,
            isRecording = isRecordingAudio,
            onPickImage = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onPickFile = { filePicker.launch(arrayOf("*/*")) },
            onToggleVoiceRecord = {
                if (isRecordingAudio) {
                    val saved = onStopAudioRecord()
                    if (saved != null) {
                        pendingName = "${saved.title} (${saved.durationLabel})"
                        pendingUri = saved.filePath
                        pendingType = "audio"
                    }
                } else {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) onStartAudioRecord() else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            onClearAttachment = {
                pendingName = ""
                pendingUri = ""
                pendingType = ""
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Write an academic message...") },
                modifier = Modifier.weight(1f),
                maxLines = 2
            )
            Spacer(Modifier.width(6.dp))
            IconButton(
                onClick = {
                    if (inputText.isNotBlank() || pendingUri.isNotBlank()) {
                        val imgUri = if (pendingType == "image") pendingUri else ""
                        onSendMessage(
                            peerName,
                            courseCode,
                            inputText.trim(),
                            imgUri,
                            pendingName,
                            pendingUri,
                            pendingType
                        )
                        inputText = ""
                        pendingName = ""
                        pendingUri = ""
                        pendingType = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}
