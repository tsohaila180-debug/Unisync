package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class UserRole {
    STUDENT,
    INSTRUCTOR
}

enum class EmailPriority(val weight: Int) {
    CRITICAL(0),
    HIGH(1),
    NORMAL(2),
    LOW(3)
}

enum class SenderRole {
    STUDENT,
    INSTRUCTOR,
    ADMIN
}

enum class EventType {
    LECTURE,
    QUIZ,
    ASSIGNMENT,
    MIDTERM,
    FINAL,
    OFFICE_HOUR,
    ALARM
}

enum class PomodoroCharacter {
    GIRL,
    BOY
}

enum class PersonalMaterialType {
    NOTE,
    FILE,
    LINK,
    VOICE_TRANSCRIPT
}

enum class CourseMaterialKind {
    LECTURE_SLIDES,
    ONLINE_LINK,
    RECORDING,
    ASSIGNMENT_BRIEF
}

enum class CourseCategory {
    GENERAL,
    MATH,
    CORE,
    GRADUATION
}

enum class CourseCompletionStatus(val label: String, val arLabel: String) {
    COMPLETED("Completed", "مكتملة"),
    IN_PROGRESS("In Progress", "قيد الدراسة"),
    NEXT_TERM("Next Term", "الترم القادم"),
    NOT_STARTED("Not Started Yet", "لم تبدأ بعد")
}

enum class TextScaleOption(val label: String, val scaleFactor: Float) {
    NORMAL("Medium", 1.0f),
    LARGE("Large", 1.14f),
    EXTRA_LARGE("Extra Large", 1.28f)
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val role: String = UserRole.STUDENT.name,
    val aouBranch: String = "Riyadh",
    val majorId: String = "cs",
    val preferredLanguage: String = "en",
    val darkMode: Boolean = false,
    val pomodoroCharacter: String = PomodoroCharacter.GIRL.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class InboxEmail(
    @DocumentId val id: String = "",
    val userId: String = "",
    val senderName: String = "",
    val senderRole: String = SenderRole.INSTRUCTOR.name,
    val courseCode: String = "",
    val subject: String = "",
    val body: String = "",
    val priority: String = EmailPriority.NORMAL.name,
    val answered: Boolean = false,
    val replyText: String? = null,
    val aiSummary: String? = null,
    val extractedDeadline: String? = null,
    val attachmentName: String = "",
    val attachmentUri: String = "",
    val attachmentType: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CalendarEvent(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val courseCode: String = "",
    val eventType: String = EventType.LECTURE.name,
    val triggerEpochMillis: Long = 0L,
    val alarmEnabled: Boolean = true,
    val ringtoneId: String = "crystal_chime",
    val volumePercent: Long = 80L,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PersonalMaterial(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val noteContent: String = "",
    val fileUri: String = "",
    val fileName: String = "",
    val materialType: String = PersonalMaterialType.NOTE.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CourseMaterial(
    @DocumentId val id: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val courseCode: String = "",
    val title: String = "",
    val description: String = "",
    val linkUrl: String = "",
    val fileName: String = "",
    val attachmentUri: String = "",
    val attachmentType: String = "",
    val materialKind: String = CourseMaterialKind.LECTURE_SLIDES.name,
    val scope: String = "AOU",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ForumPost(
    @DocumentId val id: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val category: String = "",
    val questionTitle: String = "",
    val questionBody: String = "",
    val repliesCount: Long = 0L,
    val latestReply: String = "",
    val attachmentName: String = "",
    val attachmentUri: String = "",
    val attachmentType: String = "",
    val scope: String = "AOU",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DirectChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val peerName: String = "",
    val peerRole: String = UserRole.INSTRUCTOR.name,
    val senderIsSelf: Boolean = true,
    val messageText: String = "",
    val imageUri: String = "",
    val attachmentName: String = "",
    val attachmentUri: String = "",
    val attachmentType: String = "",
    val courseCode: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GeneralChatMessage(
    @DocumentId val id: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val authorBranch: String = "Riyadh",
    val courseTag: String = "General",
    val messageText: String = "",
    val attachmentName: String = "",
    val attachmentUri: String = "",
    val attachmentType: String = "",
    val scope: String = "AOU",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class QuestionCluster(
    val topic: String,
    val courseCode: String,
    val emailIds: List<String>,
    val studentNames: List<String>,
    val suggestedUnifiedReply: String
)

data class EmailAiAnalysis(
    val summary: String,
    val extractedDeadline: String,
    val actionItem: String,
    val suggestedTimestampMillis: Long?
)

data class StudyTutorMessage(
    val id: String,
    val isUser: Boolean,
    val text: String
)

data class MindMapBranch(
    val title: String,
    val subtopics: List<String>
)

data class MindMapResult(
    val centerTopic: String,
    val branches: List<MindMapBranch>
)

data class FlashcardItem(
    val front: String,
    val back: String
)

data class GpaCourseEntry(
    val id: String,
    val code: String,
    val credits: Int,
    val gradeLabel: String,
    val gradePoint: Double
)

data class VoiceNoteItem(
    val id: String,
    val title: String,
    val filePath: String,
    val durationLabel: String,
    val createdAtMillis: Long
)

data class RoadmapCourse(
    val code: String,
    val title: String,
    val credits: Int,
    val category: CourseCategory,
    val stage: Int,
    val prerequisites: List<String>,
    val description: String
)

data class AouMajor(
    val id: String,
    val name: String,
    val faculty: String,
    val totalCredits: Int,
    val courses: List<RoadmapCourse>
)

inline fun <reified T : Enum<T>> safeEnumValue(raw: String?, default: T): T {
    if (raw.isNullOrBlank()) return default
    return enumValues<T>().firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: default
}
