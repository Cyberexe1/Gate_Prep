package com.gateprep.app.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gateprep.app.AppViewModel
import com.gateprep.app.ui.auth.AuthMode
import com.gateprep.app.ui.auth.AuthScreen
import com.gateprep.app.ui.home.HomeScreen
import com.gateprep.app.ui.mock.MockListScreen
import com.gateprep.app.ui.mock.MockTestScreen
import com.gateprep.app.ui.placeholder.ChatScreen
import com.gateprep.app.ui.practice.PracticeScreen
import com.gateprep.app.ui.profile.ProfileScreen
import com.gateprep.app.ui.subject.SubjectScreen
import com.gateprep.app.ui.theme.LocalExtra

private const val HOME = "home"
private const val MOCK = "mock"
private const val MOCK_TEST = "mocktest/{id}"
private const val CHAT = "chat"
private const val PROFILE = "profile"
private const val LOGIN = "login"
private const val SIGNUP = "signup"
private const val SUBJECT = "subject/{sid}"
private const val PRACTICE = "practice/{sid}/{mid}"

private val TAB_ROUTES = listOf(HOME, MOCK, CHAT, PROFILE)

private fun tabIndex(route: String?): Int = when (route) {
    HOME, SUBJECT -> 0
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
    val snackbar = remember { SnackbarHostState() }

    fun openProtected(destination: String, mockId: String? = null) {
        if (vm.loggedIn) {
            nav.navigate(destination)
        } else {
            vm.requireAuth(destination, mockId)
            nav.navigate(LOGIN) { launchSingleTop = true }
        }
    }

    fun authComplete() {
        val destination = vm.consumePendingDestination() ?: HOME
        nav.navigate(destination) {
            popUpTo(HOME) { saveState = false }
            launchSingleTop = true
        }
    }

    fun authBack() {
        vm.cancelPendingAuth()
        if (!nav.popBackStack()) nav.navigate(HOME) { launchSingleTop = true }
    }

    LaunchedEffect(vm.notice) {
        vm.notice?.let {
            snackbar.showSnackbar(it)
            vm.notice = null
        }
    }

    val dur = 380
    val ease = FastOutSlowInEasing
    val slide = AnimatedContentTransitionScope.SlideDirection

    Box(Modifier.fillMaxSize().background(ex.bg)) {
        NavHost(
            navController = nav,
            startDestination = HOME,
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                val from = initialState.destination.route
                val to = targetState.destination.route
                if (isTab(from) && isTab(to)) {
                    val dir = if (tabIndex(to) > tabIndex(from)) slide.Left else slide.Right
                    slideIntoContainer(dir, tween(dur, easing = ease)) + fadeIn(tween(dur))
                } else {
                    slideIntoContainer(slide.Left, tween(dur, easing = ease)) + fadeIn(tween(dur))
                }
            },
            exitTransition = {
                val from = initialState.destination.route
                val to = targetState.destination.route
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
                    onOpenSubject = { openProtected("subject/$it") },
                    onResume = { s, m -> openProtected("practice/$s/$m") },
                )
            }

            composable(MOCK) {
                MockListScreen(
                    vm = vm,
                    onOpen = { id -> nav.navigate("mocktest/$id") },
                    onRequireLogin = { id -> openProtected(MOCK, mockId = id) },
                )
            }

            composable(MOCK_TEST) { e ->
                val id = e.arguments?.getString("id").orEmpty()
                AuthGate(
                    vm = vm,
                    destination = MOCK,
                    mockId = id,
                    onLogin = { nav.navigate(LOGIN) { launchSingleTop = true } },
                ) {
                    MockTestScreen(vm, id, onExit = { nav.popBackStack() })
                }
            }

            composable(CHAT) { ChatScreen() }

            composable(PROFILE) {
                ProfileScreen(
                    vm = vm,
                    onLogin = {
                        vm.requireAuth(PROFILE)
                        nav.navigate(LOGIN) { launchSingleTop = true }
                    },
                    onSignup = {
                        vm.requireAuth(PROFILE)
                        nav.navigate(SIGNUP) { launchSingleTop = true }
                    },
                )
            }

            composable(LOGIN) {
                AuthScreen(
                    vm = vm, mode = AuthMode.LOGIN,
                    onBack = ::authBack,
                    onSwitchMode = {
                        nav.navigate(SIGNUP) {
                            popUpTo(LOGIN) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onAuthenticated = ::authComplete,
                )
            }

            composable(SIGNUP) {
                AuthScreen(
                    vm = vm, mode = AuthMode.SIGNUP,
                    onBack = ::authBack,
                    onSwitchMode = {
                        nav.navigate(LOGIN) {
                            popUpTo(SIGNUP) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onAuthenticated = ::authComplete,
                )
            }

            composable(SUBJECT) { e ->
                val sid = e.arguments?.getString("sid").orEmpty()
                AuthGate(
                    vm = vm,
                    destination = "subject/$sid",
                    onLogin = { nav.navigate(LOGIN) { launchSingleTop = true } },
                ) {
                    SubjectScreen(
                        vm = vm, subjectId = sid,
                        onBack = { nav.popBackStack() },
                        onOpenModule = { mid -> nav.navigate("practice/$sid/$mid") },
                    )
                }
            }

            composable(PRACTICE) { e ->
                val sid = e.arguments?.getString("sid").orEmpty()
                val mid = e.arguments?.getString("mid").orEmpty()
                AuthGate(
                    vm = vm,
                    destination = "practice/$sid/$mid",
                    onLogin = { nav.navigate(LOGIN) { launchSingleTop = true } },
                ) {
                    PracticeScreen(vm = vm, subjectId = sid, moduleId = mid, onBack = { nav.popBackStack() })
                }
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

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 20.dp).padding(bottom = if (showBar) 106.dp else 18.dp),
        )
    }
}

/** Also protects restored/deep routes and redirects immediately if a refresh token expires on a protected screen. */
@Composable
private fun AuthGate(
    vm: AppViewModel,
    destination: String,
    mockId: String? = null,
    onLogin: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (vm.loggedIn) {
        content()
    } else {
        LaunchedEffect(destination, mockId) {
            vm.requireAuth(destination, mockId)
            onLogin()
        }
        Box(Modifier.fillMaxSize().background(LocalExtra.current.bg), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = LocalExtra.current.brandStart)
        }
    }
}
