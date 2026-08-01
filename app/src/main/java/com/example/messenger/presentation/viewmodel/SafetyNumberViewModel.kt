package com.example.messenger.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.messenger.data.crypto.IdentityKeyStore
import com.example.messenger.data.crypto.PeerKeyRegistry
import com.example.messenger.data.crypto.SafetyNumber
import com.example.messenger.data.remote.auth.FirebaseAuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SafetyNumberState(
    val available: Boolean = false,
    val partnerId: String? = null,
    val digits: String = "",
    val verified: Boolean = false,
)

@HiltViewModel
class SafetyNumberViewModel @Inject constructor(
    private val identityKeyStore: IdentityKeyStore,
    private val peerKeyRegistry: PeerKeyRegistry,
    private val authService: FirebaseAuthService,
) : ViewModel() {

    private val _state = MutableStateFlow(SafetyNumberState())
    val state: StateFlow<SafetyNumberState> = _state.asStateFlow()

    fun load(partnerId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val myUid = authService.getCurrentUserId() ?: return@launch
            val peer = peerKeyRegistry.ensurePeerKey(partnerId)
            if (peer == null) {
                _state.value = SafetyNumberState(available = false, partnerId = partnerId)
                return@launch
            }
            val identity = identityKeyStore.getOrCreate(myUid)
            _state.value = SafetyNumberState(
                available = true,
                partnerId = partnerId,
                digits = SafetyNumber.compute(myUid, identity.publicKey, partnerId, peer.publicKey),
                verified = peer.verified,
            )
        }
    }

    fun setVerified(verified: Boolean) {
        val partnerId = _state.value.partnerId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            peerKeyRegistry.setVerified(partnerId, verified)
            _state.update { it.copy(verified = verified) }
        }
    }
}
