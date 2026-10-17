package com.example.chatfamiliar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

data class ChatMessage(
    val id: String,
    val text: String,
    val senderId: String,
    val senderName: String,
    val timestamp: Date?
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val hasLoadError: Boolean = false,
    val hasSendError: Boolean = false
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var messagesListener: ListenerRegistration? = null
    private var observedFamilyId: String? = null

    fun startListening(familyId: String) {
        if (observedFamilyId == familyId && messagesListener != null) return

        messagesListener?.remove()
        observedFamilyId = familyId
        _uiState.value = ChatUiState()

        messagesListener = db.collection(FirestoreCollections.FAMILIES)
            .document(familyId)
            .collection(FirestoreCollections.MESSAGES)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        hasLoadError = true
                    )
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents.orEmpty().map { document ->
                    ChatMessage(
                        id = document.id,
                        text = document.getString("text").orEmpty(),
                        senderId = document.getString("senderId").orEmpty(),
                        senderName = document.getString("senderName").orEmpty(),
                        timestamp = document.getTimestamp(
                            "timestamp",
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )?.toDate()
                    )
                }
                _uiState.value = _uiState.value.copy(
                    messages = messages,
                    isLoading = false,
                    hasLoadError = false
                )
            }
    }

    fun retryListening() {
        val familyId = observedFamilyId ?: return
        messagesListener?.remove()
        messagesListener = null
        startListening(familyId)
    }

    suspend fun sendMessage(text: String): Boolean {
        val cleanText = text.trim()
        if (cleanText.isEmpty() || _uiState.value.isSending) return false

        val user = auth.currentUser
        if (user == null) {
            _uiState.value = _uiState.value.copy(
                hasSendError = true
            )
            return false
        }

        val familyId = observedFamilyId
        if (familyId == null) {
            _uiState.value = _uiState.value.copy(
                hasSendError = true
            )
            return false
        }

        _uiState.value = _uiState.value.copy(
            isSending = true,
            hasSendError = false
        )
        return try {
            val profile = db.collection(FirestoreCollections.USERS)
                .document(user.uid)
                .get()
                .await()
            val senderName = profile.getString("displayName")
                ?.takeIf { it.isNotBlank() }
                ?: user.displayName?.takeIf { it.isNotBlank() }
                ?: user.email?.substringBefore("@")
                ?: getApplication<Application>().getString(R.string.main_profile_display_name)

            db.collection(FirestoreCollections.FAMILIES)
                .document(familyId)
                .collection(FirestoreCollections.MESSAGES)
                .add(
                    mapOf(
                        "text" to cleanText,
                        "senderId" to user.uid,
                        "senderName" to senderName,
                        "timestamp" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            true
        } catch (exception: FirebaseFirestoreException) {
            _uiState.value = _uiState.value.copy(
                hasSendError = true
            )
            false
        } finally {
            _uiState.value = _uiState.value.copy(isSending = false)
        }
    }

    override fun onCleared() {
        messagesListener?.remove()
        messagesListener = null
        super.onCleared()
    }
}
