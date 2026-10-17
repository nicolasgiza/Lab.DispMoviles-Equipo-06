package com.example.chatfamiliar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await

@Composable
fun UserRoleState(
    auth: FirebaseAuth = FirebaseAuth.getInstance(),
    db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    content: (@Composable (String?) -> Unit)? = null
) {
    val context = LocalContext.current
    var role by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }

    val currentUser = auth.currentUser

    LaunchedEffect(currentUser?.uid, reloadKey) {
        val uid = currentUser?.uid
        if (uid != null) {
            isLoading = true
            errorMessage = null
            try {
                role = db.collection(FirestoreCollections.USERS).document(uid)
                    .get()
                    .await()
                    .getString("role")
            } catch (exception: FirebaseFirestoreException) {
                errorMessage = exception.localizedMessage
                    ?: context.getString(R.string.role_load_error)
            } finally {
                isLoading = false
            }
        } else {
            role = null
            isLoading = false
        }
    }

    when {
        isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        errorMessage != null -> Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(errorMessage!!)
            Button(onClick = { reloadKey++ }) {
                Text(stringResource(R.string.role_retry))
            }
        }

        content != null -> content(role)
        else -> when (UserRole.fromValue(role)) {
            UserRole.ADMIN -> AdminDashboard()
            else -> UserDashboard()
        }
    }
}

@Composable
fun AdminDashboard() {
    Text(stringResource(R.string.role_admin_dashboard), modifier = Modifier.padding(16.dp))
}

@Composable
fun UserDashboard() {
    Text(stringResource(R.string.role_user_dashboard), modifier = Modifier.padding(16.dp))
}