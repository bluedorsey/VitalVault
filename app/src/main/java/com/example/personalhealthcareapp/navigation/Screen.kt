package com.example.personalhealthcareapp.navigation

/**
 * Defines all navigation routes in the app.
 */
sealed class Screen(val route: String) {
    data object Chat : Screen("chat")
    data object Upload : Screen("upload")
}
