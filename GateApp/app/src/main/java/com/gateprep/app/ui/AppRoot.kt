package com.gateprep.app.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.collectAsState
import com.gateprep.app.AppViewModel
import com.gateprep.app.ui.home.HomeScreen
import com.gateprep.app.ui.placeholder.ChatScreen
import com.gateprep.app.ui.mock.MockListScreen
import com.gateprep.app.ui.mock.MockTestScreen
import com.gateprep.app.ui.placeholder.ProfileScreen
import com.gateprep.app.ui.practice.PracticeScreen
import com.gateprep.app.ui.subject.SubjectScreen
import com.gateprep.app.ui.theme.LocalExtra

private const val HOME = "home"
private const val MOCK = "mock"
private const val MOCK_TEST = "mocktest/{id}"
private const val CHAT = "chat"
private const val PROFILE = "profile"
private const val SUBJECT = "subject/{sid}"
private const val PRACTICE = "practice/{sid}/{mid}"

private val TAB_ROUTES = listOf(HOME, MOCK, CHAT, PROFILE)

private fun tabIndex(route: String?): Int = when (route) {
    HOME, SUBJECT -> 0          // the subject list lives under the Home tab
    MOCK -> 1
    CHAT -> 2
    PROFILE -> 3
    else -> -1
}

private fun isTab(route: String?) = route in TAB_ROUTES

@Composable
fun AppRoot(vm: AppViewModel = viewModel()) {
    val ex = LocalExtra.current
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = route == null || tabIndex(route) >= 0

    val dur = 380
    val ease = FastOutSlowInEasing
    val slide = AnimatedContentTransitionScope.SlideDirection

    Box(Modifier.fillMaxSize().background(ex.bg)) {
        NavHost(
            navController = nav,
            startDestination = HOME,
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                val from = initialState.destination.route; val to = targetState.destination.route
                if (isTab(from) && isTab(to)) {
                    val dir = if (tabIndex(to) > tabIndex(from)) slide.Left else slide.Right
                    slideIntoContainer(dir, tween(dur, easing = ease)) + fadeIn(tween(dur))
                } else {
                    slideIntoContainer(slide.Left, tween(dur, easing = ease)) + fadeIn(tween(dur))
                }
            },
            exitTransition = {
                val from = initialState.destination.route; val to = targetState.destination.route
                if (isTab(from) && isTab(to)) {
                    val dir = if (tabIndex(to) > tabIndex(from)) slide.Left else slide.Right
                    slideOutOfContainer(dir, tween(dur, easing = ease)) + fadeOut(tween(dur / 2))
                } else {
                    slideOutOfContainer(slide.Left, tween(dur, easing = ease), targetOffset = { it / 4 }) + fadeOut(tween(dur))
                }
            },
            popEnterTransition = {
                slideIntoContainer(slide.Right, tween(dur, easing = ease), initialOffset = { it / 4 }) + fadeIn(tween(dur))
            },
            popExitTransition = {
                slideOutOfContainer(slide.Right, tween(dur, easing = ease)) + fadeOut(tween(dur))
            },
        ) {
            composable(HOME) {
                HomeScreen(
                    vm = vm,
                    onOpenSubject = { nav.navigate("subject/$it") },
                    onResume = { s, m -> nav.navigate("practice/$s/$m") },
                )
            }
            composable(MOCK) { MockListScreen(vm) { id -> nav.navigate("mocktest/$id") } }
            composable(MOCK_TEST) { e ->
                MockTestScreen(vm, e.arguments?.getString("id").orEmpty(), onExit = { nav.popBackStack() })
            }
            composable(CHAT) { ChatScreen() }
            composable(PROFILE) { ProfileScreen(vm) }
            composable(SUBJECT) { e ->
                val sid = e.arguments?.getString("sid").orEmpty()
                SubjectScreen(
                    vm = vm, subjectId = sid,
                    onBack = { nav.popBackStack() },
                    onOpenModule = { mid -> nav.navigate("practice/$sid/$mid") },
                )
            }
            composable(PRACTICE) { e ->
                PracticeScreen(
                    vm = vm,
                    subjectId = e.arguments?.getString("sid").orEmpty(),
                    moduleId = e.arguments?.getString("mid").orEmpty(),
                    onBack = { nav.popBackStack() },
                )
            }
        }

        AnimatedVisibility(
            visible = showBar,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(320)) { it } + fadeIn(tween(320)),
            exit = slideOutVertically(tween(260)) { it } + fadeOut(tween(200)),
        ) {
            GateBottomBar(
                selected = tabIndex(route).coerceAtLeast(0),
                onSelect = { i ->
                    val target = TAB_ROUTES[i]
                    if (route != target) {
                        nav.navigate(target) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
            )
        }
    }
}
