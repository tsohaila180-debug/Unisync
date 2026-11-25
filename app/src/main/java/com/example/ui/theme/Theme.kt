package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.model.UserRole

private val StudentLightColorScheme = lightColorScheme(
    primary = StudentPrimaryBlue,
    onPrimary = SoftOffWhite,
    primaryContainer = StudentLightBlue,
    onPrimaryContainer = StudentPrimaryNavy,
    secondary = StudentAccentBlue,
    onSecondary = SoftOffWhite,
    secondaryContainer = Color(0xFFE3ECF9),
    onSecondaryContainer = StudentPrimaryNavy,
    tertiary = StudentAccentBlue,
    onTertiary = SoftOffWhite,
    tertiaryContainer = StudentLightBlue,
    onTertiaryContainer = StudentPrimaryNavy,
    background = StudentMainBackground,
    onBackground = StudentPrimaryText,
    surface = StudentCardBackground,
    onSurface = StudentPrimaryText,
    surfaceVariant = StudentLightBlue,
    onSurfaceVariant = StudentSecondaryText,
    outline = StudentBorders,
    outlineVariant = StudentBorders,
    error = PriorityCriticalBg,
    onError = SoftOffWhite,
    errorContainer = Color(0xFFF6DFE3),
    onErrorContainer = Color(0xFF5E1622)
)

private val StudentDarkColorScheme = darkColorScheme(
    primary = StudentDarkPrimaryAccent,
    onPrimary = SoftOffWhite,
    primaryContainer = StudentDarkSurfaceVariant,
    onPrimaryContainer = StudentDarkText,
    secondary = StudentSoftSkyBlue,
    onSecondary = StudentDarkBackground,
    secondaryContainer = StudentDarkSurfaceVariant,
    onSecondaryContainer = StudentDarkText,
    tertiary = StudentSoftSkyBlue,
    onTertiary = StudentDarkBackground,
    tertiaryContainer = StudentDarkSurfaceVariant,
    onTertiaryContainer = StudentDarkText,
    background = StudentDarkBackground,
    onBackground = StudentDarkText,
    surface = StudentDarkCard,
    onSurface = StudentDarkText,
    surfaceVariant = StudentDarkSurfaceVariant,
    onSurfaceVariant = StudentDarkSecondaryText,
    outline = StudentDarkBorder,
    outlineVariant = StudentDarkBorder,
    error = Color(0xFFE57B8A),
    onError = Color(0xFF330A12),
    errorContainer = Color(0xFF4D1A24),
    onErrorContainer = StudentDarkText
)

private val InstructorLightColorScheme = lightColorScheme(
    primary = InstructorPrimaryPurple,
    onPrimary = SoftOffWhite,
    primaryContainer = InstructorLightLavender,
    onPrimaryContainer = InstructorDeepRoyalNavy,
    secondary = InstructorAccentPurple,
    onSecondary = SoftOffWhite,
    secondaryContainer = Color(0xFFE6E0F5),
    onSecondaryContainer = InstructorDeepRoyalNavy,
    tertiary = InstructorAccentPurple,
    onTertiary = SoftOffWhite,
    tertiaryContainer = InstructorLightLavender,
    onTertiaryContainer = InstructorDeepRoyalNavy,
    background = InstructorMainBackground,
    onBackground = InstructorPrimaryText,
    surface = InstructorCardBackground,
    onSurface = InstructorPrimaryText,
    surfaceVariant = InstructorLightLavender,
    onSurfaceVariant = InstructorSecondaryText,
    outline = InstructorBorders,
    outlineVariant = InstructorBorders,
    error = PriorityCriticalBg,
    onError = SoftOffWhite,
    errorContainer = Color(0xFFF6DFE3),
    onErrorContainer = Color(0xFF5E1622)
)

private val InstructorDarkColorScheme = darkColorScheme(
    primary = InstructorDarkPrimaryAccent,
    onPrimary = InstructorDarkBackground,
    primaryContainer = InstructorDarkSurfaceVariant,
    onPrimaryContainer = InstructorDarkText,
    secondary = InstructorAccentPurple,
    onSecondary = SoftOffWhite,
    secondaryContainer = InstructorDarkSurfaceVariant,
    onSecondaryContainer = InstructorDarkText,
    tertiary = InstructorSoftLavender,
    onTertiary = InstructorDarkBackground,
    tertiaryContainer = InstructorDarkSurfaceVariant,
    onTertiaryContainer = InstructorDarkText,
    background = InstructorDarkBackground,
    onBackground = InstructorDarkText,
    surface = InstructorDarkCard,
    onSurface = InstructorDarkText,
    surfaceVariant = InstructorDarkSurfaceVariant,
    onSurfaceVariant = InstructorDarkSecondaryText,
    outline = InstructorDarkBorder,
    outlineVariant = InstructorDarkBorder,
    error = Color(0xFFE57B8A),
    onError = Color(0xFF330A12),
    errorContainer = Color(0xFF4D1A24),
    onErrorContainer = InstructorDarkText
)

val UniSyncShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun UniSyncTheme(
    role: UserRole = UserRole.STUDENT,
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when (role) {
        UserRole.STUDENT -> if (darkTheme) StudentDarkColorScheme else StudentLightColorScheme
        UserRole.INSTRUCTOR -> if (darkTheme) InstructorDarkColorScheme else InstructorLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = UniSyncTypography,
        shapes = UniSyncShapes,
        content = content
    )
}
