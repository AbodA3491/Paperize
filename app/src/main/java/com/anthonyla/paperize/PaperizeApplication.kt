package com.anthonyla.paperize

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.work.Configuration
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.core.util.DataResetManager
import com.anthonyla.paperize.service.worker.WallpaperScheduler
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.WallpaperMode
import com.anthonyla.paperize.domain.repository.SettingsRepository
import com.anthonyla.paperize.service.wallpaper.WallpaperChangeService
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@HiltAndroidApp
class PaperizeApplication : Application(), Configuration.Provider, DefaultLifecycleObserver {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var wallpaperScheduler: WallpaperScheduler

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val screenOnScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val staticScreenOnReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != Intent.ACTION_SCREEN_ON) return
            screenOnScope.launch {
                if (settingsRepository.getWallpaperMode() != WallpaperMode.STATIC) return@launch
                val settings = settingsRepository.getScheduleSettings()
                val home = settings.homeEnabled && settings.homeEffects.enableChangeOnScreenOff
                val lock = settings.lockEnabled && settings.lockEffects.enableChangeOnScreenOff
                if (home && lock && settings.homeAlbumId == settings.lockAlbumId && !settings.separateSchedules) {
                    WallpaperChangeService.changeWallpaper(context, ScreenType.BOTH)
                } else {
                    if (home) WallpaperChangeService.changeWallpaper(context, ScreenType.HOME)
                    if (lock) WallpaperChangeService.changeWallpaper(context, ScreenType.LOCK)
                }
            }
        }
    }

    override fun onCreate() {
        super<Application>.onCreate()

        // Must run before any other initialization that accesses DB/preferences
        DataResetManager.performResetIfNeeded(this)

        createNotificationChannel()

        // ACTION_SCREEN_ON is a dynamic-only broadcast. Keep this receiver registered while
        // the app process is alive so Static mode can rotate on wake as well as Live mode.
        ContextCompat.registerReceiver(
            this,
            staticScreenOnReceiver,
            IntentFilter(Intent.ACTION_SCREEN_ON),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        // Process lifecycle distinguishes real background/foreground transitions from activity
        // recreation, so folder-backed albums are refreshed whenever the user returns to the app.
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_ID,
            getString(R.string.app_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    override fun onStart(owner: LifecycleOwner) {
        wallpaperScheduler.refreshAlbumsNow()
    }

    override fun onTerminate() {
        runCatching { unregisterReceiver(staticScreenOnReceiver) }
        screenOnScope.cancel()
        super.onTerminate()
    }
}
