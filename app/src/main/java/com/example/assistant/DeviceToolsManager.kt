package com.example.assistant

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent

/**
 * Manages device integration points: App Launcher, Camera, Media Controls, Settings.
 */
class DeviceToolsManager(private val context: Context) {

    private val tag = "DeviceToolsManager"

    sealed class ToolExecutionResult {
        data class Success(val message: String, val actionTitle: String) : ToolExecutionResult()
        data class Error(val error: String) : ToolExecutionResult()
    }

    /**
     * Attempts to launch an installed application by common name or package pattern.
     */
    fun launchAppByName(appName: String): ToolExecutionResult {
        val cleanName = appName.lowercase().trim()
        val packageManager = context.packageManager

        // Known common packages for fast reliable launch
        val knownPackages = mapOf(
            "youtube" to listOf("com.google.android.youtube"),
            "camera" to listOf("com.google.android.GoogleCamera", "com.android.camera"),
            "whatsapp" to listOf("com.whatsapp"),
            "spotify" to listOf("com.spotify.music"),
            "chrome" to listOf("com.android.chrome"),
            "browser" to listOf("com.android.chrome"),
            "maps" to listOf("com.google.android.apps.maps"),
            "calculator" to listOf("com.google.android.calculator", "com.android.calculator2"),
            "settings" to listOf("com.android.settings"),
            "gallery" to listOf("com.google.android.apps.photos", "com.android.gallery3d"),
            "photos" to listOf("com.google.android.apps.photos")
        )

        val targetList = knownPackages[cleanName]
        if (targetList != null) {
            for (pkg in targetList) {
                val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return ToolExecutionResult.Success(
                        "Opening ${appName.replaceFirstChar { it.uppercase() }}",
                        "App Launch: $appName"
                    )
                }
            }
        }

        // Generic search through installed launchable apps
        try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolvedApps = packageManager.queryIntentActivities(mainIntent, 0)
            for (resolveInfo in resolvedApps) {
                val label = resolveInfo.loadLabel(packageManager).toString().lowercase()
                if (label.contains(cleanName) || cleanName.contains(label)) {
                    val launchIntent = packageManager.getLaunchIntentForPackage(resolveInfo.activityInfo.packageName)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        return ToolExecutionResult.Success(
                            "Opening ${resolveInfo.loadLabel(packageManager)}",
                            "App Launch: ${resolveInfo.loadLabel(packageManager)}"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed querying installed apps: ${e.message}")
        }

        // Fallback: If not installed, offer Web search or Play Store query
        return ToolExecutionResult.Error("Could not find \"$appName\" on this device. Would you like me to search online?")
    }

    /**
     * Direct launch of Camera capture intent.
     */
    fun openCamera(): ToolExecutionResult {
        return try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolExecutionResult.Success("Opening Camera", "Camera Capture")
        } catch (e: Exception) {
            // Fallback to camera intent category
            try {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                ToolExecutionResult.Success("Opening Camera", "Camera")
            } catch (ex: Exception) {
                ToolExecutionResult.Error("Failed to open camera: ${e.message}")
            }
        }
    }

    /**
     * Media controls: Play/Pause/Next/Previous via media key events.
     */
    fun controlMedia(command: String): ToolExecutionResult {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val keyCode = when (command.lowercase().trim()) {
                "play", "resume" -> KeyEvent.KEYCODE_MEDIA_PLAY
                "pause", "stop" -> KeyEvent.KEYCODE_MEDIA_PAUSE
                "toggle" -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                "next", "skip" -> KeyEvent.KEYCODE_MEDIA_NEXT
                "previous", "back" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
                else -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            }

            audioManager?.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            audioManager?.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))

            ToolExecutionResult.Success("Media command executed: $command", "Media Control: $command")
        } catch (e: Exception) {
            ToolExecutionResult.Error("Unable to control media: ${e.message}")
        }
    }

    /**
     * Web search query.
     */
    fun searchWeb(query: String): ToolExecutionResult {
        return try {
            val uri = Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolExecutionResult.Success("Searching web for \"$query\"", "Web Search: $query")
        } catch (e: Exception) {
            ToolExecutionResult.Error("Failed to open web search: ${e.message}")
        }
    }

    /**
     * Open device settings page (e.g. Wi-Fi, Bluetooth, Battery, Sounds).
     */
    fun openSettings(section: String): ToolExecutionResult {
        return try {
            val action = when (section.lowercase().trim()) {
                "wifi", "internet" -> Settings.ACTION_WIFI_SETTINGS
                "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
                "battery" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
                "sound", "volume" -> Settings.ACTION_SOUND_SETTINGS
                "display" -> Settings.ACTION_DISPLAY_SETTINGS
                "apps" -> Settings.ACTION_APPLICATION_SETTINGS
                else -> Settings.ACTION_SETTINGS
            }
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolExecutionResult.Success("Opening $section settings", "Device Settings: $section")
        } catch (e: Exception) {
            ToolExecutionResult.Error("Failed to open settings: ${e.message}")
        }
    }

    /**
     * Sets an alarm or timer.
     */
    fun setAlarm(minutesFromNow: Int, message: String): ToolExecutionResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, minutesFromNow * 60)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolExecutionResult.Success("Timer set for $minutesFromNow minutes", "Timer: $minutesFromNow mins")
        } catch (e: Exception) {
            ToolExecutionResult.Error("Failed to set timer: ${e.message}")
        }
    }
}
