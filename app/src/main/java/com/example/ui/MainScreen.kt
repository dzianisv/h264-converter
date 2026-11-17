package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
 *
 * Pickers (v0.2.6, WL-14): batch is Pro. Free users pick one
 * video at a time (OpenDocument); Pro users pick many at once
 * (OpenMultipleDocuments) or a whole folder (OpenDocumentTree).
 * The FAB and the empty-state card share the [onPickVideo] entry,
 * so the gate lives here, once.
 */
@Composable
fun MainScreen(
    viewModel: ConverterViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()

    val pickSingleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> if (uri != null) viewModel.addVideo(uri) }
    )
    val pickMultipleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris -> viewModel.addVideos(uris) }
    )
    val pickFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { treeUri ->
            if (treeUri != null) viewModel.addVideos(collectFolderVideos(context, treeUri))
        }
    )

    val navController = rememberNavController()

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
                onPickVideo = {
                    if (isPro) pickMultipleLauncher.launch(arrayOf("video/*"))
                    else pickSingleLauncher.launch(arrayOf("video/*"))
                },
                onPickFolder = { pickFolderLauncher.launch(null) }
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

/**
 * Lists the videos directly inside a picked folder (no recursion
 * — a folder of clips is the batch case; nested folders stay out).
 * Keeps the tree permission so the queue can still read the files
 * when the conversion runs. Returns an empty list on any failure —
 * nothing is added, nothing crashes.
 */
private fun collectFolderVideos(context: Context, treeUri: Uri): List<Uri> {
    val uris = mutableListOf<Uri>()
    try {
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
        context.contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val mimeIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val nameIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val mime = cursor.getString(mimeIdx) ?: ""
                val name = cursor.getString(nameIdx) ?: ""
                val ext = name.substringAfterLast('.', "").lowercase()
                if (mime.startsWith("video/") || ext in VIDEO_EXTENSIONS) {
                    uris += DocumentsContract.buildDocumentUriUsingTree(
                        treeUri,
                        cursor.getString(idIdx),
                    )
                }
            }
        }
        context.contentResolver.takePersistableUriPermission(
            treeUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
    } catch (_: Exception) {
        // Empty list: nothing added.
    }
    return uris
}

private val VIDEO_EXTENSIONS = setOf(
    "mp4", "m4v", "mov", "mkv", "avi", "webm",
    "3gp", "3gpp", "ts", "mts", "m2ts", "wmv", "flv", "mpg", "mpeg"
)
