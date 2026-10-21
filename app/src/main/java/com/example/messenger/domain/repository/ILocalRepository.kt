package com.example.messenger.domain.repository

interface ILocalRepository {
    suspend fun resetDB()
}
