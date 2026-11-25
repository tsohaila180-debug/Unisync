package com.example.data

import android.graphics.Bitmap
import android.util.Log
import com.example.model.EmailAiAnalysis
import com.example.model.FlashcardItem
import com.example.model.InboxEmail
import com.example.model.MindMapBranch
import com.example.model.MindMapResult
import com.example.model.QuestionCluster
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.File

class AiService {
    companion object {
        private const val TAG = "UniSyncAiService"
        private const val DEFAULT_MODEL = "gemini-3.8-flash"
        private const val IMAGE_MODEL = "gemini-nano-banana-2.1"
    }

    private val emailAnalysisSchema = Schema.obj(
        properties = mapOf(
            "summary" to Schema.string(description = "Concise 1-2 sentence academic summary of the email."),
            "extractedDeadline" to Schema.string(description = "Extracted date/time deadline string or 'None' if none mentioned."),
            "actionItem" to Schema.string(description = "Primary action required by the recipient."),
            "hoursFromNow" to Schema.integer(description = "Approximate hours until deadline from now, or 48 if unclear.")
        )
    )

    private val clusterSchema = Schema.obj(
        properties = mapOf(
            "clusters" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "topic" to Schema.string(description = "Short shared question topic title"),
                        "courseCode" to Schema.string(description = "Course code e.g. M251"),
                        "emailIds" to Schema.array(
                            items = Schema.string(),
                            description = "List of matching email IDs in this group"
                        ),
                        "suggestedUnifiedReply" to Schema.string(description = "Clear unified academic reply addressing all grouped students")
                    )
                ),
                description = "Groups of repeated student questions asking the same core thing"
            )
        )
    )

    private val mindMapSchema = Schema.obj(
        properties = mapOf(
            "centerTopic" to Schema.string(description = "Central topic extracted from the study materials"),
            "branches" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "title" to Schema.string(description = "Branch heading"),
                        "subtopics" to Schema.array(items = Schema.string(), description = "Key sub-points from the materials")
                    )
                )
            )
        )
    )

    private val flashcardsSchema = Schema.obj(
        properties = mapOf(
            "cards" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "front" to Schema.string(description = "Short question or term from the uploaded/in-app materials"),
                        "back" to Schema.string(description = "Accurate concise answer from the materials")
                    )
                )
            )
        )
    )

    private val tutorModel by lazy {
        Firebase.ai.generativeModel(
            modelName = DEFAULT_MODEL,
            systemInstruction = content {
                text(
                    "You are the UniSync Study Assistant inside the Study Hub for Arab Open University (AOU) students. " +
                        "STRICT GROUNDING RULE: Answer ONLY using the books, lecture materials, written notes, and transcribed voice notes explicitly provided in the STUDY_MATERIALS_CORPUS below. " +
                        "NEVER invent facts or pull outside knowledge from the internet or general memory. " +
                        "If the corpus does not contain the answer, respond clearly: " +
                        "\"Not found in your uploaded/in-app study materials. Please upload or add the relevant book section or note to Study Hub.\" " +
                        "Respond in the same language the student uses."
                )
            }
        )
    }

    private val emailAnalysisModel by lazy {
        Firebase.ai.generativeModel(
            modelName = DEFAULT_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = emailAnalysisSchema
            }
        )
    }

    private val clusterModel by lazy {
        Firebase.ai.generativeModel(
            modelName = DEFAULT_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = clusterSchema
            }
        )
    }

    private val mindMapModel by lazy {
        Firebase.ai.generativeModel(
            modelName = DEFAULT_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = mindMapSchema
            }
        )
    }

    private val flashcardsModel by lazy {
        Firebase.ai.generativeModel(
            modelName = DEFAULT_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = flashcardsSchema
            }
        )
    }

    private val imageModel by lazy {
        Firebase.ai.generativeModel(
            modelName = IMAGE_MODEL,
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            }
        )
    }

    private val replyAssistantModel by lazy {
        Firebase.ai.generativeModel(
            modelName = DEFAULT_MODEL,
            systemInstruction = content {
                text(
                    "You are an academic correspondence assistant for an Arab Open University (AOU) faculty instructor. " +
                        "Draft concise, courteous, authoritative replies to student or deanery emails."
                )
            }
        )
    }

    suspend fun analyzeEmail(email: InboxEmail): Result<EmailAiAnalysis> {
        return try {
            val prompt = """
                Analyze this academic email and extract summary, deadline, and primary action item.
                Course: ${email.courseCode}
                From: ${email.senderName}
                Subject: ${email.subject}
                Body: ${email.body}
            """.trimIndent()

            val response = withTimeout(60_000L) {
                emailAnalysisModel.generateContent(prompt)
            }
            val raw = response.text ?: throw IllegalStateException("AI returned an empty response.")
            val json = JSONObject(raw)
            val summary = json.optString("summary", email.subject)
            val deadline = json.optString("extractedDeadline", "Upcoming")
            val action = json.optString("actionItem", "Review message")
            val hours = json.optLong("hoursFromNow", 48L).coerceIn(1L, 720L)
            Result.success(
                EmailAiAnalysis(
                    summary = summary,
                    extractedDeadline = deadline,
                    actionItem = action,
                    suggestedTimestampMillis = System.currentTimeMillis() + hours * 3600_000L
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Email analysis timed out", e)
            Result.failure(Exception("AI analysis timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Email analysis failed", e)
            Result.failure(Exception(e.localizedMessage ?: "AI analysis failed."))
        }
    }

    suspend fun clusterRepeatedStudentQuestions(emails: List<InboxEmail>): Result<List<QuestionCluster>> {
        val studentEmails = emails.filter { it.senderRole == "STUDENT" && !it.answered }
        if (studentEmails.isEmpty()) return Result.success(emptyList())

        return try {
            val formatted = studentEmails.joinToString("\n---\n") { e ->
                "ID: ${e.id}\nCourse: ${e.courseCode}\nStudent: ${e.senderName}\nSubject: ${e.subject}\nBody: ${e.body}"
            }
            val prompt = """
                Group the following unanswered student emails by shared underlying question (cluster emails asking the same thing in different phrasing).
                Only create clusters with at least 1 email, prioritizing repeated questions (2+ students asking the same topic).
                Use the exact IDs provided. Draft a clear, helpful unified reply for each group.
                
                Emails:
                $formatted
            """.trimIndent()

            val response = withTimeout(60_000L) {
                clusterModel.generateContent(prompt)
            }
            val raw = response.text ?: throw IllegalStateException("AI returned an empty clustering response.")
            val json = JSONObject(raw)
            val arr = json.optJSONArray("clusters")
            val idToStudent = studentEmails.associate { it.id to it.senderName }
            val result = buildList {
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val item = arr.getJSONObject(i)
                        val idsArr = item.optJSONArray("emailIds")
                        val ids = buildList {
                            if (idsArr != null) {
                                for (j in 0 until idsArr.length()) {
                                    val idVal = idsArr.optString(j)
                                    if (idVal in idToStudent) add(idVal)
                                }
                            }
                        }
                        if (ids.isNotEmpty()) {
                            add(
                                QuestionCluster(
                                    topic = item.optString("topic", "Repeated Student Inquiry"),
                                    courseCode = item.optString("courseCode", "AOU"),
                                    emailIds = ids,
                                    studentNames = ids.mapNotNull { idToStudent[it] },
                                    suggestedUnifiedReply = item.optString(
                                        "suggestedUnifiedReply",
                                        "Thank you for reaching out. Please check the updated guidelines on the AOU LMS."
                                    )
                                )
                            )
                        }
                    }
                }
            }
            Result.success(result)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Question clustering timed out", e)
            Result.failure(Exception("AI grouping timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Question clustering failed", e)
            Result.failure(Exception(e.localizedMessage ?: "AI grouping failed."))
        }
    }

    suspend fun draftInstructorReply(email: InboxEmail): Result<String> {
        return try {
            val prompt = """
                Draft a concise, clear reply from an AOU instructor to this email:
                From: ${email.senderName}
                Course: ${email.courseCode}
                Subject: ${email.subject}
                Message: ${email.body}
            """.trimIndent()
            val response = withTimeout(60_000L) {
                replyAssistantModel.generateContent(prompt)
            }
            val text = response.text?.trim()
            if (text.isNullOrBlank()) {
                Result.failure(Exception("AI returned an empty draft."))
            } else {
                Result.success(text)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Reply draft timed out", e)
            Result.failure(Exception("AI reply draft timed out. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Reply draft failed", e)
            Result.failure(Exception(e.localizedMessage ?: "AI reply draft failed."))
        }
    }

    fun streamStudyTutor(
        prompt: String,
        corpusText: String,
        majorName: String
    ): Flow<String> = flow {
        if (corpusText.isBlank()) {
            emit("Not found in your uploaded/in-app study materials. Please add notes, upload a textbook/file, or transcribe a voice note in Study Hub first.")
            return@flow
        }
        val fullPrompt = """
            Student Major: $majorName (Arab Open University)
            
            STUDY_MATERIALS_CORPUS (Answer strictly from these in-app materials ONLY):
            $corpusText
            
            Student Question: $prompt
        """.trimIndent()

        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            tutorModel.generateContentStream(fullPrompt).collect { chunk ->
                val chunkText = chunk.text.orEmpty()
                if (chunkText.isNotEmpty()) {
                    accumulated.append(chunkText)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("Study Assistant returned an empty response.")
        }
    }

    suspend fun transcribeVoiceNote(filePath: String): Result<String> {
        return try {
            val audioFile = File(filePath)
            if (!audioFile.exists() || audioFile.length() == 0L) {
                return Result.failure(Exception("Audio recording file not found."))
            }
            val bytes = audioFile.readBytes()
            val inputContent = content {
                inlineData(bytes, "audio/mp4")
                text("Transcribe this academic study voice recording accurately into clean written notes (keep Arabic or English as spoken).")
            }
            val response = withTimeout(60_000L) {
                tutorModel.generateContent(inputContent)
            }
            val text = response.text?.trim()
            if (text.isNullOrBlank()) {
                Result.failure(Exception("Transcription returned empty text."))
            } else {
                Result.success(text)
            }
        } catch (e: TimeoutCancellationException) {
            Result.failure(Exception("Audio transcription timed out after 60 seconds."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Audio transcription failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Audio transcription failed."))
        }
    }

    suspend fun generateBookMindMap(corpusText: String, topic: String): Result<MindMapResult> {
        if (corpusText.isBlank()) {
            return Result.failure(Exception("Please add or upload study materials/notes first so the mind map stays grounded in your books."))
        }
        return try {
            val prompt = """
                Using ONLY the following in-app study materials, create a structured study mind map${if (topic.isNotBlank()) " focused on: $topic" else ""}.
                Do not add outside facts.
                
                MATERIALS:
                $corpusText
            """.trimIndent()
            val response = withTimeout(60_000L) {
                mindMapModel.generateContent(prompt)
            }
            val raw = response.text ?: throw IllegalStateException("Mind map generation returned empty output.")
            val json = JSONObject(raw)
            val center = json.optString("centerTopic", topic.ifBlank { "Study Mind Map" })
            val branchesArr = json.optJSONArray("branches")
            val branches = buildList {
                if (branchesArr != null) {
                    for (i in 0 until branchesArr.length()) {
                        val b = branchesArr.getJSONObject(i)
                        val subsArr = b.optJSONArray("subtopics")
                        val subs = buildList {
                            if (subsArr != null) {
                                for (j in 0 until subsArr.length()) {
                                    add(subsArr.optString(j))
                                }
                            }
                        }
                        add(MindMapBranch(title = b.optString("title", "Branch ${i + 1}"), subtopics = subs))
                    }
                }
            }
            Result.success(MindMapResult(centerTopic = center, branches = branches))
        } catch (e: TimeoutCancellationException) {
            Result.failure(Exception("Mind map generation timed out."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Mind map failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Unable to generate mind map."))
        }
    }

    suspend fun generateBookFlashcards(corpusText: String, topic: String): Result<List<FlashcardItem>> {
        if (corpusText.isBlank()) {
            return Result.failure(Exception("Please add or upload study materials/notes first to generate flashcards."))
        }
        return try {
            val prompt = """
                Using ONLY the following in-app study materials, create 5 to 8 concise study flashcards${if (topic.isNotBlank()) " on: $topic" else ""}.
                Each card must come directly from the text below.
                
                MATERIALS:
                $corpusText
            """.trimIndent()
            val response = withTimeout(60_000L) {
                flashcardsModel.generateContent(prompt)
            }
            val raw = response.text ?: throw IllegalStateException("Flashcards generation returned empty output.")
            val json = JSONObject(raw)
            val arr = json.optJSONArray("cards")
            val cards = buildList {
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val item = arr.getJSONObject(i)
                        val f = item.optString("front").trim()
                        val b = item.optString("back").trim()
                        if (f.isNotEmpty() && b.isNotEmpty()) {
                            add(FlashcardItem(front = f, back = b))
                        }
                    }
                }
            }
            if (cards.isEmpty()) {
                Result.failure(Exception("No flashcards could be extracted from the current materials."))
            } else {
                Result.success(cards)
            }
        } catch (e: TimeoutCancellationException) {
            Result.failure(Exception("Flashcards generation timed out."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Flashcards failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Unable to generate flashcards."))
        }
    }

    suspend fun generateBookStudyVisual(corpusText: String, topic: String): Result<Bitmap> {
        if (corpusText.isBlank()) {
            return Result.failure(Exception("Please add or upload study materials/notes first before generating a study diagram."))
        }
        return try {
            val excerpt = corpusText.take(1500)
            val prompt = """
                Create a clean, high-clarity academic educational diagram / visual study illustration in deep navy (#142D59), academic blue (#2456A6), and royal lavender (#7056B3) on a crisp soft blue background (#EAF1FA).
                Illustrate ONLY concepts present in this student study material${if (topic.isNotBlank()) " focusing on: $topic" else ""}.
                Material excerpt:
                $excerpt
            """.trimIndent()
            val response = withTimeout(60_000L) {
                imageModel.generateContent(prompt)
            }
            val generatedBitmap = response.candidates
                .firstOrNull()
                ?.content
                ?.parts
                ?.filterIsInstance<ImagePart>()
                ?.firstOrNull()
                ?.image
                ?: throw IllegalStateException("No study visual image was returned by AI.")
            Result.success(generatedBitmap)
        } catch (e: TimeoutCancellationException) {
            Result.failure(Exception("Study visual generation timed out after 60 seconds."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Study visual generation failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Unable to generate study visual."))
        }
    }
}
