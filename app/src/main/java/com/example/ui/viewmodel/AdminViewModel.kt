package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.UiState
import com.example.data.model.Announcement
import com.example.data.model.ModerationReport
import com.example.data.model.PrivateUser
import com.example.data.model.PublicProfile
import com.example.data.repository.SocialRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminViewModel(
    private val socialRepo: SocialRepository
) : ViewModel() {

    private fun <T> Flow<T>.toAdminUiState(label: String): StateFlow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Admin observe $label failed", rootError)
                emit(UiState.Error(rootError.message ?: "Admin access required for $label"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val allProfiles: StateFlow<UiState<List<PublicProfile>>> =
        socialRepo.observeAllPublicProfiles().toAdminUiState("profiles")

    val allPrivateUsers: StateFlow<UiState<List<PrivateUser>>> =
        socialRepo.observeAdminAllUsers().toAdminUiState("users")

    val reports: StateFlow<UiState<List<ModerationReport>>> =
        socialRepo.observeAdminReports().toAdminUiState("reports")

    val announcements: StateFlow<UiState<List<Announcement>>> =
        socialRepo.observeAnnouncements().toAdminUiState("announcements")

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun publishAnnouncement(title: String, message: String) {
        viewModelScope.launch {
            socialRepo.adminPublishAnnouncement(title, message)
                .onSuccess { _events.tryEmit("Announcement published!") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Publish failed") }
        }
    }

    fun toggleUserSuspended(uid: String, suspended: Boolean) {
        viewModelScope.launch {
            socialRepo.adminSetUserSuspended(uid, suspended)
                .onSuccess { _events.tryEmit(if (suspended) "User suspended" else "User unsuspended") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Action failed") }
        }
    }

    fun resolveReport(reportId: String) {
        viewModelScope.launch {
            socialRepo.adminResolveReport(reportId)
                .onSuccess { _events.tryEmit("Report marked resolved") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Resolve failed") }
        }
    }

    private companion object {
        const val TAG = "AdminViewModel"
    }
}
