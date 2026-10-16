package com.example.aiinterviewer.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ResumeScreen(vm: InterviewViewModel, onBack: () -> Unit, onStart: () -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.loadResume(uri)
    }
    var written by remember { mutableIntStateOf(5) }
    var mcq by remember { mutableIntStateOf(5) }
    val doc = vm.resume

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        GradientHeader {
            Text("📄 Resume interview", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Gemini reads your resume and writes personalised questions and MCQs", color = Color.White.copy(alpha = 0.85f))
        }

        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = {
                    launcher.launch(
                        arrayOf(
                            "application/pdf",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            "text/plain"
                        )
                    )
                },
                enabled = !vm.analyzing && !vm.generating,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text(if (doc == null) "Choose resume (PDF, DOCX, TXT)" else "Choose a different file") }

            Text(
                "The text of your resume is sent to Google's Gemini API to create the questions. It is not saved in the app.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (vm.analyzing) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("Reading your resume...")
                }
            }
            vm.resumeError?.let { Text(it, color = Bad) }

            if (doc != null) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("📎 ${doc.fileName}", fontWeight = FontWeight.Bold)
                        Text("${doc.text.trim().split(Regex("\\s+")).size} words read", style = MaterialTheme.typography.labelMedium)
                        Text(
                            doc.text.trim().take(280).replace(Regex("\\s+"), " ") + "...",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                SectionTitle("Written questions")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(3, 5, 8, 10).forEach { n -> FilterChip(selected = written == n, onClick = { written = n }, label = { Text("$n") }) }
                }
                SectionTitle("Multiple-choice questions")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 3, 5, 8, 10).forEach { n -> FilterChip(selected = mcq == n, onClick = { mcq = n }, label = { Text("$n") }) }
                }
                Text(
                    "Total: ${written + mcq} questions", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!vm.hasKey) Text("Add your Gemini API key on the home screen (🔑 Key) first.", color = Warn)
                vm.error?.let { Text(it, color = Bad) }

                Button(
                    onClick = { vm.startResume(written, mcq, onStart) },
                    enabled = !vm.generating && vm.hasKey,
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) {
                    Text(
                        if (vm.generating) "Creating questions with Gemini..." else "Start resume interview",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Back") }
        }
    }
}
