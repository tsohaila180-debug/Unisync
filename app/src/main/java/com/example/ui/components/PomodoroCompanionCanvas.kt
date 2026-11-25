package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.PomodoroCharacter
import kotlinx.coroutines.delay

enum class CompanionMood {
    IDLE,
    WALKING_IN,
    STUDYING,
    BREAK_RELAXING
}

/**
 * Mini Cute Animated Cartoon Study Companion in the Blue/Royal-Purple UniSync palette:
 * - GIRL (default): Honey-brown hair, sky-blue academic dress, small heel shoes.
 * - BOY: Black hair, academic blue varsity T-shirt, slate trousers, stylish sneakers.
 * - On Focus Start: Walks in pulling a small backpack, sits down, opens a book & pen.
 * - On Break Start: Puts book away and sips cool sky-blue beverage while resting.
 */
@Composable
fun PomodoroCompanionCanvas(
    character: PomodoroCharacter,
    isRunning: Boolean,
    isFocusPhase: Boolean,
    size: Dp = 132.dp,
    modifier: Modifier = Modifier
) {
    var walkInDone by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning, isFocusPhase) {
        if (isRunning && isFocusPhase) {
            walkInDone = false
            delay(1100L)
            walkInDone = true
        } else {
            walkInDone = false
        }
    }

    val mood = when {
        isRunning && isFocusPhase && !walkInDone -> CompanionMood.WALKING_IN
        isRunning && isFocusPhase && walkInDone -> CompanionMood.STUDYING
        isRunning && !isFocusPhase -> CompanionMood.BREAK_RELAXING
        else -> CompanionMood.IDLE
    }

    val walkProgress by animateFloatAsState(
        targetValue = if (mood == CompanionMood.IDLE) 1f else if (mood == CompanionMood.WALKING_IN) 0.5f else 1f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "walkProgress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "companionLoop")
    val bobPhase by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bob"
    )
    val armPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arm"
    )

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Soft floor shadow
        drawOval(
            color = Color(0x33142D59),
            topLeft = Offset(w * 0.18f, h * 0.86f),
            size = Size(w * 0.64f, h * 0.08f)
        )

        val slideOffsetX = when (mood) {
            CompanionMood.WALKING_IN -> (walkProgress - 1f) * w * 0.45f
            else -> 0f
        }
        val bobY = when (mood) {
            CompanionMood.WALKING_IN -> kotlin.math.abs(bobPhase) * h * 0.03f
            CompanionMood.BREAK_RELAXING -> bobPhase * h * 0.025f
            CompanionMood.STUDYING -> bobPhase * h * 0.008f
            CompanionMood.IDLE -> bobPhase * h * 0.01f
        }

        val cx = w * 0.5f + slideOffsetX
        val groundY = h * 0.87f

        // Rolling / Pulled Backpack
        val bagX = if (mood == CompanionMood.WALKING_IN) cx - w * 0.25f else cx - w * 0.27f
        val bagY = groundY - h * 0.22f
        if (mood == CompanionMood.WALKING_IN) {
            drawLine(
                color = Color(0xFF142D59),
                start = Offset(bagX + w * 0.05f, bagY),
                end = Offset(cx - w * 0.08f, groundY - h * 0.34f),
                strokeWidth = w * 0.02f,
                cap = StrokeCap.Round
            )
        }
        drawRoundRect(
            color = if (character == PomodoroCharacter.GIRL) Color(0xFF7056B3) else Color(0xFF2456A6),
            topLeft = Offset(bagX, bagY),
            size = Size(w * 0.14f, h * 0.18f),
            cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
        )
        drawRoundRect(
            color = Color(0xFFDCE9F8).copy(alpha = 0.6f),
            topLeft = Offset(bagX + w * 0.025f, bagY + h * 0.07f),
            size = Size(w * 0.09f, h * 0.06f),
            cornerRadius = CornerRadius(w * 0.02f, w * 0.02f)
        )
        drawCircle(
            color = Color(0xFF142D59),
            radius = w * 0.022f,
            center = Offset(bagX + w * 0.03f, groundY - w * 0.015f)
        )
        drawCircle(
            color = Color(0xFF142D59),
            radius = w * 0.022f,
            center = Offset(bagX + w * 0.11f, groundY - w * 0.015f)
        )

        if (mood == CompanionMood.STUDYING) {
            drawRoundRect(
                color = Color(0xFF44317E),
                topLeft = Offset(cx - w * 0.24f, groundY - h * 0.20f),
                size = Size(w * 0.54f, h * 0.05f),
                cornerRadius = CornerRadius(w * 0.02f, w * 0.02f)
            )
            drawRoundRect(
                color = Color(0xFFF3F6FC),
                topLeft = Offset(cx - w * 0.10f, groundY - h * 0.245f),
                size = Size(w * 0.24f, h * 0.055f),
                cornerRadius = CornerRadius(w * 0.015f, w * 0.015f)
            )
            drawLine(
                color = Color(0xFF3986D9),
                start = Offset(cx + w * 0.02f, groundY - h * 0.245f),
                end = Offset(cx + w * 0.02f, groundY - h * 0.19f),
                strokeWidth = 2f
            )
        }

        val skinColor = Color(0xFFF6D5B8)
        val blushColor = Color(0xFFA18BD5)

        if (character == PomodoroCharacter.GIRL) {
            drawGirlCharacter(
                cx = cx,
                groundY = groundY,
                w = w,
                h = h,
                bobY = bobY,
                armPhase = armPhase,
                mood = mood,
                skinColor = skinColor,
                blushColor = blushColor
            )
        } else {
            drawBoyCharacter(
                cx = cx,
                groundY = groundY,
                w = w,
                h = h,
                bobY = bobY,
                armPhase = armPhase,
                mood = mood,
                skinColor = skinColor,
                blushColor = blushColor
            )
        }
    }
}

private fun DrawScope.drawGirlCharacter(
    cx: Float,
    groundY: Float,
    w: Float,
    h: Float,
    bobY: Float,
    armPhase: Float,
    mood: CompanionMood,
    skinColor: Color,
    blushColor: Color
) {
    val honeyHair = Color(0xFF8A5A36)
    val honeyHighlight = Color(0xFFA7744B)
    val skyDress = Color(0xFF83B8EB)
    val skyDressShade = Color(0xFF3986D9)
    val shoeNavy = Color(0xFF142D59)

    val headCy = groundY - h * 0.56f + bobY
    val headR = w * 0.15f

    drawOval(
        color = honeyHair,
        topLeft = Offset(cx - headR * 1.18f, headCy - headR * 0.95f),
        size = Size(headR * 2.36f, headR * 2.35f)
    )

    val legShift = if (mood == CompanionMood.WALKING_IN) (armPhase - 0.5f) * w * 0.07f else 0f
    drawLine(
        color = skinColor,
        start = Offset(cx - w * 0.045f, groundY - h * 0.22f + bobY),
        end = Offset(cx - w * 0.05f - legShift, groundY - h * 0.05f),
        strokeWidth = w * 0.036f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = skinColor,
        start = Offset(cx + w * 0.045f, groundY - h * 0.22f + bobY),
        end = Offset(cx + w * 0.05f + legShift, groundY - h * 0.05f),
        strokeWidth = w * 0.036f,
        cap = StrokeCap.Round
    )
    drawRoundRect(
        color = shoeNavy,
        topLeft = Offset(cx - w * 0.085f - legShift, groundY - h * 0.06f),
        size = Size(w * 0.065f, h * 0.042f),
        cornerRadius = CornerRadius(w * 0.02f, w * 0.02f)
    )
    drawRect(
        color = shoeNavy,
        topLeft = Offset(cx - w * 0.08f - legShift, groundY - h * 0.025f),
        size = Size(w * 0.018f, h * 0.025f)
    )
    drawRoundRect(
        color = shoeNavy,
        topLeft = Offset(cx + w * 0.02f + legShift, groundY - h * 0.06f),
        size = Size(w * 0.065f, h * 0.042f),
        cornerRadius = CornerRadius(w * 0.02f, w * 0.02f)
    )
    drawRect(
        color = shoeNavy,
        topLeft = Offset(cx + w * 0.024f + legShift, groundY - h * 0.025f),
        size = Size(w * 0.018f, h * 0.025f)
    )

    // Academic Sky-Blue Dress
    val dressPath = Path().apply {
        moveTo(cx - w * 0.07f, groundY - h * 0.41f + bobY)
        lineTo(cx + w * 0.07f, groundY - h * 0.41f + bobY)
        lineTo(cx + w * 0.145f, groundY - h * 0.18f + bobY)
        lineTo(cx - w * 0.145f, groundY - h * 0.18f + bobY)
        close()
    }
    drawPath(dressPath, color = skyDress)
    drawRoundRect(
        color = skyDressShade,
        topLeft = Offset(cx - w * 0.085f, groundY - h * 0.34f + bobY),
        size = Size(w * 0.17f, h * 0.03f),
        cornerRadius = CornerRadius(w * 0.015f, w * 0.015f)
    )

    drawCircle(color = skinColor, radius = headR, center = Offset(cx, headCy))

    val bangsPath = Path().apply {
        moveTo(cx - headR, headCy - headR * 0.15f)
        quadraticTo(cx, headCy - headR * 1.15f, cx + headR, headCy - headR * 0.15f)
        quadraticTo(cx + headR * 0.35f, headCy - headR * 0.45f, cx, headCy - headR * 0.25f)
        quadraticTo(cx - headR * 0.35f, headCy - headR * 0.45f, cx - headR, headCy - headR * 0.15f)
        close()
    }
    drawPath(bangsPath, color = honeyHair)
    drawArc(
        color = honeyHighlight,
        startAngle = 210f,
        sweepAngle = 110f,
        useCenter = false,
        topLeft = Offset(cx - headR * 0.7f, headCy - headR * 0.85f),
        size = Size(headR * 1.4f, headR * 0.6f),
        style = Stroke(width = w * 0.018f, cap = StrokeCap.Round)
    )

    // Hair bow (Royal Lavender)
    drawCircle(color = Color(0xFF7056B3), radius = w * 0.032f, center = Offset(cx + headR * 0.68f, headCy - headR * 0.72f))

    drawCircle(color = Color(0xFF142D59), radius = w * 0.02f, center = Offset(cx - headR * 0.36f, headCy + headR * 0.08f))
    drawCircle(color = Color(0xFF142D59), radius = w * 0.02f, center = Offset(cx + headR * 0.36f, headCy + headR * 0.08f))
    drawCircle(color = Color(0xFFF3F6FC), radius = w * 0.007f, center = Offset(cx - headR * 0.38f, headCy + headR * 0.05f))
    drawCircle(color = Color(0xFFF3F6FC), radius = w * 0.007f, center = Offset(cx + headR * 0.34f, headCy + headR * 0.05f))
    drawOval(color = blushColor.copy(alpha = 0.35f), topLeft = Offset(cx - headR * 0.68f, headCy + headR * 0.18f), size = Size(w * 0.05f, h * 0.025f))
    drawOval(color = blushColor.copy(alpha = 0.35f), topLeft = Offset(cx + headR * 0.35f, headCy + headR * 0.18f), size = Size(w * 0.05f, h * 0.025f))

    drawArc(
        color = Color(0xFF4A2C16),
        startAngle = 20f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(cx - headR * 0.22f, headCy + headR * 0.24f),
        size = Size(headR * 0.44f, headR * 0.24f),
        style = Stroke(width = 2.5f, cap = StrokeCap.Round)
    )

    val shoulderY = groundY - h * 0.38f + bobY
    if (mood == CompanionMood.STUDYING) {
        val penBob = (armPhase - 0.5f) * w * 0.04f
        drawLine(skinColor, Offset(cx - w * 0.08f, shoulderY), Offset(cx - w * 0.02f, groundY - h * 0.24f), strokeWidth = w * 0.03f, cap = StrokeCap.Round)
        drawLine(skinColor, Offset(cx + w * 0.08f, shoulderY), Offset(cx + w * 0.06f + penBob, groundY - h * 0.24f), strokeWidth = w * 0.03f, cap = StrokeCap.Round)
        drawLine(Color(0xFF2456A6), Offset(cx + w * 0.06f + penBob, groundY - h * 0.24f), Offset(cx + w * 0.03f + penBob, groundY - h * 0.21f), strokeWidth = w * 0.018f, cap = StrokeCap.Round)
    } else if (mood == CompanionMood.BREAK_RELAXING) {
        val sipLift = armPhase * h * 0.05f
        drawLine(skinColor, Offset(cx - w * 0.08f, shoulderY), Offset(cx - w * 0.13f, shoulderY + h * 0.08f), strokeWidth = w * 0.03f, cap = StrokeCap.Round)
        drawLine(skinColor, Offset(cx + w * 0.08f, shoulderY), Offset(cx + w * 0.11f, shoulderY + h * 0.05f - sipLift), strokeWidth = w * 0.03f, cap = StrokeCap.Round)
        drawRoundRect(
            color = Color(0xFF3986D9),
            topLeft = Offset(cx + w * 0.09f, shoulderY - sipLift),
            size = Size(w * 0.065f, h * 0.085f),
            cornerRadius = CornerRadius(w * 0.015f, w * 0.015f)
        )
        drawLine(
            color = Color(0xFFF3F6FC),
            start = Offset(cx + w * 0.12f, shoulderY - sipLift),
            end = Offset(cx + w * 0.06f, shoulderY - h * 0.05f - sipLift),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
    } else {
        drawLine(skinColor, Offset(cx - w * 0.08f, shoulderY), Offset(cx - w * 0.13f, shoulderY + h * 0.11f), strokeWidth = w * 0.03f, cap = StrokeCap.Round)
        drawLine(skinColor, Offset(cx + w * 0.08f, shoulderY), Offset(cx + w * 0.13f, shoulderY + h * 0.11f), strokeWidth = w * 0.03f, cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawBoyCharacter(
    cx: Float,
    groundY: Float,
    w: Float,
    h: Float,
    bobY: Float,
    armPhase: Float,
    mood: CompanionMood,
    skinColor: Color,
    blushColor: Color
) {
    val blackHair = Color(0xFF142D59)
    val tshirtBlue = Color(0xFF2456A6)
    val pantsSlate = Color(0xFF21183F)
    val sneakerBase = Color(0xFFF3F6FC)
    val sneakerStripe = Color(0xFF3986D9)

    val headCy = groundY - h * 0.56f + bobY
    val headR = w * 0.15f

    val legShift = if (mood == CompanionMood.WALKING_IN) (armPhase - 0.5f) * w * 0.07f else 0f
    drawLine(
        color = pantsSlate,
        start = Offset(cx - w * 0.045f, groundY - h * 0.24f + bobY),
        end = Offset(cx - w * 0.05f - legShift, groundY - h * 0.045f),
        strokeWidth = w * 0.055f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = pantsSlate,
        start = Offset(cx + w * 0.045f, groundY - h * 0.24f + bobY),
        end = Offset(cx + w * 0.05f + legShift, groundY - h * 0.045f),
        strokeWidth = w * 0.055f,
        cap = StrokeCap.Round
    )
    drawRoundRect(
        color = sneakerBase,
        topLeft = Offset(cx - w * 0.095f - legShift, groundY - h * 0.06f),
        size = Size(w * 0.08f, h * 0.048f),
        cornerRadius = CornerRadius(w * 0.025f, w * 0.025f)
    )
    drawLine(sneakerStripe, Offset(cx - w * 0.085f - legShift, groundY - h * 0.025f), Offset(cx - w * 0.02f - legShift, groundY - h * 0.025f), strokeWidth = 3f)
    drawRoundRect(
        color = sneakerBase,
        topLeft = Offset(cx + w * 0.015f + legShift, groundY - h * 0.06f),
        size = Size(w * 0.08f, h * 0.048f),
        cornerRadius = CornerRadius(w * 0.025f, w * 0.025f)
    )
    drawLine(sneakerStripe, Offset(cx + w * 0.025f + legShift, groundY - h * 0.025f), Offset(cx + w * 0.09f + legShift, groundY - h * 0.025f), strokeWidth = 3f)

    drawRoundRect(
        color = tshirtBlue,
        topLeft = Offset(cx - w * 0.105f, groundY - h * 0.42f + bobY),
        size = Size(w * 0.21f, h * 0.21f),
        cornerRadius = CornerRadius(w * 0.05f, w * 0.05f)
    )
    drawRoundRect(
        color = Color(0xFFDCE9F8),
        topLeft = Offset(cx - w * 0.045f, groundY - h * 0.35f + bobY),
        size = Size(w * 0.09f, h * 0.03f),
        cornerRadius = CornerRadius(w * 0.015f, w * 0.015f)
    )

    drawCircle(color = skinColor, radius = headR, center = Offset(cx, headCy))

    val hairPath = Path().apply {
        moveTo(cx - headR * 1.03f, headCy - headR * 0.1f)
        quadraticTo(cx - headR * 0.9f, headCy - headR * 1.25f, cx, headCy - headR * 1.1f)
        quadraticTo(cx + headR * 0.9f, headCy - headR * 1.25f, cx + headR * 1.03f, headCy - headR * 0.1f)
        quadraticTo(cx + headR * 0.4f, headCy - headR * 0.5f, cx, headCy - headR * 0.3f)
        quadraticTo(cx - headR * 0.4f, headCy - headR * 0.5f, cx - headR * 1.03f, headCy - headR * 0.1f)
        close()
    }
    drawPath(hairPath, color = blackHair)

    drawCircle(color = Color(0xFF142D59), radius = w * 0.02f, center = Offset(cx - headR * 0.36f, headCy + headR * 0.08f))
    drawCircle(color = Color(0xFF142D59), radius = w * 0.02f, center = Offset(cx + headR * 0.36f, headCy + headR * 0.08f))
    drawCircle(color = Color(0xFFF3F6FC), radius = w * 0.007f, center = Offset(cx - headR * 0.38f, headCy + headR * 0.05f))
    drawCircle(color = Color(0xFFF3F6FC), radius = w * 0.007f, center = Offset(cx + headR * 0.34f, headCy + headR * 0.05f))
    drawOval(color = blushColor.copy(alpha = 0.3f), topLeft = Offset(cx - headR * 0.65f, headCy + headR * 0.18f), size = Size(w * 0.045f, h * 0.022f))
    drawOval(color = blushColor.copy(alpha = 0.3f), topLeft = Offset(cx + headR * 0.35f, headCy + headR * 0.18f), size = Size(w * 0.045f, h * 0.022f))

    drawArc(
        color = Color(0xFF4A2C16),
        startAngle = 20f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(cx - headR * 0.22f, headCy + headR * 0.24f),
        size = Size(headR * 0.44f, headR * 0.24f),
        style = Stroke(width = 2.5f, cap = StrokeCap.Round)
    )

    val shoulderY = groundY - h * 0.38f + bobY
    if (mood == CompanionMood.STUDYING) {
        val penBob = (armPhase - 0.5f) * w * 0.04f
        drawLine(skinColor, Offset(cx - w * 0.10f, shoulderY), Offset(cx - w * 0.02f, groundY - h * 0.24f), strokeWidth = w * 0.032f, cap = StrokeCap.Round)
        drawLine(skinColor, Offset(cx + w * 0.10f, shoulderY), Offset(cx + w * 0.06f + penBob, groundY - h * 0.24f), strokeWidth = w * 0.032f, cap = StrokeCap.Round)
        drawLine(Color(0xFF142D59), Offset(cx + w * 0.06f + penBob, groundY - h * 0.24f), Offset(cx + w * 0.03f + penBob, groundY - h * 0.21f), strokeWidth = w * 0.018f, cap = StrokeCap.Round)
    } else if (mood == CompanionMood.BREAK_RELAXING) {
        val sipLift = armPhase * h * 0.05f
        drawLine(skinColor, Offset(cx - w * 0.10f, shoulderY), Offset(cx - w * 0.15f, shoulderY + h * 0.08f), strokeWidth = w * 0.032f, cap = StrokeCap.Round)
        drawLine(skinColor, Offset(cx + w * 0.10f, shoulderY), Offset(cx + w * 0.12f, shoulderY + h * 0.05f - sipLift), strokeWidth = w * 0.032f, cap = StrokeCap.Round)
        drawRoundRect(
            color = Color(0xFF83B8EB),
            topLeft = Offset(cx + w * 0.10f, shoulderY - h * 0.01f - sipLift),
            size = Size(w * 0.06f, h * 0.095f),
            cornerRadius = CornerRadius(w * 0.02f, w * 0.02f)
        )
    } else {
        drawLine(skinColor, Offset(cx - w * 0.10f, shoulderY), Offset(cx - w * 0.15f, shoulderY + h * 0.11f), strokeWidth = w * 0.032f, cap = StrokeCap.Round)
        drawLine(skinColor, Offset(cx + w * 0.10f, shoulderY), Offset(cx + w * 0.15f, shoulderY + h * 0.11f), strokeWidth = w * 0.032f, cap = StrokeCap.Round)
    }
}
