package com.example.messenger.testutil.fakes

import com.example.messenger.domain.repository.ILocalRepository

class FakeLocalRepository : ILocalRepository {

    var resetCount = 0

    override suspend fun resetDB() {
        resetCount++
    }
}
