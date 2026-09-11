package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.VipSubscriptionScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ChatViewModel

enum class AppScreen {
    CHAT,
    VIP_SUBSCRIPTION
}

class MainActivity : ComponentActivity() {

    private val chatViewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MainNavigation(viewModel = chatViewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun MainNavigation(viewModel: ChatViewModel) {
    var currentScreen by remember { mutableStateOf(AppScreen.CHAT) }

    when (currentScreen) {
        AppScreen.CHAT -> {
            ChatScreen(
                viewModel = viewModel,
                onNavigateToVip = { currentScreen = AppScreen.VIP_SUBSCRIPTION }
            )
        }
        AppScreen.VIP_SUBSCRIPTION -> {
            BackHandler {
                currentScreen = AppScreen.CHAT
            }
            VipSubscriptionScreen(
                viewModel = viewModel,
                onNavigateBack = { currentScreen = AppScreen.CHAT }
            )
        }
    }
}
