package com.example.chatfamiliar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.chatfamiliar.ui.theme.ChatFamiliarTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inicializar Firebase Auth
        auth = Firebase.auth

        setContent {
            ChatFamiliarTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    // Aquí llamamos y usamos la función RegisterScreen
                    RegisterScreen(
                        auth = auth,
                        onRegisterSuccess = {
                            // Por ahora, cuando el registro sea exitoso, recargamos o pasamos a la siguiente vista
                        },
                        onNavigateToLogin = {
                            // Aquí agregaremos la navegación a la pantalla de Login más adelante
                        }
                    )
                }
            }
        }
    }
}