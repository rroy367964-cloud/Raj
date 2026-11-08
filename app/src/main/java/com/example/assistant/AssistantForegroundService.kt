package com.example.assistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class AssistantForegroundService : Service() {

    private val tag = "AssistantFGService"
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var wakeWordEngine: WakeWordEngine? = null

    companion object {
        const val CHANNEL_ID = "mahi_voice_foreground_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.assistant.action.START"
        const val ACTION_STOP = "com.example.assistant.action.STOP"
        const val ACTION_TRIGGER_VOICE = "com.example.assistant.action.TRIGGER_VOICE"

        fun start(context: Context) {
            val intent = Intent(context, AssistantForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AssistantForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(tag, "AssistantForegroundService onCreate")
        createNotificationChannel()

        wakeWordEngine = AcousticWakeWordEngine(applicationContext, serviceScope)

        // Observe amplitude to propagate to state holder
        wakeWordEngine?.audioAmplitude?.onEach { amp ->
            AssistantStateHolder.setLiveAmplitude(amp)
        }?.launchIn(serviceScope)

        // Observe assistant state changes to update notification title/text
        AssistantStateHolder.assistantState.onEach { state ->
            updateNotificationForState(state)
        }.launchIn(serviceScope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        Log.i(tag, "onStartCommand with action: $action")

        when (action) {
            ACTION_STOP -> {
                shutdownService()
                return START_NOT_STICKY
            }
            ACTION_TRIGGER_VOICE -> {
                wakeWordEngine?.triggerManualWake()
            }
            ACTION_START -> {
                startForegroundWithNotification()
                AssistantStateHolder.setServiceActive(true)
                startWakeWordDetection()
            }
        }

        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification("Mahi is listening…", "Say \"Hey Mahi\" or tap to talk")
        val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else {
            0
        }

        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, foregroundServiceType)
        } catch (e: Exception) {
            Log.e(tag, "Failed to start foreground service: ${e.message}", e)
        }
    }

    private fun startWakeWordDetection() {
        wakeWordEngine?.startListening {
            Log.i(tag, "Wake-word triggered from background service!")
            AssistantStateHolder.setAssistantState(WakeWordState.WAKE_WORD_DETECTED)
            AssistantStateHolder.emitEvent(AssistantUiEvent.RequestStartQueryListening)
        }
    }

    private fun updateNotificationForState(state: WakeWordState) {
        if (!AssistantStateHolder.isServiceActive.value) return

        val (title, content) = when (state) {
            WakeWordState.IDLE -> "Mahi AI Standby" to "Tap or start listening"
            WakeWordState.LISTENING_FOR_WAKE_WORD -> "Mahi is listening…" to "Listening for \"Hey Mahi\""
            WakeWordState.WAKE_WORD_DETECTED -> "Hey Mahi detected!" to "Listening to your request…"
            WakeWordState.LISTENING_FOR_QUERY -> "Mahi is listening to you…" to "Speak now"
            WakeWordState.PROCESSING_RESPONSE -> "Mahi is thinking…" to "Analyzing request"
            WakeWordState.SPEAKING -> "Mahi is speaking…" to AssistantStateHolder.lastAssistantResponse.value
            WakeWordState.INITIALIZING -> "Mahi starting up…" to "Configuring audio engine"
            WakeWordState.ERROR -> "Mahi status" to "Microphone paused"
        }

        val notification = buildNotification(title, content)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(title: String, contentText: String): Notification {
        // Tap action -> Open MainActivity
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Stop Action -> Stops foreground service
        val stopIntent = Intent(this, AssistantForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Speak Now action
        val speakIntent = Intent(this, AssistantForegroundService::class.java).apply {
            action = ACTION_TRIGGER_VOICE
        }
        val speakPendingIntent = PendingIntent.getService(
            this,
            2,
            speakIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
            .addAction(android.R.drawable.ic_btn_speak_now, "Speak", speakPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_description)
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun shutdownService() {
        wakeWordEngine?.stopListening()
        wakeWordEngine?.release()
        wakeWordEngine = null

        AssistantStateHolder.setServiceActive(false)
        AssistantStateHolder.setAssistantState(WakeWordState.IDLE)
        AssistantStateHolder.setLiveAmplitude(0f)

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(tag, "AssistantForegroundService onDestroy")
        shutdownService()
        serviceScope.cancel()
    }
}
