package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("packzy_node_prefs", Context.MODE_PRIVATE)

    // Baseline settings
    private val _baseUrl = MutableStateFlow(sharedPrefs.getString("base_url", "https://admin.packzy.com/admin/login") ?: "https://admin.packzy.com/admin/login")
    val baseUrl: StateFlow<String> = _baseUrl.asStateFlow()

    private val _isAutoOpenMode = MutableStateFlow(sharedPrefs.getBoolean("auto_open_mode", true))
    val isAutoOpenMode: StateFlow<Boolean> = _isAutoOpenMode.asStateFlow()

    private val _isHapticEnabled = MutableStateFlow(sharedPrefs.getBoolean("haptic_enabled", true))
    val isHapticEnabled: StateFlow<Boolean> = _isHapticEnabled.asStateFlow()

    private val _isSoundEnabled = MutableStateFlow(sharedPrefs.getBoolean("sound_enabled", true))
    val isSoundEnabled: StateFlow<Boolean> = _isSoundEnabled.asStateFlow()

    // Temporary/Scan States
    private val _scannedId = MutableStateFlow("")
    val scannedId: StateFlow<String> = _scannedId.asStateFlow()

    private val _isEditingBaseUrl = MutableStateFlow(false)
    val isEditingBaseUrl: StateFlow<Boolean> = _isEditingBaseUrl.asStateFlow()

    private val _torchState = MutableStateFlow(false)
    val torchState: StateFlow<Boolean> = _torchState.asStateFlow()

    private val _isManualInputExpanded = MutableStateFlow(false)
    val isManualInputExpanded: StateFlow<Boolean> = _isManualInputExpanded.asStateFlow()

    private val _manualInputText = MutableStateFlow("")
    val manualInputText: StateFlow<String> = _manualInputText.asStateFlow()

    private val _isCameraActive = MutableStateFlow(true)
    val isCameraActive: StateFlow<Boolean> = _isCameraActive.asStateFlow()

    private val _lastScannedTimestamp = MutableStateFlow(0L)
    val lastScannedTimestamp: StateFlow<Long> = _lastScannedTimestamp.asStateFlow()

    // Base URL Input Temp Field
    private val _baseUrlInput = MutableStateFlow(_baseUrl.value)
    val baseUrlInput: StateFlow<String> = _baseUrlInput.asStateFlow()

    // Debounce timing: 1.5 seconds (1500 ms)
    private val scanDebounceMs = 1500L

    // Tone builder for audio synth Beep
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        toneGenerator?.release()
    }

    fun setAutoOpenMode(enabled: Boolean) {
        _isAutoOpenMode.value = enabled
        sharedPrefs.edit().putBoolean("auto_open_mode", enabled).apply()
    }

    fun setHapticEnabled(enabled: Boolean) {
        _isHapticEnabled.value = enabled
        sharedPrefs.edit().putBoolean("haptic_enabled", enabled).apply()
    }

    fun setSoundEnabled(enabled: Boolean) {
        _isSoundEnabled.value = enabled
        sharedPrefs.edit().putBoolean("sound_enabled", enabled).apply()
    }

    fun setTorchState(enabled: Boolean) {
        _torchState.value = enabled
    }

    fun setManualInputExpanded(expanded: Boolean) {
        _isManualInputExpanded.value = expanded
        // Pause camera if manual is expanded to avoid background resource drain/user distraction
        _isCameraActive.value = !expanded
    }

    fun setManualInputText(text: String) {
        _manualInputText.value = text
    }

    fun setBaseUrlInput(text: String) {
        _baseUrlInput.value = text
    }

    fun startEditingBaseUrl() {
        _baseUrlInput.value = _baseUrl.value
        _isEditingBaseUrl.value = true
    }

    fun saveBaseUrl() {
        var url = _baseUrlInput.value.trim()
        if (url.isNotEmpty() && !url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        if (url.isEmpty()) {
            url = "https://admin.packzy.com/admin/login"
        }
        _baseUrl.value = url
        _isEditingBaseUrl.value = false
        sharedPrefs.edit().putString("base_url", url).apply()
    }

    fun cancelEditingBaseUrl() {
        _isEditingBaseUrl.value = false
    }

    fun resetBaseUrl() {
        val defaultUrl = "https://admin.packzy.com/admin/login"
        _baseUrl.value = defaultUrl
        _baseUrlInput.value = defaultUrl
        _isEditingBaseUrl.value = false
        sharedPrefs.edit().putString("base_url", defaultUrl).apply()
    }

    /**
     * Compile current URL target from the Scanned ID
     */
    fun compileTargetUrl(id: String): String {
        if (id.isEmpty()) return ""
        // If it looks like a full URL already, return it directly
        if (id.startsWith("http://") || id.startsWith("https://")) {
            return id
        }
        val base = _baseUrl.value
        // If base is default Packzy login URL, replace with direct single consignment detail URL
        if (base == "https://admin.packzy.com/admin/login") {
            return "https://admin.packzy.com/admin/consignment/single/$id"
        }
        return when {
            base.contains("{id}") -> {
                base.replace("{id}", id)
            }
            base.contains("?") -> {
                "$base&id=$id"
            }
            base.endsWith("/") -> {
                "$base$id"
            }
            else -> {
                "$base?id=$id"
            }
        }
    }

    /**
     * Process a scanned or entered raw target string
     */
    fun handleCodeScanned(rawCode: String, context: Context): Boolean {
        val trimmedCode = rawCode.trim()
        if (trimmedCode.isEmpty()) return false

        val currentTime = System.currentTimeMillis()
        if (currentTime - _lastScannedTimestamp.value < scanDebounceMs) {
            // Drop scan to respect stable lock/debouncing
            return false
        }

        _lastScannedTimestamp.value = currentTime
        _scannedId.value = trimmedCode

        // Multi-sensory feedbacks
        triggerFeedback(context)

        // Actions depending on mode
        if (_isAutoOpenMode.value) {
            val destination = compileTargetUrl(trimmedCode)
            launchBrowser(destination, context)
        }
        return true
    }

    /**
     * Launch browser with the specified final target URL
     */
    fun launchBrowser(url: String, context: Context) {
        if (url.isEmpty()) return
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Play custom sound beep + vibrational haptic punch
     */
    private fun triggerFeedback(context: Context) {
        // Haptic Vibration if enabled
        if (_isHapticEnabled.value) {
            try {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        // Quick elegant double-click pattern for high feedback feel
                        val timings = longArrayOf(0, 50, 60, 50)
                        val amplitudes = intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE)
                        vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(100)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Sound cue chime if enabled
        if (_isSoundEnabled.value) {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
