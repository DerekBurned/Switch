package com.example.messenger.di

import android.content.Context
import com.example.messenger.data.crypto.IdentityKeyStore
import com.example.messenger.data.crypto.KeyWrapper
import com.example.messenger.data.crypto.KeystoreKeyWrapper
import com.example.messenger.data.crypto.ObxParticipantResolver
import com.example.messenger.data.crypto.ParticipantResolver
import com.example.messenger.data.local.obx.ObxConversation
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.objectbox.Box
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CryptoModule {

    @Provides
    @Singleton
    fun provideKeyWrapper(): KeyWrapper = KeystoreKeyWrapper()

    @Provides
    @Singleton
    fun provideIdentityKeyStore(
        @ApplicationContext context: Context,
        keyWrapper: KeyWrapper,
    ): IdentityKeyStore = IdentityKeyStore(File(context.filesDir, "e2ee"), keyWrapper)

    @Provides
    @Singleton
    fun provideParticipantResolver(
        conversationBox: Box<ObxConversation>,
        firestore: FirebaseFirestore,
    ): ParticipantResolver = ObxParticipantResolver(conversationBox, firestore)
}
