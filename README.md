# AI Interviewer – Smart Interview & Evaluation System (Android)

Kotlin + Jetpack Compose (Material 3) + Room. Single module `app`.

## How it works
- Questions are created by the Gemini API (technical / HR / mixed, 5-20 questions, 10 courses)
- Resume upload (PDF / DOCX / TXT): Gemini writes questions and MCQs from the resume
- Every written answer is graded 0-10 by Gemini with feedback and an ideal answer; MCQs are auto-checked
- Local login (PBKDF2), per-user history, score ring, topic strengths / weak areas, trend chart

## Run
1. Android Studio -> File > Open -> this folder -> Gradle sync -> Run.
2. In the app tap **🔑 Key** and paste your Gemini API key (free: https://aistudio.google.com/apikey).
   Optional default: add `GEMINI_API_KEY=your_key` to `local.properties`.
3. Model is set in `ai/GeminiClient.kt` (`MODEL`). Change it if Google retires/renames it.
