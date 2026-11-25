package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.AppStrings
import com.example.data.LocalizationData
import com.example.ui.theme.BrandIconBlue
import com.example.ui.theme.BrandIconNavy
import com.example.ui.theme.BrandIconPurple
import java.io.File

@Composable
fun UniSyncAppBadgeIcon(
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(size * 0.28f)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(BrandIconNavy, BrandIconBlue, BrandIconPurple)
                )
            )
            .border(1.2.dp, BrandIconPurple.copy(alpha = 0.65f), shape),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_unisync_logo),
            contentDescription = "UniSync Logo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun UserAvatarChip(
    profilePhotoUri: String,
    size: Dp = 42.dp,
    showCameraBadge: Boolean = false,
    testTag: String = "single_account_profile_button",
    onClick: () -> Unit
) {
    val containerSize = if (size > 48.dp) size + 8.dp else 48.dp
    val imageModel: Any? = remember(profilePhotoUri) {
        when {
            profilePhotoUri.isBlank() -> null
            profilePhotoUri.startsWith("/") -> File(profilePhotoUri)
            else -> profilePhotoUri
        }
    }
    Box(
        modifier = Modifier
            .size(containerSize)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .size(size)
                .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            if (imageModel != null) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = "Account & Settings",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Account & Settings",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(size * 0.54f)
                    )
                }
            }
        }
        if (showCameraBadge) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(22.dp)
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Change profile photo",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CompactUtilityBar(
    languageCode: String,
    onLanguageSelected: (String) -> Unit,
    strings: AppStrings,
    onOpenSupport: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var langExpanded by remember { mutableStateOf(false) }
    val currentLang = remember(languageCode) {
        LocalizationData.languages.firstOrNull { it.code == languageCode } ?: LocalizationData.languages.first()
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onOpenSupport != null) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onOpenSupport)
                    .testTag("auth_support_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SupportAgent,
                        contentDescription = strings.navSupport,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = strings.navSupport,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            Spacer(Modifier.width(1.dp))
        }

        Box {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { langExpanded = true }
                    .testTag("language_selector_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = strings.language,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = currentLang.nativeName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }

            DropdownMenu(
                expanded = langExpanded,
                onDismissRequest = { langExpanded = false }
            ) {
                LocalizationData.languages.forEach { lang ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "${lang.nativeName} (${lang.englishName})",
                                fontWeight = if (lang.code == languageCode) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            langExpanded = false
                            onLanguageSelected(lang.code)
                        }
                    )
                }
            }
        }
    }
}

private const val UNISYNC_WEB_APP_URL = "https://ais-pre-isbbqeadd355xkolrmypce-742946530069.europe-west1.run.app"

private data class PlatformDownloadOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val guide: String,
    val icon: ImageVector
)

@Composable
fun PlatformDownloadsDialog(
    strings: AppStrings? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var copiedNotice by remember { mutableStateOf<String?>(null) }

    val platforms = remember {
        listOf(
            PlatformDownloadOption(
                id = "ios",
                title = "App Store (iOS / iPadOS)",
                subtitle = "iPhone & iPad · Safari Add to Home Screen",
                guide = "Open UniSync in Safari on iPhone/iPad → tap Share → Add to Home Screen, or install via configured App Store link.",
                icon = Icons.Default.PhoneIphone
            ),
            PlatformDownloadOption(
                id = "android",
                title = "Google Play (Android)",
                subtitle = "Native Android App · Play Store & PWA",
                guide = "Running natively on Android. Share the web/app link or open via Google Play.",
                icon = Icons.Default.Android
            ),
            PlatformDownloadOption(
                id = "windows",
                title = "Windows",
                subtitle = "Windows 10 / 11 Desktop App",
                guide = "Open UniSync in Microsoft Edge or Chrome on Windows → click Install App in the address bar.",
                icon = Icons.Default.DesktopWindows
            ),
            PlatformDownloadOption(
                id = "macos",
                title = "macOS",
                subtitle = "Mac Desktop & Safari Dock App",
                guide = "Open UniSync in Safari (File → Add to Dock) or Chrome on macOS to run as a standalone desktop app.",
                icon = Icons.Default.LaptopMac
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("UniSync Apps", style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                platforms.forEach { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("download_platform_${item.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(item.icon, contentDescription = item.title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text(item.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                                }
                            }
                            Text(
                                text = item.guide,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(UNISYNC_WEB_APP_URL))
                                        copiedNotice = "Copied link for ${item.title}"
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Copy Link", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                }
                                TextButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(UNISYNC_WEB_APP_URL))
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            clipboard.setText(AnnotatedString(UNISYNC_WEB_APP_URL))
                                            copiedNotice = "Link copied"
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(strings?.openLabel ?: "Open", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                }
                            }
                        }
                    }
                }
                if (!copiedNotice.isNullOrBlank()) {
                    Text(
                        text = copiedNotice.orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text(strings?.closeLabel ?: "Close") }
        }
    )
}

@Composable
fun WorkspaceTopHeader(
    title: String,
    subtitle: String,
    profilePhotoUri: String,
    isOnline: Boolean,
    darkMode: Boolean,
    toggleThemeDescription: String = "Toggle theme",
    onToggleDarkMode: () -> Unit,
    onOpenAccountSettings: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UniSyncAppBadgeIcon(size = 38.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                if (isOnline) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onToggleDarkMode,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("header_dark_mode_button")
            ) {
                Icon(
                    imageVector = if (darkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = toggleThemeDescription,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
            }

            UserAvatarChip(
                profilePhotoUri = profilePhotoUri,
                onClick = onOpenAccountSettings
            )
        }
    }
}
