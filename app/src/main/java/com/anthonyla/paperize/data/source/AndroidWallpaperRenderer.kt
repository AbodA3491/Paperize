package com.anthonyla.paperize.data.source

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import androidx.core.net.toUri
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.util.adaptiveBrightnessAdjustment
import com.anthonyla.paperize.core.util.getDeviceScreenSize
import com.anthonyla.paperize.core.util.processBitmap
import com.anthonyla.paperize.core.util.retrieveBitmap
import com.anthonyla.paperize.core.util.usesLauncherManagedScrolling
import com.anthonyla.paperize.domain.model.ScheduleSettings
import com.anthonyla.paperize.domain.model.Wallpaper
import com.anthonyla.paperize.domain.source.WallpaperRenderer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

class AndroidWallpaperRenderer @Inject constructor(
    @param:ApplicationContext private val context: Context
) : WallpaperRenderer {
    override suspend fun render(wallpaper: Wallpaper, screen: ScreenType, settings: ScheduleSettings): Bitmap? {
        currentCoroutineContext().ensureActive()
        val effects = when (screen) {
            ScreenType.HOME, ScreenType.BOTH -> settings.homeEffects
            ScreenType.LOCK -> settings.lockEffects
            ScreenType.LIVE -> settings.liveEffects
        }
        val defaultScaling = when (screen) {
            ScreenType.HOME, ScreenType.BOTH -> settings.homeScalingType
            ScreenType.LOCK -> settings.lockScalingType
            ScreenType.LIVE -> settings.liveScalingType
        }
        // The override belongs to this Wallpaper row (album entry), not its URI. The same
        // physical image in Home and Lock albums therefore keeps independent presentation.
        val scaling = wallpaper.scalingOverride?.let(com.anthonyla.paperize.core.ScalingType::fromString)
            ?: defaultScaling
        val size = getDeviceScreenSize(context)
        var bitmap = retrieveBitmap(context, wallpaper.uri.toUri(), size.width, size.height, scaling,
            usesLauncherManagedScrolling(screen, scaling, settings.homeScrollingEnabled)) ?: return null
        try {
            if (wallpaper.cropScale > 1.001f ||
                kotlin.math.abs(wallpaper.cropOffsetX) > 0.001f ||
                kotlin.math.abs(wallpaper.cropOffsetY) > 0.001f
            ) {
                val transformed = applySavedCrop(
                    bitmap,
                    wallpaper.cropScale.coerceIn(1f, 4f),
                    wallpaper.cropOffsetX.coerceIn(-1f, 1f),
                    wallpaper.cropOffsetY.coerceIn(-1f, 1f)
                )
                bitmap.recycle()
                bitmap = transformed
            }
            val processed = processBitmap(
                source = bitmap,
                enableDarken = effects.enableDarken, darkenPercent = effects.darkenPercentage,
                enableBlur = effects.enableBlur, blurPercent = effects.blurPercentage,
                enableVignette = effects.enableVignette, vignettePercent = effects.vignettePercentage,
                enableGrayscale = effects.enableGrayscale, grayscalePercent = effects.grayscalePercentage
            )
            if (processed !== bitmap) bitmap.recycle()
            bitmap = processed
            if (settings.adaptiveBrightness) {
                val adjusted = adaptiveBrightnessAdjustment(context, bitmap)
                if (adjusted !== bitmap) bitmap.recycle()
                bitmap = adjusted
            }
            currentCoroutineContext().ensureActive()
            return bitmap
        } catch (e: Exception) {
            bitmap.recycle()
            throw e
        }
    }
}


/**
 * Applies the saved per-entry zoom and normalized pan to the decoded screen-sized bitmap.
 * Offsets are normalized (-1..1) and scale with the available overflow, making saved crops
 * stable across small display-size changes.
 */
private fun applySavedCrop(source: Bitmap, zoom: Float, offsetX: Float, offsetY: Float): Bitmap {
    val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    canvas.drawColor(Color.BLACK)
    val matrix = Matrix().apply {
        setScale(zoom, zoom)
        postTranslate(
            offsetX * source.width * (zoom - 1f) / 2f,
            offsetY * source.height * (zoom - 1f) / 2f
        )
    }
    canvas.drawBitmap(source, matrix, android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG))
    return output
}
