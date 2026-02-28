package com.tyoii.flvplayer.ui

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.tyoii.flvplayer.data.drive.DriveRepository
import com.tyoii.flvplayer.ui.browser.BrowserScreen
import com.tyoii.flvplayer.ui.browser.BrowserViewModel
import com.tyoii.flvplayer.ui.history.HistoryScreen
import com.tyoii.flvplayer.ui.login.LoginScreen
import com.tyoii.flvplayer.ui.player.PlayerScreen
import com.tyoii.flvplayer.ui.player.PlayerViewModel
import com.tyoii.flvplayer.ui.theme.FLVPlayerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var driveRepo: DriveRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check if already signed in
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            driveRepo.initDrive(account.email ?: "")
        }

        setContent {
            FLVPlayerTheme {
                MainApp(
                    isSignedIn = driveRepo.isSignedIn(),
                    driveRepo = driveRepo
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    isSignedIn: Boolean,
    driveRepo: DriveRepository
) {
    val navController = rememberNavController()
    var signedIn by remember { mutableStateOf(isSignedIn) }
    var isSigningIn by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as Activity

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isSigningIn = false
        if (result.resultCode == Activity.RESULT_OK) {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data).result
            if (account != null) {
                driveRepo.initDrive(account.email ?: "")
                signedIn = true
            }
        }
    }

    if (!signedIn) {
        LoginScreen(
            onSignIn = {
                isSigningIn = true
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .requestScopes(Scope(DriveScopes.DRIVE_READONLY))
                    .build()
                val client = GoogleSignIn.getClient(activity as Activity, gso)
                signInLauncher.launch(client.signInIntent)
            },
            isLoading = isSigningIn
        )
    } else {
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

        // Don't show bottom bar during playback
        val showBottomBar = currentRoute != "player/{fileId}/{fileName}"

        Scaffold(
            bottomBar = {
                if (showBottomBar && currentRoute != null && !currentRoute.startsWith("player")) {
                    NavigationBar {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Folder, "文件") },
                            label = { Text("文件") },
                            selected = currentRoute == "browser",
                            onClick = {
                                navController.navigate("browser") {
                                    popUpTo("browser") { inclusive = true }
                                }
                            }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.History, "历史") },
                            label = { Text("历史") },
                            selected = currentRoute == "history",
                            onClick = {
                                navController.navigate("history") {
                                    popUpTo("browser")
                                }
                            }
                        )
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "browser",
                modifier = if (showBottomBar && currentRoute != null && !currentRoute.startsWith("player"))
                    Modifier.padding(padding) else Modifier
            ) {
                composable("browser") {
                    val viewModel: BrowserViewModel = hiltViewModel()
                    val state by viewModel.state.collectAsStateWithLifecycle()

                    LaunchedEffect(Unit) {
                        if (state.files.isEmpty() && !state.isLoading) {
                            viewModel.loadFolder()
                        }
                    }

                    BrowserScreen(
                        state = state,
                        onFolderClick = { folder ->
                            viewModel.loadFolder(folder.id, folder.name)
                        },
                        onFileClick = { file ->
                            navController.navigate(
                                "player/${file.id}/${java.net.URLEncoder.encode(file.name, "UTF-8")}"
                            )
                        },
                        onPathClick = { index -> viewModel.navigateToPath(index) },
                        onSearch = { query -> viewModel.search(query) },
                        onBack = { viewModel.goBack() },
                        onSignOut = {
                            driveRepo.signOut()
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestEmail()
                                .requestScopes(Scope(DriveScopes.DRIVE_READONLY))
                                .build()
                            GoogleSignIn.getClient(activity as Activity, gso).signOut()
                            signedIn = false
                        }
                    )
                }

                composable("history") {
                    val viewModel: HistoryViewModel = hiltViewModel()
                    val historyList by viewModel.historyList.collectAsStateWithLifecycle(initialValue = emptyList())

                    HistoryScreen(
                        historyList = historyList,
                        onItemClick = { item ->
                            navController.navigate(
                                "player/${item.fileId}/${java.net.URLEncoder.encode(item.fileName, "UTF-8")}"
                            )
                        },
                        onClearAll = { viewModel.clearAll() }
                    )
                }

                composable("player/{fileId}/{fileName}") { backStackEntry ->
                    val fileId = backStackEntry.arguments?.getString("fileId") ?: ""
                    val fileName = java.net.URLDecoder.decode(
                        backStackEntry.arguments?.getString("fileName") ?: "", "UTF-8"
                    )

                    val viewModel: PlayerViewModel = hiltViewModel()
                    val state by viewModel.state.collectAsStateWithLifecycle()

                    LaunchedEffect(fileId) {
                        viewModel.prepare(fileId, fileName)
                    }

                    PlayerScreen(
                        state = state,
                        onBack = { navController.popBackStack() },
                        onPlayingChanged = { viewModel.onPlaying(it) },
                        onProgress = { pos, dur -> viewModel.onProgress(pos, dur) },
                        onError = { viewModel.onError(it) },
                        onSpeedChange = { viewModel.setPlaybackSpeed(it) },
                        onSaveProgress = { viewModel.saveProgress() }
                    )
                }
            }
        }
    }
}

@Composable
fun LocalActivity(): ComponentActivity {
    return androidx.compose.ui.platform.LocalContext.current as ComponentActivity
}
