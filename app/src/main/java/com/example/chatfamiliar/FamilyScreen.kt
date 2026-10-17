package com.example.chatfamiliar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.launch

@Composable
fun FamilyScreen(
    familyId: String,
    role: String?,
    onSignOut: () -> Unit,
    repository: FamilyRepository = remember { FamilyRepository() }
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid
    var family by remember { mutableStateOf<Family?>(null) }
    var members by remember { mutableStateOf<List<FamilyMember>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var updatingMemberUid by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var copied by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(familyId, reloadKey) {
        isLoading = true
        errorMessage = null
        try {
            family = repository.getFamily(familyId)
                ?: throw IllegalStateException(context.getString(R.string.family_not_found))
            members = repository.getFamilyMembers(familyId)
        } catch (exception: FirebaseFirestoreException) {
            errorMessage = exception.localizedMessage
                ?: context.getString(R.string.family_info_load_error)
        } catch (exception: FamilyRepositoryException) {
            errorMessage = context.getString(exception.error.stringResourceId())
        } catch (exception: IllegalStateException) {
            errorMessage = exception.message ?: context.getString(R.string.family_load_error)
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.family_title), style = MaterialTheme.typography.headlineMedium)

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (errorMessage != null) {
            Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = { reloadKey++ }) {
                Text(stringResource(R.string.family_retry))
            }
        } else {
            family?.let { currentFamily ->
                Text(currentFamily.name, style = MaterialTheme.typography.titleLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(stringResource(R.string.family_invite_code_label))
                        Text(currentFamily.inviteCode, style = MaterialTheme.typography.titleMedium)
                    }
                    OutlinedButton(onClick = {
                        clipboard.setText(AnnotatedString(currentFamily.inviteCode))
                        copied = true
                    }) {
                        Text(
                            stringResource(
                                if (copied) R.string.family_invite_copied
                                else R.string.family_copy_invite
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.family_members), style = MaterialTheme.typography.titleLarge)
                members.forEach { member ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(member.displayName, style = MaterialTheme.typography.titleMedium)
                        Text(member.email, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            stringResource(
                                R.string.family_role_label,
                                stringResource(
                                    if (UserRole.fromValue(member.role) == UserRole.ADMIN) {
                                        R.string.role_admin
                                    } else {
                                        R.string.role_user
                                    }
                                )
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (UserRole.fromValue(role) == UserRole.ADMIN && member.uid != currentUid) {
                            OutlinedButton(
                                enabled = updatingMemberUid == null,
                                onClick = {
                                    scope.launch {
                                        updatingMemberUid = member.uid
                                        errorMessage = null
                                        val newRole = if (UserRole.fromValue(member.role) == UserRole.ADMIN) {
                                            UserRole.USUARIO
                                        } else {
                                            UserRole.ADMIN
                                        }
                                        try {
                                            repository.updateMemberRole(
                                                familyId = familyId,
                                                memberUid = member.uid,
                                                newRole = newRole
                                            )
                                            reloadKey++
                                        } catch (exception: FamilyRepositoryException) {
                                            errorMessage = context.getString(
                                                exception.error.stringResourceId()
                                            )
                                        } catch (exception: FirebaseFirestoreException) {
                                            errorMessage = exception.localizedMessage
                                                ?: context.getString(R.string.role_update_error)
                                        } catch (exception: IllegalStateException) {
                                            errorMessage = exception.message
                                                ?: context.getString(R.string.role_update_error)
                                        } catch (exception: IllegalArgumentException) {
                                            errorMessage = exception.message
                                                ?: context.getString(R.string.role_update_error)
                                        } finally {
                                            updatingMemberUid = null
                                        }
                                    }
                                }
                            ) {
                                Text(
                                    if (UserRole.fromValue(member.role) == UserRole.ADMIN) {
                                        stringResource(R.string.family_change_role_user)
                                    } else {
                                        stringResource(R.string.family_change_role_admin)
                                    }
                                )
                            }
                        }
                    }
                }
                errorMessage?.let { message ->
                    Text(message, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        LanguageSelectorScreen()
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.family_sign_out))
        }
    }
}
