package com.example.chatfamiliar

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.chatfamiliar.ui.theme.ChatFamiliarTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException

private enum class AppTab(val route: String, val titleResource: Int, val iconResource: Int) {
    CHAT("chat", R.string.tab_chat, R.string.tab_chat_icon),
    VIDEO_CALL("video-call", R.string.tab_video_call, R.string.tab_video_call_icon),
    FAMILY("family", R.string.tab_family, R.string.tab_family_icon)
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChatFamiliarTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val auth = remember { FirebaseAuth.getInstance() }
                    val familyRepository = remember { FamilyRepository(auth) }
                    var familyId by remember { mutableStateOf<String?>(null) }
                    var isLoading by remember { mutableStateOf(true) }
                    var errorMessage by remember { mutableStateOf<String?>(null) }
                    var reloadKey by remember { mutableIntStateOf(0) }

                    LaunchedEffect(auth.currentUser?.uid, reloadKey) {
                        val currentUser = auth.currentUser
                        if (currentUser == null || !currentUser.isEmailVerified) {
                            if (currentUser != null) auth.signOut()
                            openLogin()
                            return@LaunchedEffect
                        }

                        isLoading = true
                        errorMessage = null
                        try {
                            familyRepository.ensureCurrentUserProfile()
                            familyId = familyRepository.getCurrentUserFamilyId()
                        } catch (exception: FirebaseFirestoreException) {
                            errorMessage = exception.localizedMessage
                                ?: context.getString(R.string.main_profile_load_error)
                        } catch (exception: IllegalStateException) {
                            errorMessage = exception.message
                                ?: context.getString(R.string.main_profile_load_error)
                        } finally {
                            isLoading = false
                        }
                    }

                    when {
                        isLoading -> LoadingScreen()
                        errorMessage != null -> ErrorScreen(
                            message = errorMessage!!,
                            onRetry = { reloadKey++ }
                        )
                        familyId == null -> FamilySetupScreen(
                            onFamilyReady = { family -> familyId = family.id }
                        )
                        else -> UserRoleState { role ->
                            AppNavigation(
                                familyId = familyId!!,
                                role = role,
                                onSignOut = {
                                    auth.signOut()
                                    openLogin()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun openLogin() {
        startActivity(
            Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }
}

@Composable
private fun AppNavigation(
    familyId: String,
    role: String?,
    onSignOut: () -> Unit
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    val selected = currentDestination?.hierarchy?.any {
                        it.route == tab.route
                    } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Text(stringResource(tab.iconResource)) },
                        label = { Text(stringResource(tab.titleResource)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppTab.CHAT.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppTab.CHAT.route) {
                ChatScreen(familyId = familyId)
            }
            composable(AppTab.VIDEO_CALL.route) {
                VideoCallScreen(
                    channelName = familyId,
                    onEndCall = { navController.navigate(AppTab.CHAT.route) }
                )
            }
            composable(AppTab.FAMILY.route) {
                FamilyScreen(
                    familyId = familyId,
                    role = role,
                    onSignOut = onSignOut
                )
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, color = MaterialTheme.colorScheme.error)
        Button(onClick = onRetry) {
            Text(stringResource(R.string.main_retry))
        }
    }
}
