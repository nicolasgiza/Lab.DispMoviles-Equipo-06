package com.example.chatfamiliar

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

@Composable
fun RegisterScreen(
    auth: FirebaseAuth,
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Crear Cuenta",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; errorMessage = null },
            label = { Text("Correo electrónico") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorMessage = null },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; errorMessage = null },
            label = { Text("Confirmar contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            isError = errorMessage != null,
            modifier = Modifier.fillMaxWidth()
        )

        errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val cleanEmail = email.trim()
                val cleanPassword = password.trim()
                val cleanConfirmPassword = confirmPassword.trim()

                if (cleanEmail.isEmpty() || cleanPassword.isEmpty() || cleanConfirmPassword.isEmpty()) {
                    errorMessage = "Por favor completa todos los campos"
                    return@Button
                }

                if (cleanPassword != cleanConfirmPassword) {
                    errorMessage = "Las contraseñas no coinciden"
                    return@Button
                }

                if (cleanPassword.length < 6) {
                    errorMessage = "La contraseña debe tener al menos 6 caracteres"
                    return@Button
                }

                isLoading = true

                auth.createUserWithEmailAndPassword(cleanEmail, cleanPassword)
                    .addOnCompleteListener { task ->
                        isLoading = false
                        if (task.isSuccessful) {

                            val user = auth.currentUser

                            // Guardar rol predeterminado en Firestore (Actividad 6)
                            user?.uid?.let { uid ->
                                saveUserRole(uid = uid, email = cleanEmail, role = UserRole.USUARIO)
                            }

                            user?.sendEmailVerification()
                                ?.addOnCompleteListener { verificationTask ->

                                    if (verificationTask.isSuccessful) {

                                        Toast.makeText(
                                            context,
                                            "Cuenta creada. Revisa tu correo para verificarla.",
                                            Toast.LENGTH_LONG
                                        ).show()

                                        onRegisterSuccess()

                                    } else {

                                        errorMessage =
                                            verificationTask.exception?.localizedMessage
                                                ?: "No se pudo enviar el correo de verificación"
                                    }
                                }

                        } else {
                            errorMessage = task.exception?.localizedMessage ?: "Error al registrar usuario"
                        }
                    }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Registrarse")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateToLogin) {
            Text("¿Ya tienes cuenta? Inicia sesión")
        }
    }
}