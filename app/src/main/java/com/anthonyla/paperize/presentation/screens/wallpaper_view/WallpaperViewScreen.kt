package com.anthonyla.paperize.presentation.screens.wallpaper_view

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.yalantis.ucrop.UCrop
import java.io.File
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.WallpaperMode
import com.anthonyla.paperize.presentation.common.theme.AppSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperViewScreen(
    wallpaperUri: String,
    wallpaperName: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WallpaperViewViewModel = hiltViewModel()
) {
    val wallpaperMode by viewModel.wallpaperMode.collectAsStateWithLifecycle()
    val wallpaper by viewModel.wallpaper.collectAsStateWithLifecycle()
    val saveCompleted by viewModel.saveCompleted.collectAsStateWithLifecycle()
    var showApplyDialog by rememberSaveable { mutableStateOf(false) }
    var scalingOverride by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val editLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            UCrop.getOutput(result.data ?: return@rememberLauncherForActivityResult)?.let { editedUri ->
                viewModel.saveEditedImage(editedUri.toString())
            }
        }
    }

    LaunchedEffect(wallpaper?.id) {
        scalingOverride = wallpaper?.scalingOverride
    }

    val scalingChoices = listOf(
        null to "Album default",
        "FIT" to "Fit",
        "FILL" to "Fill",
        "STRETCH" to "Stretch",
        "NONE" to "Original"
    )
    val previewScale = when (scalingOverride ?: wallpaper?.scalingOverride) {
        "FIT" -> ContentScale.Fit
        "STRETCH" -> ContentScale.FillBounds
        "NONE" -> ContentScale.None
        else -> ContentScale.Crop
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = {
                    Text(
                        text = wallpaperName,
                        color = MaterialTheme.colorScheme.surfaceBright,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = MaterialTheme.colorScheme.surfaceBright)
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(containerColor = Color.Transparent) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.large),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
                ) {
                    Button(onClick = {
                        val outputDir = File(context.filesDir, "edited_wallpapers").apply { mkdirs() }
                        val output = Uri.fromFile(File(outputDir, "wallpaper_${wallpaper?.id ?: System.currentTimeMillis()}_${System.currentTimeMillis()}.jpg"))
                        val options = UCrop.Options().apply {
                            setToolbarTitle("Edit wallpaper")
                            setFreeStyleCropEnabled(true)
                            setCompressionFormat(Bitmap.CompressFormat.JPEG)
                            setCompressionQuality(95)
                        }
                        editLauncher.launch(UCrop.of((wallpaper?.uri ?: wallpaperUri).toUri(), output).withOptions(options).getIntent(context))
                    }, modifier = Modifier.weight(1f)) {
                        Text("Edit image")
                    }
                    Button(onClick = { viewModel.savePresentation(scalingOverride, 1f, 0f, 0f) }, modifier = Modifier.weight(1f)) {
                        Text("Save settings")
                    }
                    if (wallpaperMode == WallpaperMode.STATIC) {
                        Button(onClick = { showApplyDialog = true }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.set_wallpaper))
                        }
                    }
                }
            }
        }
    ) { padding ->
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.scrim) {
            Column(
                modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(430.dp).clipToBounds(),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = (wallpaper?.uri ?: wallpaperUri).toUri(),
                        contentDescription = wallpaperName,
                        contentScale = previewScale,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Text(
                    "Scaling for this picture",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.surfaceBright,
                    modifier = Modifier.padding(AppSpacing.medium)
                )
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.medium),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.small)
                ) {
                    scalingChoices.chunked(3).forEach { rowChoices ->
                        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
                            rowChoices.forEach { (value, label) ->
                                FilterChip(
                                    selected = scalingOverride == value,
                                    onClick = { scalingOverride = value },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
                if (saveCompleted) {
                    Text("Image settings saved", color = MaterialTheme.colorScheme.primary)
                    LaunchedEffect(saveCompleted) { viewModel.consumeSaveCompleted() }
                }
            }
        }
    }

    if (showApplyDialog) {
        AlertDialog(
            onDismissRequest = { showApplyDialog = false },
            title = { Text(stringResource(R.string.set_wallpaper)) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        ScreenType.HOME to R.string.home_screen_btn,
                        ScreenType.LOCK to R.string.lock_screen_btn,
                        ScreenType.BOTH to R.string.home_and_lock_screens
                    ).forEach { (screenType, label) ->
                        TextButton(onClick = {
                            showApplyDialog = false
                            viewModel.applyTo(screenType)
                        }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(label)) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showApplyDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
