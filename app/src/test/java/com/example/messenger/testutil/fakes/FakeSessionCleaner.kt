package com.example.messenger.testutil.fakes

import com.example.messenger.domain.service.ISessionCleaner

class FakeSessionCleaner : ISessionCleaner {

    var loggedOutUid: String? = null
    var logoutCalls = 0
    var deletedUid: String? = null
    var deleteCalls = 0

    override suspend fun clearOnLogout(uid: String?) {
        logoutCalls++
        loggedOutUid = uid
    }

    override suspend fun clearOnDelete(uid: String?) {
        deleteCalls++
        deletedUid = uid
    }
}
