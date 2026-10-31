package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.configureAppCheck
import com.example.ui.TreeGuardAuthScreen
import com.example.ui.TreeGuardMainScaffold
import com.example.ui.authStateFlow
import com.example.ui.theme.TreeGuardTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // MANDATORY STEP 1: Configure App Check with launch intent before any UI or Firebase calls
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            TreeGuardTheme {
                TreeGuardAppRoot()
            }
        }
    }
}

@Composable
fun TreeGuardAppRoot() {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        TreeGuardAuthScreen(
            onAuthSuccess = { /* authStateFlow updates automatically */ }
        )
    } else {
        TreeGuardMainScaffold(
            currentUserId = user.uid,
            userEmail = user.email ?: "Forester"
        )
    }
}
