package com.example.messenger.data.local.obx

import io.objectbox.query.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

fun <T> Query<T>.asFlow(): Flow<List<T>> = callbackFlow {
    val query = this@asFlow
    val subscription = subscribe().observer { data -> trySend(data) }
    awaitClose {
        subscription.cancel()
        query.close()
    }
}

fun <T> queryFlow(build: () -> Query<T>): Flow<List<T>> = callbackFlow {
    val query = build()
    val subscription = query.subscribe().observer { data -> trySend(data) }
    awaitClose {
        subscription.cancel()
        query.close()
    }
}
