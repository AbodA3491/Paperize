package com.anthonyla.paperize.presentation.screens.wallpaper_view

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.WallpaperMode
import com.anthonyla.paperize.domain.model.Wallpaper
import com.anthonyla.paperize.domain.repository.SettingsRepository
import com.anthonyla.paperize.domain.repository.WallpaperRepository
import com.anthonyla.paperize.presentation.common.navigation.WallpaperViewRoute
import com.anthonyla.paperize.presentation.common.stateWhileSubscribed
import com.anthonyla.paperize.service.wallpaper.WallpaperChangeService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class WallpaperViewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @param:ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val wallpaperRepository: WallpaperRepository
) : ViewModel() {
    private val route = savedStateHandle.toRoute<WallpaperViewRoute>()

    val wallpaperMode = settingsRepository.getWallpaperModeFlow().stateWhileSubscribed(viewModelScope, WallpaperMode.STATIC)
    private val _wallpaper = MutableStateFlow<Wallpaper?>(null)
    val wallpaper = _wallpaper.asStateFlow()
    private val _saveCompleted = MutableStateFlow(false)
    val saveCompleted = _saveCompleted.asStateFlow()
    val exportFolderUri = settingsRepository.getEditedImageExportFolderFlow()
        .stateWhileSubscribed(viewModelScope, null)
    private val _exportStatus = MutableStateFlow<String?>(null)
    val exportStatus = _exportStatus.asStateFlow()

    init {
        viewModelScope.launch {
            _wallpaper.value = wallpaperRepository.getWallpaperById(route.wallpaperId)
        }
    }

    fun savePresentation(
        scalingOverride: String?,
        cropScale: Float,
        cropOffsetX: Float,
        cropOffsetY: Float
    ) {
        viewModelScope.launch {
            wallpaperRepository.updateWallpaperPresentation(
                wallpaperId = route.wallpaperId,
                scalingOverride = scalingOverride,
                cropScale = cropScale,
                cropOffsetX = cropOffsetX,
                cropOffsetY = cropOffsetY
            )
            _wallpaper.value = wallpaperRepository.getWallpaperById(route.wallpaperId)
            _saveCompleted.value = true
        }
    }

    fun setEditedImageExportFolder(uri: String?) {
        viewModelScope.launch {
            settingsRepository.updateEditedImageExportFolder(uri)
            _exportStatus.value = if (uri == null) "Export folder cleared" else "Export folder saved"
        }
    }

    fun saveEditedImage(uri: String) {
        viewModelScope.launch {
            wallpaperRepository.updateWallpaperUri(route.wallpaperId, uri)
            _wallpaper.value = wallpaperRepository.getWallpaperById(route.wallpaperId)
            _saveCompleted.value = true
            val folderUri = settingsRepository.getEditedImageExportFolderFlow().first()
            if (folderUri == null) {
                _exportStatus.value = "Edited image saved in Paperize. Choose an export folder to also save a copy there."
                return@launch
            }
            try {
                val folder = DocumentFile.fromTreeUri(context, Uri.parse(folderUri))
                    ?: throw IllegalStateException("The selected export folder is unavailable")
                if (!folder.isDirectory || !folder.canWrite()) {
                    throw IllegalStateException("The selected export folder is not writable")
                }
                val destination = folder.createFile("image/jpeg", "Paperize_" + System.currentTimeMillis() + ".jpg")
                    ?: throw IllegalStateException("Could not create the exported image")
                context.contentResolver.openInputStream(Uri.parse(uri)).use { input ->
                    requireNotNull(input) { "Could not read the edited image" }
                    context.contentResolver.openOutputStream(destination.uri, "w").use { output ->
                        requireNotNull(output) { "Could not write to the export folder" }
                        input.copyTo(output)
                    }
                }
                _exportStatus.value = "Edited image exported to your selected folder"
            } catch (error: Exception) {
                _exportStatus.value = "Image edited, but export failed: " + (error.message ?: "unknown error")
            }
        }
    }

    fun consumeExportStatus() {
        _exportStatus.value = null
    }

    fun consumeSaveCompleted() {
        _saveCompleted.value = false
    }

    fun applyTo(screenType: ScreenType) {
        require(screenType != ScreenType.LIVE)
        WallpaperChangeService.applyWallpaper(context, route.wallpaperId, screenType)
    }
}
