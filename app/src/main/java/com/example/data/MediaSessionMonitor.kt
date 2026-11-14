package com.example.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.view.KeyEvent
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference

data class MediaPlaybackState(
    val hasNotificationAccess: Boolean = false,
    val hasSession: Boolean = false,
    val isPlaying: Boolean = false,
    val isAudioActiveFallback: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val packageName: String? = null,
    val playerAppName: String? = null,
    val albumArt: Bitmap? = null,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val playbackSpeed: Float = 1f,
    val lastPositionUpdateElapsedMs: Long = 0L,
    val canSkipNext: Boolean = true,
    val canSkipPrevious: Boolean = true
)

class TildeMediaListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        MediaSessionMonitor.notifyListenerChanged()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        MediaSessionMonitor.notifyListenerChanged()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        MediaSessionMonitor.notifyListenerChanged()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        MediaSessionMonitor.notifyListenerChanged()
    }
}

class MediaSessionMonitor(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val listenerComponent = ComponentName(context, TildeMediaListenerService::class.java)
    private val mediaSessionManager =
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val _mediaState = MutableStateFlow(MediaPlaybackState())
    val mediaState: StateFlow<MediaPlaybackState> = _mediaState.asStateFlow()

    private var activeController: MediaController? = null
    private var sessionsListenerRegistered = false

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            publishFromController(activeController)
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            publishFromController(activeController)
        }

        override fun onSessionDestroyed() {
            refresh()
        }
    }

    private val sessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            mainHandler.post {
                bindBestController(controllers.orEmpty())
            }
        }

    init {
        activeMonitorRef = WeakReference(this)
        refresh()
    }

    fun hasNotificationAccess(): Boolean {
        return runCatching {
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
        }.getOrDefault(false)
    }

    fun refresh() {
        val granted = hasNotificationAccess()
        val mgr = mediaSessionManager
        if (granted && mgr != null) {
            if (!sessionsListenerRegistered) {
                runCatching {
                    mgr.addOnActiveSessionsChangedListener(
                        sessionsChangedListener,
                        listenerComponent,
                        mainHandler
                    )
                    sessionsListenerRegistered = true
                }
            }
            val controllers = runCatching {
                mgr.getActiveSessions(listenerComponent)
            }.getOrDefault(emptyList())
            bindBestController(controllers)
        } else {
            detachController()
            val musicActive = runCatching { audioManager?.isMusicActive == true }.getOrDefault(false)
            _mediaState.value = MediaPlaybackState(
                hasNotificationAccess = granted,
                hasSession = false,
                isPlaying = musicActive,
                isAudioActiveFallback = musicActive
            )
        }
    }

    private fun bindBestController(controllers: List<MediaController>) {
        val chosen = controllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING ||
                it.playbackState?.state == PlaybackState.STATE_BUFFERING
        } ?: controllers.firstOrNull {
            val md = it.metadata
            !md?.getString(MediaMetadata.METADATA_KEY_TITLE).isNullOrBlank()
        } ?: controllers.firstOrNull()

        if (chosen?.sessionToken != activeController?.sessionToken) {
            detachController()
            activeController = chosen
            chosen?.registerCallback(controllerCallback, mainHandler)
        }
        publishFromController(chosen)
    }

    private fun detachController() {
        runCatching { activeController?.unregisterCallback(controllerCallback) }
        activeController = null
    }

    private fun publishFromController(controller: MediaController?) {
        val granted = hasNotificationAccess()
        if (controller == null) {
            val musicActive = runCatching { audioManager?.isMusicActive == true }.getOrDefault(false)
            _mediaState.value = MediaPlaybackState(
                hasNotificationAccess = granted,
                hasSession = false,
                isPlaying = musicActive,
                isAudioActiveFallback = musicActive
            )
            return
        }

        val metadata = controller.metadata
        val pb = controller.playbackState
        val stateCode = pb?.state ?: PlaybackState.STATE_NONE
        val isPlaying = stateCode == PlaybackState.STATE_PLAYING ||
            stateCode == PlaybackState.STATE_BUFFERING

        val title = metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: ""
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: ""
        val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION)
            ?: ""
        val art = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.coerceAtLeast(0L) ?: 0L

        val actions = pb?.actions ?: 0L
        val canNext = (actions == 0L) || ((actions and PlaybackState.ACTION_SKIP_TO_NEXT) != 0L)
        val canPrev = (actions == 0L) || ((actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0L)

        val pkg = controller.packageName
        val appLabel = pkg?.let { p ->
            runCatching {
                val ai = context.packageManager.getApplicationInfo(p, 0)
                context.packageManager.getApplicationLabel(ai).toString()
            }.getOrNull()
        }

        _mediaState.value = MediaPlaybackState(
            hasNotificationAccess = granted,
            hasSession = true,
            isPlaying = isPlaying,
            isAudioActiveFallback = false,
            title = title.ifBlank { appLabel ?: "Unknown track" },
            artist = artist,
            album = album,
            packageName = pkg,
            playerAppName = appLabel ?: pkg,
            albumArt = art,
            durationMs = duration,
            positionMs = pb?.position?.coerceAtLeast(0L) ?: 0L,
            playbackSpeed = pb?.playbackSpeed ?: 1f,
            lastPositionUpdateElapsedMs = pb?.lastPositionUpdateTime ?: SystemClock.elapsedRealtime(),
            canSkipNext = canNext,
            canSkipPrevious = canPrev
        )
    }

    fun togglePlayPause() {
        val controller = activeController
        if (controller != null) {
            val state = controller.playbackState?.state
            if (state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING) {
                controller.transportControls.pause()
            } else {
                controller.transportControls.play()
            }
        } else {
            dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            mainHandler.postDelayed({ refresh() }, 250L)
        }
    }

    fun skipToNext() {
        val controller = activeController
        if (controller != null) {
            controller.transportControls.skipToNext()
        } else {
            dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
        }
    }

    fun skipToPrevious() {
        val controller = activeController
        if (controller != null) {
            controller.transportControls.skipToPrevious()
        } else {
            dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        }
    }

    fun seekTo(positionMs: Long) {
        activeController?.transportControls?.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun openPlayerApp() {
        val controller = activeController
        val sessionIntent = controller?.sessionActivity
        if (sessionIntent != null) {
            val sent = runCatching {
                sessionIntent.send()
                true
            }.getOrDefault(false)
            if (sent) return
        }
        val pkg = controller?.packageName ?: return
        val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } ?: return
        runCatching { context.startActivity(launchIntent) }
    }

    fun openNotificationAccessSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    private fun dispatchMediaKey(keyCode: Int) {
        val am = audioManager ?: return
        val now = SystemClock.uptimeMillis()
        runCatching {
            am.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0))
            am.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0))
        }
    }

    fun release() {
        detachController()
        if (sessionsListenerRegistered) {
            runCatching {
                mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionsChangedListener)
            }
            sessionsListenerRegistered = false
        }
    }

    companion object {
        @Volatile
        private var activeMonitorRef: WeakReference<MediaSessionMonitor>? = null

        fun notifyListenerChanged() {
            activeMonitorRef?.get()?.let { monitor ->
                monitor.mainHandler.post { monitor.refresh() }
            }
        }
    }
}
