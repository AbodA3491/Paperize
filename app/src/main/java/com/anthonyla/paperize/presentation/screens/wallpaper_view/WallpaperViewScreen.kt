package com.anthonyla.paperize.presentation.screens.wallpaper_view

import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
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
    var zoom by rememberSaveable { mutableFloatStateOf(1f) }
    var panX by rememberSaveable { mutableFloatStateOf(0f) }
    var panY by rememberSaveable { mutableFloatStateOf(0f) }

    LaunchedEffect(wallpaper?.id) {
        wallpaper?.let {
            scalingOverride = it.scalingOverride
            zoom = it.cropScale
            panX = it.cropOffsetX
            panY = it.cropOffsetY
        }
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
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.surfaceBright
                        )
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
                    Button(
                        onClick = { viewModel.savePresentation(scalingOverride, zoom, panX, panY) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Save image settings") }
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
                    modifier = Modifier.fillMaxWidth().height(430.dp).clipToBounds()
                        .pointerInput(wallpaperUri) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val rangeX = (zoom - 1f).coerceAtLeast(0.05f)
                                val rangeY = (zoom - 1f).coerceAtLeast(0.05f)
                                panX = (panX + dragAmount.x / size.width * 2f / rangeX).coerceIn(-1f, 1f)
                                panY = (panY + dragAmount.y / size.height * 2f / rangeY).coerceIn(-1f, 1f)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = wallpaperUri.toUri(),
                        contentDescription = wallpaperName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            scaleX = zoom
                            scaleY = zoom
                            translationX = panX * size.width * (zoom - 1f) / 2f
                            translationY = panY * size.height * (zoom - 1f) / 2f
                        }
                    )
                }
                Text(
                    "Drag the image to reposition it. These settings belong only to this album entry.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.surfaceBright,
                    modifier = Modifier.padding(AppSpacing.medium)
                )
                Text("Scaling", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.surfaceBright)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = AppSpacing.small),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(null to "Album default", "FILL" to "Fill", "FIT" to "Fit", "STRETCH" to "Stretch", "NONE" to "Original").forEach { (value, label) ->
                        FilterChip(
                            selected = scalingOverride == value,
                            onClick = { scalingOverride = value },
                            label = { Text(label) }
                        )
                    }
                }
                Text("Zoom: ${"%.2f".format(zoom)}×", color = MaterialTheme.colorScheme.surfaceBright)
                Slider(
                    value = zoom,
                    onValueChange = { zoom = it },
                    valueRange = 1f..4f,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.large)
                )
                Text("Horizontal position", color = MaterialTheme.colorScheme.surfaceBright)
                Slider(
                    value = panX,
                    onValueChange = { panX = it },
                    valueRange = -1f..1f,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.large)
                )
                Text("Vertical position", color = MaterialTheme.colorScheme.surfaceBright)
                Slider(
                    value = panY,
                    onValueChange = { panY = it },
                    valueRange = -1f..1f,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.large)
                )
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
                        TextButton(
                            onClick = {
                                showApplyDialog = false
                                viewModel.applyTo(screenType)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(stringResource(label)) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showApplyDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
