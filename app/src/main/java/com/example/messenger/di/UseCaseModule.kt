package com.example.messenger.di

import com.example.messenger.data.sync.NetworkObserver
import com.example.messenger.domain.repository.IAuthRepository
import com.example.messenger.domain.repository.IClipRepository
import com.example.messenger.domain.repository.IConversationRepository
import com.example.messenger.domain.repository.ILocalRepository
import com.example.messenger.domain.repository.IMessageRepository
import com.example.messenger.domain.repository.ISyncRepository
import com.example.messenger.domain.repository.IUserRepository
import com.example.messenger.domain.service.IAuthFlowGate
import com.example.messenger.domain.service.ISessionCleaner
import com.example.messenger.domain.usecase.auth.DeleteAccountUseCase
import com.example.messenger.domain.usecase.auth.GetCurrentUserUseCase
import com.example.messenger.domain.usecase.auth.LinkPhoneUseCase
import com.example.messenger.domain.usecase.auth.LoginWithPhoneNumberUseCase
import com.example.messenger.domain.usecase.auth.LogoutUseCase
import com.example.messenger.domain.usecase.auth.ObserveAuthStateUseCase
import com.example.messenger.domain.usecase.auth.ObserveSessionStateUseCase
import com.example.messenger.domain.usecase.conversation.ClearConversationForMeUseCase
import com.example.messenger.domain.usecase.conversation.CreateConversationUseCase
import com.example.messenger.domain.usecase.conversation.DeleteConversationForEveryoneUseCase
import com.example.messenger.domain.usecase.conversation.DeleteConversationUseCase
import com.example.messenger.domain.usecase.conversation.GetConversationsUseCase
import com.example.messenger.domain.usecase.conversation.MarkConversationAsReadUseCase
import com.example.messenger.domain.usecase.conversation.MarkConversationAsUnreadUseCase
import com.example.messenger.domain.usecase.conversation.SyncConversationsUseCase
import com.example.messenger.domain.usecase.message.DeleteMessageUseCase
import com.example.messenger.domain.usecase.message.GetMessagesUseCase
import com.example.messenger.domain.usecase.message.MarkMessageAsReadUseCase
import com.example.messenger.domain.usecase.message.SendMessageUseCase
import com.example.messenger.domain.usecase.message.SyncMessagesUseCase
import com.example.messenger.domain.usecase.sync.BackfillRecentHistoryUseCase
import com.example.messenger.domain.usecase.sync.ObserveNetworkUseCase
import com.example.messenger.domain.usecase.sync.ProcessSyncQueueUseCase
import com.example.messenger.domain.usecase.sync.SyncAllDataUseCase
import com.example.messenger.domain.usecase.user.GetUserByIdUseCase
import com.example.messenger.domain.usecase.user.SearchUsersUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    @Provides
    fun provideGetCurrentUserUseCase(
        repository: IAuthRepository
    ): GetCurrentUserUseCase {
        return GetCurrentUserUseCase(repository)
    }
    @Provides
    fun provideLoginWithPhoneNumberUseCase(
        repository: IAuthRepository
    ): LoginWithPhoneNumberUseCase{
        return LoginWithPhoneNumberUseCase(repository)
    }
    @Provides
    fun provideDeleteAccountUseCase(
        authRepository: IAuthRepository,
        localRepository: ILocalRepository,
        sessionCleaner: ISessionCleaner,
    ): DeleteAccountUseCase {
        return DeleteAccountUseCase(authRepository, localRepository, sessionCleaner)
    }

    @Provides
    fun provideBackfillRecentHistoryUseCase(
        conversationRepository: IConversationRepository,
        messageRepository: IMessageRepository,
    ): BackfillRecentHistoryUseCase {
        return BackfillRecentHistoryUseCase(conversationRepository, messageRepository)
    }

    @Provides
    fun provideLogoutUseCase(
        authRepository: IAuthRepository,
        userRepository: IUserRepository,
        localRepository: ILocalRepository,
        sessionCleaner: ISessionCleaner,
    ): LogoutUseCase {
        return LogoutUseCase(authRepository, userRepository, localRepository, sessionCleaner)
    }

    @Provides
    fun provideLinkPhoneUseCase(
        authRepository: IAuthRepository
    ): LinkPhoneUseCase {
        return LinkPhoneUseCase(authRepository)
    }

    @Provides
    fun provideObserveSessionStateUseCase(
        observeAuthState: ObserveAuthStateUseCase,
        userRepository: IUserRepository,
        authFlowGate: IAuthFlowGate,
    ): ObserveSessionStateUseCase {
        return ObserveSessionStateUseCase(observeAuthState, userRepository, authFlowGate)
    }

    @Provides
    fun provideObserveAuthStateUseCase(
        authRepository: IAuthRepository
    ): ObserveAuthStateUseCase {
        return ObserveAuthStateUseCase(authRepository)
    }

    @Provides
    fun provideGetConversationsUseCase(
        conversationRepository: IConversationRepository
    ): GetConversationsUseCase {
        return GetConversationsUseCase(conversationRepository)
    }

    @Provides
    fun provideDeleteConversationUseCase(
        conversationRepository: IConversationRepository
    ): DeleteConversationUseCase {
        return DeleteConversationUseCase(conversationRepository)
    }

    @Provides
    fun provideDeleteConversationForEveryoneUseCase(
        conversationRepository: IConversationRepository
    ): DeleteConversationForEveryoneUseCase {
        return DeleteConversationForEveryoneUseCase(conversationRepository)
    }

    @Provides
    fun provideClearConversationForMeUseCase(
        conversationRepository: IConversationRepository
    ): ClearConversationForMeUseCase {
        return ClearConversationForMeUseCase(conversationRepository)
    }

    @Provides
    fun provideSyncConversationsUseCase(
        conversationRepository: IConversationRepository
    ): SyncConversationsUseCase {
        return SyncConversationsUseCase(conversationRepository)
    }

    @Provides
    fun provideMarkConversationAsReadUseCase(
        conversationRepository: IConversationRepository
    ): MarkConversationAsReadUseCase {
        return MarkConversationAsReadUseCase(conversationRepository)
    }

    @Provides
    fun provideMarkConversationAsUnreadUseCase(
        conversationRepository: IConversationRepository
    ): MarkConversationAsUnreadUseCase {
        return MarkConversationAsUnreadUseCase(conversationRepository)
    }

    @Provides
    fun provideCreateConversationUseCase(
        conversationRepository: IConversationRepository,
        
        getCurrentUserUseCase: GetCurrentUserUseCase
    ): CreateConversationUseCase {
        return CreateConversationUseCase(conversationRepository, getCurrentUserUseCase)
    }

    @Provides
    fun provideDeleteMessageUseCase(
        messageRepository: IMessageRepository,
        clipRepository: IClipRepository,
    ): DeleteMessageUseCase {
        return DeleteMessageUseCase(messageRepository, clipRepository)
    }

    @Provides
    fun provideGetMessagesUseCase(
        messageRepository: IMessageRepository
    ): GetMessagesUseCase {
        return GetMessagesUseCase(messageRepository)
    }
    @Provides
    fun provideSendMessageUseCase(
        messageRepository: IMessageRepository,
        getCurrentUserUseCase: GetCurrentUserUseCase
    ): SendMessageUseCase {
        return SendMessageUseCase(messageRepository, getCurrentUserUseCase)
    }

    @Provides
    fun provideMarkMessageAsReadUseCase(
        messageRepository: IMessageRepository
    ): MarkMessageAsReadUseCase {
        return MarkMessageAsReadUseCase(messageRepository)
    }

    @Provides
    fun provideSyncMessagesUseCase(
        messageRepository: IMessageRepository
    ): SyncMessagesUseCase {
        return SyncMessagesUseCase(messageRepository)
    }

    @Provides
    fun provideGetUserByIdUseCase(
        userRepository: IUserRepository
    ): GetUserByIdUseCase {
        return GetUserByIdUseCase(userRepository)
    }

    @Provides
    fun provideSearchUsersUseCase(
        userRepository: IUserRepository
    ): SearchUsersUseCase {
        return SearchUsersUseCase(userRepository)
    }

    @Provides
    fun provideProcessSyncQueueUseCase(
        syncRepository: ISyncRepository
    ): ProcessSyncQueueUseCase {
        return ProcessSyncQueueUseCase(syncRepository)
    }

    @Provides
    fun provideSyncAllDataUseCase(
        syncRepository: ISyncRepository
    ): SyncAllDataUseCase {
        return SyncAllDataUseCase(syncRepository)
    }

    @Provides
    fun provideObserveNetworkUseCase(
        networkObserver: NetworkObserver
    ): ObserveNetworkUseCase {
        return ObserveNetworkUseCase(networkObserver)
    }

}
