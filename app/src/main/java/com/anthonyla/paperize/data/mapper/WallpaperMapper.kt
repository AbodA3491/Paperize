package com.anthonyla.paperize.data.mapper

import com.anthonyla.paperize.data.database.entities.WallpaperEntity
import com.anthonyla.paperize.domain.model.Wallpaper

fun WallpaperEntity.toDomainModel(): Wallpaper = Wallpaper(
    id = id,
    albumId = albumId,
    folderId = folderId,
    uri = uri,
    fileName = fileName,
    dateModified = dateModified,
    displayOrder = displayOrder,
    addedAt = addedAt,
    scalingOverride = scalingOverride,
    cropScale = cropScale,
    cropOffsetX = cropOffsetX,
    cropOffsetY = cropOffsetY
)

fun Wallpaper.toEntity(): WallpaperEntity = WallpaperEntity(
    id = id,
    albumId = albumId,
    folderId = folderId,
    uri = uri,
    fileName = fileName,
    dateModified = dateModified,
    displayOrder = displayOrder,
    sourceType = if (folderId == null) "DIRECT" else "FOLDER",
    addedAt = addedAt,
    scalingOverride = scalingOverride,
    cropScale = cropScale.coerceIn(1f, 4f),
    cropOffsetX = cropOffsetX.coerceIn(-1f, 1f),
    cropOffsetY = cropOffsetY.coerceIn(-1f, 1f)
)
