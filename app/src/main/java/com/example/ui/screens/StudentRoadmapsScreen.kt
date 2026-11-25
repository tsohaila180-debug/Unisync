package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.AouRoadmapsData
import com.example.data.AppStrings
import com.example.model.CourseCategory
import com.example.model.CourseCompletionStatus
import com.example.model.RoadmapCourse
import com.example.ui.theme.CategoryCoreColor
import com.example.ui.theme.CategoryGeneralColor
import com.example.ui.theme.CategoryGraduationColor
import com.example.ui.theme.CategoryMathColor
import com.example.ui.theme.InstructorAccentPurple
import com.example.ui.theme.SoftOffWhite
import com.example.ui.theme.StudentAccentBlue
import com.example.ui.theme.StudentPrimaryBlue
import com.example.ui.theme.StudentSecondaryText
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private fun colorForCategory(category: CourseCategory): Color = when (category) {
    CourseCategory.GENERAL -> CategoryGeneralColor
    CourseCategory.MATH -> CategoryMathColor
    CourseCategory.CORE -> CategoryCoreColor
    CourseCategory.GRADUATION -> CategoryGraduationColor
}

private fun labelForCategory(category: CourseCategory, strings: AppStrings): String = when (category) {
    CourseCategory.GENERAL -> strings.catGeneral
    CourseCategory.MATH -> strings.catMath
    CourseCategory.CORE -> strings.catCore
    CourseCategory.GRADUATION -> strings.catGraduation
}

private fun colorForStatus(status: CourseCompletionStatus): Color = when (status) {
    CourseCompletionStatus.COMPLETED -> StudentPrimaryBlue
    CourseCompletionStatus.IN_PROGRESS -> StudentAccentBlue
    CourseCompletionStatus.NEXT_TERM -> InstructorAccentPurple
    CourseCompletionStatus.NOT_STARTED -> StudentSecondaryText
}

private fun iconForStatus(status: CourseCompletionStatus): ImageVector = when (status) {
    CourseCompletionStatus.COMPLETED -> Icons.Default.CheckCircle
    CourseCompletionStatus.IN_PROGRESS -> Icons.Default.PlayCircle
    CourseCompletionStatus.NEXT_TERM -> Icons.Default.Schedule
    CourseCompletionStatus.NOT_STARTED -> Icons.Default.RadioButtonUnchecked
}

private fun localizedStatusLabel(status: CourseCompletionStatus, strings: AppStrings): String = when (status) {
    CourseCompletionStatus.COMPLETED -> strings.statusCompleted
    CourseCompletionStatus.IN_PROGRESS -> strings.statusInProgress
    CourseCompletionStatus.NEXT_TERM -> strings.statusNextTerm
    CourseCompletionStatus.NOT_STARTED -> strings.statusNotStarted
}

private data class FlowchartNodeLayout(
    val course: RoadmapCourse,
    val stageIdx: Int,
    val rowIdx: Int,
    val xDp: Float,
    val yDp: Float
)

private const val MAX_ZOOM_SCALE = 1.8f

private fun zoomPercentToScale(percent: Float, fitScale: Float): Float {
    val p = percent.coerceIn(0f, 200f)
    return if (p <= 100f) {
        fitScale + (p / 100f) * (1.0f - fitScale)
    } else {
        1.0f + ((p - 100f) / 100f) * (MAX_ZOOM_SCALE - 1.0f)
    }
}

private fun scaleToZoomPercent(scale: Float, fitScale: Float): Float {
    val s = scale.coerceIn(fitScale, MAX_ZOOM_SCALE)
    return if (s <= 1.0f) {
        val denom = (1.0f - fitScale).coerceAtLeast(0.01f)
        ((s - fitScale) / denom) * 100f
    } else {
        100f + ((s - 1.0f) / (MAX_ZOOM_SCALE - 1.0f)) * 100f
    }.coerceIn(0f, 200f)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudentRoadmapsScreen(
    selectedMajorId: String,
    courseStatuses: Map<String, CourseCompletionStatus>,
    strings: AppStrings,
    onSelectMajor: (String) -> Unit,
    onCourseStatusChanged: (String, CourseCompletionStatus) -> Unit
) {
    val activeMajor = remember(selectedMajorId) {
        AouRoadmapsData.allMajors.firstOrNull { it.id == selectedMajorId } ?: AouRoadmapsData.allMajors.first()
    }
    val stages = remember(activeMajor) {
        activeMajor.courses.groupBy { it.stage }.toSortedMap().entries.toList()
    }

    var inspectedCourse by remember { mutableStateOf<RoadmapCourse?>(null) }
    var highlightedCourseCode by remember(activeMajor.id) { mutableStateOf<String?>(null) }

    val statusForCourse: (RoadmapCourse) -> CourseCompletionStatus = { course ->
        courseStatuses["${activeMajor.id}:${course.code}"] ?: CourseCompletionStatus.NOT_STARTED
    }
    val completedCourses = activeMajor.courses.filter { statusForCourse(it) == CourseCompletionStatus.COMPLETED }
    val inProgressCount = activeMajor.courses.count { statusForCourse(it) == CourseCompletionStatus.IN_PROGRESS }
    val nextTermCount = activeMajor.courses.count { statusForCourse(it) == CourseCompletionStatus.NEXT_TERM }
    val completedCredits = completedCourses.sumOf { it.credits }
    val progressFraction = if (activeMajor.totalCredits > 0) {
        (completedCredits.toFloat() / activeMajor.totalCredits.toFloat()).coerceIn(0f, 1f)
    } else 0f

    // Canonical 1.0x layout geometry — spacious, crisp, zero overlap
    val nodeWidthDp = 184f
    val nodeHeightDp = 104f
    val colGapDp = 76f
    val rowGapDp = 24f
    val headerBandDp = 42f
    val boardPaddingDp = 20f

    val nodeLayouts = remember(activeMajor) {
        buildList {
            stages.forEachIndexed { colIdx, (_, coursesInStage) ->
                val x = boardPaddingDp + colIdx * (nodeWidthDp + colGapDp)
                coursesInStage.forEachIndexed { rowIdx, course ->
                    val y = boardPaddingDp + headerBandDp + rowIdx * (nodeHeightDp + rowGapDp)
                    add(FlowchartNodeLayout(course, colIdx, rowIdx, x, y))
                }
            }
        }
    }
    val layoutByCode = remember(nodeLayouts) { nodeLayouts.associateBy { it.course.code } }
    val maxRows = remember(stages) { stages.maxOfOrNull { it.value.size } ?: 1 }
    val totalBoardWidthDp = boardPaddingDp * 2 + stages.size * nodeWidthDp + (stages.size - 1).coerceAtLeast(0) * colGapDp
    val totalBoardHeightDp = boardPaddingDp * 2 + headerBandDp + maxRows * nodeHeightDp + (maxRows - 1).coerceAtLeast(0) * rowGapDp

    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        val isCompactWidth = maxWidth < 600.dp
        val defaultZoomPercent = if (isCompactWidth) 70f else 100f

        var zoomPercent by remember { mutableFloatStateOf(defaultZoomPercent) }
        var zoomInitialized by remember { mutableStateOf(false) }
        var hasViewportMeasured by remember { mutableStateOf(false) }
        var panOffset by remember { mutableStateOf(Offset.Zero) }
        var viewportSizePx by remember { mutableStateOf(Offset(1f, 1f)) }

        LaunchedEffect(isCompactWidth) {
            if (!zoomInitialized) {
                zoomPercent = defaultZoomPercent
                zoomInitialized = true
            }
        }

        val viewportWidthDp = maxWidth.value.coerceAtLeast(300f)
        val fitScale = remember(viewportWidthDp, totalBoardWidthDp) {
            ((viewportWidthDp - 16f) / totalBoardWidthDp).coerceIn(0.24f, 0.78f)
        }
        val currentScale = zoomPercentToScale(zoomPercent, fitScale)

        val boardWidthPx = with(density) { totalBoardWidthDp.dp.toPx() }
        val boardHeightPx = with(density) { totalBoardHeightDp.dp.toPx() }

        fun clampPan(rawPan: Offset, scale: Float): Offset {
            val vw = viewportSizePx.x.coerceAtLeast(1f)
            val vh = viewportSizePx.y.coerceAtLeast(1f)
            val scaledW = boardWidthPx * scale
            val scaledH = boardHeightPx * scale

            val clampedX = if (scaledW <= vw) {
                (vw - scaledW) * 0.5f
            } else {
                rawPan.x.coerceIn(vw - scaledW - 40f, 40f)
            }
            val clampedY = if (scaledH <= vh) {
                (vh - scaledH) * 0.5f
            } else {
                rawPan.y.coerceIn(vh - scaledH - 40f, 40f)
            }
            return Offset(clampedX, clampedY)
        }

        fun applyZoomAround(targetPercent: Float, pivot: Offset = Offset(viewportSizePx.x * 0.5f, viewportSizePx.y * 0.5f)) {
            val clampedPercent = targetPercent.coerceIn(0f, 200f)
            val oldScale = zoomPercentToScale(zoomPercent, fitScale)
            val newScale = zoomPercentToScale(clampedPercent, fitScale)
            zoomPercent = clampedPercent
            if (clampedPercent == 0f) {
                panOffset = clampPan(Offset.Zero, newScale)
            } else {
                val ratio = newScale / oldScale
                val shifted = Offset(
                    x = pivot.x - (pivot.x - panOffset.x) * ratio,
                    y = pivot.y - (pivot.y - panOffset.y) * ratio
                )
                panOffset = clampPan(shifted, newScale)
            }
        }

        LaunchedEffect(activeMajor.id) {
            if (hasViewportMeasured) {
                panOffset = clampPan(Offset.Zero, currentScale)
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Title + Zoom Controls (0% - 200%, -, +, Slider, Reset)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        Icons.Default.AccountTree,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text(
                            text = strings.roadmapTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${strings.zoomLabel}: ${zoomPercent.roundToInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.testTag("roadmap_zoom_percent_label")
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { applyZoomAround(0f) },
                        modifier = Modifier.testTag("roadmap_zoom_zero_button")
                    ) {
                        Text("0%", style = MaterialTheme.typography.labelMedium)
                    }
                    IconButton(
                        onClick = { applyZoomAround(zoomPercent - 15f) },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("roadmap_zoom_out_button")
                    ) {
                        Icon(Icons.Default.ZoomOut, contentDescription = strings.zoomLabel, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { applyZoomAround(zoomPercent + 15f) },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("roadmap_zoom_in_button")
                    ) {
                        Icon(Icons.Default.ZoomIn, contentDescription = strings.zoomLabel, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = {
                            zoomPercent = defaultZoomPercent
                            panOffset = clampPan(Offset.Zero, zoomPercentToScale(defaultZoomPercent, fitScale))
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("roadmap_zoom_reset_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = strings.reset, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Slider(
                value = zoomPercent,
                onValueChange = { applyZoomAround(it) },
                valueRange = 0f..200f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .testTag("roadmap_zoom_slider")
            )

            // 10 Majors Horizontal Chip Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AouRoadmapsData.allMajors.forEach { major ->
                    FilterChip(
                        selected = major.id == activeMajor.id,
                        onClick = {
                            highlightedCourseCode = null
                            onSelectMajor(major.id)
                        },
                        label = { Text(major.name, maxLines = 1) },
                        modifier = Modifier.testTag("major_chip_${major.id}")
                    )
                }
            }

            // Compact Progress Banner + Course Category Color Legend
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${strings.statusCompleted}: ${completedCourses.size}/${activeMajor.courses.size} ($completedCredits/${activeMajor.totalCredits} ${strings.creditsShort})",
                            style = MaterialTheme.typography.labelMedium,
                            color = colorForStatus(CourseCompletionStatus.COMPLETED),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.testTag("roadmap_progress_label")
                        )
                        Text(
                            text = "${(progressFraction * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50)),
                        color = colorForStatus(CourseCompletionStatus.COMPLETED),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "${strings.statusInProgress}: $inProgressCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorForStatus(CourseCompletionStatus.IN_PROGRESS)
                        )
                        Text(
                            text = "${strings.statusNextTerm}: $nextTermCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorForStatus(CourseCompletionStatus.NEXT_TERM)
                        )
                    }

                    val activeCategories = remember(activeMajor) {
                        activeMajor.courses.map { it.category }.distinct().sortedBy { it.ordinal }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .testTag("roadmap_category_legend"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        activeCategories.forEach { category ->
                            val catColor = colorForCategory(category)
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = catColor.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, catColor.copy(alpha = 0.45f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(catColor)
                                    )
                                    Text(
                                        text = labelForCategory(category, strings),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = catColor,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Interactive Visual Pan & Zoom Flowchart Canvas
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("roadmap_visual_canvas_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                        .pointerInput(fitScale, boardWidthPx, boardHeightPx) {
                            detectTransformGestures { centroid, pan, zoom, _ ->
                                val oldScale = zoomPercentToScale(zoomPercent, fitScale)
                                val newScale = (oldScale * zoom).coerceIn(fitScale, MAX_ZOOM_SCALE)
                                val newPercent = scaleToZoomPercent(newScale, fitScale)
                                val ratio = newScale / oldScale
                                val nextPan = Offset(
                                    x = centroid.x - (centroid.x - panOffset.x) * ratio + pan.x,
                                    y = centroid.y - (centroid.y - panOffset.y) * ratio + pan.y
                                )
                                zoomPercent = newPercent
                                panOffset = clampPan(nextPan, newScale)
                            }
                        }
                ) {
                    val vwPx = with(density) { maxWidth.toPx() }
                    val vhPx = with(density) { maxHeight.toPx() }
                    LaunchedEffect(vwPx, vhPx) {
                        if (vwPx > 10f && vhPx > 10f) {
                            viewportSizePx = Offset(vwPx, vhPx)
                            if (!hasViewportMeasured) {
                                hasViewportMeasured = true
                                panOffset = clampPan(Offset.Zero, currentScale)
                            } else {
                                panOffset = clampPan(panOffset, currentScale)
                            }
                        }
                    }

                    // Keep canvas coordinate math strictly LTR so lines align identically in Arabic RTL & LTR
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Box(
                            modifier = Modifier
                                .width(totalBoardWidthDp.dp)
                                .height(totalBoardHeightDp.dp)
                                .graphicsLayer {
                                    transformOrigin = TransformOrigin(0f, 0f)
                                    scaleX = currentScale
                                    scaleY = currentScale
                                    translationX = panOffset.x
                                    translationY = panOffset.y
                                }
                        ) {
                            val subtleConnectorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            val dimmedConnectorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)
                            val incomingColor = MaterialTheme.colorScheme.primary
                            val outgoingColor = MaterialTheme.colorScheme.tertiary

                            // Stage Column Header Pills
                            stages.forEachIndexed { colIdx, (stageNum, _) ->
                                val headerX = boardPaddingDp + colIdx * (nodeWidthDp + colGapDp)
                                Surface(
                                    modifier = Modifier
                                        .offset {
                                            with(density) {
                                                IntOffset(headerX.dp.toPx().roundToInt(), boardPaddingDp.dp.toPx().roundToInt())
                                            }
                                        }
                                        .width(nodeWidthDp.dp),
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "${strings.stageLabel} $stageNum",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            // Clean Routed Prerequisite Connector Arrows
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val hasHighlight = highlightedCourseCode != null

                                // Draw non-highlighted edges first, then highlighted edges on top
                                val allEdges = buildList {
                                    nodeLayouts.forEach { target ->
                                        target.course.prerequisites.forEachIndexed { pIdx, preCode ->
                                            val source = layoutByCode[preCode]
                                            if (source != null) {
                                                add(Triple(source, target, pIdx))
                                            }
                                        }
                                    }
                                }

                                val sortedEdges = allEdges.sortedBy { (source, target, _) ->
                                    val isIncoming = target.course.code == highlightedCourseCode
                                    val isOutgoing = source.course.code == highlightedCourseCode
                                    if (isIncoming || isOutgoing) 1 else 0
                                }

                                sortedEdges.forEach { (source, target, pIdx) ->
                                    val isIncoming = highlightedCourseCode != null && target.course.code == highlightedCourseCode
                                    val isOutgoing = highlightedCourseCode != null && source.course.code == highlightedCourseCode
                                    val isActiveEdge = isIncoming || isOutgoing

                                    val lineColor = when {
                                        isIncoming -> incomingColor
                                        isOutgoing -> outgoingColor
                                        hasHighlight -> dimmedConnectorColor
                                        else -> subtleConnectorColor
                                    }
                                    val strokePx = if (isActiveEdge) 3.dp.toPx() else 1.5.dp.toPx()

                                    val startX = (source.xDp + nodeWidthDp).dp.toPx()
                                    val startY = (source.yDp + nodeHeightDp * 0.5f).dp.toPx()
                                    val targetX = target.xDp.dp.toPx()
                                    val targetY = (target.yDp + nodeHeightDp * 0.5f + ((pIdx % 3) - 1) * 8f).dp.toPx()

                                    // Channel offset inside the column gutter so parallel lines separate cleanly
                                    val gutterFraction = 0.35f + ((source.rowIdx + target.rowIdx) % 4) * 0.1f
                                    val channelX = startX + (targetX - startX) * gutterFraction
                                    val tipX = targetX - 3.dp.toPx()

                                    val path = Path().apply {
                                        moveTo(startX, startY)
                                        if (target.stageIdx > source.stageIdx) {
                                            cubicTo(
                                                channelX, startY,
                                                channelX, targetY,
                                                tipX, targetY
                                            )
                                        } else {
                                            // Same-stage fallback arc cleanly around right edge
                                            val arcX = startX + 26.dp.toPx()
                                            cubicTo(
                                                arcX, startY,
                                                arcX, targetY,
                                                tipX, targetY
                                            )
                                        }
                                    }
                                    drawPath(
                                        path = path,
                                        color = lineColor,
                                        style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                    )

                                    val angle = atan2(0.0, 1.0)
                                    val arrowLen = if (isActiveEdge) 8.dp.toPx() else 6.dp.toPx()
                                    val wing1 = Offset(
                                        (tipX - arrowLen * cos(angle - 0.55)).toFloat(),
                                        (targetY - arrowLen * sin(angle - 0.55)).toFloat()
                                    )
                                    val wing2 = Offset(
                                        (tipX - arrowLen * cos(angle + 0.55)).toFloat(),
                                        (targetY - arrowLen * sin(angle + 0.55)).toFloat()
                                    )
                                    drawLine(lineColor, Offset(tipX, targetY), wing1, strokeWidth = strokePx, cap = StrokeCap.Round)
                                    drawLine(lineColor, Offset(tipX, targetY), wing2, strokeWidth = strokePx, cap = StrokeCap.Round)
                                }
                            }

                            // Course Nodes
                            nodeLayouts.forEach { layout ->
                                val course = layout.course
                                val accent = colorForCategory(course.category)
                                val status = statusForCourse(course)
                                val statusColor = colorForStatus(status)
                                val statusLabel = localizedStatusLabel(status, strings)

                                val isDirect = highlightedCourseCode == course.code
                                val isRelated = highlightedCourseCode != null && (
                                    highlightedCourseCode in course.prerequisites ||
                                        layoutByCode[highlightedCourseCode]?.course?.prerequisites?.contains(course.code) == true
                                    )

                                Surface(
                                    modifier = Modifier
                                        .offset {
                                            with(density) {
                                                IntOffset(layout.xDp.dp.toPx().roundToInt(), layout.yDp.dp.toPx().roundToInt())
                                            }
                                        }
                                        .width(nodeWidthDp.dp)
                                        .height(nodeHeightDp.dp)
                                        .clip(MaterialTheme.shapes.small)
                                        .clickable {
                                            highlightedCourseCode = course.code
                                            inspectedCourse = course
                                        }
                                        .testTag("roadmap_course_${course.code}"),
                                    shape = MaterialTheme.shapes.small,
                                    color = when {
                                        status == CourseCompletionStatus.COMPLETED -> statusColor.copy(alpha = 0.14f)
                                        status != CourseCompletionStatus.NOT_STARTED -> statusColor.copy(alpha = 0.08f)
                                        else -> accent.copy(alpha = 0.07f)
                                    },
                                    border = BorderStroke(
                                        width = when {
                                            isDirect -> 2.5.dp
                                            isRelated || status != CourseCompletionStatus.NOT_STARTED -> 1.8.dp
                                            else -> 1.2.dp
                                        },
                                        color = when {
                                            isDirect -> MaterialTheme.colorScheme.primary
                                            status != CourseCompletionStatus.NOT_STARTED -> statusColor
                                            else -> accent
                                        }
                                    ),
                                    shadowElevation = if (isDirect || isRelated) 5.dp else 1.5.dp
                                ) {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        // Header strip: Code + Credits + Expand arrow
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(accent)
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = course.code,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = SoftOffWhite,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                                Text(
                                                    text = "${course.credits}${strings.creditsShort}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = SoftOffWhite.copy(alpha = 0.9f)
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.ExpandMore,
                                                    contentDescription = strings.courseStatusTitle,
                                                    tint = SoftOffWhite,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }

                                        // Title + Prereq + Status
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 8.dp, vertical = 5.dp),
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = course.title,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )

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
                                                        imageVector = Icons.Default.LockOpen,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                    Spacer(Modifier.width(3.dp))
                                                    Text(
                                                        text = if (course.prerequisites.isEmpty()) "—" else course.prerequisites.joinToString(","),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Spacer(Modifier.width(4.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Icon(
                                                        imageVector = iconForStatus(status),
                                                        contentDescription = statusLabel,
                                                        tint = statusColor,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    if (status != CourseCompletionStatus.NOT_STARTED) {
                                                        Text(
                                                            text = statusLabel,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = statusColor,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Course Detail & Status Popover/Dialog on Tap
    inspectedCourse?.let { course ->
        val currentStatus = statusForCourse(course)
        val unlockedCourses = remember(activeMajor, course.code) {
            activeMajor.courses.filter { course.code in it.prerequisites }
        }
        AlertDialog(
            onDismissRequest = { inspectedCourse = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colorForCategory(course.category)
                    ) {
                        Text(
                            text = course.code,
                            color = SoftOffWhite,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Text(course.title, style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val catColor = colorForCategory(course.category)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = catColor.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, catColor.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = labelForCategory(course.category, strings),
                                style = MaterialTheme.typography.labelSmall,
                                color = catColor,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = "${strings.stageLabel} ${course.stage} · ${course.credits} ${strings.creditsShort}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    if (course.description.isNotBlank()) {
                        Text(course.description, style = MaterialTheme.typography.bodyMedium)
                    }

                    Text(
                        text = strings.courseStatusTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CourseCompletionStatus.entries.forEach { status ->
                            val selected = currentStatus == status
                            val stColor = colorForStatus(status)
                            val label = localizedStatusLabel(status, strings)
                            FilterChip(
                                selected = selected,
                                onClick = { onCourseStatusChanged(course.code, status) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = iconForStatus(status),
                                        contentDescription = label,
                                        modifier = Modifier.size(15.dp),
                                        tint = if (selected) stColor else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = stColor.copy(alpha = 0.16f),
                                    selectedLabelColor = stColor
                                ),
                                modifier = Modifier.testTag("roadmap_status_${status.name.lowercase()}")
                            )
                        }
                    }

                    Text(
                        text = strings.prereqLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (course.prerequisites.isEmpty()) {
                        Text(strings.foundationCourseLabel, style = MaterialTheme.typography.bodySmall)
                    } else {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            course.prerequisites.forEach { preCode ->
                                val match = activeMajor.courses.firstOrNull { it.code == preCode }
                                FilterChip(
                                    selected = true,
                                    onClick = {
                                        if (match != null) {
                                            highlightedCourseCode = match.code
                                            inspectedCourse = match
                                        }
                                    },
                                    label = { Text(if (match != null) "$preCode · ${match.title}" else preCode) }
                                )
                            }
                        }
                    }

                    if (unlockedCourses.isNotEmpty()) {
                        Text(
                            text = strings.unlocksLabel,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            unlockedCourses.forEach { nextCourse ->
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        highlightedCourseCode = nextCourse.code
                                        inspectedCourse = nextCourse
                                    },
                                    label = { Text("${nextCourse.code} · ${nextCourse.title}") }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { inspectedCourse = null }) { Text(strings.doneLabel) }
            }
        )
    }
}
