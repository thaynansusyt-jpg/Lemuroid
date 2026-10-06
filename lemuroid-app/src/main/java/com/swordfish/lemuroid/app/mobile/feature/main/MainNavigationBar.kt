package com.swordfish.lemuroid.app.mobile.feature.main

import androidx.compose.material3.MaterialTheme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

@Composable
fun MainNavigationBar(
    currentRoute: MainRoute?,
    navController: NavHostController,
) {
    AnimatedVisibility(
        visible = currentRoute?.showBottomNavigation != false,
        enter = expandVertically(),
        exit = shrinkVertically(),
    ) {
        LemuroidNavigationBar(currentRoute, navController)
    }
}

@Composable
private fun LemuroidNavigationBar(
    currentRoute: MainRoute?,
    navController: NavHostController,
) {
    val clickSound = com.swordfish.lemuroid.app.mobile.shared.compose.ui.rememberKlMenuClick()
    NavigationBar(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(28.dp)),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        MainNavigationRoutes.values().forEach { destination ->
            val isSelected = currentRoute?.root == destination.route
            val iconDrawable = if (isSelected) destination.selectedIcon else destination.unselectedIcon

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = iconDrawable,
                        contentDescription = stringResource(destination.titleId),
                    )
                },
                label = { Text(stringResource(destination.titleId), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall) },
                alwaysShowLabel = false,
                selected = isSelected,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                onClick = {
                    clickSound()
                    navController.navigate(destination.route.route) {
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        // on the back stack as users select items
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = false
                        }
                        // Avoid multiple copies of the same destination when
                        // reselecting the same item
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = false
                    }
                },
            )
        }
    }
}
