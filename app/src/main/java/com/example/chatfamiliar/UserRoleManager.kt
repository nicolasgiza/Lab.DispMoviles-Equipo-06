package com.example.chatfamiliar

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

// Definición de los 2 roles requeridos
enum class UserRole {
    ADMIN, USUARIO
}

// Función para guardar el rol en Firestore al registrarse o asignarlo
fun saveUserRole(uid: String, email: String, role: UserRole = UserRole.USUARIO) {
    val db = FirebaseFirestore.getInstance()
    val userMap = hashMapOf(
        "email" to email,
        "role" to role.name
    )

    db.collection("users").document(uid).set(userMap)
}

@Composable
fun UserRoleState(
    auth: FirebaseAuth = FirebaseAuth.getInstance(),
    db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    var role by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val currentUser = auth.currentUser

    LaunchedEffect(currentUser?.uid) {
        val uid = currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid)
                .get()
                .addOnSuccessListener { document: DocumentSnapshot ->
                    if (document.exists()) {
                        role = document.getString("role")
                    }
                    isLoading = false
                }
                .addOnFailureListener {
                    isLoading = false
                }
        } else {
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        when (role) {
            "ADMIN" -> AdminDashboard()
            else -> UserDashboard() // Por defecto se muestra la vista de Usuario
        }
    }
}

@Composable
fun AdminDashboard() {
    Text("Panel de Administración (ADMIN)", modifier = Modifier.padding(16.dp))
}

@Composable
fun UserDashboard() {
    Text("Vista Principal (USUARIO)", modifier = Modifier.padding(16.dp))
}