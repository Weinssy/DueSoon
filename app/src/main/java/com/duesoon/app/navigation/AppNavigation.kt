package com.duesoon.app.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.duesoon.app.R
import com.duesoon.app.ui.calendar.CalendarScreen
import com.duesoon.app.ui.home.HomeScreen
import com.duesoon.app.ui.settings.SettingsScreen
import com.duesoon.app.ui.focus.FocusScreen
import com.duesoon.app.ui.task.CreateTaskScreen
import com.duesoon.app.ui.task.EditTaskScreen
import com.duesoon.app.ui.task.TaskDetailScreen

object Destinations {
    const val HOME = "home"
    const val FOCUS = "focus"
    const val CALENDAR = "calendar"
    const val SETTINGS = "settings"
    const val CREATE_TASK = "create_task"
    const val TASK_DETAIL = "task_detail/{taskId}"
    const val EDIT_TASK = "edit_task/{taskId}"
    const val FOCUS_TASK = "focus_task/{taskId}"
    fun taskDetailRoute(taskId: Long) = "task_detail/$taskId"
    fun editTaskRoute(taskId: Long) = "edit_task/$taskId"
    fun focusTaskRoute(taskId: Long) = "focus_task/$taskId"
}

data class BottomNavItem(
    val route: String,
    @StringRes val titleResId: Int,
    val icon: ImageVector? = null,
    @DrawableRes val drawableResId: Int? = null
)

val bottomNavItems = listOf(
    BottomNavItem(Destinations.HOME, R.string.nav_home, icon = Icons.Filled.Home),
    BottomNavItem(Destinations.FOCUS, R.string.nav_focus, drawableResId = R.drawable.ic_nav_focus),
    BottomNavItem(Destinations.CALENDAR, R.string.nav_calendar, icon = Icons.Filled.DateRange),
    BottomNavItem(Destinations.SETTINGS, R.string.nav_settings, icon = Icons.Filled.Settings)
)

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    quickAddTrigger: Boolean = false,
    onQuickAddHandled: () -> Unit = {}
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val context = LocalContext.current
    
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMessage by navBackStackEntry?.savedStateHandle?.getStateFlow<String?>("snackbar_message", null)?.collectAsState(initial = null) ?: remember { mutableStateOf(null) }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            navBackStackEntry?.savedStateHandle?.remove<String>("snackbar_message")
        }
    }

    LaunchedEffect(quickAddTrigger) {
        if (quickAddTrigger) {
            navController.navigate(Destinations.CREATE_TASK) {
                launchSingleTop = true
            }
            onQuickAddHandled()
        }
    }

    val showBottomBar = bottomNavItems.any { it.route == currentDestination?.route }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        val title = stringResource(item.titleResId)
                        NavigationBarItem(
                            icon = {
                                when {
                                    item.icon != null -> Icon(item.icon, contentDescription = title)
                                    item.drawableResId != null -> Icon(
                                        painter = painterResource(id = item.drawableResId),
                                        contentDescription = title
                                    )
                                }
                            },
                            label = { Text(title) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destinations.HOME,
            modifier = Modifier.padding(
                bottom = if (showBottomBar) innerPadding.calculateBottomPadding() else 0.dp
            )
        ) {
            composable(Destinations.HOME) {
                HomeScreen(
                    navigateToCreateTask = { navController.navigate(Destinations.CREATE_TASK) },
                    navigateToTaskDetail = { taskId -> navController.navigate(Destinations.taskDetailRoute(taskId)) }
                )
            }
            composable(Destinations.FOCUS) {
                FocusScreen(
                    showBackButton = false,
                    onTaskCompleted = {
                        navController.currentBackStackEntry?.savedStateHandle?.set(
                            "snackbar_message",
                            context.getString(R.string.msg_task_completed)
                        )
                    }
                )
            }
            composable(Destinations.CALENDAR) {
                CalendarScreen(
                    navigateToTaskDetail = { taskId -> navController.navigate(Destinations.taskDetailRoute(taskId)) }
                )
            }
            composable(Destinations.SETTINGS) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Destinations.CREATE_TASK) {
                CreateTaskScreen(
                    navigateBack = { navController.popBackStack() },
                    onTaskSaved = {
                        navController.previousBackStackEntry?.savedStateHandle?.set("snackbar_message", context.getString(R.string.msg_task_created))
                        navController.popBackStack()
                    }
                )
            }
            composable(
                route = Destinations.TASK_DETAIL,
                arguments = listOf(navArgument("taskId") { type = NavType.LongType })
            ) {
                TaskDetailScreen(
                    navigateBack = { navController.popBackStack() },
                    navigateToEdit = { navController.navigate(Destinations.editTaskRoute(it)) },
                    navigateToFocus = { navController.navigate(Destinations.focusTaskRoute(it)) },
                    onTaskDeleted = {
                        navController.previousBackStackEntry?.savedStateHandle?.set("snackbar_message", context.getString(R.string.msg_task_deleted))
                        navController.popBackStack()
                    }
                )
            }
            composable(
                route = Destinations.EDIT_TASK,
                arguments = listOf(navArgument("taskId") { type = NavType.LongType })
            ) {
                EditTaskScreen(
                    navigateBack = { navController.popBackStack() },
                    onTaskSaved = {
                        navController.previousBackStackEntry?.savedStateHandle?.set("snackbar_message", context.getString(R.string.msg_task_updated))
                        navController.popBackStack()
                    }
                )
            }
            composable(
                route = Destinations.FOCUS_TASK,
                arguments = listOf(navArgument("taskId") { type = NavType.LongType })
            ) {
                FocusScreen(
                    navigateBack = { navController.popBackStack() },
                    onTaskCompleted = {
                        navController.previousBackStackEntry?.savedStateHandle?.set("snackbar_message", context.getString(R.string.msg_task_completed))
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
