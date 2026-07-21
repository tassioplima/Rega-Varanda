package com.tassiolima.regavaranda.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tassiolima.regavaranda.ui.chat.PlantChatScreen
import com.tassiolima.regavaranda.ui.compass.CompassScreen
import com.tassiolima.regavaranda.ui.dashboard.HealthDashboardScreen
import com.tassiolima.regavaranda.ui.home.HomeScreen
import com.tassiolima.regavaranda.ui.plant.AddEditPlantScreen
import com.tassiolima.regavaranda.ui.plant.PlantDetailScreen
import com.tassiolima.regavaranda.ui.settings.VarandaSettingsScreen

private object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val DASHBOARD = "dashboard"
    const val COMPASS = "compass"
    const val ADD_EDIT_PLANT = "plant/{plantId}"
    const val PLANT_DETAIL = "plant_detail/{plantId}"
    const val PLANT_CHAT = "plant_chat/{plantId}"
    fun addEditPlant(plantId: Long) = "plant/$plantId"
    fun plantDetail(plantId: Long) = "plant_detail/$plantId"
    fun plantChat(plantId: Long) = "plant_chat/$plantId"
}

private const val PICKED_ORIENTATION_KEY = "picked_orientation"

@Composable
fun RegaVarandaNavGraph(
    pendingPlantId: Long? = null,
    onPendingPlantIdConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()

    // Deep link vindo de uma notificação: navega direto para a planta assim que o grafo existe.
    LaunchedEffect(pendingPlantId) {
        pendingPlantId?.let { id ->
            navController.navigate(Routes.plantDetail(id))
            onPendingPlantIdConsumed()
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        enterTransition = { androidx.compose.animation.EnterTransition.None },
        exitTransition = { androidx.compose.animation.ExitTransition.None },
        popEnterTransition = { androidx.compose.animation.EnterTransition.None },
        popExitTransition = { androidx.compose.animation.ExitTransition.None }
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onAddPlant = { navController.navigate(Routes.addEditPlant(0)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenDashboard = { navController.navigate(Routes.DASHBOARD) },
                onOpenPlant = { plantId -> navController.navigate(Routes.plantDetail(plantId)) }
            )
        }
        composable(Routes.SETTINGS) { entry ->
            val pickedOrientation by entry.savedStateHandle
                .getStateFlow<String?>(PICKED_ORIENTATION_KEY, null)
                .collectAsState()
            VarandaSettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenCompass = { navController.navigate(Routes.COMPASS) },
                pickedOrientationName = pickedOrientation,
                onPickedOrientationConsumed = { entry.savedStateHandle[PICKED_ORIENTATION_KEY] = null }
            )
        }
        composable(Routes.DASHBOARD) {
            HealthDashboardScreen(
                onBack = { navController.popBackStack() },
                onOpenPlant = { plantId -> navController.navigate(Routes.plantDetail(plantId)) }
            )
        }
        composable(Routes.COMPASS) {
            CompassScreen(
                onBack = { navController.popBackStack() },
                onSelect = { orientation ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(PICKED_ORIENTATION_KEY, orientation.name)
                    navController.popBackStack()
                }
            )
        }
        composable(
            route = Routes.ADD_EDIT_PLANT,
            arguments = listOf(navArgument("plantId") { type = NavType.LongType })
        ) { entry ->
            val plantId = entry.arguments?.getLong("plantId") ?: 0L
            val pickedOrientation by entry.savedStateHandle
                .getStateFlow<String?>(PICKED_ORIENTATION_KEY, null)
                .collectAsState()
            AddEditPlantScreen(
                plantId = plantId,
                onBack = { navController.popBackStack() },
                onOpenCompass = { navController.navigate(Routes.COMPASS) },
                pickedOrientationName = pickedOrientation,
                onPickedOrientationConsumed = { entry.savedStateHandle[PICKED_ORIENTATION_KEY] = null }
            )
        }
        composable(
            route = Routes.PLANT_DETAIL,
            arguments = listOf(navArgument("plantId") { type = NavType.LongType })
        ) { backStackEntry ->
            val plantId = backStackEntry.arguments?.getLong("plantId") ?: 0L
            PlantDetailScreen(
                plantId = plantId,
                onBack = { navController.popBackStack() },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onEditPlant = { navController.navigate(Routes.addEditPlant(plantId)) },
                onOpenChat = { navController.navigate(Routes.plantChat(plantId)) }
            )
        }
        composable(
            route = Routes.PLANT_CHAT,
            arguments = listOf(navArgument("plantId") { type = NavType.LongType })
        ) { backStackEntry ->
            val plantId = backStackEntry.arguments?.getLong("plantId") ?: 0L
            PlantChatScreen(
                plantId = plantId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
