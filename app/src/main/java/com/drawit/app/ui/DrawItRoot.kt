package com.drawit.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.MaterialTheme
import com.drawit.app.AppViewModel
import com.drawit.app.R
import com.drawit.app.ui.components.DrawItBottomBar
import com.drawit.app.ui.components.NavItem
import com.drawit.app.ui.screens.AddDrawingScreen
import com.drawit.app.ui.screens.AlbumScreen
import com.drawit.app.ui.screens.DrawingDetailScreen
import com.drawit.app.ui.screens.InspirationScreen
import com.drawit.app.ui.screens.ReminderScreen
import com.drawit.app.ui.screens.TodayScreen

object Routes {
    const val TODAY = "today"
    const val INSPIRATION = "inspiration"
    const val ALBUM = "album"
    const val REMINDER = "reminder"
    const val ADD = "add"
    const val DETAIL = "detail"
}

private val navItems = listOf(
    NavItem(Routes.TODAY, "Today", R.drawable.ic_pencil),
    NavItem(Routes.INSPIRATION, "Inspire", R.drawable.ic_spark),
    NavItem(Routes.ALBUM, "Album", R.drawable.ic_album),
    NavItem(Routes.REMINDER, "Reminder", R.drawable.ic_bell)
)

@Composable
fun DrawItRoot(
    viewModel: AppViewModel,
    openAddOnStart: Boolean,
    onAddRequestHandled: () -> Unit
) {
    var showSplash by remember { mutableStateOf(true) }
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.TODAY
    val topLevel = navItems.any { it.route == currentRoute }

    LaunchedEffect(openAddOnStart) {
        if (openAddOnStart) {
            navController.navigate(Routes.ADD) { launchSingleTop = true }
            onAddRequestHandled()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        NavHost(
            navController = navController,
            startDestination = Routes.TODAY,
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                fadeIn(tween(260)) + slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { it / 14 }
            },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(260)) },
            popExitTransition = { fadeOut(tween(180)) }
        ) {
            composable(Routes.TODAY) {
                TodayScreen(
                    viewModel = viewModel,
                    onAdd = { navController.navigate(Routes.ADD) },
                    onOpenAlbum = { navController.navigate(Routes.ALBUM) { launchSingleTop = true } },
                    onOpenInspiration = { navController.navigate(Routes.INSPIRATION) { launchSingleTop = true } },
                    onOpenReminder = { navController.navigate(Routes.REMINDER) { launchSingleTop = true } },
                    onOpenDrawing = { navController.navigate("${Routes.DETAIL}/$it") }
                )
            }
            composable(Routes.INSPIRATION) { InspirationScreen() }
            composable(Routes.ALBUM) {
                AlbumScreen(
                    viewModel = viewModel,
                    onAdd = { navController.navigate(Routes.ADD) },
                    onOpenDrawing = { navController.navigate("${Routes.DETAIL}/$it") }
                )
            }
            composable(Routes.REMINDER) { ReminderScreen(viewModel = viewModel) }
            composable(
                route = Routes.ADD,
                enterTransition = {
                    slideInVertically(tween(360, easing = FastOutSlowInEasing)) { it / 5 } + fadeIn(tween(260))
                },
                popExitTransition = {
                    slideOutVertically(tween(300, easing = FastOutSlowInEasing)) { it / 5 } + fadeOut(tween(220))
                }
            ) {
                AddDrawingScreen(
                    viewModel = viewModel,
                    onClose = { navController.popBackStack() },
                    onSaved = {
                        navController.popBackStack()
                        navController.navigate(Routes.ALBUM) { launchSingleTop = true }
                    }
                )
            }
            composable(
                route = "${Routes.DETAIL}/{id}",
                enterTransition = {
                    slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { it / 6 } + fadeIn(tween(240))
                },
                popExitTransition = {
                    slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { it / 6 } + fadeOut(tween(200))
                }
            ) { entry ->
                DrawingDetailScreen(
                    viewModel = viewModel,
                    drawingId = entry.arguments?.getString("id").orEmpty(),
                    onBack = { navController.popBackStack() }
                )
            }
        }

        AnimatedVisibility(
            visible = topLevel,
            enter = slideInVertically(tween(320, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(220)),
            exit = slideOutVertically(tween(240, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(160)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            DrawItBottomBar(
                items = navItems,
                selectedRoute = currentRoute,
                onSelect = { route ->
                    if (route != currentRoute) {
                        navController.navigate(route) {
                            popUpTo(Routes.TODAY) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            )
        }

        AnimatedVisibility(
            visible = showSplash,
            enter = fadeIn(tween(0)),
            exit = fadeOut(tween(220))
        ) {
            SplashScreen(onFinished = { showSplash = false })
        }
    }
}
