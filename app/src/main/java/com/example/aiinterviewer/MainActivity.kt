package com.example.aiinterviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.aiinterviewer.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AIInterviewerTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { AppNav() }
            }
        }
    }
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val auth: AuthViewModel = viewModel()
    val vm: InterviewViewModel = viewModel()
    val start = remember { if (auth.loggedIn) "home" else "login" }
    val authRoutes = setOf("login", "register")

    // React to login / logout: switch user data and move between the auth and app screens.
    LaunchedEffect(auth.userId) {
        vm.setUser(auth.userId)
        val cur = nav.currentDestination?.route
        if (auth.loggedIn && cur != null && cur in authRoutes) {
            nav.navigate("home") { popUpTo(nav.graph.id) { inclusive = true } }
        } else if (!auth.loggedIn && cur != null && cur !in authRoutes) {
            nav.navigate("login") { popUpTo(nav.graph.id) { inclusive = true } }
        }
    }

    NavHost(nav, startDestination = start) {
        composable("login") { LoginScreen(auth, onGoRegister = { auth.clearError(); nav.navigate("register") }) }
        composable("register") { RegisterScreen(auth, onGoLogin = { auth.clearError(); nav.popBackStack() }) }
        composable("home") {
            HomeScreen(
                vm, userName = auth.userName,
                onLogout = { auth.logout() },
                onStart = { nav.navigate("interview") },
                onHistory = { nav.navigate("history") },
                onResume = { nav.navigate("resume") }
            )
        }
        composable("resume") {
            ResumeScreen(
                vm, onBack = { nav.popBackStack() },
                onStart = { nav.navigate("interview") }
            )
        }
        composable("interview") {
            InterviewScreen(vm, onFinished = { nav.navigate("result") { popUpTo("home") } })
        }
        composable("result") { ResultScreen(vm, onHome = { nav.popBackStack("home", false) }) }
        composable("history") { HistoryScreen(vm, onBack = { nav.popBackStack() }) }
    }
}
