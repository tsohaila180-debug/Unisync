package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.audio.RingtonePlayer
import com.example.model.CalendarEvent
import com.example.ui.theme.InstructorAccentPurple
import com.example.ui.theme.InstructorPrimaryPurple
import com.example.ui.theme.SoftOffWhite
import com.example.ui.theme.StudentAccentBlue
import com.example.ui.theme.StudentDarkBackground
import com.example.ui.theme.StudentPrimaryNavy
import com.example.ui.theme.StudentSoftSkyBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SmartAlarmFullScreenOverlay(
    alarm: CalendarEvent,
    onSnooze5Min: () -> Unit,
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "alarmPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(520),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val ringRipple by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.24f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringRipple"
    )
    val toneLabel = RingtonePlayer.options.firstOrNull { it.id == alarm.ringtoneId }?.label ?: "Crystal Chime"
    val nowClockText = remember {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            StudentDarkBackground,
                            StudentPrimaryNavy,
                            InstructorPrimaryPurple
                        )
                    )
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = StudentSoftSkyBlue.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, StudentSoftSkyBlue.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = "UNISYNC ACADEMIC ALARM · RINGING",
                        style = MaterialTheme.typography.labelMedium,
                        color = StudentSoftSkyBlue,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = nowClockText,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = SoftOffWhite
                )

                Spacer(Modifier.height(22.dp))

                Box(contentAlignment = Alignment.Center) {
                    Surface(
                        modifier = Modifier
                            .size(134.dp)
                            .scale(ringRipple),
                        shape = CircleShape,
                        color = StudentAccentBlue.copy(alpha = 0.22f)
                    ) {}
                    Surface(
                        modifier = Modifier
                            .size(108.dp)
                            .scale(pulse),
                        shape = CircleShape,
                        color = InstructorAccentPurple,
                        border = BorderStroke(2.dp, StudentSoftSkyBlue)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = "Ringing Academic Alarm",
                                tint = SoftOffWhite,
                                modifier = Modifier.size(54.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                if (alarm.courseCode.isNotBlank()) {
                    Text(
                        text = alarm.courseCode,
                        style = MaterialTheme.typography.titleLarge,
                        color = StudentSoftSkyBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                }

                Text(
                    text = alarm.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = SoftOffWhite,
                    textAlign = TextAlign.Center
                )

                if (alarm.notes.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = alarm.notes,
                        style = MaterialTheme.typography.bodyLarge,
                        color = StudentSoftSkyBlue.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Ringtone: $toneLabel · Volume ${alarm.volumePercent}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = SoftOffWhite.copy(alpha = 0.85f)
                )

                Spacer(Modifier.height(36.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedButton(
                        onClick = onSnooze5Min,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("alarm_snooze_button"),
                        border = BorderStroke(1.5.dp, StudentSoftSkyBlue.copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftOffWhite)
                    ) {
                        Icon(Icons.Default.Snooze, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Snooze 5m")
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("alarm_dismiss_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudentAccentBlue,
                            contentColor = SoftOffWhite
                        )
                    ) {
                        Icon(Icons.Default.AlarmOff, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Dismiss", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
