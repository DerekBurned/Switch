package com.example.messenger.di

import android.content.Context
import com.example.messenger.BuildConfig
import com.example.messenger.data.local.obx.MyObjectBox
import com.example.messenger.data.local.obx.ObxContactAlias
import com.example.messenger.data.local.obx.ObxConversation
import com.example.messenger.data.local.obx.ObxMessage
import com.example.messenger.data.local.obx.ObxPeerKey
import com.example.messenger.data.local.obx.ObxProfile
import com.example.messenger.data.local.obx.ObxRecentContact
import com.example.messenger.data.local.obx.ObxSyncQueueItem
import com.example.messenger.data.local.obx.ObxUser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.objectbox.Box
import io.objectbox.BoxStore
import io.objectbox.exception.DbSchemaException
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ObjectBoxModule {

    @Provides
    @Singleton
    fun provideBoxStore(@ApplicationContext context: Context): BoxStore =
        try {
            MyObjectBox.builder()
                .androidContext(context.applicationContext)
                .build()
        } catch (e: DbSchemaException) {
            if (!BuildConfig.DEBUG) throw e
            BoxStore.deleteAllFiles(context.applicationContext, null)
            MyObjectBox.builder()
                .androidContext(context.applicationContext)
                .build()
        }

    @Provides @Singleton
    fun provideMessageBox(store: BoxStore): Box<ObxMessage> = store.boxFor(ObxMessage::class.java)

    @Provides @Singleton
    fun provideUserBox(store: BoxStore): Box<ObxUser> = store.boxFor(ObxUser::class.java)

    @Provides @Singleton
    fun provideConversationBox(store: BoxStore): Box<ObxConversation> =
        store.boxFor(ObxConversation::class.java)

    @Provides @Singleton
    fun provideSyncQueueBox(store: BoxStore): Box<ObxSyncQueueItem> =
        store.boxFor(ObxSyncQueueItem::class.java)

    @Provides @Singleton
    fun provideProfileBox(store: BoxStore): Box<ObxProfile> = store.boxFor(ObxProfile::class.java)

    @Provides @Singleton
    fun provideContactAliasBox(store: BoxStore): Box<ObxContactAlias> =
        store.boxFor(ObxContactAlias::class.java)

    @Provides @Singleton
    fun providePeerKeyBox(store: BoxStore): Box<ObxPeerKey> = store.boxFor(ObxPeerKey::class.java)

    @Provides @Singleton
    fun provideRecentContactBox(store: BoxStore): Box<ObxRecentContact> =
        store.boxFor(ObxRecentContact::class.java)
}
