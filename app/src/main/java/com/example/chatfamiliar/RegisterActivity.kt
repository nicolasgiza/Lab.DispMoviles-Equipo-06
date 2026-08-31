package com.example.chatfamiliar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.chatfamiliar.ui.theme.ChatFamiliarTheme
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inicializar Firebase Authentication
        auth = FirebaseAuth.getInstance()

        setContent {
            ChatFamiliarTheme {

                Surface(
                    color = MaterialTheme.colorScheme.background
                ) {

                    RegisterScreen(
                        auth = auth,

                        // Después de registrarse:
                        // ir al Login
                        onRegisterSuccess = {

                            val intent = Intent(
                                this,
                                LoginAcyivity::class.java
                            )

                            startActivity(intent)
                            finish()
                        },

                        // "¿Ya tienes cuenta? Inicia sesión"
                        onNavigateToLogin = {

                            val intent = Intent(
                                this,
                                LoginAcyivity::class.java
                            )

                            startActivity(intent)
                            finish()
                        }
                    )
                }
            }
        }
    }
}