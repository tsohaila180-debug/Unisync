package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.CalendarEvent
import com.example.model.CourseCompletionStatus
import com.example.model.CourseMaterial
import com.example.model.DirectChatMessage
import com.example.model.ForumPost
import com.example.model.GeneralChatMessage
import com.example.model.GpaCourseEntry
import com.example.model.InboxEmail
import com.example.model.PersonalMaterial
import com.example.model.PomodoroCharacter
import com.example.model.TextScaleOption
import com.example.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

private val Context.dataStore by preferencesDataStore(name = "unisync_preferences")

data class LocalAppPreferences(
    val role: UserRole = UserRole.STUDENT,
    val languageCode: String = "en",
    val darkMode: Boolean = false,
    val aouBranch: String = "Riyadh",
    val majorId: String = "cs",
    val profilePhotoUri: String = "",
    val pomodoroCharacter: PomodoroCharacter = PomodoroCharacter.GIRL,
    val microsoftConnected: Boolean = false,
    val textScale: TextScaleOption = TextScaleOption.NORMAL,
    val courseStatuses: Map<String, CourseCompletionStatus> = emptyMap(),
    val focusedStudyMinutes: Int = 0,
    val gpaCourses: List<GpaCourseEntry> = emptyList(),
    val priorGpa: String = "",
    val priorCredits: String = ""
)

class PreferencesManager(private val context: Context) {
    private object Keys {
        val ROLE = stringPreferencesKey("role")
        val LANGUAGE = stringPreferencesKey("language_code")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val AOU_BRANCH = stringPreferencesKey("aou_branch")
        val MAJOR_ID = stringPreferencesKey("major_id")
        val PROFILE_PHOTO = stringPreferencesKey("profile_photo_uri")
        val POMODORO_CHARACTER = stringPreferencesKey("pomodoro_character")
        val MICROSOFT_CONNECTED = booleanPreferencesKey("microsoft_connected")
        val TEXT_SCALE = stringPreferencesKey("text_scale")
        val COURSE_STATUSES = stringPreferencesKey("course_statuses_json")
        val FOCUSED_STUDY_MINUTES = intPreferencesKey("focused_study_minutes")
        val GPA_COURSES = stringPreferencesKey("gpa_courses_json")
        val PRIOR_GPA = stringPreferencesKey("prior_gpa")
        val PRIOR_CREDITS = stringPreferencesKey("prior_credits")
        val LOCAL_PERSONAL_MATERIALS = stringPreferencesKey("local_personal_materials_json")
        val LOCAL_COURSE_MATERIALS = stringPreferencesKey("local_course_materials_json")
        val LOCAL_EVENTS = stringPreferencesKey("local_events_json")
        val LOCAL_FORUM = stringPreferencesKey("local_forum_json")
    }

    val preferencesFlow: Flow<LocalAppPreferences> = context.dataStore.data.map { prefs ->
        LocalAppPreferences(
            role = UserRole.entries.firstOrNull { it.name == prefs[Keys.ROLE] } ?: UserRole.STUDENT,
            languageCode = prefs[Keys.LANGUAGE] ?: "en",
            darkMode = prefs[Keys.DARK_MODE] ?: false,
            aouBranch = prefs[Keys.AOU_BRANCH] ?: "Riyadh",
            majorId = prefs[Keys.MAJOR_ID] ?: "cs",
            profilePhotoUri = prefs[Keys.PROFILE_PHOTO] ?: "",
            pomodoroCharacter = PomodoroCharacter.entries.firstOrNull {
                it.name == prefs[Keys.POMODORO_CHARACTER]
            } ?: PomodoroCharacter.GIRL,
            microsoftConnected = prefs[Keys.MICROSOFT_CONNECTED] ?: false,
            textScale = TextScaleOption.entries.firstOrNull {
                it.name == prefs[Keys.TEXT_SCALE]
            } ?: TextScaleOption.NORMAL,
            courseStatuses = parseStatuses(prefs[Keys.COURSE_STATUSES]),
            focusedStudyMinutes = prefs[Keys.FOCUSED_STUDY_MINUTES] ?: 0,
            gpaCourses = parseGpaCourses(prefs[Keys.GPA_COURSES]),
            priorGpa = prefs[Keys.PRIOR_GPA] ?: "",
            priorCredits = prefs[Keys.PRIOR_CREDITS] ?: ""
        )
    }

    val localPersonalMaterialsFlow: Flow<List<PersonalMaterial>> = context.dataStore.data.map { prefs ->
        parsePersonalMaterials(prefs[Keys.LOCAL_PERSONAL_MATERIALS])
    }

    val localCourseMaterialsFlow: Flow<List<CourseMaterial>> = context.dataStore.data.map { prefs ->
        parseCourseMaterials(prefs[Keys.LOCAL_COURSE_MATERIALS])
    }

    val localCalendarEventsFlow: Flow<List<CalendarEvent>?> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.LOCAL_EVENTS] ?: return@map null
        parseCalendarEvents(raw)
    }

    val localForumPostsFlow: Flow<List<ForumPost>?> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.LOCAL_FORUM] ?: return@map null
        parseForumPosts(raw)
    }

    private fun parseStatuses(raw: String?): Map<String, CourseCompletionStatus> {
        if (raw.isNullOrBlank()) return emptyMap()
        return try {
            val json = JSONObject(raw)
            buildMap {
                json.keys().forEach { key ->
                    val status = CourseCompletionStatus.entries.firstOrNull { it.name == json.optString(key) }
                    if (status != null) put(key, status)
                }
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun parseGpaCourses(raw: String?): List<GpaCourseEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        GpaCourseEntry(
                            id = obj.optString("id"),
                            code = obj.optString("code"),
                            credits = obj.optInt("credits", 3),
                            gradeLabel = obj.optString("gradeLabel", "A"),
                            gradePoint = obj.optDouble("gradePoint", 4.0)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parsePersonalMaterials(raw: String?): List<PersonalMaterial> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        PersonalMaterial(
                            id = obj.optString("id"),
                            userId = obj.optString("userId", "demo_user"),
                            title = obj.optString("title"),
                            noteContent = obj.optString("noteContent"),
                            fileUri = obj.optString("fileUri"),
                            fileName = obj.optString("fileName"),
                            materialType = obj.optString("materialType", "NOTE")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseCourseMaterials(raw: String?): List<CourseMaterial> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        CourseMaterial(
                            id = obj.optString("id"),
                            authorUid = obj.optString("authorUid", "demo_instructor"),
                            authorName = obj.optString("authorName"),
                            courseCode = obj.optString("courseCode"),
                            title = obj.optString("title"),
                            description = obj.optString("description"),
                            linkUrl = obj.optString("linkUrl"),
                            fileName = obj.optString("fileName"),
                            attachmentUri = obj.optString("attachmentUri"),
                            attachmentType = obj.optString("attachmentType"),
                            materialKind = obj.optString("materialKind")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseCalendarEvents(raw: String): List<CalendarEvent> {
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        CalendarEvent(
                            id = obj.optString("id"),
                            userId = obj.optString("userId", "demo_user"),
                            title = obj.optString("title"),
                            courseCode = obj.optString("courseCode"),
                            eventType = obj.optString("eventType"),
                            triggerEpochMillis = obj.optLong("triggerEpochMillis"),
                            alarmEnabled = obj.optBoolean("alarmEnabled", true),
                            ringtoneId = obj.optString("ringtoneId", "crystal_chime"),
                            volumePercent = obj.optLong("volumePercent", 80L),
                            notes = obj.optString("notes")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseForumPosts(raw: String): List<ForumPost> {
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        ForumPost(
                            id = obj.optString("id"),
                            authorUid = obj.optString("authorUid", "demo_user"),
                            authorName = obj.optString("authorName"),
                            category = obj.optString("category"),
                            questionTitle = obj.optString("questionTitle"),
                            questionBody = obj.optString("questionBody"),
                            repliesCount = obj.optLong("repliesCount", 0L),
                            latestReply = obj.optString("latestReply"),
                            attachmentName = obj.optString("attachmentName"),
                            attachmentUri = obj.optString("attachmentUri"),
                            attachmentType = obj.optString("attachmentType")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun setRole(role: UserRole) {
        context.dataStore.edit { it[Keys.ROLE] = role.name }
    }

    suspend fun setLanguage(code: String) {
        context.dataStore.edit { it[Keys.LANGUAGE] = code }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_MODE] = enabled }
    }

    suspend fun setAouBranch(branch: String) {
        context.dataStore.edit { it[Keys.AOU_BRANCH] = branch }
    }

    suspend fun setMajorId(majorId: String) {
        context.dataStore.edit { it[Keys.MAJOR_ID] = majorId }
    }

    suspend fun setProfilePhotoUri(uri: String) {
        context.dataStore.edit { it[Keys.PROFILE_PHOTO] = uri }
    }

    suspend fun saveAvatarFromUri(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val avatarDir = File(context.filesDir, "avatars").apply { mkdirs() }
            avatarDir.listFiles()?.forEach { it.delete() }
            val outFile = File(avatarDir, "profile_avatar_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null
            val path = outFile.absolutePath
            setProfilePhotoUri(path)
            path
        } catch (_: Exception) {
            null
        }
    }

    suspend fun setPomodoroCharacter(character: PomodoroCharacter) {
        context.dataStore.edit { it[Keys.POMODORO_CHARACTER] = character.name }
    }

    suspend fun setMicrosoftConnected(connected: Boolean) {
        context.dataStore.edit { it[Keys.MICROSOFT_CONNECTED] = connected }
    }

    suspend fun setTextScale(option: TextScaleOption) {
        context.dataStore.edit { it[Keys.TEXT_SCALE] = option.name }
    }

    suspend fun setCourseStatus(majorId: String, courseCode: String, status: CourseCompletionStatus) {
        context.dataStore.edit { prefs ->
            val current = parseStatuses(prefs[Keys.COURSE_STATUSES]).toMutableMap()
            val key = "${majorId}:${courseCode}"
            if (status == CourseCompletionStatus.NOT_STARTED) {
                current.remove(key)
            } else {
                current[key] = status
            }
            val json = JSONObject()
            current.forEach { (k, v) -> json.put(k, v.name) }
            prefs[Keys.COURSE_STATUSES] = json.toString()
        }
    }

    suspend fun saveGpaState(courses: List<GpaCourseEntry>, priorGpa: String, priorCredits: String) {
        context.dataStore.edit { prefs ->
            val arr = JSONArray()
            courses.forEach { c ->
                arr.put(
                    JSONObject().apply {
                        put("id", c.id)
                        put("code", c.code)
                        put("credits", c.credits)
                        put("gradeLabel", c.gradeLabel)
                        put("gradePoint", c.gradePoint)
                    }
                )
            }
            prefs[Keys.GPA_COURSES] = arr.toString()
            prefs[Keys.PRIOR_GPA] = priorGpa
            prefs[Keys.PRIOR_CREDITS] = priorCredits
        }
    }

    suspend fun saveLocalPersonalMaterials(items: List<PersonalMaterial>) {
        context.dataStore.edit { prefs ->
            val arr = JSONArray()
            items.forEach { m ->
                arr.put(
                    JSONObject().apply {
                        put("id", m.id)
                        put("userId", m.userId)
                        put("title", m.title)
                        put("noteContent", m.noteContent)
                        put("fileUri", m.fileUri)
                        put("fileName", m.fileName)
                        put("materialType", m.materialType)
                    }
                )
            }
            prefs[Keys.LOCAL_PERSONAL_MATERIALS] = arr.toString()
        }
    }

    suspend fun saveLocalCourseMaterials(items: List<CourseMaterial>) {
        context.dataStore.edit { prefs ->
            val arr = JSONArray()
            items.forEach { m ->
                arr.put(
                    JSONObject().apply {
                        put("id", m.id)
                        put("authorUid", m.authorUid)
                        put("authorName", m.authorName)
                        put("courseCode", m.courseCode)
                        put("title", m.title)
                        put("description", m.description)
                        put("linkUrl", m.linkUrl)
                        put("fileName", m.fileName)
                        put("attachmentUri", m.attachmentUri)
                        put("attachmentType", m.attachmentType)
                        put("materialKind", m.materialKind)
                    }
                )
            }
            prefs[Keys.LOCAL_COURSE_MATERIALS] = arr.toString()
        }
    }

    suspend fun saveLocalCalendarEvents(events: List<CalendarEvent>) {
        context.dataStore.edit { prefs ->
            val arr = JSONArray()
            events.forEach { e ->
                arr.put(
                    JSONObject().apply {
                        put("id", e.id)
                        put("userId", e.userId)
                        put("title", e.title)
                        put("courseCode", e.courseCode)
                        put("eventType", e.eventType)
                        put("triggerEpochMillis", e.triggerEpochMillis)
                        put("alarmEnabled", e.alarmEnabled)
                        put("ringtoneId", e.ringtoneId)
                        put("volumePercent", e.volumePercent)
                        put("notes", e.notes)
                    }
                )
            }
            prefs[Keys.LOCAL_EVENTS] = arr.toString()
        }
    }

    suspend fun saveLocalForumPosts(posts: List<ForumPost>) {
        context.dataStore.edit { prefs ->
            val arr = JSONArray()
            posts.forEach { p ->
                arr.put(
                    JSONObject().apply {
                        put("id", p.id)
                        put("authorUid", p.authorUid)
                        put("authorName", p.authorName)
                        put("category", p.category)
                        put("questionTitle", p.questionTitle)
                        put("questionBody", p.questionBody)
                        put("repliesCount", p.repliesCount)
                        put("latestReply", p.latestReply)
                        put("attachmentName", p.attachmentName)
                        put("attachmentUri", p.attachmentUri)
                        put("attachmentType", p.attachmentType)
                    }
                )
            }
            prefs[Keys.LOCAL_FORUM] = arr.toString()
        }
    }

    suspend fun addFocusedStudyMinutes(deltaMinutes: Int): Int {
        if (deltaMinutes <= 0) return 0
        var updatedTotal = 0
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.FOCUSED_STUDY_MINUTES] ?: 0
            updatedTotal = current + deltaMinutes
            prefs[Keys.FOCUSED_STUDY_MINUTES] = updatedTotal
        }
        return updatedTotal
    }

    companion object {
        fun resolveDisplayName(context: Context, uri: Uri, fallback: String = "Attached File"): String {
            return try {
                var name = fallback
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0 && cursor.moveToFirst()) {
                        name = cursor.getString(idx) ?: fallback
                    }
                }
                name
            } catch (_: Exception) {
                fallback
            }
        }

        fun extractReadableStudySnippet(context: Context, uri: Uri, maxChars: Int = 8000): String {
            return try {
                val mime = context.contentResolver.getType(uri).orEmpty()
                val name = resolveDisplayName(context, uri, "").lowercase()
                if (mime.startsWith("text/") || name.endsWith(".txt") || name.endsWith(".md") || name.endsWith(".csv") || name.endsWith(".json")) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                        reader.readText().take(maxChars)
                    }.orEmpty()
                } else {
                    ""
                }
            } catch (_: Exception) {
                ""
            }
        }
    }
}
