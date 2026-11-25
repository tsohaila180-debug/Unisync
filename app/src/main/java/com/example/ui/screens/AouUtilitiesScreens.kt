package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AouDirectoryData
import com.example.data.AppStrings
import com.example.data.LocalizationData
import com.example.model.GpaCourseEntry
import java.util.Locale
import java.util.UUID

private fun sanitizeGpaInput(raw: String): String {
    val cleaned = raw.replace(',', '.')
    val sb = StringBuilder()
    var hasDot = false
    for (ch in cleaned) {
        if (ch.isDigit()) {
            sb.append(ch)
        } else if (ch == '.' && !hasDot) {
            hasDot = true
            sb.append(ch)
        }
    }
    return sb.toString().take(5)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AouGpaCalculatorScreen(
    courses: List<GpaCourseEntry>,
    priorGpa: String,
    priorCredits: String,
    strings: AppStrings = LocalizationData.stringsFor("en"),
    onUpdateGpaState: (List<GpaCourseEntry>, String, String) -> Unit
) {
    var courseCode by remember { mutableStateOf("") }
    var credits by remember { mutableIntStateOf(3) }
    var selectedGradeIndex by remember { mutableIntStateOf(0) }

    // Buffered text fields so rapid typing / decimal entry never lags against async DataStore writes
    var priorGpaInput by remember { mutableStateOf(priorGpa) }
    var priorCreditsInput by remember { mutableStateOf(priorCredits) }

    LaunchedEffect(priorGpa) {
        if (priorGpa != priorGpaInput && (priorGpaInput.isEmpty() || priorGpa.isNotEmpty())) {
            priorGpaInput = priorGpa
        }
    }
    LaunchedEffect(priorCredits) {
        if (priorCredits != priorCreditsInput && (priorCreditsInput.isEmpty() || priorCredits.isNotEmpty())) {
            priorCreditsInput = priorCredits
        }
    }

    val termCredits = courses.sumOf { it.credits }
    val termPoints = courses.sumOf { it.credits * it.gradePoint }
    val termGpa = if (termCredits > 0) termPoints / termCredits else 0.0

    val prevGpaVal = priorGpaInput.toDoubleOrNull()?.coerceIn(0.0, 4.0) ?: 0.0
    val prevCrVal = priorCreditsInput.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val totalCredits = prevCrVal + termCredits
    val cumulativeGpa = if (totalCredits > 0) {
        ((prevGpaVal * prevCrVal) + termPoints) / totalCredits
    } else 0.0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        Icons.Default.Calculate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = strings.gpaTitle,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (courses.isNotEmpty() || priorGpaInput.isNotEmpty() || priorCreditsInput.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            priorGpaInput = ""
                            priorCreditsInput = ""
                            courseCode = ""
                            onUpdateGpaState(emptyList(), "", "")
                        },
                        modifier = Modifier.testTag("gpa_clear_all_button")
                    ) {
                        Icon(Icons.Default.ClearAll, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.clearAllLabel, maxLines = 1)
                    }
                }
            }
        }

        // Real-Time SIS GPA Summary Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gpa_summary_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = MaterialTheme.shapes.large
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = strings.semesterGpa,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                        Text(
                            text = String.format(Locale.US, "%.2f", termGpa),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.testTag("gpa_semester_value")
                        )
                        Text(
                            text = "$termCredits ${strings.creditsShort}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = strings.cumulativeGpa,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                        Text(
                            text = String.format(Locale.US, "%.2f", cumulativeGpa),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.testTag("gpa_cumulative_value")
                        )
                        Text(
                            text = "$totalCredits ${strings.creditsShort}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Prior Cumulative Inputs (Clean optional fields, blank by default with 0.00 / 0 placeholder)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.priorRecordOptional, style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = priorGpaInput,
                            onValueChange = { raw ->
                                val sanitized = sanitizeGpaInput(raw)
                                val parsed = sanitized.toDoubleOrNull()
                                if (sanitized.isEmpty() || sanitized == "." || (parsed != null && parsed <= 4.0)) {
                                    priorGpaInput = sanitized
                                    onUpdateGpaState(courses, sanitized, priorCreditsInput)
                                }
                            },
                            label = { Text(strings.priorGpaLabel, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            placeholder = { Text("0.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("gpa_prior_gpa_input")
                        )
                        OutlinedTextField(
                            value = priorCreditsInput,
                            onValueChange = { raw ->
                                val digits = raw.filter { it.isDigit() }.take(4)
                                priorCreditsInput = digits
                                onUpdateGpaState(courses, priorGpaInput, digits)
                            },
                            label = { Text(strings.priorCreditsLabel, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            placeholder = { Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("gpa_prior_credits_input")
                        )
                    }
                }
            }
        }

        // Simple Course Entry (A is highest — NO A+)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.addCourseLabel, style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = courseCode,
                        onValueChange = { courseCode = it },
                        label = { Text(strings.courseNameCodeLabel) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gpa_course_code_input")
                    )

                    Text(strings.creditHoursLabel, style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1, 2, 3, 4, 8).forEach { cr ->
                            FilterChip(
                                selected = credits == cr,
                                onClick = { credits = cr },
                                label = { Text("$cr ${strings.creditsShort}") },
                                modifier = Modifier.testTag("gpa_credit_$cr")
                            )
                        }
                    }

                    Text(strings.gradeLabel, style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AouDirectoryData.aouGradingScale.forEachIndexed { idx, (grade, pts) ->
                            FilterChip(
                                selected = selectedGradeIndex == idx,
                                onClick = { selectedGradeIndex = idx },
                                label = { Text("$grade (${String.format(Locale.US, "%.1f", pts)})") },
                                modifier = Modifier.testTag("gpa_grade_${grade.lowercase()}")
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val (label, pts) = AouDirectoryData.aouGradingScale[selectedGradeIndex]
                            val entry = GpaCourseEntry(
                                id = UUID.randomUUID().toString(),
                                code = courseCode.trim().uppercase().ifBlank { "COURSE ${courses.size + 1}" },
                                credits = credits,
                                gradeLabel = label,
                                gradePoint = pts
                            )
                            onUpdateGpaState(courses + entry, priorGpaInput, priorCreditsInput)
                            courseCode = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gpa_add_course_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.addCourseLabel)
                    }
                }
            }
        }

        items(courses, key = { it.id }) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gpa_course_row_${item.id}"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.code, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "${item.credits} ${strings.creditsShort} · ${strings.gradeLabel}: ${item.gradeLabel} (${String.format(Locale.US, "%.2f", item.gradePoint)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = {
                            onUpdateGpaState(
                                courses.filterNot { it.id == item.id },
                                priorGpaInput,
                                priorCreditsInput
                            )
                        }
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = strings.clearAllLabel,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
fun AouPortalsHubScreen(
    currentBranch: String,
    strings: AppStrings = LocalizationData.stringsFor("en"),
    onSelectBranch: (String) -> Unit
) {
    val context = LocalContext.current
    val portal = remember(currentBranch) {
        AouDirectoryData.branchPortals.firstOrNull {
            it.branchName.equals(currentBranch, ignoreCase = true)
        } ?: AouDirectoryData.branchPortals.first()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(strings.aouPortalsTitle, style = MaterialTheme.typography.titleLarge)
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AouDirectoryData.branchPortals.forEach { bp ->
                    FilterChip(
                        selected = bp.branchName.equals(portal.branchName, ignoreCase = true),
                        onClick = { onSelectBranch(bp.branchName) },
                        label = { Text(bp.branchName, maxLines = 1) },
                        modifier = Modifier.testTag("portal_branch_${bp.branchName.lowercase()}")
                    )
                }
            }
        }

        item {
            PortalLinkCard(
                title = "SIS · Student Information System",
                subtitle = "${portal.countryLabel} — Registration, Grades, Schedule & Tuition",
                url = portal.sisUrl,
                openLabel = strings.openLabel,
                onOpen = { openUrlSafely(context, portal.sisUrl) }
            )
        }

        item {
            PortalLinkCard(
                title = "LMS · Learning Management System",
                subtitle = "${portal.countryLabel} — Lecture Slides, TMAs & Online Exams",
                url = portal.lmsUrl,
                openLabel = strings.openLabel,
                onOpen = { openUrlSafely(context, portal.lmsUrl) }
            )
        }

        item {
            PortalLinkCard(
                title = "Admissions & Application Portal",
                subtitle = "${portal.countryLabel} — New Student Application & Programs",
                url = portal.admissionsUrl,
                openLabel = strings.openLabel,
                onOpen = { openUrlSafely(context, portal.admissionsUrl) }
            )
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun PortalLinkCard(
    title: String,
    subtitle: String,
    url: String,
    openLabel: String,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(url, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onOpen) {
                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(openLabel)
            }
        }
    }
}

@Composable
fun AouSupportDirectoryScreen(
    strings: AppStrings = LocalizationData.stringsFor("en")
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.SupportAgent, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(strings.aouSupportTitle, style = MaterialTheme.typography.titleLarge)
        }
        SupportDirectoryContent(modifier = Modifier.weight(1f).fillMaxWidth())
    }
}

@Composable
fun AouSupportDirectoryDialog(
    strings: AppStrings = LocalizationData.stringsFor("en"),
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SupportAgent, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(strings.aouSupportTitle, style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            SupportDirectoryContent(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp)
            )
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text(strings.closeLabel) }
        }
    )
}

@Composable
private fun SupportDirectoryContent(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var selectedDept by remember { mutableStateOf("All") }
    var selectedBranch by remember { mutableStateOf("All Branches") }
    var copiedEmail by remember { mutableStateOf<String?>(null) }

    val filtered = remember(selectedDept, selectedBranch) {
        AouDirectoryData.supportContacts.filter { c ->
            val deptOk = selectedDept == "All" || c.department == selectedDept
            val branchOk = selectedBranch == "All Branches" ||
                c.branchScope.equals(selectedBranch, ignoreCase = true) ||
                c.branchScope == "All Branches"
            deptOk && branchOk
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AouDirectoryData.supportDepartments.forEach { dept ->
                FilterChip(
                    selected = selectedDept == dept,
                    onClick = { selectedDept = dept },
                    label = { Text(dept, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AouDirectoryData.branchFilters.forEach { br ->
                FilterChip(
                    selected = selectedBranch == br,
                    onClick = { selectedBranch = br },
                    label = { Text(br, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                )
            }
        }

        if (!copiedEmail.isNullOrBlank()) {
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Text(
                    text = "Copied: $copiedEmail",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered, key = { it.id }) { contact ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("support_contact_${contact.id}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = contact.department,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = contact.branchScope,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(contact.roleOrTopic, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                        Text(contact.email, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(contact.email))
                                    copiedEmail = contact.email
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Copy", style = MaterialTheme.typography.labelSmall)
                            }
                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = Uri.parse("mailto:${contact.email}")
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        clipboard.setText(AnnotatedString(contact.email))
                                        copiedEmail = contact.email
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Email", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun openUrlSafely(context: android.content.Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (_: Exception) {}
}
