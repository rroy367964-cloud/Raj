package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private val tag = "MayaMainActivity"

    // Views (Pure Speak-to-Speak without text chat input)
    private lateinit var txtStatus: TextView
    private lateinit var btnMic: ImageView
    private lateinit var tvMicLabel: TextView
    private lateinit var tvVoiceStatusPill: TextView
    private lateinit var tvEnergyBadge: TextView
    private lateinit var tvSpokenDialogue: TextView

    // Interactive action & widget views
    private lateinit var btnGaane: TextView
    private lateinit var btnPadhai: TextView
    private lateinit var btnJournal: TextView
    private lateinit var ivAvatar: ImageView
    private lateinit var btnMinimize: TextView
    private lateinit var cardMausam: android.view.View
    private lateinit var cardAaj: android.view.View
    private lateinit var cardMood: android.view.View

    // Speech & Audio
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isTtsReady = false
    private var isListening = false
    private var isSpeaking = false
    private var isSpeakToSpeakModeActive = false
    private val recordAudioPermissionCode = 1
    private var energyCount = 21

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        initTextToSpeech()
        initSpeechRecognizer()
        setupListeners()
        updateLiveWidgets()
    }

    private fun initViews() {
        txtStatus = findViewById(R.id.txtStatus)
        btnMic = findViewById(R.id.btnMic)
        tvMicLabel = findViewById(R.id.tv_mic_label)
        tvVoiceStatusPill = findViewById(R.id.tv_voice_status_pill)
        tvEnergyBadge = findViewById(R.id.tv_energy_badge)
        tvSpokenDialogue = findViewById(R.id.tv_spoken_dialogue)

        btnGaane = findViewById(R.id.btn_action_gaane)
        btnPadhai = findViewById(R.id.btn_action_padhai)
        btnJournal = findViewById(R.id.btn_action_journal)
        ivAvatar = findViewById(R.id.iv_header_avatar)
        btnMinimize = findViewById(R.id.btn_minimize)

        cardMausam = findViewById(R.id.card_mausam)
        cardAaj = findViewById(R.id.card_aaj)
        cardMood = findViewById(R.id.card_mood)
    }

    private fun initTextToSpeech() {
        try {
            tts = TextToSpeech(this, this)
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize TextToSpeech: ${e.message}", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS && tts != null) {
            val ttsEngine = tts ?: return

            // Configure Hindi locale with fallback cascade: Hindi -> System default -> US English
            var langResult = ttsEngine.setLanguage(Locale("hi", "IN"))
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                langResult = ttsEngine.setLanguage(Locale.getDefault())
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    ttsEngine.setLanguage(Locale.US)
                }
            }

            ttsEngine.setPitch(1.06f) // Sweet, youthful natural pitch for Maya
            ttsEngine.setSpeechRate(1.0f)

            ttsEngine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isSpeaking = true
                    mainHandler.post {
                        if (!isFinishing && !isDestroyed) {
                            txtStatus.text = "Maya bol rahi hai..."
                            tvVoiceStatusPill.text = "🔊 Maya is speaking..."
                            updateMicButtonUi(isBusy = true)
                        }
                    }
                }

                override fun onDone(utteranceId: String?) {
                    isSpeaking = false
                    mainHandler.post {
                        if (isFinishing || isDestroyed) return@post

                        txtStatus.text = "Sun rahi hai..."
                        tvVoiceStatusPill.text = "Speak-to-Speak Active"

                        // Speak-to-Speak continuity: Once Maya finishes speaking, seamlessly listen for next user voice
                        if (isSpeakToSpeakModeActive) {
                            mainHandler.postDelayed({
                                if (isSpeakToSpeakModeActive && !isSpeaking && !isFinishing && !isDestroyed) {
                                    startListening()
                                }
                            }, 500)
                        } else {
                            updateMicButtonUi(isBusy = false)
                        }
                    }
                }

                override fun onError(utteranceId: String?) {
                    isSpeaking = false
                    mainHandler.post {
                        if (!isFinishing && !isDestroyed) {
                            txtStatus.text = "Sun rahi hai..."
                            updateMicButtonUi(isBusy = false)
                        }
                    }
                }
            })
            isTtsReady = true
        } else {
            Log.e(tag, "TextToSpeech init returned error status: $status")
            isTtsReady = false
        }
    }

    private fun initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.w(tag, "Speech recognition not natively available on this device")
            return
        }

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            setupSpeechRecognizer()
        } catch (e: Exception) {
            Log.e(tag, "Error creating SpeechRecognizer: ${e.message}", e)
        }
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                txtStatus.text = "Sun rahi hoon, boliye..."
                tvVoiceStatusPill.text = "🎙️ Listening to you (Speak in Hindi/English)..."
                updateMicButtonUi(isBusy = true)
            }

            override fun onBeginningOfSpeech() {
                txtStatus.text = "Awaaz aa rahi hai..."
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListening = false
                txtStatus.text = "Soch rahi hoon..."
                tvVoiceStatusPill.text = "✨ Thinking..."
            }

            override fun onError(error: Int) {
                isListening = false
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "Kuch sunai nahi diya, dobara boliye dost!"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Timeout. Kripya mic dabayein."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                    else -> "Sun nahi payi, dobara boliye."
                }
                txtStatus.text = errorMsg
                if (isSpeakToSpeakModeActive && error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    isSpeakToSpeakModeActive = false
                }
                updateMicButtonUi(isBusy = false)
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val spokenText = matches[0]
                    tvSpokenDialogue.text = "Aap: \"$spokenText\""
                    getResponseFromMaya(spokenText)
                } else {
                    updateMicButtonUi(isBusy = false)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    tvSpokenDialogue.text = "Sun rahi hoon: \"${matches[0]}\"..."
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun setupListeners() {
        // Center Mic / "Rok do" Button (Pure Speak-to-Speak trigger)
        btnMic.setOnClickListener {
            if (isListening || isSpeaking || isSpeakToSpeakModeActive) {
                // STOP ACTION ("Rok do")
                stopAllVoice()
            } else {
                // START SPEAK-TO-SPEAK
                isSpeakToSpeakModeActive = true
                checkAndRequestMicPermission()
            }
        }

        // Action Buttons Row: "🎵 Gaane", "📖 Padhai", "🖊️ Journal"
        btnGaane.setOnClickListener {
            consumeEnergy()
            val reply = "Chalo ek mast gaana sunte hain dost! Lo-fi beats ya romantic Bollywood?"
            txtStatus.text = reply
            tvSpokenDialogue.text = "Maya: $reply"
            speakOut(reply)
        }

        btnPadhai.setOnClickListener {
            consumeEnergy()
            val reply = "Padhai ka samay! 25 minute bilkul focus karte hain, main aapke saath baithi hoon."
            txtStatus.text = reply
            tvSpokenDialogue.text = "Maya: $reply"
            speakOut(reply)
        }

        btnJournal.setOnClickListener {
            consumeEnergy()
            val reply = "Aaj aapka din kaisa beeta? Ek pyari si baat bolkar share kijiye."
            txtStatus.text = reply
            tvSpokenDialogue.text = "Maya: $reply"
            speakOut(reply)
        }

        // Avatar click -> Energy Boost
        ivAvatar.setOnClickListener {
            energyCount += 5
            tvEnergyBadge.text = "⚡ $energyCount Energy"
            val boostMsg = "Energy boost mil gaya! ⚡ Abhi $energyCount Energy hai."
            txtStatus.text = boostMsg
            tvSpokenDialogue.text = "Maya: $boostMsg"
            speakOut(boostMsg)
        }

        // Minimize Button
        btnMinimize.setOnClickListener {
            moveTaskToBack(true)
        }

        // Menu & Bell clicks
        findViewById<android.view.View?>(R.id.btn_menu)?.setOnClickListener {
            Toast.makeText(this, "Maya Speak-to-Speak Menu", Toast.LENGTH_SHORT).show()
        }

        findViewById<android.view.View?>(R.id.btn_bell)?.setOnClickListener {
            Toast.makeText(this, "No new notifications", Toast.LENGTH_SHORT).show()
        }

        // Widget Cards Click: Mausam, Aaj, Mood
        cardMausam.setOnClickListener {
            val weatherReply = "Aaj ka mausam bilkul saaf aur suhana hai, taapmaan 28 degree Celsius hai dost!"
            txtStatus.text = weatherReply
            tvSpokenDialogue.text = "Maya: $weatherReply"
            speakOut(weatherReply)
        }

        cardAaj.setOnClickListener {
            val todayStr = SimpleDateFormat("EEEE, d MMMM", Locale("hi", "IN")).format(Date())
            val dateReply = "Aaj $todayStr hai. Chaliye aaj ka din shandaar banate hain!"
            txtStatus.text = dateReply
            tvSpokenDialogue.text = "Maya: $dateReply"
            speakOut(dateReply)
        }

        cardMood.setOnClickListener {
            val moodReply = "Mood ekdum mast aur pyaar bhara hai! Sab badhiya chal raha hai dost."
            txtStatus.text = moodReply
            tvSpokenDialogue.text = "Maya: $moodReply"
            speakOut(moodReply)
        }

        // Bottom Navigation Bar items
        findViewById<android.view.View>(R.id.nav_item_home).setOnClickListener {
            stopAllVoice()
            txtStatus.text = "Sun rahi hai..."
            Toast.makeText(this, "Home screen", Toast.LENGTH_SHORT).show()
        }

        findViewById<android.view.View>(R.id.nav_item_scan).setOnClickListener {
            val reply = "Scan mode ready! Kisi bhi text ya QR ko camera se scan kar sakte hain."
            txtStatus.text = reply
            tvSpokenDialogue.text = "Maya: $reply"
            speakOut(reply)
        }

        findViewById<android.view.View>(R.id.nav_item_yaadein).setOnClickListener {
            val reply = "Yaadein: Mujhe aapki har pasand yaad hai, jaise der raat baatein aur chai!"
            txtStatus.text = reply
            tvSpokenDialogue.text = "Maya: $reply"
            speakOut(reply)
        }

        findViewById<android.view.View>(R.id.nav_item_chat)?.setOnClickListener {
            // In Speak-to-Speak mode, tapping Chat immediately triggers voice conversation!
            isSpeakToSpeakModeActive = true
            checkAndRequestMicPermission()
        }
    }

    private fun checkAndRequestMicPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                recordAudioPermissionCode
            )
        } else {
            startListening()
        }
    }

    private fun startListening() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { startListening() }
            return
        }

        if (isFinishing || isDestroyed) return

        // Validate permission before calling
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            checkAndRequestMicPermission()
            return
        }

        tts?.stop()
        isSpeaking = false

        if (speechRecognizer == null) {
            initSpeechRecognizer()
        }

        try {
            speechRecognizer?.cancel()

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Maya sun rahi hai, boliye...")
            }
            speechRecognizer?.startListening(intent)
            txtStatus.text = "Sun rahi hoon..."
            tvVoiceStatusPill.text = "🎙️ Listening to you..."
            updateMicButtonUi(isBusy = true)
        } catch (e: Exception) {
            Log.e(tag, "startListening failed: ${e.message}", e)
            txtStatus.text = "Mic error aaya hai."
            updateMicButtonUi(isBusy = false)
        }
    }

    private fun stopAllVoice() {
        isSpeakToSpeakModeActive = false
        mainHandler.removeCallbacksAndMessages(null)

        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
        } catch (ignored: Exception) {}

        try {
            tts?.stop()
        } catch (ignored: Exception) {}

        isListening = false
        isSpeaking = false
        txtStatus.text = "Sun rahi hai..."
        tvVoiceStatusPill.text = "Speak-to-Speak Active (Tap Mic)"
        updateMicButtonUi(isBusy = false)
    }

    private fun updateMicButtonUi(isBusy: Boolean) {
        if (isBusy) {
            btnMic.setImageResource(R.drawable.ic_stop)
            tvMicLabel.text = "Rok do"
        } else {
            btnMic.setImageResource(R.drawable.ic_nav_mic)
            tvMicLabel.text = "Mic"
        }
    }

    private fun getResponseFromMaya(query: String) {
        consumeEnergy()
        val lower = query.lowercase().trim()

        val reply = when {
            lower.contains("kaise ho") || lower.contains("kaisi ho") || lower.contains("how are you") ->
                "Main bilkul mast hoon dost! Aapke aate hi meri screen chamak uthi. Aap batayein, aaj din kaisa raha?"
            lower.contains("namaste") || lower.contains("hello") || lower.contains("hi") || lower.contains("hey") ->
                "Namaste dost! Main Maya hoon. Boliye, aaj main aapke liye kya kar sakti hoon?"
            lower.contains("naam") || lower.contains("who are you") || lower.contains("tum kaun ho") ->
                "Mera naam Maya hai, aapki apni AI companion! Hum dosti, padhai, gaane, sab kuch aamne-saamne aawaz mein share kar sakte hain."
            lower.contains("gaana") || lower.contains("song") || lower.contains("music") ->
                "Kya baat hai! Ek pyara sa gaana ho jaye: 'Tere jaisa yaar kahan, kahan aisa yaarana!' Chaliye mast music shuru karte hain!"
            lower.contains("padhai") || lower.contains("study") || lower.contains("exam") ->
                "Chaliye kitab kholiye dost! Main time track karungi. 25 minute full focus, fir 5 minute break!"
            lower.contains("joke") || lower.contains("chutkula") || lower.contains("hasi") ->
                "Sunिए ek chutkula: Ek baar battery ne phone se kaha, 'Agar main na hoti to tu kahan hota?' Phone bola: 'Showpiece mein!' Haha!"
            lower.contains("weather") || lower.contains("mausam") ->
                "Aaj ka mausam 28 degree Celsius aur ekdum clear hai dost!"
            lower.contains("shukriya") || lower.contains("thank") || lower.contains("dhanyawad") ->
                "Dosti mein no thanks! Main hamesha aapke saath hoon dost."
            else ->
                "Haan dost, maine suna: \"$query\"! Yeh toh sach mein bahut dilchasp baat hai."
        }

        txtStatus.text = reply
        tvSpokenDialogue.text = "Maya: $reply"
        speakOut(reply)
    }

    private fun speakOut(text: String) {
        if (isTtsReady && tts != null) {
            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            }
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "maya_speech_${System.currentTimeMillis()}")
        } else {
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
        }
    }

    private fun consumeEnergy() {
        if (energyCount > 0) {
            energyCount--
            tvEnergyBadge.text = "⚡ $energyCount Energy"
        }
    }

    private fun updateLiveWidgets() {
        try {
            val dayFormat = SimpleDateFormat("d", Locale.getDefault())
            val monthDayFormat = SimpleDateFormat("EEE, MMM", Locale.getDefault())
            val now = Date()

            findViewById<TextView?>(R.id.tv_widget_date_num)?.text = dayFormat.format(now)
            findViewById<TextView?>(R.id.tv_widget_date_str)?.text = monthDayFormat.format(now)
        } catch (e: Exception) {
            Log.e(tag, "Error updating widgets: ${e.message}")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == recordAudioPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted!", Toast.LENGTH_SHORT).show()
                startListening()
            } else {
                Toast.makeText(this, "Microphone permission chahiye aapse baat karne ke liye!", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)

        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (e: Exception) {
            Log.e(tag, "Error shutting down TTS: ${e.message}", e)
        }

        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e(tag, "Error destroying SpeechRecognizer: ${e.message}", e)
        }

        super.onDestroy()
    }
}
