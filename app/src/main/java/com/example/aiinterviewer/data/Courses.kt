package com.example.aiinterviewer.data

object Courses {
    val all = listOf(
        "Android", "Java/OOP", "Database", "Data Structures", "Python",
        "Operating Systems", "Networks", "Web Dev", "AI/ML", "Software Eng"
    )

    private val emojis = mapOf(
        "Android" to "📱", "Java/OOP" to "☕", "Database" to "🗄️", "Data Structures" to "🌳",
        "Python" to "🐍", "Operating Systems" to "⚙️", "Networks" to "🌐", "Web Dev" to "🕸️",
        "AI/ML" to "🤖", "Software Eng" to "🧩", "HR" to "🤝", "Resume" to "📄"
    )

    fun emoji(topic: String) = emojis[topic] ?: "📘"
}
