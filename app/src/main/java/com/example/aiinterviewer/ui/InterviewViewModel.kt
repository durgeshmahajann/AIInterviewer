package com.example.aiinterviewer.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiinterviewer.ai.ApiKeyStore
import com.example.aiinterviewer.ai.GeminiService
import com.example.aiinterviewer.data.*
import com.example.aiinterviewer.resume.ResumeDoc
import com.example.aiinterviewer.resume.ResumeExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class InterviewViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDb.get(app).dao()
    private val userId = MutableStateFlow(-1L)

    // ---- Gemini API key (entered in the app, stored on the device) ----
    var apiKey by mutableStateOf(ApiKeyStore.get(app)); private set
    val hasKey: Boolean get() = apiKey.isNotBlank()
    fun saveKey(key: String) {
        ApiKeyStore.set(getApplication<Application>(), key.trim())
        apiKey = key.trim()
        error = null
    }

    /** History of the currently logged-in user only. */
    val history = userId
        .flatMapLatest { dao.forUser(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var questions by mutableStateOf<List<Question>>(emptyList()); private set
    var results by mutableStateOf<List<AnswerEval>>(emptyList()); private set
    var index by mutableIntStateOf(0); private set
    var evaluating by mutableStateOf(false); private set
    var generating by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    private var category = Category.TECHNICAL

    // ---- resume (kept in memory only; never written to the database) ----
    var resume by mutableStateOf<ResumeDoc?>(null); private set
    var resumeError by mutableStateOf<String?>(null); private set
    var analyzing by mutableStateOf(false); private set

    fun setUser(id: Long) {
        userId.value = id
        questions = emptyList(); results = emptyList(); index = 0
        evaluating = false; generating = false; error = null
        resume = null; resumeError = null
    }

    private fun friendly(e: Exception): String =
        if (e is IOException) (e.message ?: "Network error") else "Unexpected response from Gemini. Please try again."

    private fun begin(cat: Category, qs: List<Question>) {
        category = cat
        questions = qs
        results = emptyList()
        index = 0
        evaluating = false
        error = null
    }

    // ------------------------------------------------------------ start an interview
    fun start(cat: Category, courses: Set<String>, count: Int, onReady: () -> Unit) {
        if (generating) return
        if (!hasKey) { error = "Add your Gemini API key first."; return }
        viewModelScope.launch {
            generating = true
            error = null
            try {
                begin(cat, GeminiService.generateQuestions(apiKey, cat, courses, count))
                onReady()
            } catch (e: Exception) {
                error = "Couldn't create questions: ${friendly(e)}"
            }
            generating = false
        }
    }

    fun loadResume(uri: Uri) {
        viewModelScope.launch {
            analyzing = true
            resumeError = null
            try {
                val (name, text) = withContext(Dispatchers.IO) { ResumeExtractor.extract(getApplication<Application>(), uri) }
                if (text.trim().length < 40) {
                    resume = null
                    resumeError = "Couldn't find readable text in this file. Scanned or image-only PDFs aren't supported - try a text-based PDF, DOCX or TXT."
                } else {
                    resume = ResumeDoc(name, text)
                }
            } catch (e: Exception) {
                resume = null
                resumeError = "Couldn't read this file. Please try another PDF, DOCX or TXT."
            }
            analyzing = false
        }
    }

    fun startResume(written: Int, mcqs: Int, onReady: () -> Unit) {
        val doc = resume ?: return
        if (generating) return
        if (!hasKey) { error = "Add your Gemini API key on the home screen first."; return }
        viewModelScope.launch {
            generating = true
            error = null
            try {
                begin(Category.RESUME, GeminiService.generateResumeQuestions(apiKey, doc.text, written, mcqs))
                onReady()
            } catch (e: Exception) {
                error = "Couldn't create questions: ${friendly(e)}"
            }
            generating = false
        }
    }

    // ------------------------------------------------------------ answering
    private fun mcqEval(q: Question, chosen: String): Eval {
        val correct = q.options.getOrNull(q.correctIndex)
        return if (chosen == correct) Eval(10, "Correct!")
        else Eval(0, "Incorrect. The correct answer is: $correct")
    }

    /** For written questions [answer] is the text; for MCQs it is the chosen option's text. */
    fun submit(answer: String, onFinished: () -> Unit) {
        if (evaluating) return
        val q = questions.getOrNull(index) ?: return
        viewModelScope.launch {
            evaluating = true
            error = null
            val e = try {
                when {
                    answer.isBlank() -> Eval(
                        0,
                        if (q.isMcq) "Skipped. The correct answer is: ${q.options.getOrNull(q.correctIndex)}" else "Skipped - no answer given."
                    )
                    q.isMcq -> mcqEval(q, answer)
                    else -> GeminiService.evaluate(apiKey, q, answer)
                }
            } catch (ex: Exception) {
                error = "Couldn't evaluate your answer: ${friendly(ex)}. Tap Submit to try again."
                evaluating = false
                return@launch
            }
            results = results + AnswerEval(q, answer, e.score, e.feedback)
            evaluating = false
            if (index + 1 < questions.size) {
                index++
            } else {
                saveSession()
                onFinished()
            }
        }
    }

    fun topicAverages(): Map<String, Double> =
        results.groupBy { it.question.topic }.mapValues { (_, v) -> v.map { it.score }.average() }

    fun overallPercent(): Int =
        if (results.isEmpty()) 0 else results.sumOf { it.score } * 100 / (results.size * 10)

    private suspend fun saveSession() {
        val uid = userId.value
        if (uid <= 0) return
        val ts = topicAverages().entries.joinToString(";") { String.format(Locale.US, "%s:%.1f", it.key, it.value) }
        dao.insert(
            SessionEntity(
                userId = uid,
                timestamp = System.currentTimeMillis(),
                category = category.label,
                questionCount = results.size,
                overall = overallPercent(),
                topicScores = ts
            )
        )
    }

    fun clearHistory() = viewModelScope.launch { dao.clear(userId.value) }
}
