package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.viewmodel.ConverterViewModel

/**
 * App navigation: two real destinations.
 * - "queue": the main screen (queue, settings, history).
 * - "detail/{itemId}": one file's trim + conversion properties.
 * The detail screen is a destination, not a dialog — back
 * returns to the queue.
 */
@Composable
fun MainScreen(
    viewModel: ConverterViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val pickVideosLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris -> viewModel.addVideos(uris) }
    )

    NavHost(
        navController = navController,
        startDestination = "queue",
        modifier = modifier
    ) {
        composable(route = "queue") {
            QueueScreen(
                viewModel = viewModel,
                onOpenDetail = { itemId ->
                    navController.navigate("detail/$itemId")
                },
                onPickVideo = { pickVideosLauncher.launch(arrayOf("video/*")) }
            )
        }
        composable(
            route = "detail/{itemId}",
            arguments = listOf(
                navArgument("itemId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId") ?: return@composable
            DetailScreen(
                itemId = itemId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
