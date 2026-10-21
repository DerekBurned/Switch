package com.example.messenger.presentation.screens.calls

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.messenger.presentation.components.call.LocalCallBarInset
import com.example.messenger.presentation.components.common.LocalClipBarInset
import com.example.messenger.presentation.screens.calls.CallsScreenContent
import com.example.messenger.presentation.theme.LocalTabBarPosition
import com.example.messenger.presentation.theme.model.BarPosition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsScreen(
    onCallBack: (partnerId: String, partnerName: String, video: Boolean) -> Unit = { _, _, _ -> },
    viewModel: CallsViewModel = androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel(),
) {
    val calls by viewModel.calls.collectAsStateWithLifecycle()
    val barAtBottom = LocalTabBarPosition.current == BarPosition.BOTTOM
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Spacer(
            modifier = Modifier.height(
                (if (barAtBottom) 0.dp else 64.dp) + LocalCallBarInset.current + LocalClipBarInset.current,
            ),
        )
        CallsScreenContent(
            calls = calls,
            onCallBack = onCallBack,
            modifier = Modifier.weight(1f),
        )
        if (barAtBottom) {
            Spacer(modifier = Modifier.navigationBarsPadding().height(64.dp))
        }
    }
}
