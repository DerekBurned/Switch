package com.example.messenger.presentation.navigation.graph

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.core.os.bundleOf
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

@Composable
internal fun ProvideNavArgs(vararg args: Pair<String, Any?>, content: @Composable () -> Unit) {
    val owner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    }
    val entries = args.toList()
    val wrapped = remember(owner, entries) {
        NavArgsViewModelStoreOwner(owner, bundleOf(*entries.toTypedArray()))
    }
    CompositionLocalProvider(LocalViewModelStoreOwner provides wrapped) {
        content()
    }
}

private class NavArgsViewModelStoreOwner(
    private val delegate: ViewModelStoreOwner,
    private val args: Bundle,
) : ViewModelStoreOwner, HasDefaultViewModelProviderFactory {

    private val base = delegate as HasDefaultViewModelProviderFactory

    override val viewModelStore: ViewModelStore
        get() = delegate.viewModelStore

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() = base.defaultViewModelProviderFactory

    override val defaultViewModelCreationExtras: CreationExtras
        get() = MutableCreationExtras(base.defaultViewModelCreationExtras).apply {
            this[DEFAULT_ARGS_KEY] = args
        }
}
