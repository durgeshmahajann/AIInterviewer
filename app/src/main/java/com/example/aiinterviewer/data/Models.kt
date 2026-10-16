package com.example.aiinterviewer.data

enum class Category(val label: String, val emoji: String) {
    TECHNICAL("Technical", "💻"), HR("HR", "🤝"), MIXED("Mixed", "🎲"), RESUME("Resume", "📄")
}

data class Question(
    val id: Int,
    val category: Category,
    val topic: String,
    val text: String,
    val keywords: List<String>,                 // lowercase; used by the offline evaluator
    val options: List<String> = emptyList(),    // non-empty => multiple-choice question
    val correctIndex: Int = -1
) {
    val isMcq: Boolean get() = options.isNotEmpty()
}

data class Eval(val score: Int, val feedback: String)   // score 0..10

data class AnswerEval(val question: Question, val answer: String, val score: Int, val feedback: String)
