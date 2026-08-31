package com.example.chatfamiliar

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import android.widget.TextView


class LoginAcyivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_login_acyivity)

        // Inicializar Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Referencias de los elementos de la pantalla
        val emailEditText = findViewById<EditText>(R.id.etEmail)
        val passwordEditText = findViewById<EditText>(R.id.etPassword)
        val loginButton = findViewById<Button>(R.id.btnLogin)
        val registerTextView = findViewById<TextView>(R.id.tvRegister)

        registerTextView.setOnClickListener {

            val intent = Intent(
                this,
                RegisterActivity::class.java
            )

            startActivity(intent)
            finish()
        }
        // Ajuste de pantalla
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->

            val systemBars =
                insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        // Botón iniciar sesión
        loginButton.setOnClickListener {

            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            // Validar campos vacíos
            if (email.isEmpty() || password.isEmpty()) {

                Toast.makeText(
                    this,
                    "Completa todos los campos",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            // Iniciar sesión con Firebase
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->

                    if (task.isSuccessful) {

                        val user = auth.currentUser

                        // Actividad 3:
                        // comprobar si el correo ya fue verificado
                        if (user != null && user.isEmailVerified) {

                            Toast.makeText(
                                this,
                                "Inicio de sesión exitoso",
                                Toast.LENGTH_SHORT
                            ).show()

                            // Ir a MainActivity
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)

                            // Evita volver al Login con el botón atrás
                            finish()

                        } else {

                            Toast.makeText(
                                this,
                                "Debes verificar tu correo electrónico antes de iniciar sesión",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                    } else {

                        Toast.makeText(
                            this,
                            task.exception?.localizedMessage
                                ?: "Error al iniciar sesión",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }
}