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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.drawit.app.AppViewModel
import com.drawit.app.R
import com.drawit.app.ui.celebrate.CelebrationOverlay
import com.drawit.app.ui.celebrate.StreakNoticeOverlay
import com.drawit.app.ui.celebrate.WelcomeOverlay
import com.drawit.app.ui.components.DrawItBottomBar
import com.drawit.app.ui.components.NavItem
import com.drawit.app.ui.screens.AddDrawingScreen
import com.drawit.app.ui.screens.AlbumScreen
import com.drawit.app.ui.screens.DrawingDetailScreen
import com.drawit.app.ui.screens.InspirationScreen
import com.drawit.app.ui.screens.ProfileScreen
import com.drawit.app.ui.screens.ReminderScreen
import com.drawit.app.ui.screens.SettingsScreen
import com.drawit.app.ui.screens.ShopScreen
import com.drawit.app.ui.screens.StreakScreen
import com.drawit.app.ui.screens.TodayScreen
import com.drawit.app.ui.theme.DI

object Routes {
    const val TODAY = "today"
    const val INSPIRATION = "inspiration"
    const val ALBUM = "album"
    const val REMINDER = "reminder"
    const val PROFILE = "profile"
    const val ADD = "add"
    const val DETAIL = "detail"
    const val STREAK = "streak"
    const val SETTINGS = "settings"
    const val SHOP = "shop"
}

private val navItems = listOf(
    NavItem(Routes.TODAY, "Today", R.drawable.ic_home) { DI.colors.brand },
    NavItem(Routes.INSPIRATION, "Inspire", R.drawable.ic_bulb) { DI.colors.goldLip },
    NavItem(Routes.ALBUM, "Album", R.drawable.ic_album) { DI.colors.purple },
    NavItem(Routes.REMINDER, "Remind", R.drawable.ic_bell) { DI.colors.ice },
    NavItem(Routes.PROFILE, "Me", R.drawable.ic_profile) { DI.colors.green }
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

    val celebration by viewModel.celebration.collectAsStateWithLifecycle()
    val welcome by viewModel.welcome.collectAsStateWithLifecycle()
    val notice by viewModel.streakNotice.collectAsStateWithLifecycle()
    val look by viewModel.look.collectAsStateWithLifecycle()
    val drawings by viewModel.drawings.collectAsStateWithLifecycle()
    val streak by viewModel.streak.collectAsStateWithLifecycle()

    LaunchedEffect(openAddOnStart) {
        if (openAddOnStart) {
            navController.navigate(Routes.ADD) { launchSingleTop = true }
            onAddRequestHandled()
        }
    }

    Box(Modifier.fillMaxSize().background(DI.colors.background)) {
        Column(Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Routes.TODAY,
                modifier = Modifier.weight(1f),
                enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { it / 16 } },
                exitTransition = { fadeOut(tween(160)) },
                popEnterTransition = { fadeIn(tween(220)) },
                popExitTransition = { fadeOut(tween(160)) }
            ) {
                composable(Routes.TODAY) {
                    TodayScreen(
                        viewModel = viewModel,
                        onAdd = { navController.navigate(Routes.ADD) },
                        onOpenAlbum = { navController.switchTab(Routes.ALBUM) },
                        onOpenInspiration = { navController.switchTab(Routes.INSPIRATION) },
                        onOpenReminder = { navController.switchTab(Routes.REMINDER) },
                        onOpenStreak = { navController.navigate(Routes.STREAK) },
                        onOpenProfile = { navController.switchTab(Routes.PROFILE) },
                        onOpenShop = { navController.navigate(Routes.SHOP) },
                        onOpenDrawing = { navController.navigate("${Routes.DETAIL}/$it") }
                    )
                }
                composable(Routes.INSPIRATION) { InspirationScreen(viewModel) }
                composable(Routes.ALBUM) {
                    AlbumScreen(
                        viewModel = viewModel,
                        onAdd = { navController.navigate(Routes.ADD) },
                        onOpenDrawing = { navController.navigate("${Routes.DETAIL}/$it") }
                    )
                }
                composable(Routes.REMINDER) { ReminderScreen(viewModel) }
                composable(Routes.PROFILE) {
                    ProfileScreen(
                        viewModel = viewModel,
                        onOpenShop = { navController.navigate(Routes.SHOP) },
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                        onOpenStreak = { navController.navigate(Routes.STREAK) }
                    )
                }
                composable(
                    route = Routes.ADD,
                    enterTransition = { slideInVertically(tween(360, easing = FastOutSlowInEasing)) { it / 5 } + fadeIn(tween(240)) },
                    popExitTransition = { slideOutVertically(tween(300, easing = FastOutSlowInEasing)) { it / 5 } + fadeOut(tween(200)) }
                ) {
                    // The celebration overlay takes over as soon as the save lands.
                    AddDrawingScreen(viewModel = viewModel, onClose = { navController.popBackStack() }, onSaved = { navController.popBackStack() })
                }
                composable(Routes.STREAK, enterTransition = secondaryEnter, popExitTransition = secondaryExit) {
                    StreakScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                }
                composable(Routes.SHOP, enterTransition = secondaryEnter, popExitTransition = secondaryExit) {
                    ShopScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                }
                composable(Routes.SETTINGS, enterTransition = secondaryEnter, popExitTransition = secondaryExit) {
                    SettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                }
                composable("${Routes.DETAIL}/{id}", enterTransition = secondaryEnter, popExitTransition = secondaryExit) { entry ->
                    DrawingDetailScreen(
                        viewModel = viewModel,
                        drawingId = entry.arguments?.getString("id").orEmpty(),
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            AnimatedVisibility(
                visible = topLevel,
                enter = slideInVertically(tween(280, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(200)),
                exit = slideOutVertically(tween(220, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(150))
            ) {
                DrawItBottomBar(
                    items = navItems,
                    selectedRoute = currentRoute,
                    onSelect = { route -> if (route != currentRoute) navController.switchTab(route) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Overlays, most important last so it sits on top.
        notice?.let { pending ->
            if (celebration == null && welcome == null) {
                StreakNoticeOverlay(pending, onDone = { viewModel.clearStreakNotice() })
            }
        }
        welcome?.let { gift ->
            if (!showSplash) {
                WelcomeOverlay(
                    gift,
                    onShop = {
                        viewModel.clearWelcome()
                        navController.navigate(Routes.SHOP)
                    },
                    onDone = { viewModel.clearWelcome() }
                )
            }
        }
        celebration?.let { c ->
            CelebrationOverlay(
                celebration = c,
                look = look,
                drawn = remember(drawings) { viewModel.drawnDays(drawings) },
                frozen = streak.frozenDays,
                onDone = { viewModel.clearCelebration() }
            )
        }

        AnimatedVisibility(visible = showSplash, enter = fadeIn(tween(0)), exit = fadeOut(tween(220))) {
            SplashScreen(onFinished = { showSplash = false })
        }
    }
}

private val secondaryEnter: androidx.compose.animation.AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> androidx.compose.animation.EnterTransition =
    { slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { it / 6 } + fadeIn(tween(220)) }

private val secondaryExit: androidx.compose.animation.AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> androidx.compose.animation.ExitTransition =
    { slideOutHorizontally(tween(280, easing = FastOutSlowInEasing)) { it / 6 } + fadeOut(tween(180)) }

/** Moves between bottom-bar tabs, keeping each tab's own scroll state. */
private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(Routes.TODAY) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
