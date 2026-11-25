package com.example.data

import android.content.Context
import com.example.R
import com.example.model.CalendarEvent
import com.example.model.CourseMaterial
import com.example.model.DirectChatMessage
import com.example.model.EmailPriority
import com.example.model.EventType
import com.example.model.ForumPost
import com.example.model.GeneralChatMessage
import com.example.model.InboxEmail
import com.example.model.PersonalMaterial
import com.example.model.PomodoroCharacter
import com.example.model.SenderRole
import com.example.model.UserProfile
import com.example.model.UserRole
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class UniSyncRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    private fun <T> unwrapSnapshotFlow(
        path: String,
        upstream: Flow<T>
    ): Flow<T> = upstream.catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && !auth.currentUser?.uid.isNullOrBlank()) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
        }
        throw error
    }

    // 1. Profile
    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            unwrapSnapshotFlow(
                path,
                db.collection("users").document(userId)
                    .snapshots()
                    .map { snapshot ->
                        if (snapshot.exists()) {
                            snapshot.toObject(
                                UserProfile::class.java,
                                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                            )
                        } else null
                    }
            )
        )
    }

    suspend fun ensureUserProfile(
        displayName: String,
        role: UserRole,
        aouBranch: String,
        majorId: String,
        preferredLanguage: String,
        darkMode: Boolean,
        pomodoroCharacter: PomodoroCharacter
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to displayName.take(80).ifBlank { "AOU Member" },
                    "role" to role.name,
                    "aouBranch" to aouBranch.take(60).ifBlank { "Kuwait" },
                    "majorId" to majorId.take(40),
                    "preferredLanguage" to preferredLanguage.take(5).ifBlank { "en" },
                    "darkMode" to darkMode,
                    "pomodoroCharacter" to pomodoroCharacter.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                docRef.update(
                    mapOf(
                        "role" to role.name,
                        "preferredLanguage" to preferredLanguage.take(5).ifBlank { "en" },
                        "darkMode" to darkMode,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProfileFields(fields: Map<String, Any>): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            val allowed = fields.filterKeys {
                it in setOf(
                    "displayName", "role", "aouBranch", "majorId",
                    "preferredLanguage", "darkMode", "pomodoroCharacter"
                )
            } + ("updatedAt" to FieldValue.serverTimestamp())
            db.collection("users").document(uid).update(allowed).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // 2. Inbox
    fun observeInbox(userId: String): Flow<List<InboxEmail>> = flow {
        val path = "users/$userId/inbox"
        emitAll(
            unwrapSnapshotFlow(
                path,
                db.collection("users").document(userId).collection("inbox")
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(
                            InboxEmail::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                    }
            )
        )
    }

    suspend fun addInboxEmail(email: InboxEmail): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/inbox"
        return try {
            val docRef = db.collection("users").document(uid).collection("inbox").document()
            val payload = mapOf(
                "userId" to uid,
                "senderName" to email.senderName.take(80).ifBlank { "Academic Contact" },
                "senderRole" to email.senderRole,
                "courseCode" to email.courseCode.take(20).ifBlank { "AOU" },
                "subject" to email.subject.take(160).ifBlank { "Academic Message" },
                "body" to email.body.take(4000).ifBlank { "Message body" },
                "priority" to email.priority,
                "answered" to email.answered,
                "replyText" to email.replyText?.take(4000),
                "aiSummary" to email.aiSummary?.take(1000),
                "extractedDeadline" to email.extractedDeadline?.take(80),
                "attachmentName" to email.attachmentName.take(160),
                "attachmentUri" to email.attachmentUri.take(500),
                "attachmentType" to email.attachmentType.take(40),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateInboxAiSummary(
        emailId: String,
        aiSummary: String,
        extractedDeadline: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/inbox/$emailId"
        return try {
            db.collection("users").document(uid).collection("inbox").document(emailId)
                .update(
                    mapOf(
                        "aiSummary" to aiSummary.take(1000),
                        "extractedDeadline" to extractedDeadline.take(80),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun replyToInboxEmails(emailIds: List<String>, replyText: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/inbox"
        return try {
            val batch = db.batch()
            emailIds.filter { it.isNotBlank() }.forEach { id ->
                val ref = db.collection("users").document(uid).collection("inbox").document(id)
                batch.update(
                    ref,
                    mapOf(
                        "answered" to true,
                        "replyText" to replyText.take(4000),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // 3. Calendar Events & Smart Alarms
    fun observeCalendarEvents(userId: String): Flow<List<CalendarEvent>> = flow {
        val path = "users/$userId/calendar_events"
        emitAll(
            unwrapSnapshotFlow(
                path,
                db.collection("users").document(userId).collection("calendar_events")
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(
                            CalendarEvent::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ).sortedBy { it.triggerEpochMillis }
                    }
            )
        )
    }

    suspend fun addCalendarEvent(event: CalendarEvent): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/calendar_events"
        return try {
            val docRef = db.collection("users").document(uid).collection("calendar_events").document()
            val payload = mapOf(
                "userId" to uid,
                "title" to event.title.take(120).ifBlank { "Academic Event" },
                "courseCode" to event.courseCode.take(20),
                "eventType" to event.eventType,
                "triggerEpochMillis" to event.triggerEpochMillis.coerceAtLeast(1L),
                "alarmEnabled" to event.alarmEnabled,
                "ringtoneId" to event.ringtoneId.take(40).ifBlank { "crystal_chime" },
                "volumePercent" to event.volumePercent.coerceIn(0L, 100L),
                "notes" to event.notes.take(500),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCalendarEvent(eventId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/calendar_events/$eventId"
        return try {
            db.collection("users").document(uid).collection("calendar_events").document(eventId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // 4. Student Manual-Only Personal Materials & Notes (NEVER auto-seeded)
    fun observePersonalMaterials(userId: String): Flow<List<PersonalMaterial>> = flow {
        val path = "users/$userId/personal_materials"
        emitAll(
            unwrapSnapshotFlow(
                path,
                db.collection("users").document(userId).collection("personal_materials")
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(
                            PersonalMaterial::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                    }
            )
        )
    }

    suspend fun addPersonalMaterial(material: PersonalMaterial): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/personal_materials"
        return try {
            val docRef = db.collection("users").document(uid).collection("personal_materials").document()
            val payload = mapOf(
                "userId" to uid,
                "title" to material.title.take(120).ifBlank { "Untitled Note" },
                "noteContent" to material.noteContent.take(5000),
                "fileUri" to material.fileUri.take(500),
                "fileName" to material.fileName.take(160),
                "materialType" to material.materialType,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deletePersonalMaterial(materialId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/personal_materials/$materialId"
        return try {
            db.collection("users").document(uid).collection("personal_materials").document(materialId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // 5. Direct Student-Instructor Chat
    fun observeDirectChat(userId: String): Flow<List<DirectChatMessage>> = flow {
        val path = "users/$userId/direct_chats"
        emitAll(
            unwrapSnapshotFlow(
                path,
                db.collection("users").document(userId).collection("direct_chats")
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(
                            DirectChatMessage::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
                    }
            )
        )
    }

    suspend fun sendDirectChatMessage(msg: DirectChatMessage): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/direct_chats"
        return try {
            val docRef = db.collection("users").document(uid).collection("direct_chats").document()
            val payload = mapOf(
                "userId" to uid,
                "peerName" to msg.peerName.take(80).ifBlank { "Academic Peer" },
                "peerRole" to msg.peerRole,
                "senderIsSelf" to msg.senderIsSelf,
                "messageText" to msg.messageText.take(2000),
                "imageUri" to msg.imageUri.take(500),
                "attachmentName" to msg.attachmentName.take(160),
                "attachmentUri" to msg.attachmentUri.take(500),
                "attachmentType" to msg.attachmentType.take(40),
                "courseCode" to msg.courseCode.take(20),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // 6. Instructor-Published Course Materials (filtered by scope == "AOU" to match rules)
    fun observeCourseMaterials(): Flow<List<CourseMaterial>> = flow {
        val path = "course_materials"
        emitAll(
            unwrapSnapshotFlow(
                path,
                db.collection(path)
                    .whereEqualTo("scope", "AOU")
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(
                            CourseMaterial::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                    }
            )
        )
    }

    suspend fun publishCourseMaterial(material: CourseMaterial): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "course_materials"
        return try {
            val docRef = db.collection(path).document()
            val payload = mapOf(
                "authorUid" to uid,
                "authorName" to material.authorName.take(80).ifBlank { "AOU Faculty" },
                "courseCode" to material.courseCode.take(20).ifBlank { "TM111" },
                "title" to material.title.take(140).ifBlank { "Lecture Resource" },
                "description" to material.description.take(1000),
                "linkUrl" to material.linkUrl.take(500),
                "fileName" to material.fileName.take(160),
                "attachmentUri" to material.attachmentUri.take(500),
                "attachmentType" to material.attachmentType.take(40),
                "materialKind" to material.materialKind,
                "scope" to "AOU",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCourseMaterial(materialId: String): Result<Unit> {
        val path = "course_materials/$materialId"
        return try {
            requireUserId()
            db.collection("course_materials").document(materialId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // 7. Freshmen Q&A Forum (filtered by scope == "AOU" to match rules)
    fun observeForumPosts(): Flow<List<ForumPost>> = flow {
        val path = "forum_posts"
        emitAll(
            unwrapSnapshotFlow(
                path,
                db.collection(path)
                    .whereEqualTo("scope", "AOU")
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(
                            ForumPost::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                    }
            )
        )
    }

    suspend fun createForumPost(post: ForumPost): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "forum_posts"
        return try {
            val docRef = db.collection(path).document()
            val payload = mapOf(
                "authorUid" to uid,
                "authorName" to post.authorName.take(80).ifBlank { "AOU Student" },
                "category" to post.category.take(40).ifBlank { "General" },
                "questionTitle" to post.questionTitle.take(160).ifBlank { "Question" },
                "questionBody" to post.questionBody.take(2000).ifBlank { "Details" },
                "repliesCount" to 0L,
                "latestReply" to "",
                "attachmentName" to post.attachmentName.take(160),
                "attachmentUri" to post.attachmentUri.take(500),
                "attachmentType" to post.attachmentType.take(40),
                "scope" to "AOU",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun replyToForumPost(postId: String, currentCount: Long, reply: String): Result<Unit> {
        val path = "forum_posts/$postId"
        return try {
            requireUserId()
            db.collection("forum_posts").document(postId)
                .update(
                    mapOf(
                        "repliesCount" to (currentCount + 1L).coerceAtMost(1000L),
                        "latestReply" to reply.take(1000),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // 8. General Student Community Chat (filtered by scope == "AOU" to match rules)
    fun observeGeneralStudentChat(): Flow<List<GeneralChatMessage>> = flow {
        val path = "general_student_chat"
        emitAll(
            unwrapSnapshotFlow(
                path,
                db.collection(path)
                    .whereEqualTo("scope", "AOU")
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(
                            GeneralChatMessage::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
                    }
            )
        )
    }

    suspend fun sendGeneralStudentMessage(msg: GeneralChatMessage): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "general_student_chat"
        return try {
            val docRef = db.collection(path).document()
            val payload = mapOf(
                "authorUid" to uid,
                "authorName" to msg.authorName.take(80).ifBlank { "AOU Student" },
                "authorBranch" to msg.authorBranch.take(60).ifBlank { "Riyadh" },
                "courseTag" to msg.courseTag.take(30).ifBlank { "General" },
                "messageText" to msg.messageText.take(2000).ifBlank { "Hello AOU peers!" },
                "attachmentName" to msg.attachmentName.take(160),
                "attachmentUri" to msg.attachmentUri.take(500),
                "attachmentType" to msg.attachmentType.take(40),
                "scope" to "AOU",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteGeneralStudentMessage(messageId: String): Result<Unit> {
        val path = "general_student_chat/$messageId"
        return try {
            requireUserId()
            db.collection("general_student_chat").document(messageId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // Seed initial academic inbox & calendar deadlines on first sign-in if empty
    // NOTE: NEVER seeds personal_materials so Student Materials remains strictly manual-only empty!
    suspend fun seedInitialWorkspaceIfEmpty(role: UserRole) {
        val uid = auth.currentUser?.uid ?: return
        try {
            val inboxSnap = db.collection("users").document(uid).collection("inbox").limit(1).get().await()
            if (inboxSnap.isEmpty) {
                val sampleEmails = if (role == UserRole.INSTRUCTOR) {
                    SampleAcademicData.instructorEmails(uid)
                } else {
                    SampleAcademicData.studentEmails(uid)
                }
                sampleEmails.forEach { addInboxEmail(it) }
            }
            val calSnap = db.collection("users").document(uid).collection("calendar_events").limit(1).get().await()
            if (calSnap.isEmpty) {
                val events = if (role == UserRole.INSTRUCTOR) {
                    SampleAcademicData.instructorEvents(uid)
                } else {
                    SampleAcademicData.studentEvents(uid)
                }
                events.forEach { addCalendarEvent(it) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid/seed")
        }
    }
}

object SampleAcademicData {
    fun studentEmails(uid: String): List<InboxEmail> = listOf(
        InboxEmail(
            id = "s1",
            userId = uid,
            senderName = "Dr. Tariq Al-Mansoor",
            senderRole = SenderRole.INSTRUCTOR.name,
            courseCode = "M251",
            subject = "CRITICAL: M251 TMA01 Submission Deadline & Viva Schedule",
            body = "Dear students, please upload your M251 OOP TMA01 solution on the AOU LMS before Friday 23:59. Viva slots start Sunday 10:00 AM in Lab 2.",
            priority = EmailPriority.CRITICAL.name,
            answered = false
        ),
        InboxEmail(
            id = "s2",
            userId = uid,
            senderName = "AOU Examinations Office",
            senderRole = SenderRole.ADMIN.name,
            courseCode = "TM112",
            subject = "Midterm Assessment Seating Plan Released",
            body = "Your TM112 Midterm Assessment is scheduled for next Tuesday at 14:00 in Main Hall A. Bring your AOU Student ID.",
            priority = EmailPriority.HIGH.name,
            answered = false
        ),
        InboxEmail(
            id = "s3",
            userId = uid,
            senderName = "Prof. Nadia Haddad",
            senderRole = SenderRole.INSTRUCTOR.name,
            courseCode = "M269",
            subject = "Week 6 Tutorial Recording & Graph Algorithms Notes",
            body = "We covered BFS/DFS complexity today. Please review Chapter 4 exercises before next week's quiz.",
            priority = EmailPriority.NORMAL.name,
            answered = false
        ),
        InboxEmail(
            id = "s4",
            userId = uid,
            senderName = "AOU Student Affairs",
            senderRole = SenderRole.ADMIN.name,
            courseCode = "AOU",
            subject = "Spring Semester Academic Advising Reminder",
            body = "Advising hours are open Monday–Wednesday 11:00–13:00 for all FCS and FBS majors.",
            priority = EmailPriority.LOW.name,
            answered = false
        )
    )

    fun instructorEmails(uid: String): List<InboxEmail> = listOf(
        InboxEmail(
            id = "i1",
            userId = uid,
            senderName = "AOU Academic Deanery",
            senderRole = SenderRole.ADMIN.name,
            courseCode = "TM354",
            subject = "CRITICAL: Final Exam Question Paper Moderation Due",
            body = "Please submit the moderated TM354 final exam booklet and marking rubric to the Examination Committee by Thursday 16:00.",
            priority = EmailPriority.CRITICAL.name,
            answered = false
        ),
        InboxEmail(
            id = "i2",
            userId = uid,
            senderName = "Yousef Al-Sabah (ID 231044)",
            senderRole = SenderRole.STUDENT.name,
            courseCode = "M251",
            subject = "Question about TMA01 UML diagram format",
            body = "Doctor, should we export the M251 TMA01 class diagram as PNG inside the Word file or submit a separate PDF?",
            priority = EmailPriority.HIGH.name,
            answered = false
        ),
        InboxEmail(
            id = "i3",
            userId = uid,
            senderName = "Reem Al-Qahtani (ID 231198)",
            senderRole = SenderRole.STUDENT.name,
            courseCode = "M251",
            subject = "M251 TMA01 class diagram submission format?",
            body = "Hello Professor, do we embed the UML class diagram inside the TMA01 DOCX or attach it separately on LMS?",
            priority = EmailPriority.NORMAL.name,
            answered = false
        ),
        InboxEmail(
            id = "i4",
            userId = uid,
            senderName = "Karim Hassan (ID 232011)",
            senderRole = SenderRole.STUDENT.name,
            courseCode = "M251",
            subject = "Clarification on UML file type for TMA01",
            body = "Dr., can we paste the UML diagram screenshot into the M251 TMA Word document or upload a separate file?",
            priority = EmailPriority.NORMAL.name,
            answered = false
        ),
        InboxEmail(
            id = "i5",
            userId = uid,
            senderName = "Lina Darwish (ID 224890)",
            senderRole = SenderRole.STUDENT.name,
            courseCode = "TM351",
            subject = "TM351 Lab 4 SQL dataset link not opening",
            body = "Good evening Doctor, the CSV dataset link for TM351 Lab 4 returns 404 on LMS. Could you re-upload it?",
            priority = EmailPriority.HIGH.name,
            answered = false
        ),
        InboxEmail(
            id = "i6",
            userId = uid,
            senderName = "Faisal Al-Otaibi (ID 224912)",
            senderRole = SenderRole.STUDENT.name,
            courseCode = "TM351",
            subject = "Cannot download TM351 Lab 4 dataset",
            body = "Professor, several of us are getting an error downloading the TM351 Lab 4 CSV file on LMS.",
            priority = EmailPriority.LOW.name,
            answered = false
        )
    )

    fun studentEvents(uid: String): List<CalendarEvent> {
        val now = System.currentTimeMillis()
        return listOf(
            CalendarEvent(
                id = "ce1",
                userId = uid,
                title = "M251 OOP Lecture & Live Coding",
                courseCode = "M251",
                eventType = EventType.LECTURE.name,
                triggerEpochMillis = now + 2 * 3600_000L + 18 * 60_000L,
                alarmEnabled = true,
                ringtoneId = "crystal_chime",
                volumePercent = 85L,
                notes = "AOU Building B, Room 204"
            ),
            CalendarEvent(
                id = "ce2",
                userId = uid,
                title = "TM112 Quiz 2 (Online LMS)",
                courseCode = "TM112",
                eventType = EventType.QUIZ.name,
                triggerEpochMillis = now + 26 * 3600_000L,
                alarmEnabled = true,
                ringtoneId = "academic_bell",
                volumePercent = 90L,
                notes = "Covers Blocks 1 & 2"
            ),
            CalendarEvent(
                id = "ce3",
                userId = uid,
                title = "M251 TMA01 Final Deadline",
                courseCode = "M251",
                eventType = EventType.ASSIGNMENT.name,
                triggerEpochMillis = now + 72 * 3600_000L,
                alarmEnabled = true,
                ringtoneId = "urgent_pulse",
                volumePercent = 95L,
                notes = "Upload PDF/DOCX on AOU LMS"
            ),
            CalendarEvent(
                id = "ce4",
                userId = uid,
                title = "M269 Midterm Exam",
                courseCode = "M269",
                eventType = EventType.MIDTERM.name,
                triggerEpochMillis = now + 120 * 3600_000L,
                alarmEnabled = true,
                ringtoneId = "gentle_harp",
                volumePercent = 80L,
                notes = "Main Exam Hall"
            )
        )
    }

    fun instructorEvents(uid: String): List<CalendarEvent> {
        val now = System.currentTimeMillis()
        return listOf(
            CalendarEvent(
                id = "ie1",
                userId = uid,
                title = "M251 Section 01 Lecture",
                courseCode = "M251",
                eventType = EventType.LECTURE.name,
                triggerEpochMillis = now + 1 * 3600_000L + 42 * 60_000L,
                alarmEnabled = true,
                ringtoneId = "academic_bell",
                volumePercent = 85L,
                notes = "Smart Board Room 204"
            ),
            CalendarEvent(
                id = "ie2",
                userId = uid,
                title = "Student Office Hours (FCS)",
                courseCode = "M251/TM351",
                eventType = EventType.OFFICE_HOUR.name,
                triggerEpochMillis = now + 5 * 3600_000L,
                alarmEnabled = true,
                ringtoneId = "crystal_chime",
                volumePercent = 75L,
                notes = "Faculty Wing Office 312 & Teams"
            ),
            CalendarEvent(
                id = "ie3",
                userId = uid,
                title = "TM354 Midterm Invigilation",
                courseCode = "TM354",
                eventType = EventType.MIDTERM.name,
                triggerEpochMillis = now + 48 * 3600_000L,
                alarmEnabled = true,
                ringtoneId = "urgent_pulse",
                volumePercent = 90L,
                notes = "Main Hall A"
            )
        )
    }

    fun sampleForumPosts(): List<ForumPost> = listOf(
        ForumPost(
            id = "fp1",
            authorUid = "seed",
            authorName = "Ahmad K. (CS Year 2)",
            category = "Study Tips",
            questionTitle = "How to prepare for M251 & M269 in the same semester?",
            questionBody = "Any advice on balancing Java OOP TMA tasks with M269 data structures?",
            repliesCount = 2,
            latestReply = "Start M251 UML diagrams early and practice M269 recursion trees weekly."
        ),
        ForumPost(
            id = "fp2",
            authorUid = "seed",
            authorName = "Mariam S. (ITC Freshmen)",
            category = "Registration",
            questionTitle = "Should I take EL111 and GR101 together in my first term?",
            questionBody = "Looking for recommendations on first-semester credit load at AOU.",
            repliesCount = 1,
            latestReply = "Yes! EL111 + GR101 + AR111 + TU170 is the classic balanced freshmen load."
        )
    )

    fun sampleDirectChats(uid: String, role: UserRole): List<DirectChatMessage> =
        if (role == UserRole.STUDENT) {
            listOf(
                DirectChatMessage(
                    id = "dc1",
                    userId = uid,
                    peerName = "Dr. Tariq Al-Mansoor",
                    peerRole = UserRole.INSTRUCTOR.name,
                    senderIsSelf = false,
                    messageText = "Welcome to M251! Feel free to send course questions or diagram screenshots here without sharing phone numbers.",
                    imageUri = "",
                    courseCode = "M251"
                )
            )
        } else {
            listOf(
                DirectChatMessage(
                    id = "dc2",
                    userId = uid,
                    peerName = "Yousef Al-Sabah (Student)",
                    peerRole = UserRole.STUDENT.name,
                    senderIsSelf = false,
                    messageText = "Hello Doctor, I uploaded my updated class diagram draft for feedback.",
                    imageUri = "",
                    courseCode = "M251"
                )
            )
        }

    fun sampleGeneralStudentChats(): List<GeneralChatMessage> = listOf(
        GeneralChatMessage(
            id = "gc1",
            authorUid = "seed_1",
            authorName = "Ahmad Al-Mutairi",
            authorBranch = "Kuwait",
            courseTag = "M251",
            messageText = "Hey everyone! Anyone want to review M251 Java UML class diagrams together before Friday?",
            scope = "AOU"
        ),
        GeneralChatMessage(
            id = "gc2",
            authorUid = "seed_2",
            authorName = "Mariam Hassan",
            authorBranch = "Egypt",
            courseTag = "TM112",
            messageText = "Welcome! Remember Block 2 summary slides are helpful for Quiz 2 revision.",
            scope = "AOU"
        ),
        GeneralChatMessage(
            id = "gc3",
            authorUid = "seed_3",
            authorName = "Omar Al-Qahtani",
            authorBranch = "Saudi Arabia",
            courseTag = "General",
            messageText = "Tip for freshmen: take EL111 + GR101 + TU170 together for a smooth first semester.",
            scope = "AOU"
        )
    )
}
