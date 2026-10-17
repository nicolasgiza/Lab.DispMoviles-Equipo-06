package com.example.chatfamiliar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.launch

@Composable
fun FamilySetupScreen(
    onFamilyReady: (Family) -> Unit,
    repository: FamilyRepository = remember { FamilyRepository() }
) {
    val context = LocalContext.current
    var familyName by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun submit(action: suspend () -> Family) {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                onFamilyReady(action())
            } catch (exception: FamilyRepositoryException) {
                errorMessage = context.getString(exception.error.stringResourceId())
            } catch (exception: IllegalArgumentException) {
                errorMessage = exception.message
                    ?: context.getString(R.string.setup_review_data)
            } catch (exception: IllegalStateException) {
                errorMessage = exception.message
                    ?: context.getString(R.string.setup_operation_error)
            } catch (exception: FirebaseFirestoreException) {
                errorMessage = exception.localizedMessage
                    ?: context.getString(R.string.setup_connection_error)
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(R.string.family_setup_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = familyName,
            onValueChange = { familyName = it; errorMessage = null },
            label = { Text(stringResource(R.string.family_name_label)) },
            singleLine = true,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { submit { repository.createFamily(familyName) } },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.family_create_button))
        }

        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = inviteCode,
            onValueChange = {
                inviteCode = it.filter(Char::isLetterOrDigit).take(6).uppercase()
                errorMessage = null
            },
            label = { Text(stringResource(R.string.family_invite_code_label)) },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Ascii
            ),
            singleLine = true,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { submit { repository.joinFamily(inviteCode) } },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.family_join_button))
        }

        errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(message, color = MaterialTheme.colorScheme.error)
        }
        if (isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }
    }
}
