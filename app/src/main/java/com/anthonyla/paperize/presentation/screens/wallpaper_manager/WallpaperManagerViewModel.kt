package com.anthonyla.paperize.presentation.screens.wallpaper_manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anthonyla.paperize.domain.model.AlbumSummary
import com.anthonyla.paperize.domain.model.Wallpaper
import com.anthonyla.paperize.domain.repository.AlbumRepository
import com.anthonyla.paperize.presentation.common.stateWhileSubscribed
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class WallpaperManagerViewModel @Inject constructor(
    private val repository: AlbumRepository
) : ViewModel() {
    val wallpapers: StateFlow<List<Wallpaper>> =
        repository.getAllWallpapers().stateWhileSubscribed(viewModelScope, emptyList())

    val albums: StateFlow<List<AlbumSummary>> =
        repository.getAlbumSummaries().stateWhileSubscribed(viewModelScope, emptyList())

    private val _selected = MutableStateFlow<Set<String>>(emptySet())
    val selected: StateFlow<Set<String>> = _selected.asStateFlow()

    fun toggleSelection(id: String) {
        _selected.value = if (id in _selected.value) _selected.value - id else _selected.value + id
    }

    fun selectAll() {
        _selected.value = wallpapers.value.map { it.id }.toSet()
    }

    fun clearSelection() { _selected.value = emptySet() }

    fun deleteSelected() {
        val selectedIds = _selected.value
        if (selectedIds.isEmpty()) return
        viewModelScope.launch {
            wallpapers.value.filter { it.id in selectedIds }
                .groupBy { it.albumId }
                .forEach { (albumId, items) ->
                    repository.removeWallpapersFromAlbum(albumId, items.map { it.id })
                }
            _selected.value = emptySet()
        }
    }
}
