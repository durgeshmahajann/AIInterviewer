package com.example.aiinterviewer.resume

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.InputStream
import java.util.zip.ZipInputStream

data class ResumeDoc(val fileName: String, val text: String)

/** Reads plain text out of a PDF, DOCX or TXT resume. Blocking: call from Dispatchers.IO. */
object ResumeExtractor {

    fun extract(context: Context, uri: Uri): Pair<String, String> {
        val name = displayName(context, uri)
        val lower = name.lowercase()
        val mime = context.contentResolver.getType(uri) ?: ""
        val stream = context.contentResolver.openInputStream(uri) ?: error("Cannot open file")
        val text = stream.use { s ->
            when {
                mime.contains("pdf") || lower.endsWith(".pdf") -> pdf(context, s)
                lower.endsWith(".docx") || mime.contains("wordprocessingml") -> docx(s)
                else -> s.bufferedReader().readText()
            }
        }
        return name to text.replace("\r", "").take(20000)
    }

    private fun pdf(context: Context, s: InputStream): String {
        PDFBoxResourceLoader.init(context.applicationContext)
        return PDDocument.load(s).use { PDFTextStripper().getText(it) }
    }

    private fun docx(s: InputStream): String {
        ZipInputStream(s).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    val xml = String(zip.readBytes(), Charsets.UTF_8)
                    return xml
                        .replace("</w:p>", "\n")
                        .replace("<w:br/>", "\n")
                        .replace("<w:tab/>", " ")
                        .replace(Regex("<[^>]+>"), "")
                        .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                        .replace("&quot;", "\"").replace("&apos;", "'")
                }
                entry = zip.nextEntry
            }
        }
        return ""
    }

    private fun displayName(context: Context, uri: Uri): String {
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (i >= 0 && c.moveToFirst()) return c.getString(i)
        }
        return uri.lastPathSegment ?: "resume"
    }
}
