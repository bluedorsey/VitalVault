package com.example.personalhealthcareapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.personalhealthcareapp.uiux.MainScreen
import com.example.personalhealthcareapp.uiux.UploadScreen

/**
 * App-level navigation graph.
 * Chat is the start destination; Upload is navigated to from the Chat screen.
 */
@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Chat.route
    ) {
        composable(Screen.Chat.route) {
            MainScreen(
                onNavigateToUpload = {
                    navController.navigate(Screen.Upload.route)
                }
            )
        }

        composable(Screen.Upload.route) {
            UploadScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
