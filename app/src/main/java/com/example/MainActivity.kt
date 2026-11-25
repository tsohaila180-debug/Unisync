package com.example

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.VoiceNoteManager
import com.example.auth.attemptAutoSignIn
import com.example.auth.authStateFlow
import com.example.auth.configureAppCheck
import com.example.auth.signOutUser
import com.example.data.AiService
import com.example.data.AppStrings
import com.example.data.LocalAppPreferences
import com.example.data.LocalizationData
import com.example.data.PreferencesManager
import com.example.data.UniSyncRepository
import com.example.focus.FocusLockBridge
import com.example.model.UserRole
import com.example.ui.components.SmartAlarmFullScreenOverlay
import com.example.ui.components.UniSyncAppBadgeIcon
import com.example.ui.components.WorkspaceTopHeader
import com.example.ui.screens.AouGpaCalculatorScreen
import com.example.ui.screens.AouPortalsHubScreen
import com.example.ui.screens.AouSupportDirectoryDialog
import com.example.ui.screens.AouSupportDirectoryScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CalendarAndSmartAlarmScreen
import com.example.ui.screens.DrawerAccountSettingsContent
import com.example.ui.screens.InstructorChatAndEmailScreen
import com.example.ui.screens.InstructorDashboardScreen
import com.example.ui.screens.InstructorMaterialsScreen
import com.example.ui.screens.StudentChatScreen
import com.example.ui.screens.StudentHomeScreen
import com.example.ui.screens.StudentHubScreen
import com.example.ui.screens.StudentInboxScreen
import com.example.ui.screens.StudentPomodoroScreen
import com.example.ui.screens.StudentRoadmapsScreen
import com.example.ui.theme.UniSyncTheme
import com.example.viewmodel.UniSyncViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

private enum class StudentDestination(val icon: ImageVector) {
    HOME(Icons.Default.Home),
    INBOX(Icons.Default.Inbox),
    POMODORO(Icons.Default.Timer),
    CALENDAR(Icons.Default.CalendarMonth),
    CHAT(Icons.Default.Chat),
    HUB(Icons.Default.Forum),
    ROADMAPS(Icons.Default.AccountTree),
    GPA(Icons.Default.Calculate),
    PORTALS(Icons.Default.Language),
    SUPPORT(Icons.Default.SupportAgent)
}

private enum class InstructorDestination(val icon: ImageVector) {
    SMART_INBOX(Icons.Default.Email),
    CALENDAR(Icons.Default.CalendarMonth),
    MATERIALS(Icons.Default.CloudUpload),
    CHAT_EMAIL(Icons.Default.Chat),
    PORTALS(Icons.Default.Language),
    SUPPORT(Icons.Default.SupportAgent)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // CRITICAL: Initialize Firebase App Check synchronously before any UI or AI/Firestore calls
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        setContent {
            UniSyncRootApp()
        }
    }
}

@Composable
private fun UniSyncRootApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefsManager = remember(context) { PreferencesManager(context.applicationContext) }
    val prefs by prefsManager.preferencesFlow.collectAsStateWithLifecycle(initialValue = LocalAppPreferences())

    val currentUser by remember { Firebase.auth.authStateFlow() }
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    var demoRole by rememberSaveable { mutableStateOf<UserRole?>(null) }

    val credentialManager = remember(context) { CredentialManager.create(context) }

    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { demoRole = null },
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            demoRole = null
        }
    }

    val effectiveRole = if (currentUser == null && demoRole != null) demoRole!! else prefs.role
    val strings = remember(prefs.languageCode) { LocalizationData.stringsFor(prefs.languageCode) }
    val layoutDirection = remember(prefs.languageCode) {
        LocalizationData.layoutDirectionFor(prefs.languageCode)
    }
    val baseDensity = LocalDensity.current
    val scaledDensity = remember(baseDensity, prefs.textScale) {
        Density(
            density = baseDensity.density,
            fontScale = baseDensity.fontScale * prefs.textScale.scaleFactor
        )
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalDensity provides scaledDensity
    ) {
        UniSyncTheme(role = effectiveRole, darkTheme = prefs.darkMode) {
            if (currentUser == null && demoRole == null) {
                AuthScreen(
                    selectedRole = prefs.role,
                    onSelectRole = { role -> scope.launch { prefsManager.setRole(role) } },
                    languageCode = prefs.languageCode,
                    onLanguageSelected = { code -> scope.launch { prefsManager.setLanguage(code) } },
                    darkMode = prefs.darkMode,
                    onToggleDarkMode = { scope.launch { prefsManager.setDarkMode(!prefs.darkMode) } },
                    isOnline = remember { isDeviceOnline(context) },
                    strings = strings,
                    onGoogleAuthSuccess = { demoRole = null },
                    onEnterDemoMode = { role ->
                        scope.launch {
                            prefsManager.setRole(role)
                            demoRole = role
                        }
                    }
                )
            } else {
                val sessionKey = currentUser?.uid ?: "demo_${demoRole?.name}"
                key(sessionKey) {
                    val vm: UniSyncViewModel = viewModel(
                        key = sessionKey,
                        factory = object : ViewModelProvider.Factory {
                            @Suppress("UNCHECKED_CAST")
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                val repo = if (currentUser != null) {
                                    UniSyncRepository(context.applicationContext)
                                } else null
                                return UniSyncViewModel(
                                    repository = repo,
                                    prefsManager = prefsManager,
                                    aiService = AiService(),
                                    voiceNoteManager = VoiceNoteManager(context.applicationContext),
                                    appContext = context.applicationContext,
                                    currentUserId = currentUser?.uid,
                                    initialDemoRole = demoRole
                                ) as T
                            }
                        }
                    )

                    UniSyncWorkspace(
                        viewModel = vm,
                        displayName = currentUser?.displayName ?: "AOU Academic User",
                        strings = strings,
                        onExitDemoOrSignOut = {
                            if (currentUser != null) {
                                signOutUser(credentialManager, scope) {
                                    demoRole = null
                                }
                            } else {
                                demoRole = null
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun UniSyncWorkspace(
    viewModel: UniSyncViewModel,
    displayName: String,
    strings: AppStrings,
    onExitDemoOrSignOut: () -> Unit
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val currentTimeMillis by viewModel.currentTimeMillis.collectAsStateWithLifecycle()

    val inboxState by viewModel.inboxState.collectAsStateWithLifecycle()
    val calendarState by viewModel.calendarState.collectAsStateWithLifecycle()
    val personalMaterialsState by viewModel.personalMaterialsState.collectAsStateWithLifecycle()
    val courseMaterialsState by viewModel.courseMaterialsState.collectAsStateWithLifecycle()
    val forumPostsState by viewModel.forumPostsState.collectAsStateWithLifecycle()
    val directChatState by viewModel.directChatState.collectAsStateWithLifecycle()
    val generalChatState by viewModel.generalChatState.collectAsStateWithLifecycle()

    val pomodoroState by viewModel.pomodoroState.collectAsStateWithLifecycle()
    val ringingAlarm by viewModel.ringingAlarm.collectAsStateWithLifecycle()

    val tutorMessages by viewModel.tutorMessages.collectAsStateWithLifecycle()
    val tutorStreaming by viewModel.tutorStreaming.collectAsStateWithLifecycle()
    val mindMapState by viewModel.mindMapState.collectAsStateWithLifecycle()
    val flashcardsState by viewModel.flashcardsState.collectAsStateWithLifecycle()
    val studyVisualState by viewModel.studyVisualState.collectAsStateWithLifecycle()
    val transcribingNoteId by viewModel.transcribingNoteId.collectAsStateWithLifecycle()
    val clustersState by viewModel.clustersState.collectAsStateWithLifecycle()
    val aiActionMessage by viewModel.aiActionMessage.collectAsStateWithLifecycle()
    val analyzingEmailId by viewModel.aiAnalyzingEmailId.collectAsStateWithLifecycle()

    val voiceNotes by viewModel.voiceNotes.collectAsStateWithLifecycle()
    val isRecordingVoice by viewModel.isRecordingVoice.collectAsStateWithLifecycle()
    val playingVoiceNoteId by viewModel.playingVoiceNoteId.collectAsStateWithLifecycle()

    var studentDest by rememberSaveable { mutableStateOf(StudentDestination.HOME) }
    var instructorDest by rememberSaveable { mutableStateOf(InstructorDestination.SMART_INBOX) }
    var drawerShowSettings by rememberSaveable { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()
    val isPomodoroLocked = prefs.role == UserRole.STUDENT && pomodoroState.isRunning && pomodoroState.isFocusPhase

    DisposableEffect(lifecycleOwner, isPomodoroLocked) {
        if (!isPomodoroLocked) {
            FocusLockBridge.toggleScreenPinning(context, false)
            return@DisposableEffect onDispose {}
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    viewModel.recordDistractionAttempt()
                    FocusLockBridge.sendFocusReturnAlertNotification(
                        context = context,
                        title = "UniSync Focus Lock Active",
                        message = "Return to your AOU focus session — timer is still locked."
                    )
                    viewModel.reopenFocusLockOverlay()
                }
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.reopenFocusLockOverlay()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(aiActionMessage, isPomodoroLocked) {
        val msg = aiActionMessage
        if (!msg.isNullOrBlank()) {
            if (!isPomodoroLocked || msg.contains("Milestone")) {
                snackbarHostState.showSnackbar(msg)
            }
            viewModel.clearAiMessage()
        }
    }

    if (isPomodoroLocked) {
        BackHandler {
            viewModel.recordDistractionAttempt()
            viewModel.reopenFocusLockOverlay()
        }
    } else if (prefs.role == UserRole.STUDENT && studentDest != StudentDestination.HOME) {
        BackHandler { studentDest = StudentDestination.HOME }
    } else if (prefs.role == UserRole.INSTRUCTOR && instructorDest != InstructorDestination.SMART_INBOX) {
        BackHandler { instructorDest = InstructorDestination.SMART_INBOX }
    }

    val roleSubtitle = buildString {
        append(if (prefs.role == UserRole.STUDENT) strings.studentRole else strings.instructorRole)
        append(" · ${strings.aouPrefix} ")
        append(prefs.aouBranch)
        append(" · ")
        append(if (viewModel.isCloudAuthenticated) strings.cloudLabel else strings.demoLabel)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isPomodoroLocked,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(316.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UniSyncAppBadgeIcon(size = 42.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("UniSync", style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = "${strings.aouPrefix} · ${prefs.aouBranch}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !drawerShowSettings,
                            onClick = { drawerShowSettings = false },
                            label = { Text(strings.navigationLabel, maxLines = 1) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("drawer_tab_nav")
                        )
                        FilterChip(
                            selected = drawerShowSettings,
                            onClick = { drawerShowSettings = true },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = strings.settings,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(strings.settings, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("drawer_tab_settings")
                        )
                    }
                }
                HorizontalDivider()

                if (drawerShowSettings) {
                    DrawerAccountSettingsContent(
                        prefs = prefs,
                        strings = strings,
                        isCloudAuthenticated = viewModel.isCloudAuthenticated,
                        onCloseDrawer = { drawerScope.launch { drawerState.close() } },
                        onRoleSwitch = { newRole ->
                            viewModel.setRole(newRole)
                            studentDest = StudentDestination.HOME
                            instructorDest = InstructorDestination.SMART_INBOX
                        },
                        onLanguageSelected = { viewModel.setLanguage(it) },
                        onToggleDarkMode = { viewModel.toggleDarkMode() },
                        onTextScaleSelected = { viewModel.setTextScale(it) },
                        onBranchSelected = { viewModel.setAouBranch(it) },
                        onMajorSelected = { viewModel.setMajorId(it) },
                        onProfilePhotoPicked = { viewModel.saveProfilePhotoFromUri(it) },
                        onOpenGpaCalculator = if (prefs.role == UserRole.STUDENT) {
                            {
                                if (isPomodoroLocked) {
                                    viewModel.recordDistractionAttempt()
                                    viewModel.reopenFocusLockOverlay()
                                } else {
                                    studentDest = StudentDestination.GPA
                                }
                            }
                        } else null,
                        onOpenPortalsHub = {
                            if (isPomodoroLocked) {
                                viewModel.recordDistractionAttempt()
                                viewModel.reopenFocusLockOverlay()
                            } else if (prefs.role == UserRole.STUDENT) {
                                studentDest = StudentDestination.PORTALS
                            } else {
                                instructorDest = InstructorDestination.PORTALS
                            }
                        },
                        onOpenSupportDirectory = { showSupportDialog = true },
                        onSignOut = onExitDemoOrSignOut
                    )
                } else {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Spacer(Modifier.height(6.dp))
                        if (prefs.role == UserRole.STUDENT) {
                            StudentDestination.entries.forEach { dest ->
                                val label = studentDestLabel(dest, strings)
                                NavigationDrawerItem(
                                    icon = { Icon(dest.icon, contentDescription = label) },
                                    label = { Text(label) },
                                    selected = studentDest == dest,
                                    onClick = {
                                        if (isPomodoroLocked && dest != StudentDestination.POMODORO) {
                                            viewModel.recordDistractionAttempt()
                                            viewModel.reopenFocusLockOverlay()
                                        } else {
                                            studentDest = dest
                                        }
                                        drawerScope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                                )
                            }
                        } else {
                            InstructorDestination.entries.forEach { dest ->
                                val label = instructorDestLabel(dest, strings)
                                NavigationDrawerItem(
                                    icon = { Icon(dest.icon, contentDescription = label) },
                                    label = { Text(label) },
                                    selected = instructorDest == dest,
                                    onClick = {
                                        instructorDest = dest
                                        drawerScope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Settings, contentDescription = strings.settings) },
                            label = { Text(strings.settings) },
                            selected = false,
                            onClick = { drawerShowSettings = true },
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                                .testTag("drawer_open_settings_item")
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            val useNavigationRail = maxWidth >= 600.dp

            Row(modifier = Modifier.fillMaxSize()) {
                if (useNavigationRail) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        IconButton(
                            onClick = {
                                if (isPomodoroLocked) {
                                    viewModel.recordDistractionAttempt()
                                    viewModel.reopenFocusLockOverlay()
                                } else {
                                    drawerShowSettings = false
                                    drawerScope.launch { drawerState.open() }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Open sidebar menu")
                        }
                        Spacer(Modifier.height(8.dp))
                        if (prefs.role == UserRole.STUDENT) {
                            listOf(
                                StudentDestination.HOME,
                                StudentDestination.INBOX,
                                StudentDestination.POMODORO,
                                StudentDestination.CALENDAR,
                                StudentDestination.CHAT,
                                StudentDestination.HUB,
                                StudentDestination.ROADMAPS,
                                StudentDestination.GPA
                            ).forEach { dest ->
                                val label = studentDestLabel(dest, strings)
                                NavigationRailItem(
                                    selected = studentDest == dest,
                                    onClick = {
                                        if (isPomodoroLocked && dest != StudentDestination.POMODORO) {
                                            viewModel.recordDistractionAttempt()
                                            viewModel.reopenFocusLockOverlay()
                                        } else {
                                            studentDest = dest
                                        }
                                    },
                                    icon = { Icon(dest.icon, contentDescription = label) },
                                    label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                )
                            }
                        } else {
                            InstructorDestination.entries.forEach { dest ->
                                val label = instructorDestLabel(dest, strings)
                                NavigationRailItem(
                                    selected = instructorDest == dest,
                                    onClick = { instructorDest = dest },
                                    icon = { Icon(dest.icon, contentDescription = label) },
                                    label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                )
                            }
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    containerColor = MaterialTheme.colorScheme.background,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    topBar = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!useNavigationRail) {
                                    IconButton(
                                        onClick = {
                                            if (isPomodoroLocked) {
                                                viewModel.recordDistractionAttempt()
                                                viewModel.reopenFocusLockOverlay()
                                            } else {
                                                drawerShowSettings = false
                                                drawerScope.launch { drawerState.open() }
                                            }
                                        },
                                        modifier = Modifier.testTag("open_sidebar_button")
                                    ) {
                                        Icon(
                                            Icons.Default.Menu,
                                            contentDescription = "Open sidebar",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    WorkspaceTopHeader(
                                        title = "UniSync",
                                        subtitle = roleSubtitle,
                                        profilePhotoUri = prefs.profilePhotoUri,
                                        isOnline = isOnline,
                                        darkMode = prefs.darkMode,
                                        toggleThemeDescription = if (prefs.darkMode) strings.lightMode else strings.darkMode,
                                        onToggleDarkMode = { viewModel.toggleDarkMode() },
                                        onOpenAccountSettings = {
                                            if (isPomodoroLocked) {
                                                viewModel.recordDistractionAttempt()
                                                viewModel.reopenFocusLockOverlay()
                                            } else {
                                                drawerShowSettings = true
                                                drawerScope.launch { drawerState.open() }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    },
                    bottomBar = {
                        if (!useNavigationRail) {
                            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                                if (prefs.role == UserRole.STUDENT) {
                                    val compactStudentTabs = listOf(
                                        StudentDestination.HOME,
                                        StudentDestination.INBOX,
                                        StudentDestination.POMODORO,
                                        StudentDestination.HUB,
                                        StudentDestination.ROADMAPS
                                    )
                                    compactStudentTabs.forEach { dest ->
                                        val label = studentDestLabel(dest, strings)
                                        NavigationBarItem(
                                            selected = studentDest == dest,
                                            onClick = {
                                                if (isPomodoroLocked && dest != StudentDestination.POMODORO) {
                                                    viewModel.recordDistractionAttempt()
                                                    viewModel.reopenFocusLockOverlay()
                                                } else {
                                                    studentDest = dest
                                                }
                                            },
                                            icon = { Icon(dest.icon, contentDescription = label) },
                                            label = {
                                                Text(
                                                    text = label,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            modifier = Modifier.testTag("nav_student_${dest.name.lowercase()}")
                                        )
                                    }
                                } else {
                                    val compactInstructorTabs = listOf(
                                        InstructorDestination.SMART_INBOX,
                                        InstructorDestination.CALENDAR,
                                        InstructorDestination.MATERIALS,
                                        InstructorDestination.CHAT_EMAIL
                                    )
                                    compactInstructorTabs.forEach { dest ->
                                        val label = instructorDestLabel(dest, strings)
                                        NavigationBarItem(
                                            selected = instructorDest == dest,
                                            onClick = { instructorDest = dest },
                                            icon = { Icon(dest.icon, contentDescription = label) },
                                            label = {
                                                Text(
                                                    text = label,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            modifier = Modifier.testTag("nav_instructor_${dest.name.lowercase()}")
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (prefs.role == UserRole.STUDENT) {
                            when (studentDest) {
                                StudentDestination.HOME -> StudentHomeScreen(
                                    calendarState = calendarState,
                                    currentTimeMillis = currentTimeMillis,
                                    strings = strings,
                                    onOpenFullCalendar = { studentDest = StudentDestination.CALENDAR }
                                )
                                StudentDestination.INBOX -> StudentInboxScreen(
                                    inboxState = inboxState,
                                    analyzingEmailId = analyzingEmailId,
                                    isRecordingAudio = isRecordingVoice,
                                    playingAudioId = playingVoiceNoteId,
                                    strings = strings,
                                    onSummarizeAi = { viewModel.summarizeEmailWithAi(it) },
                                    onAddDeadlineToCalendar = { viewModel.addExtractedDeadlineToCalendar(it) },
                                    onComposeEmail = { to, code, sub, body, prio, attN, attU, attT ->
                                        viewModel.composeEmail(to, code, sub, body, prio, attN, attU, attT)
                                    },
                                    onStartAudioRecord = { viewModel.startVoiceRecording("Email_Audio") },
                                    onStopAudioRecord = { viewModel.stopVoiceRecording() },
                                    onTogglePlayAudio = { id, path -> viewModel.playAudioPath(id, path) }
                                )
                                StudentDestination.POMODORO -> StudentPomodoroScreen(
                                    pomodoroState = pomodoroState,
                                    character = prefs.pomodoroCharacter,
                                    focusedStudyMinutes = prefs.focusedStudyMinutes,
                                    strings = strings,
                                    onSelectCharacter = { viewModel.setPomodoroCharacter(it) },
                                    onSelectPreset = { f, b -> viewModel.setPomodoroPreset(f, b) },
                                    onStart = { viewModel.startPomodoro() },
                                    onPause = { viewModel.pausePomodoro() },
                                    onReset = { viewModel.resetPomodoro() },
                                    onReopenAppLock = { viewModel.reopenFocusLockOverlay() },
                                    onRecordDistractionAttempt = { viewModel.recordDistractionAttempt() },
                                    onDismissFocusExitWarning = { viewModel.dismissFocusExitWarning() },
                                    onDismissMilestoneReward = { viewModel.dismissMilestoneReward() }
                                )
                                StudentDestination.CALENDAR -> CalendarAndSmartAlarmScreen(
                                    calendarState = calendarState,
                                    currentTimeMillis = currentTimeMillis,
                                    strings = strings,
                                    onAddEvent = { t, c, type, ts, alarm, ring, vol, notes ->
                                        viewModel.addCalendarEvent(t, c, type, ts, alarm, ring, vol, notes)
                                    },
                                    onDeleteEvent = { viewModel.deleteCalendarEvent(it) },
                                    onTestOverlayAlert = { viewModel.triggerSmartAlarm(it) }
                                )
                                StudentDestination.CHAT -> StudentChatScreen(
                                    directChatState = directChatState,
                                    generalChatState = generalChatState,
                                    currentUid = Firebase.auth.currentUser?.uid,
                                    isCloudAuthenticated = viewModel.isCloudAuthenticated,
                                    displayName = displayName,
                                    isRecordingAudio = isRecordingVoice,
                                    playingAudioId = playingVoiceNoteId,
                                    strings = strings,
                                    onSendDirectChat = { peer, code, text, img, attN, attU, attT ->
                                        viewModel.sendDirectChatMessage(peer, code, text, img, attN, attU, attT)
                                    },
                                    onSendGeneralChat = { author, tag, text, attN, attU, attT ->
                                        viewModel.sendGeneralChatMessage(author, tag, text, attN, attU, attT)
                                    },
                                    onDeleteGeneralChat = { viewModel.deleteGeneralChatMessage(it) },
                                    onStartAudioRecord = { viewModel.startVoiceRecording("Chat_Audio") },
                                    onStopAudioRecord = { viewModel.stopVoiceRecording() },
                                    onTogglePlayAudio = { id, path -> viewModel.playAudioPath(id, path) }
                                )
                                StudentDestination.HUB -> StudentHubScreen(
                                    tutorMessages = tutorMessages,
                                    tutorStreaming = tutorStreaming,
                                    mindMapState = mindMapState,
                                    flashcardsState = flashcardsState,
                                    studyVisualState = studyVisualState,
                                    forumState = forumPostsState,
                                    personalMaterialsState = personalMaterialsState,
                                    courseMaterialsState = courseMaterialsState,
                                    voiceNotes = voiceNotes,
                                    isRecordingVoice = isRecordingVoice,
                                    playingVoiceNoteId = playingVoiceNoteId,
                                    transcribingNoteId = transcribingNoteId,
                                    displayName = displayName,
                                    strings = strings,
                                    onSendTutorPrompt = { viewModel.sendTutorPrompt(it) },
                                    onGenerateMindMap = { viewModel.generateStudyMindMap(it) },
                                    onGenerateFlashcards = { viewModel.generateStudyFlashcards(it) },
                                    onGenerateStudyVisual = { viewModel.generateStudyVisual(it) },
                                    onCreateForumPost = { author, cat, title, body, attN, attU, attT ->
                                        viewModel.createForumPost(author, cat, title, body, attN, attU, attT)
                                    },
                                    onReplyForumPost = { post, reply ->
                                        viewModel.replyToForumPost(post, reply)
                                    },
                                    onAddPersonalMaterial = { title, content, uri, fileName, type ->
                                        viewModel.addPersonalMaterial(title, content, uri, fileName, type)
                                    },
                                    onDeletePersonalMaterial = { viewModel.deletePersonalMaterial(it) },
                                    onStartVoiceRecord = { viewModel.startVoiceRecording("AOU_Study") },
                                    onStopVoiceRecord = { viewModel.stopVoiceRecording() },
                                    onToggleVoicePlay = { viewModel.toggleVoiceNotePlayback(it) },
                                    onToggleAudioPath = { id, path -> viewModel.playAudioPath(id, path) },
                                    onTranscribeVoiceNote = { viewModel.transcribeVoiceNoteToNotes(it) },
                                    onDeleteVoiceNote = { viewModel.deleteVoiceNote(it) }
                                )
                                StudentDestination.ROADMAPS -> StudentRoadmapsScreen(
                                    selectedMajorId = prefs.majorId,
                                    courseStatuses = prefs.courseStatuses,
                                    strings = strings,
                                    onSelectMajor = { viewModel.setMajorId(it) },
                                    onCourseStatusChanged = { code, status ->
                                        viewModel.setCourseStatus(prefs.majorId, code, status)
                                    }
                                )
                                StudentDestination.GPA -> AouGpaCalculatorScreen(
                                    courses = prefs.gpaCourses,
                                    priorGpa = prefs.priorGpa,
                                    priorCredits = prefs.priorCredits,
                                    strings = strings,
                                    onUpdateGpaState = { list, pg, pc ->
                                        viewModel.updateGpaState(list, pg, pc)
                                    }
                                )
                                StudentDestination.PORTALS -> AouPortalsHubScreen(
                                    currentBranch = prefs.aouBranch,
                                    strings = strings,
                                    onSelectBranch = { viewModel.setAouBranch(it) }
                                )
                                StudentDestination.SUPPORT -> AouSupportDirectoryScreen(strings = strings)
                            }
                        } else {
                            when (instructorDest) {
                                InstructorDestination.SMART_INBOX -> InstructorDashboardScreen(
                                    inboxState = inboxState,
                                    calendarState = calendarState,
                                    clustersState = clustersState,
                                    analyzingEmailId = analyzingEmailId,
                                    currentTimeMillis = currentTimeMillis,
                                    isRecordingAudio = isRecordingVoice,
                                    playingAudioId = playingVoiceNoteId,
                                    strings = strings,
                                    onGroupQuestionsWithAi = { viewModel.groupRepeatedStudentQuestionsWithAi() },
                                    onSendUnifiedReply = { cluster, reply ->
                                        viewModel.sendUnifiedReplyToCluster(cluster, reply)
                                    },
                                    onSummarizeAi = { viewModel.summarizeEmailWithAi(it) },
                                    onDraftReplyAi = { viewModel.draftReplyWithAi(it) },
                                    onReplySingleEmail = { email, reply ->
                                        viewModel.replyToSingleEmail(email, reply)
                                    },
                                    onComposeEmail = { to, code, sub, body, prio, attN, attU, attT ->
                                        viewModel.composeEmail(to, code, sub, body, prio, attN, attU, attT)
                                    },
                                    onStartAudioRecord = { viewModel.startVoiceRecording("Inst_Email") },
                                    onStopAudioRecord = { viewModel.stopVoiceRecording() },
                                    onTogglePlayAudio = { id, path -> viewModel.playAudioPath(id, path) }
                                )
                                InstructorDestination.CALENDAR -> CalendarAndSmartAlarmScreen(
                                    calendarState = calendarState,
                                    currentTimeMillis = currentTimeMillis,
                                    strings = strings,
                                    onAddEvent = { t, c, type, ts, alarm, ring, vol, notes ->
                                        viewModel.addCalendarEvent(t, c, type, ts, alarm, ring, vol, notes)
                                    },
                                    onDeleteEvent = { viewModel.deleteCalendarEvent(it) },
                                    onTestOverlayAlert = { viewModel.triggerSmartAlarm(it) }
                                )
                                InstructorDestination.MATERIALS -> InstructorMaterialsScreen(
                                    courseMaterialsState = courseMaterialsState,
                                    currentUid = Firebase.auth.currentUser?.uid,
                                    isCloudAuthenticated = viewModel.isCloudAuthenticated,
                                    isRecordingAudio = isRecordingVoice,
                                    playingAudioId = playingVoiceNoteId,
                                    strings = strings,
                                    onPublishMaterial = { author, code, title, desc, link, file, kind, attU, attT ->
                                        viewModel.publishCourseMaterial(author, code, title, desc, link, file, kind, attU, attT)
                                    },
                                    onDeleteMaterial = { viewModel.deleteCourseMaterial(it) },
                                    onStartAudioRecord = { viewModel.startVoiceRecording("Lecture_Audio") },
                                    onStopAudioRecord = { viewModel.stopVoiceRecording() },
                                    onTogglePlayAudio = { id, path -> viewModel.playAudioPath(id, path) }
                                )
                                InstructorDestination.CHAT_EMAIL -> InstructorChatAndEmailScreen(
                                    directChatState = directChatState,
                                    isRecordingAudio = isRecordingVoice,
                                    playingAudioId = playingVoiceNoteId,
                                    strings = strings,
                                    onSendDirectChat = { peer, code, text, img, attN, attU, attT ->
                                        viewModel.sendDirectChatMessage(peer, code, text, img, attN, attU, attT)
                                    },
                                    onComposeEmail = { to, code, sub, body, prio, attN, attU, attT ->
                                        viewModel.composeEmail(to, code, sub, body, prio, attN, attU, attT)
                                    },
                                    onStartAudioRecord = { viewModel.startVoiceRecording("Inst_Chat") },
                                    onStopAudioRecord = { viewModel.stopVoiceRecording() },
                                    onTogglePlayAudio = { id, path -> viewModel.playAudioPath(id, path) }
                                )
                                InstructorDestination.PORTALS -> AouPortalsHubScreen(
                                    currentBranch = prefs.aouBranch,
                                    strings = strings,
                                    onSelectBranch = { viewModel.setAouBranch(it) }
                                )
                                InstructorDestination.SUPPORT -> AouSupportDirectoryScreen(strings = strings)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSupportDialog) {
        AouSupportDirectoryDialog(strings = strings, onDismiss = { showSupportDialog = false })
    }

    ringingAlarm?.let { alarm ->
        SmartAlarmFullScreenOverlay(
            alarm = alarm,
            onSnooze5Min = { viewModel.snoozeSmartAlarm5Min() },
            onDismiss = { viewModel.dismissSmartAlarm() }
        )
    }
}

private fun studentDestLabel(dest: StudentDestination, strings: AppStrings): String = when (dest) {
    StudentDestination.HOME -> strings.navHome
    StudentDestination.INBOX -> strings.navInbox
    StudentDestination.POMODORO -> strings.navPomodoro
    StudentDestination.CALENDAR -> strings.navCalendar
    StudentDestination.CHAT -> strings.navChat
    StudentDestination.HUB -> strings.navStudyHub
    StudentDestination.ROADMAPS -> strings.navRoadmaps
    StudentDestination.GPA -> strings.navGpaCalc
    StudentDestination.PORTALS -> strings.navPortals
    StudentDestination.SUPPORT -> strings.navSupport
}

private fun instructorDestLabel(dest: InstructorDestination, strings: AppStrings): String = when (dest) {
    InstructorDestination.SMART_INBOX -> strings.navSmartInbox
    InstructorDestination.CALENDAR -> strings.navCalendar
    InstructorDestination.MATERIALS -> strings.navMaterials
    InstructorDestination.CHAT_EMAIL -> strings.navChatEmail
    InstructorDestination.PORTALS -> strings.navPortals
    InstructorDestination.SUPPORT -> strings.navSupport
}

private fun isDeviceOnline(context: Context): Boolean {
    return try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val net = cm?.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } catch (_: Exception) {
        true
    }
}
