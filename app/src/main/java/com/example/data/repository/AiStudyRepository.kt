package com.example.data.repository

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.RequestOptions
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class AiPlanSuggestion(
    val summary: String,
    val recommendedMinutes: Int,
    val tasks: List<AiSuggestedTask>
)

data class AiSuggestedTask(
    val title: String,
    val subject: String,
    val priority: String
)

class AiStudyRepository {
    private val aiClient by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
    }

    private val chatModel by lazy {
        aiClient.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.7f
                maxOutputTokens = 1024
                thinkingConfig = thinkingConfig {
                    thinkingLevel = ThinkingLevel.LOW
                }
            },
            requestOptions = RequestOptions(timeoutInMillis = 45_000L),
            systemInstruction = content {
                text(
                    "You are Study Time AI Coach, an encouraging, practical academic productivity mentor. " +
                        "Help students plan focused study blocks, organize revision schedules, apply active recall " +
                        "and spaced repetition, and reflect on their analytics. Be concise, structured, and actionable."
                )
            }
        )
    }

    private val structuredPlanModel by lazy {
        val schema = Schema.obj(
            properties = mapOf(
                "summary" to Schema.string(description = "Brief encouraging coaching summary"),
                "recommendedMinutes" to Schema.integer(description = "Suggested total focus minutes for today"),
                "tasks" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "title" to Schema.string(description = "Actionable study task title"),
                            "subject" to Schema.string(description = "Relevant subject name"),
                            "priority" to Schema.enumeration(listOf("HIGH", "MEDIUM", "LOW"))
                        )
                    ),
                    description = "3 to 5 prioritized study tasks"
                )
            )
        )
        aiClient.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
                maxOutputTokens = 1024
                thinkingConfig = thinkingConfig {
                    thinkingLevel = ThinkingLevel.LOW
                }
            },
            requestOptions = RequestOptions(timeoutInMillis = 45_000L)
        )
    }

    fun streamStudyAdvice(prompt: String): Flow<String> = flow {
        val accumulated = StringBuilder()
        var streamTimedOutEmpty = false
        try {
            withTimeout(30_000L) {
                chatModel.generateContentStream(prompt).collect { chunk ->
                    val part = chunk.text.orEmpty()
                    if (part.isNotEmpty()) {
                        accumulated.append(part)
                        emit(accumulated.toString())
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            if (accumulated.isBlank()) {
                Log.w(TAG, "Streaming produced no chunks within 30s; falling back to unary generateContent", e)
                streamTimedOutEmpty = true
            } else {
                // Keep partial answer already streamed rather than failing
                return@flow
            }
        }

        if (streamTimedOutEmpty) {
            val response = withTimeout(30_000L) {
                chatModel.generateContent(prompt)
            }
            val text = response.text.orEmpty()
            if (text.isNotBlank()) {
                emit(text)
                return@flow
            }
        }

        if (accumulated.isBlank()) {
            throw IllegalStateException("AI Coach returned an empty response.")
        }
    }.flowOn(Dispatchers.IO)

    suspend fun generateStructuredStudyPlan(
        subjectsSummary: String,
        pendingTasksSummary: String,
        dailyGoalMinutes: Long
    ): Result<AiPlanSuggestion> = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                Build a focused study plan for today.
                Daily Goal: $dailyGoalMinutes minutes
                Subjects: $subjectsSummary
                Pending Tasks & Exams: $pendingTasksSummary
                Suggest 3 to 5 specific, high-impact tasks using active recall or Pomodoro blocks.
            """.trimIndent()

            val response = withTimeout(45_000L) {
                structuredPlanModel.generateContent(prompt)
            }
            val raw = response.text ?: throw IllegalStateException("Empty AI plan response.")
            val json = JSONObject(raw)
            val summary = json.getString("summary")
            val minutes = json.optInt("recommendedMinutes", dailyGoalMinutes.toInt())
            val arr = json.getJSONArray("tasks")
            val tasks = (0 until arr.length()).map { idx ->
                val obj = arr.getJSONObject(idx)
                AiSuggestedTask(
                    title = obj.getString("title"),
                    subject = obj.optString("subject", "General"),
                    priority = obj.optString("priority", "MEDIUM")
                )
            }
            Result.success(AiPlanSuggestion(summary, minutes, tasks))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI plan generation timed out", e)
            Result.failure(Exception("AI Coach took too long to respond. Please check your connection and try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate structured study plan", e)
            Result.failure(e)
        }
    }

    private companion object {
        const val TAG = "AiStudyRepository"
    }
}
