package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.auth.MICROSOFT_ENTRA_CLIENT_ID
import com.example.auth.launchMicrosoftEntraSignIn
import com.example.auth.onGoogleSignInClicked
import com.example.data.AppStrings
import com.example.model.UserRole
import com.example.ui.components.CompactUtilityBar
import com.example.ui.components.PlatformDownloadsDialog
import com.example.ui.components.UniSyncAppBadgeIcon

@Composable
fun AuthScreen(
    selectedRole: UserRole,
    onSelectRole: (UserRole) -> Unit,
    languageCode: String,
    onLanguageSelected: (String) -> Unit,
    darkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    isOnline: Boolean,
    strings: AppStrings,
    onGoogleAuthSuccess: () -> Unit,
    onEnterDemoMode: (UserRole) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var isSigningIn by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showDownloadsDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }

    val gradientBg = if (selectedRole == UserRole.STUDENT) {
        Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                MaterialTheme.colorScheme.background
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                MaterialTheme.colorScheme.background
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CompactUtilityBar(
                languageCode = languageCode,
                onLanguageSelected = onLanguageSelected,
                strings = strings,
                onOpenSupport = { showSupportDialog = true }
            )

            Spacer(Modifier.height(18.dp))

            // Clean Brand Header: UniSync only (no AOU text attached directly under logo)
            UniSyncAppBadgeIcon(size = 68.dp)
            Spacer(Modifier.height(12.dp))
            Text(
                text = "UniSync",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = strings.welcomeSubtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(18.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = strings.chooseRole,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(12.dp))

                    // Clear Student vs Instructor Role Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RoleSelectionTile(
                            title = strings.studentRole,
                            subtitle = strings.studentRoleSubtitle,
                            icon = Icons.Default.School,
                            selected = selectedRole == UserRole.STUDENT,
                            onClick = { onSelectRole(UserRole.STUDENT) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("role_student_card")
                        )
                        RoleSelectionTile(
                            title = strings.instructorRole,
                            subtitle = strings.instructorRoleSubtitle,
                            icon = Icons.Default.LaptopMac,
                            selected = selectedRole == UserRole.INSTRUCTOR,
                            onClick = { onSelectRole(UserRole.INSTRUCTOR) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("role_instructor_card")
                        )
                    }

                    Spacer(Modifier.height(22.dp))

                    // Primary Google Sign-In Button
                    Button(
                        onClick = {
                            statusMessage = null
                            isSigningIn = true
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isSigningIn = false
                                    onGoogleAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isSigningIn = false
                                    statusMessage = msg
                                },
                                scope = scope,
                                onAuthCancelled = {
                                    isSigningIn = false
                                }
                            )
                        },
                        enabled = !isSigningIn,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("google_signin_button"),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        if (isSigningIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.AccountCircle, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = strings.signInGoogle,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Microsoft / Outlook Entra ID Sign-In (preserving Client ID)
                    OutlinedButton(
                        onClick = {
                            val launched = launchMicrosoftEntraSignIn(context)
                            statusMessage = if (launched) {
                                "Microsoft Entra ID OAuth started (Client: ${MICROSOFT_ENTRA_CLIENT_ID.take(8)}…). Sign in with Google as well for Firestore cloud sync."
                            } else {
                                "No browser found for Microsoft Entra ID redirect. You can still use Google Sign-In or Demo Account."
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("microsoft_signin_button"),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = strings.signInMicrosoft,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Prominent Demo Account Button (Local Sandbox — Zero Firestore calls)
                    Button(
                        onClick = { onEnterDemoMode(selectedRole) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("demo_account_button"),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${strings.tryDemoAccount} (${if (selectedRole == UserRole.STUDENT) strings.studentRole else strings.instructorRole})",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    if (!statusMessage.isNullOrBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = statusMessage.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            TextButton(
                onClick = { showDownloadsDialog = true },
                modifier = Modifier.testTag("platform_downloads_button")
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = strings.getAppsLabel,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(14.dp))
        }
    }

    if (showDownloadsDialog) {
        PlatformDownloadsDialog(strings = strings, onDismiss = { showDownloadsDialog = false })
    }
    if (showSupportDialog) {
        AouSupportDirectoryDialog(strings = strings, onDismiss = { showSupportDialog = false })
    }
}

@Composable
private fun RoleSelectionTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val bgColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    }

    Surface(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = bgColor,
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(24.dp)
                )
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
