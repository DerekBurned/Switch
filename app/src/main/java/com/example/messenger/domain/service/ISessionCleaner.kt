package com.example.messenger.domain.service

interface ISessionCleaner {
    suspend fun clearOnLogout(uid: String?)
}
