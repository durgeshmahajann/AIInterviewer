package com.example.aiinterviewer.ai

import android.content.Context
import com.example.aiinterviewer.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Stores the user's Gemini API key on the device (falls back to the optional build-time default). */
object ApiKeyStore {
    private const val PREFS = "settings"
    private const val KEY = "gemini_key"

    fun get(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)?.takeIf { it.isNotBlank() }
            ?: BuildConfig.GEMINI_API_KEY

    fun set(ctx: Context, key: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, key).apply()
    }
}

/** Minimal blocking client for the Gemini generateContent REST endpoint. Call from Dispatchers.IO. */
object GeminiClient {
    /** Change this if Google renames or retires the model (see ai.google.dev/gemini-api/docs/models). */
    const val MODEL = "gemini-3.1-flash-lite"

    @Throws(IOException::class)
    fun generate(apiKey: String, prompt: String, temperature: Double = 0.8): String {
        val body = JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
            .put(
                "generationConfig", JSONObject()
                    .put("temperature", temperature)
                    .put("responseMimeType", "application/json")
                    .put("maxOutputTokens", 8192)
            )

        val conn = (URL("https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent")
            .openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 60000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("x-goog-api-key", apiKey)
        }
        conn.outputStream.use { it.write(body.toString().toByteArray()) }

        val code = conn.responseCode
        val resp = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.readText().orEmpty()
        if (code !in 200..299) throw IOException(friendlyError(code, resp))

        val parts = JSONObject(resp).getJSONArray("candidates").getJSONObject(0)
            .getJSONObject("content").getJSONArray("parts")
        return buildString { for (i in 0 until parts.length()) append(parts.getJSONObject(i).optString("text")) }
    }

    private fun friendlyError(code: Int, resp: String): String {
        val msg = try { JSONObject(resp).getJSONObject("error").getString("message") } catch (e: Exception) { "" }
        return when {
            code == 403 || msg.contains("API key", ignoreCase = true) -> "Invalid or unauthorised Gemini API key"
            code == 404 -> "Gemini model '$MODEL' was not found (update GeminiClient.MODEL)"
            code == 429 -> "Gemini rate limit reached - wait a minute and try again"
            else -> "Gemini error $code${if (msg.isNotBlank()) ": $msg" else ""}"
        }
    }
}
