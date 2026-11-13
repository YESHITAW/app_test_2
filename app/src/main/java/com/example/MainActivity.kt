package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.ui.EchoDeckApp
import com.example.ui.InitialTabTarget
import com.example.ui.theme.EchoDeckTheme

class MainActivity : ComponentActivity() {

    private var pendingTab by mutableStateOf(InitialTabTarget.DECK)
    private var pendingDialNumber by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        parseLaunchIntent(intent)

        setContent {
            EchoDeckTheme {
                EchoDeckApp(
                    initialTabTarget = pendingTab,
                    prefilledDialNumber = pendingDialNumber,
                    onConsumeIntentNavigation = {
                        pendingTab = InitialTabTarget.DECK
                        pendingDialNumber = null
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        parseLaunchIntent(intent)
    }

    private fun parseLaunchIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val data = intent.data
        if ((action == Intent.ACTION_DIAL || action == Intent.ACTION_VIEW) && data?.scheme == "tel") {
            pendingDialNumber = data.schemeSpecificPart.orEmpty()
            pendingTab = InitialTabTarget.DIALER
        } else if (action == Intent.ACTION_DIAL) {
            pendingTab = InitialTabTarget.DIALER
        } else if (intent.getStringExtra("open_tab") == "inbox") {
            pendingTab = InitialTabTarget.INBOX
        }
    }
}
