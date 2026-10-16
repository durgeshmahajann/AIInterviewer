package com.example.aiinterviewer.ui

import android.app.Application
import android.content.Context
import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiinterviewer.data.AppDb
import com.example.aiinterviewer.data.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Local (on-device) accounts: PBKDF2-hashed passwords in Room, session id in SharedPreferences. */
class AuthViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDb.get(app).userDao()
    private val prefs = app.getSharedPreferences("auth", Context.MODE_PRIVATE)

    var userId by mutableLongStateOf(prefs.getLong("uid", -1L)); private set
    var userName by mutableStateOf(prefs.getString("name", "") ?: ""); private set
    var error by mutableStateOf<String?>(null); private set
    var busy by mutableStateOf(false); private set

    val loggedIn: Boolean get() = userId > 0

    fun clearError() { error = null }

    fun register(name: String, email: String, password: String, confirm: String) {
        val n = name.trim()
        val e = email.trim().lowercase()
        when {
            n.isEmpty() -> error = "Please enter your name"
            !Patterns.EMAIL_ADDRESS.matcher(e).matches() -> error = "Enter a valid email address"
            password.length < 6 -> error = "Password must be at least 6 characters"
            password != confirm -> error = "Passwords do not match"
            else -> viewModelScope.launch {
                busy = true
                if (dao.findByEmail(e) != null) {
                    error = "An account with this email already exists"
                } else {
                    val salt = newSalt()
                    val hash = withContext(Dispatchers.Default) { hash(password, salt) }
                    val id = dao.insert(UserEntity(name = n, email = e, salt = toHex(salt), passwordHash = hash))
                    startSession(id, n)
                }
                busy = false
            }
        }
    }

    fun login(email: String, password: String) {
        val e = email.trim().lowercase()
        if (e.isEmpty() || password.isEmpty()) { error = "Enter your email and password"; return }
        viewModelScope.launch {
            busy = true
            val user = dao.findByEmail(e)
            val ok = user != null &&
                withContext(Dispatchers.Default) { hash(password, fromHex(user.salt)) } == user.passwordHash
            if (ok && user != null) startSession(user.id, user.name) else error = "Invalid email or password"
            busy = false
        }
    }

    fun logout() {
        prefs.edit().clear().apply()
        userId = -1L
        userName = ""
    }

    private fun startSession(id: Long, name: String) {
        prefs.edit().putLong("uid", id).putString("name", name).apply()
        error = null
        userName = name
        userId = id
    }

    private fun newSalt() = ByteArray(16).also { SecureRandom().nextBytes(it) }
    private fun toHex(b: ByteArray) = b.joinToString("") { "%02x".format(it) }
    private fun fromHex(s: String) = s.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    private fun hash(password: String, salt: ByteArray): String {
        val spec = PBEKeySpec(password.toCharArray(), salt, 10000, 256)
        return toHex(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded)
    }
}
