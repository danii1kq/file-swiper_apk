package com.danni.fileswiper

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.danni.fileswiper.data.model.SessionRequest
import com.danni.fileswiper.ui.setup.SetupScreen
import com.danni.fileswiper.ui.swipe.SwipeScreen
import com.danni.fileswiper.ui.trash.TrashScreen
import com.danni.fileswiper.viewmodel.SetupViewModel
import com.danni.fileswiper.viewmodel.SwipeViewModel

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val setupViewModel: SetupViewModel = viewModel()
    val swipeViewModel: SwipeViewModel = viewModel()

    NavHost(navController = navController, startDestination = "setup") {
        composable("setup") {
            SetupScreen(
                onSessionReady = { request: SessionRequest ->
                    swipeViewModel.startSession(
                        files = request.files,
                        isMonthly = request.isMonthly,
                        monthKey = request.monthKey,
                        monthLabel = request.monthLabel
                    )
                    navController.navigate("swipe") { launchSingleTop = true }
                },
                viewModel = setupViewModel
            )
        }
        composable("swipe") {
            SwipeScreen(
                onFinished = {
                    navController.navigate("trash") {
                        popUpTo("swipe") { inclusive = true }
                    }
                },
                onFinishRequested = {
                    swipeViewModel.abandonSession()
                    navController.navigate("trash") {
                        popUpTo("swipe") { inclusive = true }
                    }
                },
                // Mid-session shortcut: peek the trash and come back to the
                // same queue afterwards (session state lives in the ViewModel).
                onViewTrash = {
                    navController.navigate("trash") { launchSingleTop = true }
                },
                viewModel = swipeViewModel
            )
        }
        composable("trash") {
            TrashScreen(
                onHomeRequested = {
                    // Back goes wherever it came from: the ongoing swipe
                    // session when peeking mid-session, or the setup screen
                    // after a finished session.
                    if (!navController.popBackStack()) {
                        navController.navigate("setup")
                    }
                },
                viewModel = swipeViewModel
            )
        }
    }
}
