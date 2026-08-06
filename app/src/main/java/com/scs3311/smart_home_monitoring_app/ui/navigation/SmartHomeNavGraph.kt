package com.scs3311.smart_home_monitoring_app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.scs3311.smart_home_monitoring_app.SmartHomeAppContainer
import com.scs3311.smart_home_monitoring_app.ui.screens.DeviceDetailScreen
import com.scs3311.smart_home_monitoring_app.ui.screens.HomeScreen
import com.scs3311.smart_home_monitoring_app.ui.screens.UsageReportScreen
import com.scs3311.smart_home_monitoring_app.viewmodel.DeviceDetailViewModel
import com.scs3311.smart_home_monitoring_app.viewmodel.HomeViewModel
import com.scs3311.smart_home_monitoring_app.viewmodel.UsageViewModel

@Composable
fun SmartHomeNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val repository = SmartHomeAppContainer.repository

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            val viewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.Factory(repository)
            )
            HomeScreen(
                viewModel = viewModel,
                onDeviceClick = { deviceId ->
                    navController.navigate(Routes.deviceDetail(deviceId))
                },
                onUsageClick = { navController.navigate(Routes.USAGE) }
            )
        }

        composable(
            route = Routes.DEVICE_DETAIL,
            arguments = listOf(navArgument("deviceId") { type = NavType.StringType })
        ) { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: return@composable
            val viewModel: DeviceDetailViewModel = viewModel(
                factory = DeviceDetailViewModel.Factory(repository, deviceId)
            )
            DeviceDetailScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.USAGE) {
            val viewModel: UsageViewModel = viewModel(
                factory = UsageViewModel.Factory(repository)
            )
            UsageReportScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
