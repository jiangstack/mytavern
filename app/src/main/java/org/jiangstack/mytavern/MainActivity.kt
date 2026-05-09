package org.jiangstack.mytavern

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.jiangstack.mytavern.domain.model.ThemeMode
import org.jiangstack.mytavern.ui.navigation.NavGraph
import org.jiangstack.mytavern.ui.navigation.Screen
import org.jiangstack.mytavern.ui.theme.MyTavernTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as MyTavernApplication).container
        val userPreferencesRepository = container.userPreferencesRepository

        setContent {
            val themeMode by userPreferencesRepository.themeMode
                .collectAsState(initial = ThemeMode.SYSTEM)

            MyTavernTheme(themeMode = themeMode) {
                MyTavernApp()
            }
        }
    }
}

data class TopLevelRoute(val screen: Screen, val icon: ImageVector, val labelResId: Int)

val topLevelRoutes = listOf(
    TopLevelRoute(Screen.ChatList, Icons.Default.Chat, R.string.nav_chat),
    TopLevelRoute(Screen.CharacterList, Icons.Default.Person, R.string.nav_character),
    TopLevelRoute(Screen.WorldBookList, Icons.Default.Book, R.string.nav_worldbook),
    TopLevelRoute(Screen.Settings, Icons.Default.Settings, R.string.nav_settings)
)

@Composable
fun MyTavernApp() {
    val navController = rememberNavController()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            val isTopLevelDestination = topLevelRoutes.any { route ->
                currentDestination?.route == route.screen.route
            }

            if (isTopLevelDestination) {
                NavigationBar {
                    topLevelRoutes.forEach { route ->
                        NavigationBarItem(
                            icon = { Icon(route.icon, contentDescription = stringResource(route.labelResId)) },
                            label = { Text(stringResource(route.labelResId)) },
                            selected = currentDestination?.route == route.screen.route,
                            onClick = {
                                navController.navigate(route.screen.route) {
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
        NavGraph(
            navController = navController,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        )
    }
}
