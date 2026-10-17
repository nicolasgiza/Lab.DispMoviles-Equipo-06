package com.example.chatfamiliar

import androidx.annotation.StringRes
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class Family(
    val id: String,
    val name: String,
    val inviteCode: String,
    val createdBy: String,
    val createdAt: Timestamp?
)

data class FamilyMember(
    val uid: String,
    val displayName: String,
    val email: String,
    val role: String
)

enum class FamilyError {
    FAMILY_NAME_REQUIRED,
    AUTH_REQUIRED,
    INVITE_CODE_UNIQUE,
    INVITE_CODE_INVALID,
    FAMILY_CODE_NOT_FOUND,
    FAMILY_INVALID_NAME,
    FAMILY_INVALID_CREATOR,
    FAMILY_INVALID_INVITE_CODE,
    CANNOT_CHANGE_OWN_ROLE,
    NOT_FAMILY_MEMBER,
    ADMIN_ONLY,
    MEMBER_NOT_IN_FAMILY,
    FAMILY_ID_REQUIRED
}

class FamilyRepositoryException(val error: FamilyError) : RuntimeException()

@StringRes
fun FamilyError.stringResourceId(): Int = when (this) {
    FamilyError.FAMILY_NAME_REQUIRED -> R.string.family_error_name_required
    FamilyError.AUTH_REQUIRED -> R.string.family_error_auth_required
    FamilyError.INVITE_CODE_UNIQUE -> R.string.family_error_code_unique
    FamilyError.INVITE_CODE_INVALID -> R.string.family_error_code_invalid
    FamilyError.FAMILY_CODE_NOT_FOUND -> R.string.family_error_code_not_found
    FamilyError.FAMILY_INVALID_NAME -> R.string.family_error_invalid_name
    FamilyError.FAMILY_INVALID_CREATOR -> R.string.family_error_invalid_creator
    FamilyError.FAMILY_INVALID_INVITE_CODE -> R.string.family_error_invalid_code
    FamilyError.CANNOT_CHANGE_OWN_ROLE -> R.string.family_error_change_own_role
    FamilyError.NOT_FAMILY_MEMBER -> R.string.family_error_not_member
    FamilyError.ADMIN_ONLY -> R.string.family_error_admin_only
    FamilyError.MEMBER_NOT_IN_FAMILY -> R.string.family_error_target_not_member
    FamilyError.FAMILY_ID_REQUIRED -> R.string.family_error_family_required
}

class FamilyRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun createFamily(name: String): Family {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) {
            throw FamilyRepositoryException(FamilyError.FAMILY_NAME_REQUIRED)
        }

        val user = auth.currentUser
            ?: throw FamilyRepositoryException(FamilyError.AUTH_REQUIRED)
        val families = db.collection(FirestoreCollections.FAMILIES)
        val userRef = db.collection(FirestoreCollections.USERS).document(user.uid)

        repeat(MAX_INVITE_CODE_ATTEMPTS) {
            val familyId = families.document().id
            val inviteCode = generateInviteCode()
            val matchingCode = families
                .whereEqualTo("inviteCode", inviteCode)
                .limit(1)
                .get()
                .await()
                .documents
                .isNotEmpty()
            if (matchingCode) return@repeat

            val familyRef = families.document(familyId)
            val batch = db.batch()
            batch.set(
                familyRef,
                mapOf(
                    "name" to cleanName,
                    "inviteCode" to inviteCode,
                    "createdBy" to user.uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                userRef,
                mapOf(
                    "role" to UserRole.ADMIN.name,
                    "familyId" to familyId
                )
            )
            batch.commit().await()
            return Family(
                id = familyId,
                name = cleanName,
                inviteCode = inviteCode,
                createdBy = user.uid,
                createdAt = null
            )
        }

        throw FamilyRepositoryException(FamilyError.INVITE_CODE_UNIQUE)
    }

    suspend fun joinFamily(inviteCode: String): Family {
        val cleanCode = inviteCode.trim().uppercase()
        if (!isValidInviteCode(cleanCode)) {
            throw FamilyRepositoryException(FamilyError.INVITE_CODE_INVALID)
        }

        val user = auth.currentUser
            ?: throw FamilyRepositoryException(FamilyError.AUTH_REQUIRED)
        val userRef = db.collection(FirestoreCollections.USERS).document(user.uid)
        val familyMatch = db.collection(FirestoreCollections.FAMILIES)
            .whereEqualTo("inviteCode", cleanCode)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?: throw FamilyRepositoryException(FamilyError.FAMILY_CODE_NOT_FOUND)
        val familyRef = familyMatch.reference

        return db.runTransaction { transaction ->
            val familySnapshot = transaction.get(familyRef)
            if (!familySnapshot.exists() || familySnapshot.getString("inviteCode") != cleanCode) {
                throw FamilyRepositoryException(FamilyError.FAMILY_CODE_NOT_FOUND)
            }
            val familyName = familySnapshot.getString("name")
                ?: throw FamilyRepositoryException(FamilyError.FAMILY_INVALID_NAME)
            val createdBy = familySnapshot.getString("createdBy")
                ?: throw FamilyRepositoryException(FamilyError.FAMILY_INVALID_CREATOR)

            transaction.update(
                userRef,
                mapOf(
                    "role" to UserRole.USUARIO.name,
                    "familyId" to familySnapshot.id
                )
            )

            Family(
                id = familySnapshot.id,
                name = familyName,
                inviteCode = cleanCode,
                createdBy = createdBy,
                createdAt = familySnapshot.getTimestamp("createdAt")
            )
        }.await()
    }

    suspend fun getFamily(familyId: String): Family? {
        val snapshot = db.collection(FirestoreCollections.FAMILIES)
            .document(familyId)
            .get()
            .await()
        if (!snapshot.exists()) return null

        return Family(
            id = snapshot.id,
            name = snapshot.getString("name")
                ?: throw FamilyRepositoryException(FamilyError.FAMILY_INVALID_NAME),
            inviteCode = snapshot.getString("inviteCode")
                ?: throw FamilyRepositoryException(FamilyError.FAMILY_INVALID_INVITE_CODE),
            createdBy = snapshot.getString("createdBy")
                ?: throw FamilyRepositoryException(FamilyError.FAMILY_INVALID_CREATOR),
            createdAt = snapshot.getTimestamp("createdAt")
        )
    }

    suspend fun getFamilyMembers(familyId: String): List<FamilyMember> {
        val snapshot = db.collection(FirestoreCollections.USERS)
            .whereEqualTo("familyId", familyId)
            .get()
            .await()

        return snapshot.documents.map { document ->
            val email = document.getString("email").orEmpty()
            FamilyMember(
                uid = document.id,
                displayName = document.getString("displayName")
                    ?.takeIf { it.isNotBlank() }
                    ?: email.substringBefore("@"),
                email = email,
                role = document.getString("role") ?: UserRole.USUARIO.name
            )
        }
    }

    suspend fun updateMemberRole(familyId: String, memberUid: String, newRole: UserRole) {
        val currentUser = auth.currentUser
            ?: throw FamilyRepositoryException(FamilyError.AUTH_REQUIRED)
        if (currentUser.uid == memberUid) {
            throw FamilyRepositoryException(FamilyError.CANNOT_CHANGE_OWN_ROLE)
        }

        val currentUserRef = db.collection(FirestoreCollections.USERS)
            .document(currentUser.uid)
        val memberRef = db.collection(FirestoreCollections.USERS)
            .document(memberUid)

        db.runTransaction { transaction ->
            val currentUserSnapshot = transaction.get(currentUserRef)
            val memberSnapshot = transaction.get(memberRef)
            if (currentUserSnapshot.getString("familyId") != familyId) {
                throw FamilyRepositoryException(FamilyError.NOT_FAMILY_MEMBER)
            }
            if (currentUserSnapshot.getString("role") != UserRole.ADMIN.name) {
                throw FamilyRepositoryException(FamilyError.ADMIN_ONLY)
            }
            if (memberSnapshot.getString("familyId") != familyId) {
                throw FamilyRepositoryException(FamilyError.MEMBER_NOT_IN_FAMILY)
            }
            transaction.update(memberRef, "role", newRole.name)
        }.await()
    }

    suspend fun getCurrentUserFamilyId(): String? {
        val user = auth.currentUser
            ?: throw FamilyRepositoryException(FamilyError.AUTH_REQUIRED)
        return db.collection(FirestoreCollections.USERS)
            .document(user.uid)
            .get()
            .await()
            .getString("familyId")
    }

    suspend fun ensureCurrentUserProfile() {
        val user = auth.currentUser
            ?: throw FamilyRepositoryException(FamilyError.AUTH_REQUIRED)
        val userRef = db.collection(FirestoreCollections.USERS).document(user.uid)
        if (userRef.get().await().exists()) return

        val email = user.email.orEmpty()
        val displayName = user.displayName
            ?.takeIf { it.isNotBlank() }
            ?: email.substringBefore("@")
        userRef.set(
            mapOf(
                "email" to email,
                "displayName" to displayName,
                "familyId" to null,
                "role" to UserRole.USUARIO.name
            )
        ).await()
    }

    private companion object {
        const val MAX_INVITE_CODE_ATTEMPTS = 20
    }
}
