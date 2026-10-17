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
import androidx.compose.ui.res.stringResource
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
            text = stringResource(R.string.register_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; errorMessage = null },
            label = { Text(stringResource(R.string.login_email)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorMessage = null },
            label = { Text(stringResource(R.string.login_password)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; errorMessage = null },
            label = { Text(stringResource(R.string.register_confirm_password)) },
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
                val cleanEmail = normalizeEmail(email)
                val cleanPassword = password.trim()
                val cleanConfirmPassword = confirmPassword.trim()

                if (cleanEmail.isEmpty() || cleanPassword.isEmpty() || cleanConfirmPassword.isEmpty()) {
                    errorMessage = context.getString(R.string.register_complete_fields)
                    return@Button
                }

                if (cleanPassword != cleanConfirmPassword) {
                    errorMessage = context.getString(R.string.register_password_mismatch)
                    return@Button
                }

                if (cleanPassword.length < 6) {
                    errorMessage = context.getString(R.string.register_password_minimum)
                    return@Button
                }

                isLoading = true

                auth.createUserWithEmailAndPassword(cleanEmail, cleanPassword)
                    .addOnCompleteListener { task ->
                        isLoading = false
                        if (task.isSuccessful) {

                            val user = auth.currentUser
                            if (user == null) {
                                errorMessage = context.getString(R.string.register_user_missing)
                                return@addOnCompleteListener
                            }

                            user.sendEmailVerification()
                                .addOnCompleteListener { verificationTask ->

                                    if (verificationTask.isSuccessful) {

                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.register_success),
                                            Toast.LENGTH_LONG
                                        ).show()

                                        onRegisterSuccess()

                                    } else {

                                        errorMessage =
                                            verificationTask.exception?.localizedMessage
                                                ?: context.getString(R.string.register_verification_error)
                                    }
                                }

                        } else {
                            errorMessage = task.exception?.localizedMessage
                                ?: context.getString(R.string.register_error)
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
                Text(stringResource(R.string.register_button))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateToLogin) {
            Text(stringResource(R.string.register_login_link))
        }
    }
}