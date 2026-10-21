package com.example.messenger.data.remote.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrashlyticsUserSync @Inject constructor(
    private val auth: FirebaseAuth,
    private val crashlytics: FirebaseCrashlytics,
) {

    fun start() {
        auth.addAuthStateListener { firebaseAuth ->
            crashlytics.setUserId(firebaseAuth.currentUser?.uid.orEmpty())
        }
    }
}
