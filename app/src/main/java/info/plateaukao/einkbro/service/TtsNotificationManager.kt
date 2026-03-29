package info.plateaukao.einkbro.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.media.app.NotificationCompat.MediaStyle
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.viewmodel.TtsReadingState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class TtsNotificationManager(private val context: Context) {

    private val notificationManager = NotificationManagerCompat.from(context)

    private val _actionFlow = MutableSharedFlow<TtsNotificationAction>(extraBufferCapacity = 1)
    val actionFlow: SharedFlow<TtsNotificationAction> = _actionFlow

    private val mediaSession = MediaSessionCompat(context, "EinkBroTts").apply {
        setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                _actionFlow.tryEmit(TtsNotificationAction.PLAY_PAUSE)
            }

            override fun onPause() {
                _actionFlow.tryEmit(TtsNotificationAction.PLAY_PAUSE)
            }

            override fun onStop() {
                _actionFlow.tryEmit(TtsNotificationAction.CLOSE)
            }
        })
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Text to Speech",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "TTS playback controls"
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun updateNotification(
        title: String,
        readingState: TtsReadingState,
        progress: String,
        currentText: String = "",
    ) {
        if (readingState == TtsReadingState.IDLE) {
            dismissNotification()
            return
        }

        val displayTitle = title.ifEmpty { "EinkBro TTS" }
        updateMediaSession(displayTitle, readingState, progress)

        val statusText = when (readingState) {
            TtsReadingState.PREPARING -> "Preparing\u2026"
            TtsReadingState.PLAYING -> "Playing $progress"
            TtsReadingState.PAUSED -> "Paused $progress"
            TtsReadingState.IDLE -> ""
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID).apply {
            setSmallIcon(R.drawable.ic_tts)
            setContentTitle(displayTitle)
            setContentText(statusText)
            setOngoing(readingState != TtsReadingState.PAUSED)
            setAutoCancel(false)
            setSilent(true)
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

            // Action index 0: Play/Pause
            if (readingState == TtsReadingState.PLAYING) {
                addAction(
                    android.R.drawable.ic_media_pause,
                    "Pause",
                    createActionIntent(ACTION_PLAY_PAUSE)
                )
            } else if (readingState == TtsReadingState.PAUSED) {
                addAction(
                    android.R.drawable.ic_media_play,
                    "Play",
                    createActionIntent(ACTION_PLAY_PAUSE)
                )
            }

            // Action index 1: Stop
            addAction(
                R.drawable.ic_stop,
                "Stop",
                createActionIntent(ACTION_STOP)
            )

            // Action index 2: Close
            addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Close",
                createActionIntent(ACTION_CLOSE)
            )

            // MediaStyle — shows in system media controls panel
            setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
        }

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED ||
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        ) {
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        }
    }

    private fun updateMediaSession(
        title: String,
        readingState: TtsReadingState,
        progress: String,
    ) {
        val state = when (readingState) {
            TtsReadingState.PLAYING -> PlaybackStateCompat.STATE_PLAYING
            TtsReadingState.PAUSED -> PlaybackStateCompat.STATE_PAUSED
            TtsReadingState.PREPARING -> PlaybackStateCompat.STATE_BUFFERING
            TtsReadingState.IDLE -> PlaybackStateCompat.STATE_STOPPED
        }

        val actions = PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_STOP or
                PlaybackStateCompat.ACTION_PLAY_PAUSE

        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1f)
                .setActions(actions)
                .build()
        )

        mediaSession.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, progress)
                .build()
        )

        mediaSession.isActive = true
    }

    fun dismissNotification() {
        mediaSession.isActive = false
        notificationManager.cancel(NOTIFICATION_ID)
    }

    fun release() {
        dismissNotification()
        mediaSession.release()
    }

    fun handleAction(action: String?) {
        val ttsAction = when (action) {
            ACTION_PLAY_PAUSE -> TtsNotificationAction.PLAY_PAUSE
            ACTION_STOP -> TtsNotificationAction.STOP
            ACTION_CLOSE -> TtsNotificationAction.CLOSE
            else -> return
        }
        _actionFlow.tryEmit(ttsAction)
    }

    private fun createActionIntent(action: String): PendingIntent {
        val intent = Intent(action).apply {
            setClass(context, TtsNotificationReceiver::class.java)
        }
        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val CHANNEL_ID = "TTS_PLAYBACK"
        const val NOTIFICATION_ID = 2
        const val ACTION_PLAY_PAUSE = "info.plateaukao.einkbro.TTS_PLAY_PAUSE"
        const val ACTION_STOP = "info.plateaukao.einkbro.TTS_STOP"
        const val ACTION_CLOSE = "info.plateaukao.einkbro.TTS_CLOSE"
    }
}

enum class TtsNotificationAction {
    PLAY_PAUSE, STOP, CLOSE
}
