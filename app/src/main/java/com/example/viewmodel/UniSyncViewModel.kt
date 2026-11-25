package com.example.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.RingtonePlayer
import com.example.audio.VoiceNoteManager
import com.example.data.AiService
import com.example.data.AouRoadmapsData
import com.example.data.LocalAppPreferences
import com.example.data.PreferencesManager
import com.example.data.SampleAcademicData
import com.example.data.UniSyncRepository
import com.example.model.CalendarEvent
import com.example.model.CourseCompletionStatus
import com.example.model.CourseMaterial
import com.example.model.CourseMaterialKind
import com.example.model.DirectChatMessage
import com.example.model.EmailPriority
import com.example.model.EventType
import com.example.model.FlashcardItem
import com.example.model.ForumPost
import com.example.model.GeneralChatMessage
import com.example.model.GpaCourseEntry
import com.example.model.InboxEmail
import com.example.model.MindMapResult
import com.example.model.PersonalMaterial
import com.example.model.PersonalMaterialType
import com.example.model.PomodoroCharacter
import com.example.model.QuestionCluster
import com.example.model.SenderRole
import com.example.model.StudyTutorMessage
import com.example.model.TextScaleOption
import com.example.model.UiState
import com.example.model.UserRole
import com.example.model.VoiceNoteItem
import com.example.model.safeEnumValue
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class PomodoroState(
    val focusMinutes: Int = 25,
    val breakMinutes: Int = 5,
    val remainingSeconds: Int = 25 * 60,
    val isRunning: Boolean = false,
    val isFocusPhase: Boolean = true,
    val isAppLockVisible: Boolean = false,
    val completedSessions: Int = 0,
    val distractionAttemptsBlocked: Int = 0,
    val focusExitWarning: String? = null,
    val latestMilestoneReached: Int? = null
)

class UniSyncViewModel(
    private val repository: UniSyncRepository?,
    private val prefsManager: PreferencesManager,
    private val aiService: AiService,
    private val voiceNoteManager: VoiceNoteManager,
    private val appContext: Context,
    private val currentUserId: String?,
    private val initialDemoRole: UserRole?
) : ViewModel() {

    companion object {
        private const val TAG = "UniSyncVM"
        private const val STOP_TIMEOUT_MS = 5000L
    }

    val isCloudAuthenticated: Boolean = !currentUserId.isNullOrBlank() && repository != null

    val preferences = prefsManager.preferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = LocalAppPreferences(
            role = initialDemoRole ?: UserRole.STUDENT
        )
    )

    private val _isOnline = MutableStateFlow(checkOnlineNow(appContext))
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _currentTimeMillis = MutableStateFlow(System.currentTimeMillis())
    val currentTimeMillis: StateFlow<Long> = _currentTimeMillis.asStateFlow()

    // Demo in-memory backing states (hydrated from DataStore so changes persist across restarts)
    private val _demoInbox = MutableStateFlow(
        if (!isCloudAuthenticated) {
            if ((initialDemoRole ?: UserRole.STUDENT) == UserRole.INSTRUCTOR) {
                SampleAcademicData.instructorEmails("demo_user")
            } else {
                SampleAcademicData.studentEmails("demo_user")
            }
        } else emptyList()
    )
    private val _demoCalendar = MutableStateFlow(
        if (!isCloudAuthenticated) {
            if ((initialDemoRole ?: UserRole.STUDENT) == UserRole.INSTRUCTOR) {
                SampleAcademicData.instructorEvents("demo_user")
            } else {
                SampleAcademicData.studentEvents("demo_user")
            }
        } else emptyList()
    )
    private val _demoPersonalMaterials = MutableStateFlow<List<PersonalMaterial>>(emptyList())
    private val _demoCourseMaterials = MutableStateFlow<List<CourseMaterial>>(emptyList())
    private val _demoForumPosts = MutableStateFlow(
        if (!isCloudAuthenticated) SampleAcademicData.sampleForumPosts() else emptyList()
    )
    private val _demoDirectChats = MutableStateFlow(
        if (!isCloudAuthenticated) {
            SampleAcademicData.sampleDirectChats("demo_user", initialDemoRole ?: UserRole.STUDENT)
        } else emptyList()
    )
    private val _demoGeneralChat = MutableStateFlow(
        if (!isCloudAuthenticated) SampleAcademicData.sampleGeneralStudentChats() else emptyList()
    )

    val inboxState: StateFlow<UiState<List<InboxEmail>>> = buildInboxFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val calendarState: StateFlow<UiState<List<CalendarEvent>>> = buildCalendarFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val personalMaterialsState: StateFlow<UiState<List<PersonalMaterial>>> = buildPersonalMaterialsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val courseMaterialsState: StateFlow<UiState<List<CourseMaterial>>> = buildCourseMaterialsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val forumPostsState: StateFlow<UiState<List<ForumPost>>> = buildForumPostsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val directChatState: StateFlow<UiState<List<DirectChatMessage>>> = buildDirectChatFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val generalChatState: StateFlow<UiState<List<GeneralChatMessage>>> = buildGeneralChatFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    // Pomodoro State
    private val _pomodoroState = MutableStateFlow(PomodoroState())
    val pomodoroState: StateFlow<PomodoroState> = _pomodoroState.asStateFlow()
    private var pomodoroJob: Job? = null

    // Active Smart Alarm Overlay
    private val _ringingAlarm = MutableStateFlow<CalendarEvent?>(null)
    val ringingAlarm: StateFlow<CalendarEvent?> = _ringingAlarm.asStateFlow()
    private val dismissedAlarmIds = mutableSetOf<String>()

    // Book-Grounded Study AI States
    private val _tutorMessages = MutableStateFlow(
        listOf(
            StudyTutorMessage(
                id = "welcome",
                isUser = false,
                text = "Study Hub AI answers strictly from your uploaded books, written notes, transcribed voice memos, and instructor materials inside UniSync."
            )
        )
    )
    val tutorMessages: StateFlow<List<StudyTutorMessage>> = _tutorMessages.asStateFlow()
    private val _tutorStreaming = MutableStateFlow(false)
    val tutorStreaming: StateFlow<Boolean> = _tutorStreaming.asStateFlow()

    private val _mindMapState = MutableStateFlow<UiState<MindMapResult>?>(null)
    val mindMapState: StateFlow<UiState<MindMapResult>?> = _mindMapState.asStateFlow()

    private val _flashcardsState = MutableStateFlow<UiState<List<FlashcardItem>>?>(null)
    val flashcardsState: StateFlow<UiState<List<FlashcardItem>>?> = _flashcardsState.asStateFlow()

    private val _studyVisualState = MutableStateFlow<UiState<Bitmap>?>(null)
    val studyVisualState: StateFlow<UiState<Bitmap>?> = _studyVisualState.asStateFlow()

    private val _transcribingNoteId = MutableStateFlow<String?>(null)
    val transcribingNoteId: StateFlow<String?> = _transcribingNoteId.asStateFlow()

    private val _clustersState = MutableStateFlow<UiState<List<QuestionCluster>>>(UiState.Success(emptyList()))
    val clustersState: StateFlow<UiState<List<QuestionCluster>>> = _clustersState.asStateFlow()

    private val _aiActionMessage = MutableStateFlow<String?>(null)
    val aiActionMessage: StateFlow<String?> = _aiActionMessage.asStateFlow()

    private val _aiAnalyzingEmailId = MutableStateFlow<String?>(null)
    val aiAnalyzingEmailId: StateFlow<String?> = _aiAnalyzingEmailId.asStateFlow()

    // Voice Notes & Audio Attachments
    private val _voiceNotes = MutableStateFlow(voiceNoteManager.listSavedNotes())
    val voiceNotes: StateFlow<List<VoiceNoteItem>> = _voiceNotes.asStateFlow()
    private val _isRecordingVoice = MutableStateFlow(false)
    val isRecordingVoice: StateFlow<Boolean> = _isRecordingVoice.asStateFlow()
    private val _playingVoiceNoteId = MutableStateFlow<String?>(null)
    val playingVoiceNoteId: StateFlow<String?> = _playingVoiceNoteId.asStateFlow()

    init {
        registerConnectivityCallback(appContext)
        startClockTicker()
        if (!isCloudAuthenticated) {
            viewModelScope.launch {
                val savedPersonal = prefsManager.localPersonalMaterialsFlow.first()
                if (savedPersonal.isNotEmpty()) {
                    _demoPersonalMaterials.value = savedPersonal
                }
                val savedCourseMat = prefsManager.localCourseMaterialsFlow.first()
                if (savedCourseMat.isNotEmpty()) {
                    _demoCourseMaterials.value = savedCourseMat
                }
                val savedEvents = prefsManager.localCalendarEventsFlow.first()
                if (!savedEvents.isNullOrEmpty()) {
                    _demoCalendar.value = savedEvents
                }
                val savedForum = prefsManager.localForumPostsFlow.first()
                if (!savedForum.isNullOrEmpty()) {
                    _demoForumPosts.value = savedForum
                }
            }
        }
        if (isCloudAuthenticated && repository != null) {
            viewModelScope.launch {
                val p = preferences.value
                repository.ensureUserProfile(
                    displayName = "AOU Academic User",
                    role = p.role,
                    aouBranch = p.aouBranch,
                    majorId = p.majorId,
                    preferredLanguage = p.languageCode,
                    darkMode = p.darkMode,
                    pomodoroCharacter = p.pomodoroCharacter
                )
                repository.seedInitialWorkspaceIfEmpty(p.role)
            }
        }
    }

    private fun <T> wrapFirestoreFlow(
        upstream: Flow<T>,
        errorMsg: String
    ): Flow<UiState<T>> = upstream
        .map<T, UiState<T>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, errorMsg, rootError)
            emit(UiState.Error(rootError.message ?: errorMsg))
        }

    private fun buildInboxFlow(): Flow<UiState<List<InboxEmail>>> =
        if (isCloudAuthenticated && repository != null && currentUserId != null) {
            wrapFirestoreFlow(repository.observeInbox(currentUserId), "Failed to observe inbox")
        } else {
            _demoInbox.map { UiState.Success(it) }
        }

    private fun buildCalendarFlow(): Flow<UiState<List<CalendarEvent>>> =
        if (isCloudAuthenticated && repository != null && currentUserId != null) {
            wrapFirestoreFlow(repository.observeCalendarEvents(currentUserId), "Failed to observe calendar")
        } else {
            _demoCalendar.map { UiState.Success(it) }
        }

    private fun buildPersonalMaterialsFlow(): Flow<UiState<List<PersonalMaterial>>> =
        if (isCloudAuthenticated && repository != null && currentUserId != null) {
            wrapFirestoreFlow(repository.observePersonalMaterials(currentUserId), "Failed to observe personal materials")
        } else {
            _demoPersonalMaterials.map { UiState.Success(it) }
        }

    private fun buildCourseMaterialsFlow(): Flow<UiState<List<CourseMaterial>>> =
        if (isCloudAuthenticated && repository != null) {
            wrapFirestoreFlow(repository.observeCourseMaterials(), "Failed to observe course materials")
        } else {
            _demoCourseMaterials.map { UiState.Success(it) }
        }

    private fun buildForumPostsFlow(): Flow<UiState<List<ForumPost>>> =
        if (isCloudAuthenticated && repository != null) {
            wrapFirestoreFlow(repository.observeForumPosts(), "Failed to observe forum posts")
        } else {
            _demoForumPosts.map { UiState.Success(it) }
        }

    private fun buildDirectChatFlow(): Flow<UiState<List<DirectChatMessage>>> =
        if (isCloudAuthenticated && repository != null && currentUserId != null) {
            wrapFirestoreFlow(repository.observeDirectChat(currentUserId), "Failed to observe direct chats")
        } else {
            _demoDirectChats.map { UiState.Success(it) }
        }

    private fun buildGeneralChatFlow(): Flow<UiState<List<GeneralChatMessage>>> =
        if (isCloudAuthenticated && repository != null) {
            wrapFirestoreFlow(repository.observeGeneralStudentChat(), "Failed to observe general student chat")
        } else {
            _demoGeneralChat.map { UiState.Success(it) }
        }

    private fun startClockTicker() {
        viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                _currentTimeMillis.value = now
                val events = (calendarState.value as? UiState.Success)?.data.orEmpty()
                val dueAlarm = events.firstOrNull { ev ->
                    ev.alarmEnabled &&
                        ev.id !in dismissedAlarmIds &&
                        ev.triggerEpochMillis in (now - 15_000L)..now
                }
                if (dueAlarm != null && _ringingAlarm.value == null) {
                    triggerSmartAlarm(dueAlarm)
                }
                delay(1000L)
            }
        }
    }

    fun triggerSmartAlarm(event: CalendarEvent) {
        _ringingAlarm.value = event
        RingtonePlayer.startAlarmLoop(event.ringtoneId, event.volumePercent.toInt(), viewModelScope)
    }

    fun dismissSmartAlarm() {
        _ringingAlarm.value?.let { dismissedAlarmIds.add(it.id) }
        _ringingAlarm.value = null
        RingtonePlayer.stop()
    }

    fun snoozeSmartAlarm5Min() {
        val current = _ringingAlarm.value ?: return
        dismissedAlarmIds.add(current.id)
        _ringingAlarm.value = null
        RingtonePlayer.stop()
        val snoozeTitle = if (current.title.endsWith("(Snoozed)")) current.title else "${current.title} (Snoozed)"
        addCalendarEvent(
            title = snoozeTitle,
            courseCode = current.courseCode,
            eventType = safeEnumValue(current.eventType, EventType.ALARM),
            triggerEpochMillis = System.currentTimeMillis() + 5 * 60_000L,
            alarmEnabled = true,
            ringtoneId = current.ringtoneId,
            volumePercent = current.volumePercent.toInt(),
            notes = current.notes
        )
    }

    // Preferences & Role Actions
    fun setRole(role: UserRole) {
        viewModelScope.launch {
            prefsManager.setRole(role)
            if (isCloudAuthenticated && repository != null) {
                repository.updateProfileFields(mapOf("role" to role.name))
            } else {
                _demoInbox.value = if (role == UserRole.INSTRUCTOR) {
                    SampleAcademicData.instructorEmails("demo_user")
                } else {
                    SampleAcademicData.studentEmails("demo_user")
                }
                val savedEvents = prefsManager.localCalendarEventsFlow.first()
                if (savedEvents.isNullOrEmpty()) {
                    _demoCalendar.value = if (role == UserRole.INSTRUCTOR) {
                        SampleAcademicData.instructorEvents("demo_user")
                    } else {
                        SampleAcademicData.studentEvents("demo_user")
                    }
                }
                _demoDirectChats.value = SampleAcademicData.sampleDirectChats("demo_user", role)
            }
        }
    }

    fun setLanguage(code: String) {
        viewModelScope.launch {
            prefsManager.setLanguage(code)
            if (isCloudAuthenticated && repository != null) {
                repository.updateProfileFields(mapOf("preferredLanguage" to code))
            }
        }
    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            val next = !preferences.value.darkMode
            prefsManager.setDarkMode(next)
            if (isCloudAuthenticated && repository != null) {
                repository.updateProfileFields(mapOf("darkMode" to next))
            }
        }
    }

    fun setAouBranch(branch: String) {
        viewModelScope.launch {
            prefsManager.setAouBranch(branch)
            if (isCloudAuthenticated && repository != null) {
                repository.updateProfileFields(mapOf("aouBranch" to branch))
            }
        }
    }

    fun setMajorId(majorId: String) {
        viewModelScope.launch {
            prefsManager.setMajorId(majorId)
            if (isCloudAuthenticated && repository != null) {
                repository.updateProfileFields(mapOf("majorId" to majorId))
            }
        }
    }

    fun saveProfilePhotoFromUri(uri: Uri) {
        viewModelScope.launch {
            val savedPath = prefsManager.saveAvatarFromUri(uri)
            if (savedPath != null) {
                _aiActionMessage.value = "Profile photo updated."
            } else {
                _aiActionMessage.value = "Unable to load selected photo."
            }
        }
    }

    fun setPomodoroCharacter(character: PomodoroCharacter) {
        viewModelScope.launch {
            prefsManager.setPomodoroCharacter(character)
            if (isCloudAuthenticated && repository != null) {
                repository.updateProfileFields(mapOf("pomodoroCharacter" to character.name))
            }
        }
    }

    fun setTextScale(option: TextScaleOption) {
        viewModelScope.launch {
            prefsManager.setTextScale(option)
        }
    }

    fun setCourseStatus(majorId: String, courseCode: String, status: CourseCompletionStatus) {
        viewModelScope.launch {
            prefsManager.setCourseStatus(majorId, courseCode, status)
        }
    }

    fun updateGpaState(courses: List<GpaCourseEntry>, priorGpa: String, priorCredits: String) {
        viewModelScope.launch {
            prefsManager.saveGpaState(courses, priorGpa, priorCredits)
        }
    }

    fun clearAiMessage() {
        _aiActionMessage.value = null
    }

    // Pomodoro Controls
    private var focusSecondAccumulator: Int = 0

    fun setPomodoroPreset(focusMin: Int, breakMin: Int) {
        pomodoroJob?.cancel()
        focusSecondAccumulator = 0
        _pomodoroState.value = PomodoroState(
            focusMinutes = focusMin,
            breakMinutes = breakMin,
            remainingSeconds = focusMin * 60,
            isRunning = false,
            isFocusPhase = true,
            isAppLockVisible = false,
            completedSessions = _pomodoroState.value.completedSessions,
            latestMilestoneReached = _pomodoroState.value.latestMilestoneReached
        )
    }

    fun startPomodoro() {
        val st = _pomodoroState.value
        if (st.isRunning) return
        _pomodoroState.update {
            it.copy(
                isRunning = true,
                isAppLockVisible = it.isFocusPhase,
                focusExitWarning = null
            )
        }
        pomodoroJob?.cancel()
        pomodoroJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val curr = _pomodoroState.value
                if (!curr.isRunning) break

                if (curr.isFocusPhase) {
                    focusSecondAccumulator += 1
                    if (focusSecondAccumulator >= 60) {
                        focusSecondAccumulator = 0
                        awardFocusedStudyMinutes(1)
                    }
                }

                if (curr.remainingSeconds > 1) {
                    _pomodoroState.update { it.copy(remainingSeconds = it.remainingSeconds - 1) }
                } else {
                    if (curr.isFocusPhase) {
                        if (focusSecondAccumulator >= 30) {
                            focusSecondAccumulator = 0
                            awardFocusedStudyMinutes(1)
                        } else {
                            focusSecondAccumulator = 0
                        }
                        RingtonePlayer.playTone("crystal_chime", 75, viewModelScope)
                        _pomodoroState.update {
                            it.copy(
                                isFocusPhase = false,
                                remainingSeconds = it.breakMinutes * 60,
                                isAppLockVisible = false,
                                focusExitWarning = null,
                                completedSessions = it.completedSessions + 1
                            )
                        }
                    } else {
                        RingtonePlayer.playTone("academic_bell", 75, viewModelScope)
                        _pomodoroState.update {
                            it.copy(
                                isRunning = false,
                                isFocusPhase = true,
                                remainingSeconds = it.focusMinutes * 60,
                                isAppLockVisible = false,
                                focusExitWarning = null
                            )
                        }
                        break
                    }
                }
            }
        }
    }

    private suspend fun awardFocusedStudyMinutes(delta: Int) {
        val prev = preferences.value.focusedStudyMinutes
        val updated = prefsManager.addFocusedStudyMinutes(delta)
        if (updated / 30 > prev / 30) {
            val milestoneMinutes = (updated / 30) * 30
            RingtonePlayer.playTone("crystal_chime", 85, viewModelScope)
            _pomodoroState.update { it.copy(latestMilestoneReached = milestoneMinutes) }
            _aiActionMessage.value = "Milestone Unlocked! $milestoneMinutes total focused study minutes earned!"
        }
    }

    fun pausePomodoro() {
        pomodoroJob?.cancel()
        _pomodoroState.update {
            it.copy(
                isRunning = false,
                isAppLockVisible = false,
                focusExitWarning = null
            )
        }
    }

    fun resetPomodoro() {
        pomodoroJob?.cancel()
        focusSecondAccumulator = 0
        val st = _pomodoroState.value
        _pomodoroState.value = st.copy(
            remainingSeconds = st.focusMinutes * 60,
            isRunning = false,
            isFocusPhase = true,
            isAppLockVisible = false,
            distractionAttemptsBlocked = 0,
            focusExitWarning = null
        )
    }

    fun reopenFocusLockOverlay() {
        val st = _pomodoroState.value
        if (st.isRunning && st.isFocusPhase) {
            _pomodoroState.update { it.copy(isAppLockVisible = true) }
        }
    }

    fun recordDistractionAttempt() {
        val st = _pomodoroState.value
        if (st.isRunning && st.isFocusPhase) {
            val count = st.distractionAttemptsBlocked + 1
            _pomodoroState.update {
                it.copy(
                    distractionAttemptsBlocked = count,
                    focusExitWarning = "Focus Lock Active ($count attempt${if (count > 1) "s" else ""} blocked). Stay focused until the timer finishes."
                )
            }
        }
    }

    fun dismissFocusExitWarning() {
        _pomodoroState.update { it.copy(focusExitWarning = null) }
    }

    fun dismissMilestoneReward() {
        _pomodoroState.update { it.copy(latestMilestoneReached = null) }
    }

    fun unlockFocusOverlay() {
        pausePomodoro()
    }

    // Inbox Operations (with attachment support)
    fun composeEmail(
        recipientOrSender: String,
        courseCode: String,
        subject: String,
        body: String,
        priority: EmailPriority,
        attachmentName: String = "",
        attachmentUri: String = "",
        attachmentType: String = ""
    ) {
        val role = preferences.value.role
        val email = InboxEmail(
            id = UUID.randomUUID().toString(),
            userId = currentUserId ?: "demo_user",
            senderName = "Sent to $recipientOrSender",
            senderRole = if (role == UserRole.STUDENT) SenderRole.STUDENT.name else SenderRole.INSTRUCTOR.name,
            courseCode = courseCode.ifBlank { "AOU" },
            subject = subject,
            body = body,
            priority = priority.name,
            answered = true,
            replyText = "Dispatched via UniSync",
            attachmentName = attachmentName,
            attachmentUri = attachmentUri,
            attachmentType = attachmentType
        )
        viewModelScope.launch {
            if (isCloudAuthenticated && repository != null) {
                val res = repository.addInboxEmail(email)
                _aiActionMessage.value = if (res.isSuccess) "Email sent & archived." else res.exceptionOrNull()?.message
            } else {
                _demoInbox.update { listOf(email.copy(createdAt = Timestamp.now())) + it }
                _aiActionMessage.value = "Email sent in local workspace."
            }
        }
    }

    fun summarizeEmailWithAi(email: InboxEmail) {
        viewModelScope.launch {
            _aiAnalyzingEmailId.value = email.id
            val result = aiService.analyzeEmail(email)
            _aiAnalyzingEmailId.value = null
            result.onSuccess { analysis ->
                val summaryText = "${analysis.summary} (Action: ${analysis.actionItem})"
                if (isCloudAuthenticated && repository != null) {
                    repository.updateInboxAiSummary(email.id, summaryText, analysis.extractedDeadline)
                } else {
                    _demoInbox.update { list ->
                        list.map {
                            if (it.id == email.id) {
                                it.copy(aiSummary = summaryText, extractedDeadline = analysis.extractedDeadline)
                            } else it
                        }
                    }
                }
                _aiActionMessage.value = "AI extracted deadline: ${analysis.extractedDeadline}"
            }.onFailure { err ->
                _aiActionMessage.value = err.message ?: "AI analysis failed."
            }
        }
    }

    fun addExtractedDeadlineToCalendar(email: InboxEmail) {
        val trigger = System.currentTimeMillis() + 24 * 3600_000L
        addCalendarEvent(
            title = "${email.courseCode}: ${email.subject.take(45)}",
            courseCode = email.courseCode,
            eventType = EventType.ASSIGNMENT,
            triggerEpochMillis = trigger,
            alarmEnabled = true,
            ringtoneId = "academic_bell",
            volumePercent = 85,
            notes = email.extractedDeadline ?: email.aiSummary ?: email.subject
        )
        _aiActionMessage.value = "Added deadline & smart alarm to Calendar."
    }

    fun groupRepeatedStudentQuestionsWithAi() {
        val emails = (inboxState.value as? UiState.Success)?.data.orEmpty()
        viewModelScope.launch {
            _clustersState.value = UiState.Loading
            val res = aiService.clusterRepeatedStudentQuestions(emails)
            res.onSuccess { clusters ->
                _clustersState.value = UiState.Success(clusters)
                if (clusters.isEmpty()) {
                    _aiActionMessage.value = "No unanswered student emails to group."
                }
            }.onFailure { err ->
                _clustersState.value = UiState.Error(err.message ?: "AI grouping failed.")
            }
        }
    }

    fun sendUnifiedReplyToCluster(cluster: QuestionCluster, replyText: String) {
        viewModelScope.launch {
            if (isCloudAuthenticated && repository != null) {
                val res = repository.replyToInboxEmails(cluster.emailIds, replyText)
                if (res.isSuccess) {
                    _aiActionMessage.value = "Unified reply sent to ${cluster.emailIds.size} students."
                    _clustersState.update { state ->
                        if (state is UiState.Success) {
                            UiState.Success(state.data.filterNot { it.topic == cluster.topic })
                        } else state
                    }
                } else {
                    _aiActionMessage.value = res.exceptionOrNull()?.message
                }
            } else {
                val idSet = cluster.emailIds.toSet()
                _demoInbox.update { list ->
                    list.map { if (it.id in idSet) it.copy(answered = true, replyText = replyText) else it }
                }
                _clustersState.update { state ->
                    if (state is UiState.Success) {
                        UiState.Success(state.data.filterNot { it.topic == cluster.topic })
                    } else state
                }
                _aiActionMessage.value = "Unified reply sent to ${cluster.emailIds.size} students."
            }
        }
    }

    fun replyToSingleEmail(email: InboxEmail, replyText: String) {
        viewModelScope.launch {
            if (isCloudAuthenticated && repository != null) {
                val res = repository.replyToInboxEmails(listOf(email.id), replyText)
                _aiActionMessage.value = if (res.isSuccess) "Reply sent." else res.exceptionOrNull()?.message
            } else {
                _demoInbox.update { list ->
                    list.map { if (it.id == email.id) it.copy(answered = true, replyText = replyText) else it }
                }
                _aiActionMessage.value = "Reply sent."
            }
        }
    }

    suspend fun draftReplyWithAi(email: InboxEmail): Result<String> =
        aiService.draftInstructorReply(email)

    // Calendar & Alarm Operations
    fun addCalendarEvent(
        title: String,
        courseCode: String,
        eventType: EventType,
        triggerEpochMillis: Long,
        alarmEnabled: Boolean,
        ringtoneId: String,
        volumePercent: Int,
        notes: String
    ) {
        val event = CalendarEvent(
            id = UUID.randomUUID().toString(),
            userId = currentUserId ?: "demo_user",
            title = title,
            courseCode = courseCode,
            eventType = eventType.name,
            triggerEpochMillis = triggerEpochMillis,
            alarmEnabled = alarmEnabled,
            ringtoneId = ringtoneId,
            volumePercent = volumePercent.toLong(),
            notes = notes
        )
        viewModelScope.launch {
            if (isCloudAuthenticated && repository != null) {
                val res = repository.addCalendarEvent(event)
                if (res.isFailure) {
                    _aiActionMessage.value = res.exceptionOrNull()?.message
                }
            } else {
                val updated = (_demoCalendar.value + event).sortedBy { ev -> ev.triggerEpochMillis }
                _demoCalendar.value = updated
                prefsManager.saveLocalCalendarEvents(updated)
            }
        }
    }

    fun deleteCalendarEvent(eventId: String) {
        viewModelScope.launch {
            if (isCloudAuthenticated && repository != null) {
                repository.deleteCalendarEvent(eventId)
            } else {
                val updated = _demoCalendar.value.filterNot { it.id == eventId }
                _demoCalendar.value = updated
                prefsManager.saveLocalCalendarEvents(updated)
            }
        }
    }

    // Build Strict Book-Grounded Corpus ONLY from student's own materials, notes, transcripts, and instructor materials
    private fun buildStudyMaterialsCorpus(): String {
        val personal = (personalMaterialsState.value as? UiState.Success)?.data.orEmpty()
        val instructorMats = (courseMaterialsState.value as? UiState.Success)?.data.orEmpty()

        val sb = StringBuilder()
        personal.forEach { m ->
            sb.appendLine("--- STUDENT MATERIAL: ${m.title} (${m.materialType}) ---")
            if (m.noteContent.isNotBlank()) sb.appendLine(m.noteContent)
            if (m.fileName.isNotBlank()) sb.appendLine("File: ${m.fileName}")
            if (m.fileUri.isNotBlank()) {
                val snippet = PreferencesManager.extractReadableStudySnippet(appContext, Uri.parse(m.fileUri))
                if (snippet.isNotBlank()) sb.appendLine(snippet)
            }
        }
        instructorMats.forEach { cm ->
            sb.appendLine("--- COURSE MATERIAL: ${cm.courseCode} ${cm.title} ---")
            if (cm.description.isNotBlank()) sb.appendLine(cm.description)
            if (cm.fileName.isNotBlank()) sb.appendLine("File: ${cm.fileName}")
        }
        return sb.toString().trim()
    }

    // Book-Grounded Study AI Tutor
    fun sendTutorPrompt(prompt: String) {
        if (prompt.isBlank() || _tutorStreaming.value) return
        val majorName = AouRoadmapsData.allMajors.firstOrNull { it.id == preferences.value.majorId }?.name
            ?: "Computer Science"
        val corpus = buildStudyMaterialsCorpus()
        val userMsg = StudyTutorMessage(UUID.randomUUID().toString(), isUser = true, text = prompt)
        val aiMsgId = UUID.randomUUID().toString()
        _tutorMessages.update { it + userMsg + StudyTutorMessage(aiMsgId, isUser = false, text = "...") }
        _tutorStreaming.value = true
        viewModelScope.launch {
            try {
                aiService.streamStudyTutor(prompt, corpus, majorName).collect { partial ->
                    _tutorMessages.update { list ->
                        list.map { if (it.id == aiMsgId) it.copy(text = partial) else it }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _tutorMessages.update { list ->
                    list.map {
                        if (it.id == aiMsgId) {
                            it.copy(text = "Error: ${e.localizedMessage ?: "Unable to reach Study Assistant."}")
                        } else it
                    }
                }
            } finally {
                _tutorStreaming.value = false
            }
        }
    }

    fun transcribeVoiceNoteToNotes(note: VoiceNoteItem) {
        viewModelScope.launch {
            _transcribingNoteId.value = note.id
            val res = aiService.transcribeVoiceNote(note.filePath)
            _transcribingNoteId.value = null
            res.onSuccess { transcript ->
                addPersonalMaterial(
                    title = "Voice Transcript: ${note.title}",
                    noteContent = transcript,
                    fileUri = note.filePath,
                    fileName = "${note.title}.m4a",
                    type = PersonalMaterialType.VOICE_TRANSCRIPT
                )
                _aiActionMessage.value = "Voice note transcribed and saved into your study notes."
            }.onFailure { err ->
                _aiActionMessage.value = err.message ?: "Transcription failed."
            }
        }
    }

    fun generateStudyMindMap(topic: String) {
        viewModelScope.launch {
            _mindMapState.value = UiState.Loading
            val res = aiService.generateBookMindMap(buildStudyMaterialsCorpus(), topic)
            _mindMapState.value = res.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "Failed to generate mind map.") }
            )
        }
    }

    fun generateStudyFlashcards(topic: String) {
        viewModelScope.launch {
            _flashcardsState.value = UiState.Loading
            val res = aiService.generateBookFlashcards(buildStudyMaterialsCorpus(), topic)
            _flashcardsState.value = res.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "Failed to generate flashcards.") }
            )
        }
    }

    fun generateStudyVisual(topic: String) {
        viewModelScope.launch {
            _studyVisualState.value = UiState.Loading
            val res = aiService.generateBookStudyVisual(buildStudyMaterialsCorpus(), topic)
            _studyVisualState.value = res.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "Failed to generate study diagram.") }
            )
        }
    }

    // Personal Materials
    fun addPersonalMaterial(
        title: String,
        noteContent: String,
        fileUri: String,
        fileName: String,
        type: PersonalMaterialType
    ) {
        val mat = PersonalMaterial(
            id = UUID.randomUUID().toString(),
            userId = currentUserId ?: "demo_user",
            title = title,
            noteContent = noteContent,
            fileUri = fileUri,
            fileName = fileName,
            materialType = type.name
        )
        viewModelScope.launch {
            if (isCloudAuthenticated && repository != null) {
                repository.addPersonalMaterial(mat)
            } else {
                val updated = listOf(mat.copy(createdAt = Timestamp.now())) + _demoPersonalMaterials.value
                _demoPersonalMaterials.value = updated
                prefsManager.saveLocalPersonalMaterials(updated)
            }
        }
    }

    fun deletePersonalMaterial(materialId: String) {
        viewModelScope.launch {
            if (isCloudAuthenticated && repository != null) {
                repository.deletePersonalMaterial(materialId)
            } else {
                val updated = _demoPersonalMaterials.value.filterNot { it.id == materialId }
                _demoPersonalMaterials.value = updated
                prefsManager.saveLocalPersonalMaterials(updated)
            }
        }
    }

    // Instructor Course Materials
    fun publishCourseMaterial(
        authorName: String,
        courseCode: String,
        title: String,
        description: String,
        linkUrl: String,
        fileName: String,
        kind: CourseMaterialKind,
        attachmentUri: String = "",
        attachmentType: String = ""
    ) {
        val mat = CourseMaterial(
            id = UUID.randomUUID().toString(),
            authorUid = currentUserId ?: "demo_instructor",
            authorName = authorName.ifBlank { "AOU Faculty" },
            courseCode = courseCode.ifBlank { "M251" },
            title = title,
            description = description,
            linkUrl = linkUrl,
            fileName = fileName,
            attachmentUri = attachmentUri,
            attachmentType = attachmentType,
            materialKind = kind.name,
            scope = "AOU"
        )
        viewModelScope.launch {
            if (isCloudAuthenticated && repository != null) {
                val res = repository.publishCourseMaterial(mat)
                _aiActionMessage.value = if (res.isSuccess) "Course material published." else res.exceptionOrNull()?.message
            } else {
                val updated = listOf(mat.copy(createdAt = Timestamp.now())) + _demoCourseMaterials.value
                _demoCourseMaterials.value = updated
                prefsManager.saveLocalCourseMaterials(updated)
                _aiActionMessage.value = "Course material published."
            }
        }
    }

    fun deleteCourseMaterial(materialId: String) {
        viewModelScope.launch {
            if (isCloudAuthenticated && repository != null) {
                repository.deleteCourseMaterial(materialId)
            } else {
                val updated = _demoCourseMaterials.value.filterNot { it.id == materialId }
                _demoCourseMaterials.value = updated
                prefsManager.saveLocalCourseMaterials(updated)
            }
        }
    }

    // Freshmen / Orientation Q&A Forum (with file/image/voice attachment support)
    fun createForumPost(
        authorName: String,
        category: String,
        title: String,
        body: String,
        attachmentName: String = "",
        attachmentUri: String = "",
        attachmentType: String = ""
    ) {
        val post = ForumPost(
            id = UUID.randomUUID().toString(),
            authorUid = currentUserId ?: "demo_user",
            authorName = authorName.ifBlank { "AOU Student" },
            category = category.ifBlank { "General" },
            questionTitle = title,
            questionBody = body,
            repliesCount = 0L,
            latestReply = "",
            attachmentName = attachmentName,
            attachmentUri = attachmentUri,
            attachmentType = attachmentType,
            scope = "AOU"
        )
        viewModelScope.launch {
            if (isCloudAuthenticated && repository != null) {
                repository.createForumPost(post)
            } else {
                val updated = listOf(post.copy(createdAt = Timestamp.now())) + _demoForumPosts.value
                _demoForumPosts.value = updated
                prefsManager.saveLocalForumPosts(updated)
            }
        }
    }

    fun replyToForumPost(post: ForumPost, reply: String) {
        viewModelScope.launch {
            if (isCloudAuthenticated && repository != null) {
                repository.replyToForumPost(post.id, post.repliesCount, reply)
            } else {
                val updated = _demoForumPosts.value.map {
                    if (it.id == post.id) {
                        it.copy(repliesCount = it.repliesCount + 1L, latestReply = reply)
                    } else it
                }
                _demoForumPosts.value = updated
                prefsManager.saveLocalForumPosts(updated)
            }
        }
    }

    // Direct Academic Chat (Text + Image + File + Voice)
    fun sendDirectChatMessage(
        peerName: String,
        courseCode: String,
        messageText: String,
        imageUri: String = "",
        attachmentName: String = "",
        attachmentUri: String = "",
        attachmentType: String = ""
    ) {
        if (messageText.isBlank() && imageUri.isBlank() && attachmentUri.isBlank()) return
        val myRole = preferences.value.role
        val peerRole = if (myRole == UserRole.STUDENT) UserRole.INSTRUCTOR else UserRole.STUDENT
        val msg = DirectChatMessage(
            id = UUID.randomUUID().toString(),
            userId = currentUserId ?: "demo_user",
            peerName = peerName,
            peerRole = peerRole.name,
            senderIsSelf = true,
            messageText = messageText,
            imageUri = imageUri,
            attachmentName = attachmentName,
            attachmentUri = attachmentUri,
            attachmentType = attachmentType,
            courseCode = courseCode.ifBlank { "M251" }
        )
        viewModelScope.launch {
            if (isCloudAuthenticated && repository != null) {
                repository.sendDirectChatMessage(msg)
            } else {
                _demoDirectChats.update { it + msg.copy(createdAt = Timestamp.now()) }
            }
        }
    }

    // General Student Community Chat (Text + Image + File + Voice)
    fun sendGeneralChatMessage(
        authorName: String,
        courseTag: String,
        messageText: String,
        attachmentName: String = "",
        attachmentUri: String = "",
        attachmentType: String = ""
    ) {
        if (messageText.isBlank() && attachmentUri.isBlank()) return
        val msg = GeneralChatMessage(
            id = UUID.randomUUID().toString(),
            authorUid = currentUserId ?: "demo_user",
            authorName = authorName.ifBlank { "AOU Student" },
            authorBranch = preferences.value.aouBranch,
            courseTag = courseTag.ifBlank { "General" },
            messageText = messageText.trim(),
            attachmentName = attachmentName,
            attachmentUri = attachmentUri,
            attachmentType = attachmentType,
            scope = "AOU"
        )
        viewModelScope.launch {
            if (isCloudAuthenticated && repository != null) {
                val res = repository.sendGeneralStudentMessage(msg)
                if (res.isFailure) {
                    _aiActionMessage.value = res.exceptionOrNull()?.message
                }
            } else {
                _demoGeneralChat.update { it + msg.copy(createdAt = Timestamp.now()) }
            }
        }
    }

    fun deleteGeneralChatMessage(messageId: String) {
        viewModelScope.launch {
            if (isCloudAuthenticated && repository != null) {
                repository.deleteGeneralStudentMessage(messageId)
            } else {
                _demoGeneralChat.update { list -> list.filterNot { it.id == messageId } }
            }
        }
    }

    // Voice Notes & Audio Recording Helpers
    fun startVoiceRecording(prefix: String = "AOU_Study"): Boolean {
        val ok = voiceNoteManager.startRecording(prefix)
        _isRecordingVoice.value = ok
        if (!ok) {
            _aiActionMessage.value = "Microphone permission or audio hardware unavailable."
        }
        return ok
    }

    fun stopVoiceRecording(): VoiceNoteItem? {
        val saved = voiceNoteManager.stopRecording()
        _isRecordingVoice.value = false
        if (saved != null) {
            _voiceNotes.value = voiceNoteManager.listSavedNotes()
        }
        return saved
    }

    fun playAudioPath(id: String, path: String) {
        if (_playingVoiceNoteId.value == id) {
            voiceNoteManager.stopPlayback()
            _playingVoiceNoteId.value = null
        } else {
            _playingVoiceNoteId.value = id
            voiceNoteManager.playNote(path) {
                _playingVoiceNoteId.value = null
            }
        }
    }

    fun toggleVoiceNotePlayback(note: VoiceNoteItem) {
        playAudioPath(note.id, note.filePath)
    }

    fun deleteVoiceNote(note: VoiceNoteItem) {
        if (_playingVoiceNoteId.value == note.id) {
            _playingVoiceNoteId.value = null
        }
        voiceNoteManager.deleteNote(note.filePath)
        _voiceNotes.value = voiceNoteManager.listSavedNotes()
    }

    private fun checkOnlineNow(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val net = cm?.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(net) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            true
        }
    }

    private fun registerConnectivityCallback(context: Context) {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            val req = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(req, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isOnline.value = true
                }
                override fun onLost(network: Network) {
                    _isOnline.value = checkOnlineNow(context)
                }
            })
        } catch (_: Exception) {
        }
    }

    override fun onCleared() {
        super.onCleared()
        pomodoroJob?.cancel()
        RingtonePlayer.stop()
        voiceNoteManager.stopPlayback()
    }
}
