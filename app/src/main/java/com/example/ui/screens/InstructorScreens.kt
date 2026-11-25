package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.AppStrings
import com.example.model.CalendarEvent
import com.example.model.CourseMaterial
import com.example.model.CourseMaterialKind
import com.example.model.DirectChatMessage
import com.example.model.EmailPriority
import com.example.model.InboxEmail
import com.example.model.QuestionCluster
import com.example.model.SenderRole
import com.example.model.UiState
import com.example.model.VoiceNoteItem
import com.example.model.safeEnumValue
import kotlinx.coroutines.launch

private fun buildInstantRepeatedQuestionClusters(emails: List<InboxEmail>): List<QuestionCluster> {
    val unansweredStudentEmails = emails.filter {
        safeEnumValue(it.senderRole, SenderRole.STUDENT) == SenderRole.STUDENT && !it.answered
    }
    if (unansweredStudentEmails.isEmpty()) return emptyList()

    return unansweredStudentEmails
        .groupBy { it.courseCode.ifBlank { "AOU" } }
        .values
        .filter { it.size >= 2 }
        .map { group ->
            val code = group.first().courseCode.ifBlank { "AOU" }
            val commonTopic = group.first().subject.take(58)
            QuestionCluster(
                topic = "$code Repeated Inquiry: $commonTopic",
                courseCode = code,
                emailIds = group.map { it.id },
                studentNames = group.map { it.senderName },
                suggestedUnifiedReply = "Dear students, regarding $code ($commonTopic): please follow the updated specification posted in the AOU Course Materials tab and LMS folder."
            )
        }
}

@Composable
fun InstructorDashboardScreen(
    inboxState: UiState<List<InboxEmail>>,
    calendarState: UiState<List<CalendarEvent>>,
    clustersState: UiState<List<QuestionCluster>>,
    analyzingEmailId: String?,
    currentTimeMillis: Long,
    isRecordingAudio: Boolean,
    playingAudioId: String?,
    strings: AppStrings,
    onGroupQuestionsWithAi: () -> Unit,
    onSendUnifiedReply: (QuestionCluster, String) -> Unit,
    onSummarizeAi: (InboxEmail) -> Unit,
    onDraftReplyAi: suspend (InboxEmail) -> Result<String>,
    onReplySingleEmail: (InboxEmail, String) -> Unit,
    onComposeEmail: (String, String, String, String, EmailPriority, String, String, String) -> Unit,
    onStartAudioRecord: () -> Boolean,
    onStopAudioRecord: () -> VoiceNoteItem?,
    onTogglePlayAudio: (String, String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var priorityFilter by remember { mutableStateOf<EmailPriority?>(null) }
    var criticalFirst by remember { mutableStateOf(true) }
    var showCompose by remember { mutableStateOf(false) }
    var activeUnifiedCluster by remember { mutableStateOf<QuestionCluster?>(null) }
    var unifiedReplyDraft by remember { mutableStateOf("") }

    var replyingEmail by remember { mutableStateOf<InboxEmail?>(null) }
    var singleReplyDraft by remember { mutableStateOf("") }
    var draftingEmailId by remember { mutableStateOf<String?>(null) }
    var draftError by remember { mutableStateOf<String?>(null) }

    val events = (calendarState as? UiState.Success)?.data.orEmpty()
    val allEmails = (inboxState as? UiState.Success)?.data.orEmpty()

    val filteredEmails = remember(allEmails, searchQuery, priorityFilter, criticalFirst) {
        val q = searchQuery.trim().lowercase()
        allEmails
            .filter { email ->
                val matchesSearch = q.isBlank() ||
                    email.senderName.lowercase().contains(q) ||
                    email.subject.lowercase().contains(q) ||
                    email.courseCode.lowercase().contains(q) ||
                    email.body.lowercase().contains(q)
                val matchesPrio = priorityFilter == null ||
                    safeEnumValue(email.priority, EmailPriority.NORMAL) == priorityFilter
                matchesSearch && matchesPrio
            }
            .let { list ->
                if (criticalFirst) {
                    list.sortedBy { safeEnumValue(it.priority, EmailPriority.NORMAL).weight }
                } else {
                    list.sortedByDescending { safeEnumValue(it.priority, EmailPriority.NORMAL).weight }
                }
            }
    }

    var selectedClusterIndex by remember { mutableIntStateOf(0) }
    val aiClusters = (clustersState as? UiState.Success)?.data.orEmpty()
    val previewClusters = remember(allEmails) { buildInstantRepeatedQuestionClusters(allEmails) }
    val displayedClusters = if (aiClusters.isNotEmpty()) aiClusters else previewClusters
    val activeCluster = displayedClusters.getOrNull(selectedClusterIndex.coerceIn(0, (displayedClusters.size - 1).coerceAtLeast(0)))

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCompose = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("instructor_compose_email_fab")
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card 1 — Most Repeated Student Questions (Compact, Scannable + Unified Reply)
            item {
                Spacer(Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Forum,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = strings.repeatedQuestions,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                            TextButton(
                                onClick = onGroupQuestionsWithAi,
                                enabled = clustersState !is UiState.Loading,
                                modifier = Modifier.testTag("ai_group_questions_button")
                            ) {
                                if (clustersState is UiState.Loading) {
                                    CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                                }
                                Spacer(Modifier.width(4.dp))
                                Text(strings.groupWithAi, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                            }
                        }

                        if (clustersState is UiState.Error) {
                            Text(
                                text = clustersState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 2
                            )
                        }

                        if (activeCluster == null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.MarkEmailRead,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "All repeated student inquiries have been answered.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            if (displayedClusters.size > 1) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    displayedClusters.forEachIndexed { idx, cluster ->
                                        FilterChip(
                                            selected = idx == selectedClusterIndex,
                                            onClick = { selectedClusterIndex = idx },
                                            label = {
                                                Text(
                                                    text = "${cluster.courseCode} (${cluster.emailIds.size})",
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "${activeCluster.courseCode} · ${activeCluster.studentNames.size} students — ${activeCluster.topic}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2
                            )
                            Text(
                                text = "Students: ${activeCluster.studentNames.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )

                            Button(
                                onClick = {
                                    activeUnifiedCluster = activeCluster
                                    unifiedReplyDraft = activeCluster.suggestedUnifiedReply
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("unified_reply_button_${activeCluster.courseCode}")
                            ) {
                                Icon(Icons.Default.Forum, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("${strings.unifiedReplyAll} (${activeCluster.emailIds.size})", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }

            // Card 2 — Next Up
            item {
                NextUpCountdownCard(
                    events = events,
                    currentTimeMillis = currentTimeMillis,
                    sectionTitle = strings.nextUp
                )
            }

            // 3. Priority-Ordered Important Emails
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = strings.priorityInbox,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search student name, course code (M251), or keyword...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("instructor_email_search")
                    )
                    EmailPriorityFilterAndSortBar(
                        selectedFilter = priorityFilter,
                        criticalFirst = criticalFirst,
                        allLabel = strings.allLabel,
                        onSelectFilter = { priorityFilter = it },
                        onToggleSort = { criticalFirst = !criticalFirst }
                    )
                }
            }

            if (inboxState is UiState.Loading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (inboxState is UiState.Error) {
                item {
                    Text(inboxState.message, color = MaterialTheme.colorScheme.error)
                }
            } else {
                items(filteredEmails, key = { it.id }) { email ->
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PriorityBadge(priority = prio)
                                    Text(
                                        text = email.courseCode,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (email.answered) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Answered",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "Replied",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            Text(
                                text = email.senderName,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.tertiary
                            )
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
                                itemId = "inst_email_${email.id}",
                                playingAudioId = playingAudioId,
                                onToggleAudio = onTogglePlayAudio
                            )

                            if (!email.aiSummary.isNullOrBlank()) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                                        Text(
                                            text = "AI Summary: ${email.aiSummary}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        if (!email.extractedDeadline.isNullOrBlank()) {
                                            Text(
                                                text = "Deadline: ${email.extractedDeadline}",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }

                            if (email.answered && !email.replyText.isNullOrBlank()) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "Your Reply: ${email.replyText}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.fillMaxWidth().padding(10.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onSummarizeAi(email) },
                                    enabled = analyzingEmailId != email.id
                                ) {
                                    if (analyzingEmailId == email.id) {
                                        CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.summarizeAi)
                                }

                                OutlinedButton(
                                    onClick = {
                                        draftError = null
                                        draftingEmailId = email.id
                                        scope.launch {
                                            val res = onDraftReplyAi(email)
                                            draftingEmailId = null
                                            res.onSuccess { draft ->
                                                replyingEmail = email
                                                singleReplyDraft = draft
                                            }.onFailure { err ->
                                                draftError = err.message
                                                replyingEmail = email
                                                singleReplyDraft = "Dear ${email.senderName},\n\nThank you for your inquiry regarding ${email.courseCode}."
                                            }
                                        }
                                    },
                                    enabled = draftingEmailId != email.id
                                ) {
                                    if (draftingEmailId == email.id) {
                                        CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    Text("AI Reply Draft")
                                }

                                Button(
                                    onClick = {
                                        draftError = null
                                        replyingEmail = email
                                        singleReplyDraft = email.replyText ?: ""
                                    }
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Reply")
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(76.dp)) }
        }
    }

    activeUnifiedCluster?.let { cluster ->
        AlertDialog(
            onDismissRequest = { activeUnifiedCluster = null },
            title = { Text("${strings.unifiedReplyAll} (${cluster.emailIds.size})") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Recipients: ${cluster.studentNames.joinToString(", ")}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = unifiedReplyDraft,
                        onValueChange = { unifiedReplyDraft = it },
                        label = { Text("Unified Academic Reply") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (unifiedReplyDraft.isNotBlank()) {
                            onSendUnifiedReply(cluster, unifiedReplyDraft.trim())
                            activeUnifiedCluster = null
                        }
                    }
                ) {
                    Text("Send to All (${cluster.emailIds.size})")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeUnifiedCluster = null }) { Text("Cancel") }
            }
        )
    }

    replyingEmail?.let { email ->
        AlertDialog(
            onDismissRequest = { replyingEmail = null },
            title = { Text("Reply to ${email.senderName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${email.courseCode} · ${email.subject}", style = MaterialTheme.typography.labelLarge)
                    if (!draftError.isNullOrBlank()) {
                        Text(
                            text = "Note: AI draft fallback used (${draftError}).",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    OutlinedTextField(
                        value = singleReplyDraft,
                        onValueChange = { singleReplyDraft = it },
                        label = { Text("Reply Message") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (singleReplyDraft.isNotBlank()) {
                            onReplySingleEmail(email, singleReplyDraft.trim())
                            replyingEmail = null
                        }
                    }
                ) {
                    Text("Send Reply")
                }
            },
            dismissButton = {
                TextButton(onClick = { replyingEmail = null }) { Text("Cancel") }
            }
        )
    }

    if (showCompose) {
        ComposeEmailDialog(
            recipientLabel = "Student Name or AOU Department",
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
fun InstructorMaterialsScreen(
    courseMaterialsState: UiState<List<CourseMaterial>>,
    currentUid: String?,
    isCloudAuthenticated: Boolean,
    isRecordingAudio: Boolean,
    playingAudioId: String?,
    strings: AppStrings,
    onPublishMaterial: (String, String, String, String, String, String, CourseMaterialKind, String, String) -> Unit,
    onDeleteMaterial: (String) -> Unit,
    onStartAudioRecord: () -> Boolean,
    onStopAudioRecord: () -> VoiceNoteItem?,
    onTogglePlayAudio: (String, String) -> Unit
) {
    val context = LocalContext.current
    var authorName by remember { mutableStateOf("Dr. Tariq Al-Mansoor") }
    var courseCode by remember { mutableStateOf("M251") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var linkUrl by remember { mutableStateOf("") }
    var pickedFileName by remember { mutableStateOf("") }
    var pickedAttachmentUri by remember { mutableStateOf("") }
    var pickedAttachmentType by remember { mutableStateOf("") }
    var selectedKind by remember { mutableStateOf(CourseMaterialKind.LECTURE_SLIDES) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            takePersistablePermission(context, uri)
            pickedFileName = resolveDisplayName(context, uri, "Slide_Diagram.jpg")
            pickedAttachmentUri = uri.toString()
            pickedAttachmentType = "image"
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            takePersistablePermission(context, uri)
            val name = resolveDisplayName(context, uri, "Lecture_File.pdf")
            pickedFileName = name
            pickedAttachmentUri = uri.toString()
            pickedAttachmentType = "file"
            if (linkUrl.isBlank()) linkUrl = uri.toString()
        }
    }

    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onStartAudioRecord()
    }

    val materials = (courseMaterialsState as? UiState.Success)?.data.orEmpty()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${strings.uploadMaterial} & Online Links",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Published items automatically feed into students' Study Hub.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = courseCode,
                            onValueChange = { courseCode = it },
                            label = { Text("Course Code") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = authorName,
                            onValueChange = { authorName = it },
                            label = { Text("Faculty Name") },
                            singleLine = true,
                            modifier = Modifier.weight(1.5f)
                        )
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Material / Lecture Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("material_title_input")
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description / Notes for Students") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = linkUrl,
                        onValueChange = { linkUrl = it },
                        label = { Text("Online Link URL (LMS / Teams / Video)") },
                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CourseMaterialKind.entries.forEach { kind ->
                            FilterChip(
                                selected = selectedKind == kind,
                                onClick = { selectedKind = kind },
                                label = { Text(kind.name.replace("_", " ")) }
                            )
                        }
                    }

                    AttachmentActionPickerRow(
                        attachmentName = pickedFileName,
                        attachmentType = pickedAttachmentType,
                        isRecording = isRecordingAudio,
                        onPickImage = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        onPickFile = { filePicker.launch(arrayOf("*/*")) },
                        onToggleVoiceRecord = {
                            if (isRecordingAudio) {
                                val saved = onStopAudioRecord()
                                if (saved != null) {
                                    pickedFileName = "${saved.title} (${saved.durationLabel})"
                                    pickedAttachmentUri = saved.filePath
                                    pickedAttachmentType = "audio"
                                    selectedKind = CourseMaterialKind.RECORDING
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
                            pickedFileName = ""
                            pickedAttachmentUri = ""
                            pickedAttachmentType = ""
                        }
                    )

                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onPublishMaterial(
                                    authorName.trim(),
                                    courseCode.trim(),
                                    title.trim(),
                                    description.trim(),
                                    linkUrl.trim(),
                                    pickedFileName,
                                    selectedKind,
                                    pickedAttachmentUri,
                                    pickedAttachmentType
                                )
                                title = ""
                                description = ""
                                linkUrl = ""
                                pickedFileName = ""
                                pickedAttachmentUri = ""
                                pickedAttachmentType = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("publish_material_button")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Publish Material")
                    }
                }
            }
        }

        item {
            Text(
                text = "Published AOU Course Materials (${materials.size})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (materials.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        text = "No materials published yet. Upload slides, audio notes, or links above.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(materials, key = { it.id }) { mat ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "${mat.courseCode} · ${mat.title}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${mat.materialKind.replace("_", " ")} · By ${mat.authorName}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (mat.description.isNotBlank()) {
                                Text(mat.description, style = MaterialTheme.typography.bodyMedium)
                            }
                            if (mat.linkUrl.isNotBlank()) {
                                Text(
                                    text = mat.linkUrl,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            AttachmentPreviewBlock(
                                attachmentName = mat.fileName,
                                attachmentUri = mat.attachmentUri,
                                attachmentType = mat.attachmentType,
                                itemId = "inst_mat_${mat.id}",
                                playingAudioId = playingAudioId,
                                onToggleAudio = onTogglePlayAudio
                            )
                        }

                        if (!isCloudAuthenticated || mat.authorUid == currentUid) {
                            IconButton(onClick = { onDeleteMaterial(mat.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete material", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
fun InstructorChatAndEmailScreen(
    directChatState: UiState<List<DirectChatMessage>>,
    isRecordingAudio: Boolean,
    playingAudioId: String?,
    strings: AppStrings,
    onSendDirectChat: (String, String, String, String, String, String, String) -> Unit,
    onComposeEmail: (String, String, String, String, EmailPriority, String, String, String) -> Unit,
    onStartAudioRecord: () -> Boolean,
    onStopAudioRecord: () -> VoiceNoteItem?,
    onTogglePlayAudio: (String, String) -> Unit
) {
    var showCompose by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.navChatEmail,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Text, image, file & voice academic chat · Official Email",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = { showCompose = true },
                modifier = Modifier.testTag("instructor_chat_compose_email_button")
            ) {
                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("New Email")
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            DirectAcademicChatTab(
                messages = (directChatState as? UiState.Success)?.data.orEmpty(),
                defaultPeerName = "Yousef Al-Sabah (Student)",
                isRecordingAudio = isRecordingAudio,
                playingAudioId = playingAudioId,
                onSendMessage = onSendDirectChat,
                onStartAudioRecord = onStartAudioRecord,
                onStopAudioRecord = onStopAudioRecord,
                onTogglePlayAudio = onTogglePlayAudio
            )
        }
    }

    if (showCompose) {
        ComposeEmailDialog(
            recipientLabel = "Student Name or Course Cohort",
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
