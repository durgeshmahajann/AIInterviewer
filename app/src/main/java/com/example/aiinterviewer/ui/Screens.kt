package com.example.aiinterviewer.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.aiinterviewer.data.Category
import com.example.aiinterviewer.data.Courses
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// ------------------------------------------------------------------ API key dialog
@Composable
fun ApiKeyDialog(current: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gemini API key") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Get a free key at aistudio.google.com/apikey. It is stored only on this device and is used to create questions and evaluate your answers.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true, label = { Text("API key") })
            }
        },
        confirmButton = { TextButton(onClick = { onSave(text) }, enabled = text.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ------------------------------------------------------------------ Home
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    vm: InterviewViewModel,
    userName: String,
    onLogout: () -> Unit,
    onStart: () -> Unit,
    onHistory: () -> Unit,
    onResume: () -> Unit
) {
    var category by remember { mutableStateOf(Category.TECHNICAL) }
    var selected by remember { mutableStateOf(setOf("Android", "Java/OOP", "Database")) }
    var count by remember { mutableFloatStateOf(10f) }
    var showKey by remember { mutableStateOf(false) }
    val sessions by vm.history.collectAsState()

    val n = count.toInt().coerceIn(5, 20)
    val needsCourses = category != Category.HR
    val canStart = !vm.generating && (!needsCourses || selected.isNotEmpty())
    val avg = if (sessions.isEmpty()) "-" else "${sessions.map { it.overall }.average().roundToInt()}%"
    val best = if (sessions.isEmpty()) "-" else "${sessions.maxOf { it.overall }}%"

    if (showKey) {
        ApiKeyDialog(vm.apiKey, onSave = { vm.saveKey(it); showKey = false }, onDismiss = { showKey = false })
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        GradientHeader {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Hello, $userName 👋", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Ready for a mock interview?", color = Color.White.copy(alpha = 0.85f))
                }
                TextButton(onClick = { showKey = true }) { Text("🔑 Key", color = Color.White) }
                TextButton(onClick = onLogout) { Text("Logout", color = Color.White) }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Interviews", "${sessions.size}", Modifier.weight(1f))
                StatCard("Average", avg, Modifier.weight(1f))
                StatCard("Best", best, Modifier.weight(1f))
            }
        }

        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (!vm.hasKey) {
                Card(
                    Modifier.fillMaxWidth().clickable { showKey = true },
                    colors = CardDefaults.cardColors(containerColor = Warn.copy(alpha = 0.18f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("🔑 Add your Gemini API key", fontWeight = FontWeight.Bold)
                        Text(
                            "Needed to create questions and evaluate answers. Tap here to add it.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Card(
                Modifier.fillMaxWidth().clickable { onResume() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("📄", style = MaterialTheme.typography.headlineMedium)
                    Column(Modifier.weight(1f)) {
                        Text("Resume-based interview", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Upload your resume to get personalised questions and MCQs", style = MaterialTheme.typography.bodySmall)
                    }
                    Text("›", style = MaterialTheme.typography.headlineMedium)
                }
            }

            SectionTitle("Interview type")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Category.values().filter { it != Category.RESUME }.forEach { c ->
                    FilterChip(selected = category == c, onClick = { category = c }, label = { Text("${c.emoji} ${c.label}") })
                }
            }

            if (needsCourses) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("Courses (${selected.size} selected)")
                    Row {
                        TextButton(onClick = { selected = Courses.all.toSet() }) { Text("All") }
                        TextButton(onClick = { selected = emptySet() }) { Text("Clear") }
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Courses.all.forEach { c ->
                        FilterChip(
                            selected = c in selected,
                            onClick = { selected = if (c in selected) selected - c else selected + c },
                            label = { Text("${Courses.emoji(c)} $c") }
                        )
                    }
                }
            }

            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        SectionTitle("Number of questions")
                        Pill("$n", MaterialTheme.colorScheme.primary)
                    }
                    Slider(value = n.toFloat(), onValueChange = { count = it }, valueRange = 5f..20f, steps = 14)
                    Text(
                        "Questions are created fresh by Gemini each time (5 to 20).",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            vm.error?.let { Text(it, color = Bad, style = MaterialTheme.typography.bodyMedium) }

            Button(
                onClick = { if (!vm.hasKey) showKey = true else vm.start(category, selected, n, onStart) },
                enabled = canStart,
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Text(
                    if (vm.generating) "Creating questions with Gemini..." else "Start interview",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            OutlinedButton(onClick = onHistory, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Performance history") }
            Text(
                "Questions and evaluation powered by Gemini",
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ------------------------------------------------------------------ Interview
@Composable
fun InterviewScreen(vm: InterviewViewModel, onFinished: () -> Unit) {
    val q = vm.questions.getOrNull(vm.index)
    if (q == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No active interview.") }
        return
    }
    var answer by remember(vm.index) { mutableStateOf("") }
    var selected by remember(vm.index) { mutableIntStateOf(-1) }
    val last = vm.index == vm.questions.size - 1
    val words = answer.trim().split(Regex("\\s+")).count { it.isNotBlank() }
    val progress by animateFloatAsState(vm.index / vm.questions.size.toFloat(), label = "progress")

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Question ${vm.index + 1}/${vm.questions.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Pill("${Courses.emoji(q.topic)} ${q.topic}", MaterialTheme.colorScheme.primary)
        }
        LinearProgressIndicator(
            progress = { progress },
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50))
        )

        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.padding(22.dp)) {
                Text(
                    if (q.isMcq) "Multiple choice - pick one answer" else q.category.label + " question",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(6.dp))
                Text(q.text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }

        if (q.isMcq) {
            q.options.forEachIndexed { i, opt ->
                val sel = selected == i
                Card(
                    Modifier.fillMaxWidth().clickable(enabled = !vm.evaluating) { selected = i },
                    colors = CardDefaults.cardColors(
                        containerColor = if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    border = if (sel) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = sel, onClick = null)
                        Spacer(Modifier.width(10.dp))
                        Text(opt, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        } else {
            OutlinedTextField(
                value = answer, onValueChange = { answer = it },
                label = { Text("Type your answer") }, minLines = 7,
                enabled = !vm.evaluating, shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "$words words - aim for 40+ with an example",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        vm.error?.let { Text(it, color = Bad, style = MaterialTheme.typography.bodyMedium) }

        Button(
            onClick = { vm.submit(if (q.isMcq) q.options[selected] else answer, onFinished) },
            enabled = !vm.evaluating && (if (q.isMcq) selected >= 0 else answer.isNotBlank()),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text(
                if (vm.evaluating) "Gemini is evaluating..." else if (last) "Finish interview" else "Submit answer"
            )
        }

        OutlinedButton(
            onClick = { vm.submit("", onFinished) }, enabled = !vm.evaluating,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) { Text("Skip question") }

        if (vm.evaluating) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
    }
}

// ------------------------------------------------------------------ Result
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResultScreen(vm: InterviewViewModel, onHome: () -> Unit) {
    val pct = vm.overallPercent()
    val topics = vm.topicAverages()
    val strengths = topics.filter { it.value >= 7 }.keys
    val weaknesses = topics.filter { it.value < 5 }.keys
    val answered = vm.results.count { it.answer.isNotBlank() }
    val verdict = when {
        pct >= 75 -> "Excellent - interview ready! 🎉"
        pct >= 50 -> "Good - keep practising 👍"
        else -> "Needs improvement - review the feedback 📚"
    }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Interview complete", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                ScoreRing(pct)
                Spacer(Modifier.height(12.dp))
                Text(verdict, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text("$answered of ${vm.results.size} questions answered", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { SectionTitle("Topic performance") }
        items(topics.entries.toList()) { (t, avg) ->
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${Courses.emoji(t)} $t")
                    Text(String.format(Locale.US, "%.1f/10", avg), fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (avg / 10).toFloat() },
                    color = scoreColor(avg.roundToInt()),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50))
                )
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("💪 Strengths", fontWeight = FontWeight.Bold)
                    if (strengths.isEmpty()) Text("None yet - keep practising.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { strengths.forEach { Pill(it, Good) } }
                    Text("🎯 Areas to improve", fontWeight = FontWeight.Bold)
                    if (weaknesses.isEmpty()) Text("None - nice work.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { weaknesses.forEach { Pill(it, Bad) } }
                }
            }
        }
        item { SectionTitle("Question-wise feedback") }
        items(vm.results) { r ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(r.question.topic, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        ScoreBadge(r.score)
                    }
                    Text(r.question.text, fontWeight = FontWeight.Bold)
                    if (r.question.isMcq) Text("Your answer: ${r.answer.ifBlank { "(skipped)" }}")
                    Text(r.feedback, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Button(onClick = onHome, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Back to home") }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ------------------------------------------------------------------ History
@Composable
fun HistoryScreen(vm: InterviewViewModel, onBack: () -> Unit) {
    val sessions by vm.history.collectAsState()
    val fmt = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    val agg = sessions
        .flatMap { it.topicScores.split(";").filter { p -> p.contains(":") } }
        .groupBy({ it.substringBefore(":") }, { it.substringAfter(":").toDoubleOrNull() ?: 0.0 })
        .mapValues { it.value.average() }

    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Performance history", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            if (sessions.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("No interviews yet. Complete one to see your progress.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (sessions.isNotEmpty()) {
            item {
                val recent = sessions.take(10).reversed()
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        SectionTitle("Score trend (last ${recent.size})")
                        Row(
                            Modifier.fillMaxWidth().height(150.dp).padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            recent.forEach { s ->
                                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                                    Text("${s.overall}", style = MaterialTheme.typography.labelSmall)
                                    Spacer(Modifier.height(2.dp))
                                    Box(
                                        Modifier.fillMaxWidth().height((8 + s.overall).dp)
                                            .background(scoreColor(s.overall / 10), RoundedCornerShape(6.dp))
                                    )
                                }
                            }
                        }
                        if (sessions.size >= 2) {
                            val diff = sessions.first().overall - sessions.last().overall
                            Spacer(Modifier.height(8.dp))
                            Text(
                                when {
                                    diff > 0 -> "📈 Up $diff points since your first attempt."
                                    diff < 0 -> "📉 Down ${-diff} points since your first attempt."
                                    else -> "Same as your first attempt."
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            item { SectionTitle("Overall by topic") }
            items(agg.entries.toList()) { (t, avg) ->
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${Courses.emoji(t)} $t")
                        Text(String.format(Locale.US, "%.1f/10", avg), fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (avg / 10).toFloat() },
                        color = scoreColor(avg.roundToInt()),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50))
                    )
                }
            }
            item { SectionTitle("Past interviews") }
            items(sessions) { s ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("${s.category} - ${s.questionCount} questions", fontWeight = FontWeight.Bold)
                            Text(fmt.format(Date(s.timestamp)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Pill("${s.overall}%", scoreColor(s.overall / 10))
                    }
                }
            }
            item { OutlinedButton(onClick = { vm.clearHistory() }, modifier = Modifier.fillMaxWidth()) { Text("Clear history") } }
        }
        item { Button(onClick = onBack, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Back") } }
    }
}
