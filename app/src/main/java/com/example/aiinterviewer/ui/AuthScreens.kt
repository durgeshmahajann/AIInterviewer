package com.example.aiinterviewer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
private fun AuthScaffold(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        GradientHeader {
            Spacer(Modifier.height(24.dp))
            Text("🎯", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(8.dp))
            Text("AI Interviewer", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Smart Interview & Evaluation System", color = Color.White.copy(alpha = 0.85f))
            Spacer(Modifier.height(16.dp))
        }
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        })
    }
}

@Composable
private fun ErrorText(msg: String?) {
    if (msg != null) Text(msg, color = Bad, style = MaterialTheme.typography.bodyMedium)
}

@Composable
fun LoginScreen(auth: AuthViewModel, onGoRegister: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var pw by remember { mutableStateOf("") }
    AuthScaffold("Welcome back", "Log in to continue your interview practice") {
        AppField(email, { email = it; auth.clearError() }, "Email", KeyboardType.Email)
        PasswordField(pw, { pw = it; auth.clearError() }, "Password")
        ErrorText(auth.error)
        Button(
            onClick = { auth.login(email, pw) }, enabled = !auth.busy,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text(if (auth.busy) "Please wait..." else "Log in") }
        TextButton(onClick = onGoRegister, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("New here? Create an account")
        }
    }
}

@Composable
fun RegisterScreen(auth: AuthViewModel, onGoLogin: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pw by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    AuthScaffold("Create account", "Your progress is saved to your profile") {
        AppField(name, { name = it; auth.clearError() }, "Full name")
        AppField(email, { email = it; auth.clearError() }, "Email", KeyboardType.Email)
        PasswordField(pw, { pw = it; auth.clearError() }, "Password (min 6 characters)")
        PasswordField(confirm, { confirm = it; auth.clearError() }, "Confirm password")
        ErrorText(auth.error)
        Button(
            onClick = { auth.register(name, email, pw, confirm) }, enabled = !auth.busy,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text(if (auth.busy) "Please wait..." else "Sign up") }
        TextButton(onClick = onGoLogin, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Already have an account? Log in")
        }
    }
}
