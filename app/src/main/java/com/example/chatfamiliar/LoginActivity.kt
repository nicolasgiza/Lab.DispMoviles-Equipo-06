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
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import android.widget.TextView
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.launch


class LoginActivity : AppCompatActivity() {

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

            val email = normalizeEmail(emailEditText.text.toString())
            val password = passwordEditText.text.toString().trim()

            // Validar campos vacíos
            if (email.isEmpty() || password.isEmpty()) {

                Toast.makeText(
                    this,
                    getString(R.string.login_complete_fields),
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
                            loginButton.isEnabled = false
                            lifecycleScope.launch {
                                try {
                                    FamilyRepository(auth).ensureCurrentUserProfile()
                                    Toast.makeText(
                                        this@LoginActivity,
                                        getString(R.string.login_success),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    startActivity(
                                        Intent(this@LoginActivity, MainActivity::class.java)
                                    )
                                    finish()
                                } catch (exception: FirebaseFirestoreException) {
                                    loginButton.isEnabled = true
                                    Toast.makeText(
                                        this@LoginActivity,
                                        exception.localizedMessage
                                            ?: getString(R.string.main_profile_load_error),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }

                        } else {
                            auth.signOut() // <-- Cierra la sesión activa no verificada
                            Toast.makeText(
                                this,
                                getString(R.string.login_verify_email),
                                Toast.LENGTH_LONG
                            ).show()
                        }

                    } else {

                        Toast.makeText(
                            this,
                            task.exception?.localizedMessage
                                ?: getString(R.string.login_error),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }
}