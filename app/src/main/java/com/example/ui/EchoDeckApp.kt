package com.example.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Voicemail
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Voicemail
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.ui.screens.DeckScreen
import com.example.ui.screens.DialerScreen
import com.example.ui.screens.GreetingsScreen
import com.example.ui.screens.InboxScreen
import kotlinx.serialization.Serializable

@Serializable
object DeckRoute

@Serializable
object InboxRoute

@Serializable
object GreetingsRoute

@Serializable
object DialerRoute

enum class InitialTabTarget {
    DECK,
    INBOX,
    DIALER
}

private data class EchoNavItem(
    val route: Any,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun EchoDeckApp(
    initialTabTarget: InitialTabTarget = InitialTabTarget.DECK,
    prefilledDialNumber: String? = null,
    onConsumeIntentNavigation: () -> Unit = {},
    viewModel: EchoDeckViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    LaunchedEffect(initialTabTarget, prefilledDialNumber) {
        if (!prefilledDialNumber.isNullOrBlank()) {
            viewModel.setDialerNumber(prefilledDialNumber)
        }
        when (initialTabTarget) {
            InitialTabTarget.INBOX -> {
                navController.navigate(InboxRoute) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
                onConsumeIntentNavigation()
            }
            InitialTabTarget.DIALER -> {
                navController.navigate(DialerRoute) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
                onConsumeIntentNavigation()
            }
            InitialTabTarget.DECK -> Unit
        }
    }

    val navItems = listOf(
        EchoNavItem(
            route = DeckRoute,
            labelRes = R.string.nav_deck,
            selectedIcon = Icons.Filled.GraphicEq,
            unselectedIcon = Icons.Outlined.GraphicEq,
            testTag = "nav_deck"
        ),
        EchoNavItem(
            route = InboxRoute,
            labelRes = R.string.nav_inbox,
            selectedIcon = Icons.Filled.Voicemail,
            unselectedIcon = Icons.Outlined.Voicemail,
            testTag = "nav_inbox"
        ),
        EchoNavItem(
            route = GreetingsRoute,
            labelRes = R.string.nav_greetings,
            selectedIcon = Icons.Filled.RecordVoiceOver,
            unselectedIcon = Icons.Outlined.RecordVoiceOver,
            testTag = "nav_greetings"
        ),
        EchoNavItem(
            route = DialerRoute,
            labelRes = R.string.nav_dialer,
            selectedIcon = Icons.Filled.Dialpad,
            unselectedIcon = Icons.Outlined.Dialpad,
            testTag = "nav_dialer"
        )
    )

    fun navigateToTab(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        navItems.forEach { item ->
                            val selected = currentDestination?.hasRoute(item.route::class) == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(item.route) },
                                icon = {
                                    NavItemIcon(
                                        item = item,
                                        selected = selected,
                                        unreadCount = uiState.unreadCount
                                    )
                                },
                                label = { Text(stringResource(item.labelRes)) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail {
                        navItems.forEach { item ->
                            val selected = currentDestination?.hasRoute(item.route::class) == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(item.route) },
                                icon = {
                                    NavItemIcon(
                                        item = item,
                                        selected = selected,
                                        unreadCount = uiState.unreadCount
                                    )
                                },
                                label = { Text(stringResource(item.labelRes)) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = DeckRoute,
                    modifier = Modifier.weight(1f)
                ) {
                    composable<DeckRoute> {
                        DeckScreen(
                            uiState = uiState,
                            onRefreshReadiness = viewModel::refreshReadiness,
                            onSetMachineEnabled = viewModel::setMachineEnabled,
                            onSetRingsBeforeAnswer = viewModel::setRingsBeforeAnswer,
                            onSetMaxRecordingSeconds = viewModel::setMaxRecordingSeconds,
                            onSetSpeakerphoneCapture = viewModel::setSpeakerphoneCapture,
                            onSetPlayBeepAfterGreeting = viewModel::setPlayBeepAfterGreeting,
                            onStartAcousticTest = viewModel::startAcousticSelfTest,
                            onStopAcousticTest = viewModel::stopAcousticSelfTest,
                            onOpenGreetings = { navigateToTab(GreetingsRoute) }
                        )
                    }
                    composable<InboxRoute> {
                        InboxScreen(
                            uiState = uiState,
                            onTogglePlayPause = viewModel::toggleMessagePlayback,
                            onSeekPlayback = viewModel::seekPlayback,
                            onToggleSpeaker = viewModel::togglePlaybackSpeaker,
                            onToggleStar = viewModel::toggleStarMessage,
                            onToggleRead = viewModel::markMessageRead,
                            onUpdateNotes = viewModel::updateMessageNotes,
                            onDeleteMessage = viewModel::deleteMessage,
                            onCreateShareIntent = viewModel::createShareAudioIntent,
                            onCallBack = { number ->
                                viewModel.placeCall(number)
                            }
                        )
                    }
                    composable<GreetingsRoute> {
                        GreetingsScreen(
                            uiState = uiState,
                            onActivateGreeting = viewModel::activateGreeting,
                            onPreviewGreeting = viewModel::previewGreeting,
                            onDeleteGreeting = viewModel::deleteGreeting,
                            onStartRecording = viewModel::startRecordingNewGreeting,
                            onStopRecording = viewModel::stopRecordingNewGreeting,
                            onPreviewPending = viewModel::previewPendingRecording,
                            onSavePendingRecording = { title ->
                                viewModel.savePendingRecordedGreeting(title, activateImmediately = true)
                            },
                            onDiscardPending = viewModel::discardPendingGreetingFile,
                            onSaveTtsGreeting = { title, script ->
                                viewModel.saveTtsGreeting(title, script, activateImmediately = true)
                            }
                        )
                    }
                    composable<DialerRoute> {
                        DialerScreen(
                            uiState = uiState,
                            onAppendChar = viewModel::appendDialerChar,
                            onBackspace = viewModel::backspaceDialer,
                            onClear = viewModel::clearDialer,
                            onPlaceCall = { number -> viewModel.placeCall(number) },
                            onSetScreenUnknown = viewModel::setScreenUnknownCallers,
                            onAddRule = viewModel::addScreeningRule,
                            onDeleteRule = viewModel::deleteScreeningRule
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavItemIcon(
    item: EchoNavItem,
    selected: Boolean,
    unreadCount: Int
) {
    val icon = if (selected) item.selectedIcon else item.unselectedIcon
    val label = stringResource(item.labelRes)
    if (item.route is InboxRoute && unreadCount > 0) {
        BadgedBox(
            badge = {
                Badge {
                    Text(if (unreadCount > 99) "99+" else unreadCount.toString())
                }
            }
        ) {
            Icon(imageVector = icon, contentDescription = label)
        }
    } else {
        Icon(imageVector = icon, contentDescription = label)
    }
}
