package com.example.testcomposethierry.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.paging.compose.LazyPagingItems
import com.example.testcomposethierry.data.models.DataArtElement
import com.example.testcomposethierry.ui.setup.RoutingScreen
import com.example.testcomposethierry.ui.view_models.ArtViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun NavigationScreen(
    activity: ComponentActivity,
    stateListArt: LazyPagingItems<DataArtElement>,
    artViewModel: ArtViewModel,
) {
    val navController = rememberNavController()
    navController.addOnDestinationChangedListener { controller, destination, args ->
        CoroutineScope(Dispatchers.IO).launch {
            // needed to avoid seeing the old text in the Detail Screen
            // but we cannot set null when navigating to the Detail screen or it interfers with the data
            if (destination.route != RoutingScreen.MyDetailScreen.route) {
                artViewModel.setActiveDetailData(null)
            }
        }
    }
    val items = listOf(
        RoutingScreen.MyListScreen,
        RoutingScreen.MyDetailScreen,
    )
    val activeRow by artViewModel.activeRow.collectAsStateWithLifecycle()
    Scaffold(
        backgroundColor = MaterialTheme.colors.secondary,
        bottomBar = {
            BottomNavigation {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    BottomNavigationItem(
                        icon = {
                            Icon(
                                imageVector = when(screen) {
                                    RoutingScreen.MyListScreen -> Icons.Filled.Favorite
                                    RoutingScreen.MyDetailScreen -> Icons.Filled.AccountBox
                                },
                                contentDescription = null
                            )
                        },
                        label = { Text(stringResource(screen.resourceId)) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                // on the back stack as users select items
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }

                                // Avoid multiple copies of the same destination when
                                // reselecting the same item
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController,
            startDestination = RoutingScreen.MyListScreen.route,
            androidx.compose.ui.Modifier.padding(innerPadding)
        ) {
            composable(RoutingScreen.MyListScreen.route) {
                ListScreen(
                    activeRow = activeRow,
                    stateListArt = stateListArt,
                    navController = navController,

                )
            }
            // anomaly: as reported in this stack overflow, navigating, recomposes twice
            // https://stackoverflow.com/questions/69190119/jetpack-compose-recompose-with-success-state-twice-when-exiting-current-composab?noredirect=1&lq=1
            composable(RoutingScreen.MyDetailScreen.route) { backStackEntry ->
                val previousRow = if (activeRow != -1) activeRow else 0
                val rowId = try {
                    backStackEntry.arguments?.getString("rowId")?.toInt() ?: previousRow
                } catch (_: Exception) {
                    previousRow
                }
                DetailScreen(stateListArt = stateListArt, rowId = rowId, artViewModel = artViewModel)
                LaunchedEffect(Unit) {
                    delay(300.milliseconds)
                    if (rowId != activeRow) {
                        artViewModel.setActiveRow(owner = activity, rowId = rowId)
                    }
                }
            }
        }
    }
}
