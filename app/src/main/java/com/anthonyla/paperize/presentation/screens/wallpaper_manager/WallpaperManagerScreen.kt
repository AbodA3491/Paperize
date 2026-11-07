package com.anthonyla.paperize.presentation.screens.wallpaper_manager

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.presentation.common.components.EmptyCollection
import com.anthonyla.paperize.presentation.common.theme.AppGrid
import com.anthonyla.paperize.presentation.common.theme.AppSpacing
import com.anthonyla.paperize.presentation.screens.album_view.components.WallpaperItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperManagerScreen(
    onBackClick: () -> Unit,
    onOpenWallpaper: (String, String, String) -> Unit,
    viewModel: WallpaperManagerViewModel = hiltViewModel()
) {
    val wallpapers by viewModel.wallpapers.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val albumNames = remember(albums) { albums.associate { it.id to it.name } }

    BackHandler(enabled = selected.isNotEmpty()) { viewModel.clearSelection() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (selected.isEmpty()) "Wallpaper manager" else "${selected.size} selected") },
                navigationIcon = {
                    IconButton(onClick = { if (selected.isNotEmpty()) viewModel.clearSelection() else onBackClick() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selected.isNotEmpty()) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove selected")
                        }
                        IconButton(onClick = viewModel::selectAll) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select all")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (wallpapers.isEmpty()) {
            EmptyCollection(
                title = "No wallpapers yet",
                hint = "Add wallpapers to an album and they will appear here.",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(AppGrid.itemMinSize),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(AppSpacing.gridPadding),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.gridSpacing),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.gridSpacing)
            ) {
                items(wallpapers, key = { it.id }) { wallpaper ->
                    WallpaperItem(
                        wallpaperUri = wallpaper.uri,
                        wallpaperName = wallpaper.displayFileName,
                        isSelected = wallpaper.id in selected,
                        isSelectionMode = selected.isNotEmpty(),
                        onClick = {
                            if (selected.isNotEmpty()) viewModel.toggleSelection(wallpaper.id)
                            else onOpenWallpaper(wallpaper.id, wallpaper.uri, wallpaper.fileName)
                        },
                        onLongClick = { viewModel.toggleSelection(wallpaper.id) },
                        modifier = Modifier.fillMaxWidth().aspectRatio(Constants.WALLPAPER_ASPECT_RATIO)
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Remove selected wallpapers?") },
            text = { Text("This removes the selected items from their Paperize albums. Original files on your device are not deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deleteSelected()
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
